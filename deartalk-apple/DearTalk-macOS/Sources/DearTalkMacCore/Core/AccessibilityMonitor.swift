import Foundation
import AppKit
import ApplicationServices

public struct CursorInfo: Equatable {
    public let screenPoint: CGPoint
    public let elementRect: CGRect
    public let selectedText: String
    public let fullText: String
}

/// macOS Accessibility API (AXUIElement) real-time text & cursor tracking monitor
public final class AccessibilityMonitor: ObservableObject {
    public static let shared = AccessibilityMonitor()

    @Published public var isMonitoring: Bool = false
    @Published public var hasAccessibilityPermission: Bool = false
    @Published public var currentText: String = ""
    @Published public var cursorInfo: CursorInfo?
    @Published public var latestDiffResult: DiffResult?
    public var lastFocusedElement: AXUIElement?

    private var debounceTimer: Timer?
    private var pollingTimer: Timer?
    private var permissionRetryTimer: Timer?
    private let intentEngine = DearTalkIntentEngine.shared
    private let toneManager = CustomToneManager.shared

    public init() {
        checkPermission()
    }

    public func checkPermission() {
        let options: NSDictionary = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: false]
        let trusted = AXIsProcessTrustedWithOptions(options)
        hasAccessibilityPermission = trusted
    }

    /// Clears stale TCC permission cache from previous builds to prevent toggle confusion
    public func resetStaleTccPermission() {
        let task = Process()
        task.executableURL = URL(fileURLWithPath: "/usr/bin/tccutil")
        task.arguments = ["reset", "Accessibility", "ai.deartalk.mac"]
        try? task.run()
        task.waitUntilExit()
        DearTalkLogger.info("🧹 Stale accessibility TCC cache successfully reset", category: "Accessibility")
    }

    public func requestPermission() {
        // 1. Reset stale ghost cache
        resetStaleTccPermission()

        // 2. Prompt for permission and open Privacy & Security settings
        let options: NSDictionary = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true]
        let trusted = AXIsProcessTrustedWithOptions(options)
        hasAccessibilityPermission = trusted

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            if let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility") {
                NSWorkspace.shared.open(url)
            }
        }
        startMonitoring()
    }

    public func startMonitoring() {
        checkPermission()
        startActivePolling()

        if !hasAccessibilityPermission {
            DearTalkLogger.warning("Waiting for Accessibility permission... Background polling active", category: "Accessibility")
        }
    }

    private func startActivePolling() {
        isMonitoring = true
        DearTalkLogger.info("🟢 Real-time accessibility text monitoring started (Interval: 150ms)", category: "Accessibility")

        // Poll focused element changes across active applications every 150ms
        pollingTimer?.invalidate()
        pollingTimer = Timer.scheduledTimer(withTimeInterval: 0.15, repeats: true) { [weak self] _ in
            self?.pollActiveFocusedElement()
        }
    }

    public func stopMonitoring() {
        isMonitoring = false
        pollingTimer?.invalidate()
        pollingTimer = nil
        debounceTimer?.invalidate()
        debounceTimer = nil
        permissionRetryTimer?.invalidate()
        permissionRetryTimer = nil
        DearTalkLogger.info("🔴 Real-time accessibility text monitoring stopped", category: "Accessibility")
    }

    private func pollActiveFocusedElement() {
        guard let frontApp = NSWorkspace.shared.frontmostApplication else { return }

        // DearTalk 자체 창에 포커스가 있을 때는 모니터링 건너뛰기
        if frontApp.bundleIdentifier == "ai.deartalk.mac" || frontApp.processIdentifier == ProcessInfo.processInfo.processIdentifier {
            return
        }

        let appElement = AXUIElementCreateApplication(frontApp.processIdentifier)

        // 1. Electron/Chromium/WebKit 접근성 트리 강제 활성화 (조회 전에 먼저 깨워야 함)
        AXUIElementSetAttributeValue(appElement, "AXEnhancedUserInterface" as CFString, kCFBooleanTrue)
        AXUIElementSetAttributeValue(appElement, "AXManualAccessibility" as CFString, kCFBooleanTrue)

        var targetElement: AXUIElement? = nil
        var focusedElementValue: AnyObject?

        // 2. 1차: 활성 앱의 직속 포커스된 UI 요소 조회
        let result = AXUIElementCopyAttributeValue(
            appElement,
            kAXFocusedUIElementAttribute as CFString,
            &focusedElementValue
        )
        if result == .success, let el = focusedElementValue, CFGetTypeID(el) == AXUIElementGetTypeID() {
            targetElement = (el as! AXUIElement)
        }

        // 3. 2차: 활성 윈도우(FocusedWindow)를 통한 하위 포커스 요소 조회 (Chromium/Electron 필수 대응)
        if targetElement == nil {
            var focusedWindowValue: AnyObject?
            if AXUIElementCopyAttributeValue(appElement, kAXFocusedWindowAttribute as CFString, &focusedWindowValue) == .success,
               let win = focusedWindowValue, CFGetTypeID(win) == AXUIElementGetTypeID() {
                let winElement = (win as! AXUIElement)
                AXUIElementSetAttributeValue(winElement, "AXEnhancedUserInterface" as CFString, kCFBooleanTrue)

                var winFocusedVal: AnyObject?
                if AXUIElementCopyAttributeValue(winElement, kAXFocusedUIElementAttribute as CFString, &winFocusedVal) == .success,
                   let el = winFocusedVal, CFGetTypeID(el) == AXUIElementGetTypeID() {
                    targetElement = (el as! AXUIElement)
                } else {
                    targetElement = winElement
                }
            }
        }

        // 4. 3차: SystemWide 글로벌 포커스 요소 폴백
        if targetElement == nil {
            let systemWide = AXUIElementCreateSystemWide()
            var sysVal: AnyObject?
            if AXUIElementCopyAttributeValue(systemWide, kAXFocusedUIElementAttribute as CFString, &sysVal) == .success,
               let el = sysVal, CFGetTypeID(el) == AXUIElementGetTypeID() {
                targetElement = (el as! AXUIElement)
            }
        }

        guard let axElement = targetElement else {
            // 실패 시 로깅 (1회성 또는 디버그)
            if hasAccessibilityPermission {
                DearTalkLogger.debug("❌ [\(frontApp.localizedName ?? "")] 포커스 요소 획득 실패 (AXError: \(result.rawValue))", category: "Accessibility")
            }
            return
        }

        self.lastFocusedElement = axElement

        if !hasAccessibilityPermission {
            DispatchQueue.main.async { [weak self] in
                self?.hasAccessibilityPermission = true
            }
            DearTalkLogger.info("✅ Accessibility 권한 실제 활성화 확인 (앱: \(frontApp.localizedName ?? ""))", category: "Accessibility")
        }

        // Chromium/Electron/WebKit 앱의 개별 요소 접근성 트리 강제 활성화
        AXUIElementSetAttributeValue(axElement, "AXEnhancedUserInterface" as CFString, kCFBooleanTrue)

        if let text = extractUniversalText(from: axElement) {
            let pos = extractElementPosition(axElement: axElement)
            DearTalkLogger.info("📝 [\(frontApp.localizedName ?? "")] 텍스트 추출 성공: '\(text.prefix(30))' (좌표: \(pos != nil ? "\(Int(pos!.x)),\(Int(pos!.y))" : "nil"))", category: "Accessibility")
            handleDetectedText(text, element: axElement, point: pos)
        }
    }

    /// AppKit, WebKit, Chromium, Java, SwiftUI 등 모든 종류의 입력창에서 텍스트를 추출하는 범용 엔진
    private func extractUniversalText(from element: AXUIElement) -> String? {
        // 1. 표준 kAXValueAttribute 검사 (String & NSAttributedString)
        var textValue: AnyObject?
        if AXUIElementCopyAttributeValue(element, kAXValueAttribute as CFString, &textValue) == .success,
           let val = textValue {
            if let str = val as? String, !str.isEmpty {
                return str
            } else if let attrStr = val as? NSAttributedString, !attrStr.string.isEmpty {
                return attrStr.string
            }
        }

        // 2. kAXSelectedTextAttribute (선택된 텍스트 또는 포커스 텍스트)
        var selectedTextValue: AnyObject?
        if AXUIElementCopyAttributeValue(element, kAXSelectedTextAttribute as CFString, &selectedTextValue) == .success,
           let selStr = selectedTextValue as? String, !selStr.isEmpty {
            return selStr
        }

        // 3. Chromium / WebKit 파라미터화된 텍스트 범위 추출 (Antigravity, VS Code, Chrome, Slack, Notion)
        var numCharsValue: AnyObject?
        if AXUIElementCopyAttributeValue(element, "AXNumberOfCharacters" as CFString, &numCharsValue) == .success,
           let numChars = numCharsValue as? Int, numChars > 0 {
            var range = CFRange(location: 0, length: min(numChars, 1000))
            if let rangeVal = AXValueCreate(.cfRange, &range) {
                var stringForRangeValue: AnyObject?
                if AXUIElementCopyParameterizedAttributeValue(
                    element,
                    "AXStringForRange" as CFString,
                    rangeVal,
                    &stringForRangeValue
                ) == .success, let rangeStr = stringForRangeValue as? String, !rangeStr.isEmpty {
                    return rangeStr
                }
            }
        }

        // 4. AXDescription 또는 AXTitle (일부 웹 검색창, 텍스트 필드)
        var descValue: AnyObject?
        if AXUIElementCopyAttributeValue(element, kAXDescriptionAttribute as CFString, &descValue) == .success,
           let descStr = descValue as? String, !descStr.isEmpty {
            return descStr
        }

        // 5. 자식/부모 요소 재귀 탐색 (Web ContentEditable 리치 에디터 계층 구조 대응)
        var childrenValue: AnyObject?
        if AXUIElementCopyAttributeValue(element, kAXChildrenAttribute as CFString, &childrenValue) == .success,
           let children = childrenValue as? [AXUIElement] {
            for child in children.prefix(3) {
                if let childText = extractUniversalText(from: child), !childText.isEmpty {
                    return childText
                }
            }
        }

        return nil
    }

    private func extractElementPosition(axElement: AXUIElement) -> CGPoint? {
        var posValue: AnyObject?
        var sizeValue: AnyObject?
        if AXUIElementCopyAttributeValue(axElement, kAXPositionAttribute as CFString, &posValue) == .success,
           AXUIElementCopyAttributeValue(axElement, kAXSizeAttribute as CFString, &sizeValue) == .success {
            var point = CGPoint.zero
            var size = CGSize.zero
            if let posVal = posValue, CFGetTypeID(posVal) == AXValueGetTypeID() {
                AXValueGetValue(posVal as! AXValue, .cgPoint, &point)
            }
            if let sizeVal = sizeValue, CFGetTypeID(sizeVal) == AXValueGetTypeID() {
                AXValueGetValue(sizeVal as! AXValue, .cgSize, &size)
            }

            guard point != .zero, let primaryScreen = NSScreen.screens.first else {
                return nil
            }

            // Convert macOS AX global coordinates (Top-Left based) to Cocoa coordinates (Bottom-Left based)
            let primaryHeight = primaryScreen.frame.height
            let cocoaY = primaryHeight - point.y - size.height

            // Place the floating panel directly above the focused input field
            let targetY = cocoaY + size.height + 10
            let targetX = max(20, point.x)

            return CGPoint(x: targetX, y: targetY)
        }
        return nil
    }

    public func handleDetectedText(_ text: String, element: AXUIElement? = nil, point: CGPoint? = nil) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed != currentText else { return }

        currentText = trimmed
        if let pt = point {
            self.cursorInfo = CursorInfo(screenPoint: pt, elementRect: .zero, selectedText: "", fullText: trimmed)
        }
        debounceTimer?.invalidate()

        guard !trimmed.isEmpty else {
            latestDiffResult = nil
            return
        }

        DearTalkLogger.debug("⌨️ Focused text change detected: '\(trimmed.prefix(30))...'", category: "Accessibility")

        // Trigger on-device AI Diff calculation 300ms after user pauses typing
        debounceTimer = Timer.scheduledTimer(withTimeInterval: 0.30, repeats: false) { [weak self] _ in
            Task { [weak self] in
                await self?.processTextWithAi(trimmed)
            }
        }
    }

    public func processTextWithAi(_ text: String) async {
        let tone = toneManager.currentTone
        let result: IntentResult

        DearTalkLogger.info("🧠 On-device AI inference started: '\(text.prefix(40))' (Tone: \(tone.name))", category: "Engine")

        if toneManager.isTranslationMode {
            result = await intentEngine.processTranslation(textInput: text, target: toneManager.currentTranslation)
        } else {
            result = await intentEngine.processWithTone(textInput: text, tone: tone)
        }

        if case .success(let refinedText, let msg) = result {
            let diff = DiffEngine.computeWordDiff(original: text, suggested: refinedText)
            DearTalkLogger.info("✨ AI suggestion completed: '\(refinedText.prefix(40))' (HasChanges: \(diff.hasChanges), Msg: \(msg))", category: "Engine")
            await MainActor.run {
                self.latestDiffResult = diff
            }
        } else if case .error(let fallback, let err) = result {
            DearTalkLogger.warning("⚠️ AI inference warning/preserving original: \(err)", category: "Engine")
            let diff = DiffEngine.computeWordDiff(original: text, suggested: fallback)
            await MainActor.run {
                self.latestDiffResult = diff
            }
        }
    }
}
