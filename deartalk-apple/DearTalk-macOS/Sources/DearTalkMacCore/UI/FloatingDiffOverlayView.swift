import SwiftUI
import AppKit

public struct DiffBadgeView: View {
    public let operations: [DiffOp]

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(operations.enumerated()), id: \.offset) { _, op in
                switch op {
                case .unchanged(let text):
                    Text(text)
                        .foregroundColor(Color(nsColor: .secondaryLabelColor))
                        .font(.system(size: 13, weight: .regular))

                case .removed(let text):
                    Text(text)
                        .foregroundColor(Color(red: 0.98, green: 0.45, blue: 0.45))
                        .strikethrough(true, color: Color(red: 0.98, green: 0.45, blue: 0.45))
                        .font(.system(size: 13, weight: .medium))
                        .padding(.horizontal, 2)
                        .background(Color(red: 0.4, green: 0.1, blue: 0.1).opacity(0.4))
                        .cornerRadius(3)

                case .added(let text):
                    Text(text)
                        .foregroundColor(Color(red: 0.22, green: 0.85, blue: 0.65))
                        .font(.system(size: 13, weight: .bold))
                        .padding(.horizontal, 2)
                        .background(Color(red: 0.05, green: 0.3, blue: 0.2).opacity(0.45))
                        .cornerRadius(3)
                }
            }
        }
    }
}

public struct FloatingDiffOverlayView: View {
    @ObservedObject private var toneManager = CustomToneManager.shared
    @State private var currentDiff: DiffResult
    @State private var selectedTone: String = "tone_refine"
    @State private var isProcessing: Bool = false
    @State private var copiedNotice: Bool = false

    public let onApply: (String) -> Void
    public let onDismiss: () -> Void

    public init(diffResult: DiffResult, onApply: @escaping (String) -> Void, onDismiss: @escaping () -> Void) {
        self._currentDiff = State(initialValue: diffResult)
        self.onApply = onApply
        self.onDismiss = onDismiss
    }

    private var availableTones: [CustomTone] {
        CustomToneManager.shared.defaultTones
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // 1. Top Header: App Icon, Title, Tab completion toggle, and Dismiss button
            HStack {
                HStack(spacing: 6) {
                    Text("✨")
                        .font(.system(size: 13))
                    Text(UiStrings.appName)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                    if isProcessing {
                        ProgressView()
                            .scaleEffect(0.5)
                            .frame(width: 12, height: 12)
                    }
                }

                Spacer()

                HStack(spacing: 6) {
                    // Tab Auto-completion ON/OFF toggle chip
                    Button(action: {
                        toneManager.isTabCompletionEnabled.toggle()
                    }) {
                        HStack(spacing: 3) {
                            Text("⇥")
                                .font(.system(size: 10, weight: .bold))
                            Text(toneManager.isTabCompletionEnabled ? UiStrings.tabApplyOn : UiStrings.tabApplyOff)
                                .font(.system(size: 10, weight: .semibold))
                        }
                        .foregroundColor(toneManager.isTabCompletionEnabled ? .cyan : Color(nsColor: .secondaryLabelColor))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(toneManager.isTabCompletionEnabled ? Color.cyan.opacity(0.18) : Color.white.opacity(0.08))
                        .cornerRadius(4)
                    }
                    .buttonStyle(.plain)
                    .help(toneManager.isTabCompletionEnabled ? UiStrings.tabApplyTooltipOn : UiStrings.tabApplyTooltipOff)

                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(Color(nsColor: .secondaryLabelColor))
                            .padding(4)
                    }
                    .buttonStyle(.plain)
                    .help(UiStrings.dismissShortcut)
                }
            }

            // 2. Interactive Tone & Manner Chips Selector
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(availableTones, id: \.id) { tone in
                        Button(action: {
                            changeTone(tone.id)
                        }) {
                            HStack(spacing: 4) {
                                Text(tone.icon)
                                    .font(.system(size: 11))
                                Text(tone.name)
                                    .font(.system(size: 11, weight: selectedTone == tone.id ? .bold : .medium))
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .foregroundColor(selectedTone == tone.id ? .black : Color(nsColor: .secondaryLabelColor))
                            .background(
                                selectedTone == tone.id
                                    ? Color(red: 0.22, green: 0.85, blue: 0.65)
                                    : Color.white.opacity(0.08)
                            )
                            .cornerRadius(12)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            // 3. 2-Line Layout (Line 1: Immutable Original, Line 2: Real-time AI DIFF)
            VStack(alignment: .leading, spacing: 6) {
                // Line 1: Immutable Original
                HStack(alignment: .top, spacing: 6) {
                    Text(UiStrings.originalBadge)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(Color(nsColor: .tertiaryLabelColor))
                        .padding(.horizontal, 4)
                        .padding(.vertical, 1)
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(3)

                    Text(currentDiff.original)
                        .font(.system(size: 12))
                        .foregroundColor(Color(nsColor: .secondaryLabelColor))
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.white.opacity(0.03))
                .cornerRadius(6)

                // Line 2: Real-time AI DIFF Result
                HStack(alignment: .top, spacing: 6) {
                    Text(UiStrings.aiBadge)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.black)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 1)
                        .background(Color(red: 0.22, green: 0.85, blue: 0.65))
                        .cornerRadius(3)

                    DiffBadgeView(operations: currentDiff.operations)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.black.opacity(0.3))
                .cornerRadius(6)
            }
            .padding(8)
            .background(Color.black.opacity(0.35))
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
            )

            // 4. Action Toolbar: Copy, Regenerate, Apply
            HStack(spacing: 8) {
                // Copy to Clipboard Button
                Button(action: copyToClipboard) {
                    HStack(spacing: 4) {
                        Image(systemName: copiedNotice ? "checkmark" : "doc.on.doc")
                            .font(.system(size: 10))
                        Text(copiedNotice ? UiStrings.copied : UiStrings.copySuggestion)
                            .font(.system(size: 11))
                    }
                    .foregroundColor(copiedNotice ? .green : Color(nsColor: .secondaryLabelColor))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.06))
                    .cornerRadius(6)
                }
                .buttonStyle(.plain)

                // Regenerate Button
                Button(action: regenerate) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 10))
                        Text(UiStrings.regenerate)
                            .font(.system(size: 11))
                    }
                    .foregroundColor(Color(nsColor: .secondaryLabelColor))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.06))
                    .cornerRadius(6)
                }
                .buttonStyle(.plain)

                Spacer()

                // Apply Replacement Button
                Button(action: {
                    onApply(currentDiff.suggested)
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                        Text(toneManager.isTabCompletionEnabled ? UiStrings.applyNowWithTab : UiStrings.applyNow)
                            .font(.system(size: 11, weight: .bold))
                    }
                    .foregroundColor(.black)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(Color(red: 0.22, green: 0.85, blue: 0.65))
                    .cornerRadius(6)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .frame(minWidth: 340, maxWidth: 500)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(
                            LinearGradient(
                                colors: [Color.cyan.opacity(0.4), Color.mint.opacity(0.2), Color.white.opacity(0.1)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                )
                .shadow(color: Color.black.opacity(0.4), radius: 16, x: 0, y: 8)
        )
    }

    private func changeTone(_ toneId: String) {
        selectedTone = toneId
        guard !isProcessing else { return }
        isProcessing = true

        Task {
            let engine = DearTalkIntentEngine.shared
            let original = currentDiff.original
            let newSuggested: String

            if toneId == "tone_refine" {
                let res = await engine.process(textInput: original)
                newSuggested = res.text
            } else if let tone = CustomToneManager.shared.defaultTones.first(where: { $0.id == toneId }) {
                let res = await engine.processWithTone(textInput: original, tone: tone)
                newSuggested = res.text
            } else {
                let res = await engine.process(textInput: original)
                newSuggested = res.text
            }

            await MainActor.run {
                let newDiff = DiffEngine.computeWordDiff(original: original, suggested: newSuggested)
                self.currentDiff = newDiff
                OverlayPanelController.shared.updateCurrentDiffResult(newDiff)
                self.isProcessing = false
            }
        }
    }

    private func regenerate() {
        changeTone(selectedTone)
    }

    private func copyToClipboard() {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(currentDiff.suggested, forType: .string)
        withAnimation {
            copiedNotice = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            withAnimation {
                self.copiedNotice = false
            }
        }
    }
}

/// NSPanel 기반의 포커스를 뺏지 않는 실시간 플로팅 오버레이 윈도우 컨트롤러
public final class OverlayPanelController: NSObject {
    public static let shared = OverlayPanelController()

    private var panel: NSPanel?
    /// 현재 표시 중인 DiffResult — 동일 diff에 대한 중복 show() 호출 방지
    private var currentDiffResult: DiffResult?
    /// 수동(버튼 클릭)으로 띄운 패널은 모니터링 체인이 자동으로 숨기지 못하도록 보호
    private var isManuallyTriggered = false

    /// 전역 및 로컬 키보드 이벤트 모니터 (Tab 키로 교체, Esc로 닫기)
    private var globalKeyMonitor: Any?
    private var localKeyMonitor: Any?

    public override init() {
        super.init()
    }

    /// 사용자가 톤앤매너 칩을 눌러 변경한 최신 DiffResult 동기화 (Tab 키 치환 시 최신 톤 반영)
    public func updateCurrentDiffResult(_ diff: DiffResult) {
        self.currentDiffResult = diff
    }

    /// 사용자가 버튼 등으로 직접 패널을 띄울 때 호출 — 모니터링에 의한 자동 숨김으로부터 보호됨
    public func showManual(diffResult: DiffResult, near point: CGPoint? = nil) {
        DearTalkLogger.info("🖱️ 플로팅 패널 수동 표시 요청 (isManuallyTriggered → true)", category: "Overlay")
        isManuallyTriggered = true
        currentDiffResult = nil // 수동 표시 시 중복 가드를 리셋하여 강제 표시
        show(diffResult: diffResult, near: point)
    }

    /// Accessibility 모니터링 Combine 체인 전용 — 수동 표시 중이면 숨기지 않음
    public func hideIfAutomatic() {
        if isManuallyTriggered {
            DearTalkLogger.debug("🛡️ 수동 표시 중이므로 자동 숨김 무시 (isManuallyTriggered=true)", category: "Overlay")
            return
        }
        DearTalkLogger.info("🔄 모니터링 체인에 의한 자동 패널 숨김", category: "Overlay")
        hide()
    }

    public func show(diffResult: DiffResult, near point: CGPoint? = nil) {
        // 동일한 diff가 이미 표시 중이면 중복 갱신 방지 (화면 깜빡임 제거)
        if currentDiffResult == diffResult, panel?.isVisible == true {
            return
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            self.currentDiffResult = diffResult

            let overlayView = FloatingDiffOverlayView(
                diffResult: diffResult,
                onApply: { [weak self] newText in
                    TextReplacementService.shared.applyReplacement(newText: newText)
                    self?.hide()
                },
                onDismiss: { [weak self] in
                    self?.hide()
                }
            )

            let hostingView = NSHostingView(rootView: overlayView)
            let fittingSize = hostingView.fittingSize
            let width = max(340, fittingSize.width)
            let height = max(110, fittingSize.height)
            let finalSize = CGSize(width: width, height: height)

            if self.panel == nil {
                let p = NSPanel(
                    contentRect: NSRect(origin: .zero, size: finalSize),
                    styleMask: [.nonactivatingPanel, .borderless],
                    backing: .buffered,
                    defer: false
                )
                p.isOpaque = false
                p.backgroundColor = .clear
                p.level = .popUpMenu // 활성 창 최상단 플로팅 레벨
                p.hasShadow = true
                p.isFloatingPanel = true
                p.hidesOnDeactivate = false // 다른 앱 전환 시에도 숨김 방지
                p.becomesKeyOnlyIfNeeded = true
                p.isMovableByWindowBackground = true
                p.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
                self.panel = p
            }

            guard let panel = self.panel else { return }
            panel.contentView = hostingView
            panel.setContentSize(finalSize)

            let targetPoint: CGPoint
            if let pt = point {
                targetPoint = pt
            } else if let screen = NSScreen.main {
                let visibleFrame = screen.visibleFrame
                targetPoint = CGPoint(x: visibleFrame.midX - finalSize.width / 2, y: visibleFrame.midY + 60)
            } else {
                targetPoint = CGPoint(x: 300, y: 400)
            }

            panel.setFrameOrigin(targetPoint)
            panel.orderFrontRegardless() // 포커스를 뺏지 않고 모든 앱 위에 최상단 표시
            DearTalkLogger.info("✨ 플로팅 오버레이 패널 화면 표시 (위치: (\(Int(targetPoint.x)), \(Int(targetPoint.y))), 크기: \(Int(finalSize.width))x\(Int(finalSize.height)))", category: "Overlay")

            // Tab 키(48) 및 Esc 키(53) 전역 단축키 리스너 활성화
            self.setupKeyMonitors()
        }
    }

    private func setupKeyMonitors() {
        removeKeyMonitors()

        // 외부 앱(카카오톡, 브라우저, IDE 등) 포커스 상태에서의 Tab/Esc 키 인터셉트
        globalKeyMonitor = NSEvent.addGlobalMonitorForEvents(matching: .keyDown) { [weak self] event in
            guard let self = self, self.panel?.isVisible == true, let diff = self.currentDiffResult else { return }

            if event.keyCode == 48 { // Tab 키
                if CustomToneManager.shared.isTabCompletionEnabled {
                    DearTalkLogger.info("⌨️ 전역 Tab 키 입력 감지 -> AI 교정 텍스트 즉시 치환 실행", category: "Overlay")
                    TextReplacementService.shared.applyReplacement(newText: diff.suggested)
                    self.hide()
                } else {
                    DearTalkLogger.debug("ℹ️ Tab 자동완성이 꺼져 있어 Tab 키를 가로채지 않음 (IDE/에디터 들여쓰기 보호)", category: "Overlay")
                }
            } else if event.keyCode == 53 { // Esc 키
                DearTalkLogger.info("⌨️ 전역 Esc 키 입력 감지 -> 플로팅 패널 닫기", category: "Overlay")
                self.hide()
            }
        }

        // DearTalk 자체 창 포커스 상태에서의 Tab/Esc 키 처리
        localKeyMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self] event in
            guard let self = self, self.panel?.isVisible == true, let diff = self.currentDiffResult else { return event }

            if event.keyCode == 48 { // Tab 키
                if CustomToneManager.shared.isTabCompletionEnabled {
                    DearTalkLogger.info("⌨️ 로컬 Tab 키 입력 감지 -> AI 교정 텍스트 즉시 치환 실행", category: "Overlay")
                    TextReplacementService.shared.applyReplacement(newText: diff.suggested)
                    self.hide()
                    return nil // Tab 키 전파 방지
                }
            } else if event.keyCode == 53 { // Esc 키
                DearTalkLogger.info("⌨️ 로컬 Esc 키 입력 감지 -> 플로팅 패널 닫기", category: "Overlay")
                self.hide()
                return nil
            }
            return event
        }
    }

    private func removeKeyMonitors() {
        if let gm = globalKeyMonitor {
            NSEvent.removeMonitor(gm)
            globalKeyMonitor = nil
        }
        if let lm = localKeyMonitor {
            NSEvent.removeMonitor(lm)
            localKeyMonitor = nil
        }
    }

    public func hide() {
        DearTalkLogger.info("🔴 플로팅 패널 숨김 (isManuallyTriggered: \(isManuallyTriggered) → false)", category: "Overlay")
        isManuallyTriggered = false
        currentDiffResult = nil
        removeKeyMonitors()
        DispatchQueue.main.async { [weak self] in
            self?.panel?.orderOut(nil)
        }
    }
}
