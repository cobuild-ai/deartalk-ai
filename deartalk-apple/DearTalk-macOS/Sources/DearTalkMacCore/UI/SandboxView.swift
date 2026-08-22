import SwiftUI

public struct SandboxView: View {
    @StateObject private var monitor = AccessibilityMonitor.shared
    @StateObject private var toneManager = CustomToneManager.shared
    @StateObject private var intentEngine = DearTalkIntentEngine.shared
    @StateObject private var downloader = ModelDownloader.shared
    @StateObject private var runtimeSetup = RuntimeSetupManager.shared
    @State private var inputText: String = ""
    @State private var liveDiffResult: DiffResult?
    @State private var statusNotice: String?
    @State private var isProcessing: Bool = false
    @State private var debounceTask: Task<Void, Never>?
    @State private var showCopiedAlert: Bool = false

    private let samplePresets = [
        "내일 아침 9시 만나 이것을 좀 공손하게 바꿔 줘",
        "지금 출발했어 조금 늦을 것 같아 정중하게 다듬어줘",
        "다음 주 화요일 오후 2시 어떠냐고 물어봐줘",
        "정리한 파일 보냈으니 확인해봐 공손하게 바꿔줘",
        "Let's meet tomorrow at 9 AM make it polite",
        "I sent the updated file please review politely"
    ]

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header & Status Bar
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text("✨")
                            .font(.system(size: 18))
                        Text(UiStrings.sandboxTitle)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                    }
                    Text(intentEngine.isModelLoaded ? UiStrings.sandboxModelActive : UiStrings.sandboxModelWaiting)
                        .font(.system(size: 11))
                        .foregroundColor(intentEngine.isModelLoaded ? Color(red: 0.4, green: 0.85, blue: 0.7) : Color.orange)
                }

                Spacer()

                // Test trigger button for floating overlay
                Button(action: {
                    let diff = DiffEngine.computeWordDiff(original: "오늘 약속 있어?", suggested: "오늘 약속이 있으신가요?")
                    OverlayPanelController.shared.showManual(diffResult: diff)
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "sparkles")
                            .font(.system(size: 10, weight: .bold))
                        Text(UiStrings.sandboxShowPanelBtn)
                            .font(.system(size: 11, weight: .bold))
                    }
                    .foregroundColor(.black)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(red: 0.22, green: 0.85, blue: 0.65))
                    .cornerRadius(6)
                }
                .buttonStyle(.plain)

                // Monitoring status toggle button
                Button(action: {
                    if !monitor.hasAccessibilityPermission {
                        OnboardingWindowController.shared.showOnboarding()
                    } else if monitor.isMonitoring {
                        monitor.stopMonitoring()
                    } else {
                        monitor.startMonitoring()
                    }
                }) {
                    HStack(spacing: 5) {
                        Circle()
                            .fill(monitor.isMonitoring && monitor.hasAccessibilityPermission ? Color.green : Color.orange)
                            .frame(width: 8, height: 8)
                        Text(monitor.hasAccessibilityPermission ? (monitor.isMonitoring ? UiStrings.statusActive : UiStrings.statusPaused) : UiStrings.permissionNeeded)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(monitor.isMonitoring && monitor.hasAccessibilityPermission ? .green : .orange)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(monitor.isMonitoring && monitor.hasAccessibilityPermission ? Color.green.opacity(0.3) : Color.orange.opacity(0.3), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .help(monitor.isMonitoring ? UiStrings.sandboxMonitoringTooltipActive : UiStrings.sandboxMonitoringTooltipPaused)
            }
            .padding(16)
            .background(Color(red: 0.1, green: 0.12, blue: 0.16))

            Divider()
                .background(Color.white.opacity(0.1))

            // 메인 콘텐츠 영역
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // 환경 미준비 시 온디바이스 AI 환경 진단 및 원클릭 설정 카드
                    if !intentEngine.isModelLoaded || !runtimeSetup.hasRuntimeBinary {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(alignment: .top, spacing: 10) {
                                Text("🛠️")
                                    .font(.system(size: 22))
                                VStack(alignment: .leading, spacing: 3) {
                                    HStack {
                                        Text(UiStrings.envDiagnosisTitle)
                                            .font(.system(size: 13, weight: .bold))
                                            .foregroundColor(.white)
                                        Spacer()
                                        Text(intentEngine.isModelLoaded ? "준비 완료" : "환경 구성 필요")
                                            .font(.system(size: 10, weight: .medium))
                                            .foregroundColor(intentEngine.isModelLoaded ? .green : .orange)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background((intentEngine.isModelLoaded ? Color.green : Color.orange).opacity(0.15))
                                            .cornerRadius(4)
                                    }
                                    Text("온디바이스 LLM 신경망(Gemma)과 Metal GPU 가속 런타임(llama.cpp)을 진단하고 원클릭으로 설치합니다.")
                                        .font(.system(size: 11))
                                        .foregroundColor(Color(nsColor: .secondaryLabelColor))
                                }
                            }

                            Divider()
                                .background(Color.white.opacity(0.1))

                            // 1. 모델 파일 상태
                            HStack {
                                Text(UiStrings.envModelStatus)
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(.white)
                                Spacer()
                                if runtimeSetup.hasModelFile {
                                    Text("\(UiStrings.envStatusInstalled) (\(runtimeSetup.modelFileSizeMB) MB)")
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.green)
                                } else {
                                    Text(UiStrings.envStatusMissing)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.orange)
                                }
                            }

                            // 2. Metal 가속 런타임 상태
                            HStack {
                                Text(UiStrings.envRuntimeStatus)
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(.white)
                                Spacer()
                                if runtimeSetup.hasRuntimeBinary {
                                    Text(UiStrings.envStatusInstalled)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.green)
                                } else {
                                    Text(UiStrings.envStatusMissing)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.orange)
                                }
                            }

                            // 3. 데몬 서비스 상태
                            HStack {
                                Text(UiStrings.envDaemonStatus)
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(.white)
                                Spacer()
                                if intentEngine.isModelLoaded || runtimeSetup.isDaemonRunning {
                                    Text(UiStrings.envStatusRunning)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.cyan)
                                } else {
                                    Text(UiStrings.envStatusStopped)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(.gray)
                                }
                            }

                            // 다운로드 및 설치 액션 영역
                            if downloader.isDownloading {
                                VStack(alignment: .leading, spacing: 6) {
                                    ProgressView(value: downloader.progress, total: 1.0)
                                        .progressViewStyle(.linear)
                                        .tint(Color(red: 0.22, green: 0.85, blue: 0.65))

                                    HStack {
                                        Text(downloader.statusMessage)
                                            .font(.system(size: 11, weight: .medium))
                                            .foregroundColor(Color(red: 0.22, green: 0.85, blue: 0.65))
                                        Spacer()
                                        Button("취소") {
                                            downloader.cancelDownload()
                                        }
                                        .font(.system(size: 10))
                                        .buttonStyle(.plain)
                                        .foregroundColor(.red)
                                    }
                                }
                                .padding(.top, 4)
                            } else if runtimeSetup.isInstalling {
                                VStack(alignment: .leading, spacing: 6) {
                                    ProgressView()
                                        .progressViewStyle(.linear)
                                    Text(runtimeSetup.installProgressMessage)
                                        .font(.system(size: 11))
                                        .foregroundColor(Color.cyan)
                                }
                            } else {
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack(spacing: 8) {
                                        if !runtimeSetup.hasModelFile {
                                            Button(action: {
                                                downloader.startDownload()
                                            }) {
                                                HStack(spacing: 4) {
                                                    Image(systemName: "arrow.down.circle.fill")
                                                        .font(.system(size: 11, weight: .bold))
                                                    Text("📦 모델 다운로드 (1.6GB)")
                                                        .font(.system(size: 11, weight: .bold))
                                                }
                                                .foregroundColor(.black)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(Color(red: 0.22, green: 0.85, blue: 0.65))
                                                .cornerRadius(6)
                                            }
                                            .buttonStyle(.plain)
                                        }

                                        if !runtimeSetup.hasRuntimeBinary {
                                            if runtimeSetup.hasHomebrew {
                                                Button(action: {
                                                    runtimeSetup.installLlamaCppViaBrew()
                                                }) {
                                                    HStack(spacing: 4) {
                                                        Image(systemName: "bolt.fill")
                                                            .font(.system(size: 11, weight: .bold))
                                                        Text(UiStrings.envInstallRuntimeBtn)
                                                            .font(.system(size: 11, weight: .bold))
                                                    }
                                                    .foregroundColor(.white)
                                                    .padding(.horizontal, 12)
                                                    .padding(.vertical, 6)
                                                    .background(Color.blue)
                                                    .cornerRadius(6)
                                                }
                                                .buttonStyle(.plain)
                                            }

                                            Button(action: {
                                                runtimeSetup.copyBrewInstallCommand()
                                                showCopiedAlert = true
                                                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                                                    showCopiedAlert = false
                                                }
                                            }) {
                                                HStack(spacing: 4) {
                                                    Image(systemName: "doc.on.doc")
                                                        .font(.system(size: 10))
                                                    Text(showCopiedAlert ? UiStrings.envBrewCmdCopied : UiStrings.envCopyBrewCmd)
                                                        .font(.system(size: 11))
                                                }
                                                .foregroundColor(.white)
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 6)
                                                .background(Color.white.opacity(0.12))
                                                .cornerRadius(6)
                                            }
                                            .buttonStyle(.plain)
                                        }

                                        Button(action: {
                                            Task {
                                                await runtimeSetup.diagnoseEnvironment()
                                                intentEngine.detectAndInitOnDeviceModel()
                                            }
                                        }) {
                                            HStack(spacing: 4) {
                                                Image(systemName: "arrow.clockwise")
                                                    .font(.system(size: 10))
                                                Text(UiStrings.envRefreshDiagnosis)
                                                    .font(.system(size: 11))
                                            }
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 6)
                                            .background(Color.white.opacity(0.1))
                                            .cornerRadius(6)
                                        }
                                        .buttonStyle(.plain)
                                    }

                                    if !runtimeSetup.hasRuntimeBinary {
                                        Text(UiStrings.envBrewInstruction)
                                            .font(.system(size: 10))
                                            .foregroundColor(Color.cyan.opacity(0.8))
                                    }
                                }
                                .padding(.top, 4)
                            }

                            if let error = downloader.errorMessage ?? runtimeSetup.lastInstallError {
                                Text(error)
                                    .font(.system(size: 10))
                                    .foregroundColor(.red)
                            }
                        }
                        .padding(14)
                        .background(Color(red: 0.12, green: 0.15, blue: 0.22))
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.cyan.opacity(0.35), lineWidth: 1)
                        )
                    }

                    // 1. 톤앤매너 선택 바
                    VStack(alignment: .leading, spacing: 8) {
                        Text(UiStrings.currentTone)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(Color(nsColor: .secondaryLabelColor))

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(toneManager.defaultTones) { tone in
                                    let isSelected = toneManager.selectedToneId == tone.id
                                    Button(action: {
                                        toneManager.selectedToneId = tone.id
                                        triggerAiDiff(inputText)
                                    }) {
                                        HStack(spacing: 4) {
                                            Text(tone.icon)
                                            Text(tone.name)
                                                .font(.system(size: 12, weight: isSelected ? .bold : .regular))
                                        }
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                        .background(isSelected ? Color(red: 0.25, green: 0.45, blue: 0.95) : Color.white.opacity(0.06))
                                        .foregroundColor(isSelected ? .white : Color(nsColor: .secondaryLabelColor))
                                        .cornerRadius(8)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    // 2. 실시간 입력 에디터
                    VStack(alignment: .leading, spacing: 6) {
                        Text(UiStrings.originalText)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(Color(nsColor: .secondaryLabelColor))

                        TextEditor(text: $inputText)
                            .font(.system(size: 14))
                            .frame(height: 110)
                            .padding(8)
                            .background(Color(red: 0.08, green: 0.1, blue: 0.13))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.white.opacity(0.15), lineWidth: 1)
                            )
                            .onChange(of: inputText) { _, newValue in
                                triggerAiDiff(newValue)
                            }
                    }

                    // 3. 실시간 DIFF 비교 뷰
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(UiStrings.suggestedText)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(Color(red: 0.22, green: 0.85, blue: 0.65))

                            if isProcessing {
                                ProgressView()
                                    .scaleEffect(0.6)
                                Text(UiStrings.analyzing)
                                    .font(.system(size: 11))
                                    .foregroundColor(Color(nsColor: .tertiaryLabelColor))
                            }

                            Spacer()

                            // 범례
                            HStack(spacing: 12) {
                                HStack(spacing: 4) {
                                    Circle().fill(Color(red: 0.98, green: 0.45, blue: 0.45)).frame(width: 6, height: 6)
                                    Text(UiStrings.diffLegendRemoved).font(.system(size: 10)).foregroundColor(Color(nsColor: .tertiaryLabelColor))
                                }
                                HStack(spacing: 4) {
                                    Circle().fill(Color(red: 0.22, green: 0.85, blue: 0.65)).frame(width: 6, height: 6)
                                    Text(UiStrings.diffLegendAdded).font(.system(size: 10)).foregroundColor(Color(nsColor: .tertiaryLabelColor))
                                }
                            }
                        }

                        if let diff = liveDiffResult, !diff.operations.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                if diff.hasChanges {
                                    DiffBadgeView(operations: diff.operations)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                } else {
                                    HStack(spacing: 8) {
                                        Text(intentEngine.isModelLoaded ? "✨" : "🔒")
                                        Text(statusNotice ?? (intentEngine.isModelLoaded ? UiStrings.noDiffFound : UiStrings.modelNotLoaded))
                                            .font(.system(size: 13, weight: .medium))
                                            .foregroundColor(intentEngine.isModelLoaded ? Color(red: 0.4, green: 0.85, blue: 0.7) : Color.orange)
                                    }
                                    .padding(.vertical, 8)
                                }
                            }
                            .padding(14)
                            .background(Color(red: 0.05, green: 0.07, blue: 0.1))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(diff.hasChanges ? Color(red: 0.22, green: 0.85, blue: 0.65).opacity(0.3) : Color.white.opacity(0.1), lineWidth: 1)
                            )
                        } else {
                            HStack {
                                Spacer()
                                Text(UiStrings.sandboxPlaceholder)
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(nsColor: .tertiaryLabelColor))
                                    .multilineTextAlignment(.center)
                                    .padding(.vertical, 24)
                                Spacer()
                            }
                            .background(Color(red: 0.05, green: 0.07, blue: 0.1).opacity(0.5))
                            .cornerRadius(10)
                        }
                    }

                    // 4. 프리셋 원클릭 테스트 칩
                    VStack(alignment: .leading, spacing: 8) {
                        Text(UiStrings.testPresets)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(Color(nsColor: .secondaryLabelColor))

                        FlowLayout(spacing: 6) {
                            ForEach(samplePresets, id: \.self) { preset in
                                Button(action: {
                                    inputText = preset
                                    triggerAiDiff(preset)
                                }) {
                                    Text(preset)
                                        .font(.system(size: 11))
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 5)
                                        .background(Color.white.opacity(0.06))
                                        .foregroundColor(Color(nsColor: .secondaryLabelColor))
                                        .cornerRadius(6)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 6)
                                                .stroke(Color.white.opacity(0.1), lineWidth: 1)
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(16)
            }
        }
        .frame(minWidth: 580, minHeight: 520)
        .background(Color(red: 0.08, green: 0.09, blue: 0.12))
    }

    private func triggerAiDiff(_ text: String) {
        debounceTask?.cancel()
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmed.isEmpty else {
            liveDiffResult = nil
            statusNotice = nil
            isProcessing = false
            return
        }

        isProcessing = true
        debounceTask = Task {
            try? await Task.sleep(nanoseconds: 300_000_000) // 300ms 디바운스
            guard !Task.isCancelled else { return }

            let tone = toneManager.currentTone
            let res = await intentEngine.processWithTone(textInput: trimmed, tone: tone)

            if case .success(let refined, let message) = res {
                let diff = DiffEngine.computeWordDiff(original: trimmed, suggested: refined)
                await MainActor.run {
                    self.liveDiffResult = diff
                    self.statusNotice = message
                    self.isProcessing = false
                    if diff.hasChanges {
                        OverlayPanelController.shared.showManual(diffResult: diff)
                    }
                }
            } else if case .error(let fallback, let error) = res {
                let diff = DiffEngine.computeWordDiff(original: trimmed, suggested: fallback)
                await MainActor.run {
                    self.liveDiffResult = diff
                    self.statusNotice = "⚠️ 로컬 AI 처리: \(error)"
                    self.isProcessing = false
                    if diff.hasChanges {
                        OverlayPanelController.shared.showManual(diffResult: diff)
                    }
                }
            }
        }
    }
}

/// 단순 플로우 레이아웃 헬퍼
private struct FlowLayout: Layout {
    var spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var height: CGFloat = 0
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var maxHeightInRow: CGFloat = 0

        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth, currentX > 0 {
                currentX = 0
                currentY += maxHeightInRow + spacing
                maxHeightInRow = 0
            }
            currentX += size.width + spacing
            maxHeightInRow = max(maxHeightInRow, size.height)
        }
        height = currentY + maxHeightInRow
        return CGSize(width: maxWidth, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var currentX = bounds.minX
        var currentY = bounds.minY
        var maxHeightInRow: CGFloat = 0

        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if currentX + size.width > bounds.maxX, currentX > bounds.minX {
                currentX = bounds.minX
                currentY += maxHeightInRow + spacing
                maxHeightInRow = 0
            }
            view.place(at: CGPoint(x: currentX, y: currentY), proposal: .unspecified)
            currentX += size.width + spacing
            maxHeightInRow = max(maxHeightInRow, size.height)
        }
    }
}
