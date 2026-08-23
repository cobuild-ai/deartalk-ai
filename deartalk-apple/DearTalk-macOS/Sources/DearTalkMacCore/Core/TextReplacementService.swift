import Foundation
import AppKit
import ApplicationServices

public final class TextReplacementService {
    public static let shared = TextReplacementService()

    public func applyReplacement(newText: String) {
        // 1. 모니터링이 기억하고 있는 직전 포커스된 AXElement에 직접 텍스트 주입
        if let axElement = AccessibilityMonitor.shared.lastFocusedElement {
            let setResult = AXUIElementSetAttributeValue(axElement, kAXValueAttribute as CFString, newText as CFTypeRef)
            if setResult == .success {
                DearTalkLogger.info("✅ [lastFocusedElement] 텍스트 직접 치환 완료: '\(newText)'", category: "TextReplacement")
                return
            }
        }

        // 2. 현재 활성 앱의 포커스 요소 조회 후 텍스트 주입
        if let frontApp = NSWorkspace.shared.frontmostApplication {
            let appElement = AXUIElementCreateApplication(frontApp.processIdentifier)
            var focusedElementValue: AnyObject?
            if AXUIElementCopyAttributeValue(appElement, kAXFocusedUIElementAttribute as CFString, &focusedElementValue) == .success,
               let focusedElement = focusedElementValue,
               CFGetTypeID(focusedElement) == AXUIElementGetTypeID() {
                let axElement = focusedElement as! AXUIElement
                let setResult = AXUIElementSetAttributeValue(axElement, kAXValueAttribute as CFString, newText as CFTypeRef)
                if setResult == .success {
                    DearTalkLogger.info("✅ [frontApp: \(frontApp.localizedName ?? "")] 텍스트 직접 치환 완료: '\(newText)'", category: "TextReplacement")
                    return
                }
            }
        }

        // 3. SystemWide 포커스 요소 시도
        let systemWide = AXUIElementCreateSystemWide()
        var focusedElementValue: AnyObject?
        let result = AXUIElementCopyAttributeValue(
            systemWide,
            kAXFocusedUIElementAttribute as CFString,
            &focusedElementValue
        )
        if result == .success, let focusedElement = focusedElementValue,
           CFGetTypeID(focusedElement) == AXUIElementGetTypeID() {
            let axElement = focusedElement as! AXUIElement
            let setResult = AXUIElementSetAttributeValue(axElement, kAXValueAttribute as CFString, newText as CFTypeRef)
            if setResult == .success {
                DearTalkLogger.info("✅ [SystemWide] 텍스트 직접 치환 완료: '\(newText)'", category: "TextReplacement")
                return
            }
        }

        // 4. 폴백: 클립보드 복사 후 붙여넣기(Cmd+V) 시뮬레이션
        pasteboardFallback(newText: newText)
    }

    private func pasteboardFallback(newText: String) {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(newText, forType: .string)

        let src = CGEventSource(stateID: .hidSystemState)

        // Cmd + A (전체 선택)
        let keyADown = CGEvent(keyboardEventSource: src, virtualKey: 0x00, keyDown: true)
        keyADown?.flags = .maskCommand
        let keyAUp = CGEvent(keyboardEventSource: src, virtualKey: 0x00, keyDown: false)
        keyAUp?.flags = .maskCommand

        keyADown?.post(tap: .cghidEventTap)
        keyAUp?.post(tap: .cghidEventTap)

        // Cmd + V (붙여넣기)
        let keyVDown = CGEvent(keyboardEventSource: src, virtualKey: 0x09, keyDown: true)
        keyVDown?.flags = .maskCommand
        let keyVUp = CGEvent(keyboardEventSource: src, virtualKey: 0x09, keyDown: false)
        keyVUp?.flags = .maskCommand

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
            keyVDown?.post(tap: .cghidEventTap)
            keyVUp?.post(tap: .cghidEventTap)
        }

        DearTalkLogger.info("📋 클립보드 붙여넣기 시뮬레이션으로 텍스트 치환 완료", category: "TextReplacement")
    }
}
