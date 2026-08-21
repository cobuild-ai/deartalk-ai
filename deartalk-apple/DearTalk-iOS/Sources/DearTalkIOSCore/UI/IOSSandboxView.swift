import SwiftUI

public struct IOSSandboxView: View {
    @StateObject private var engine = DearTalkIntentEngine.shared
    @StateObject private var toneManager = CustomToneManager.shared
    @StateObject private var downloader = ModelDownloader.shared

    @State private var inputText: String = ""
    @State private var outputText: String = ""
    @State private var selectedToneId: String = "refine"
    @State private var isCopied: Bool = false
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header & Model Status
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(UiStrings.appName)
                                .font(.title2)
                                .fontWeight(.bold)
                            Text(UiStrings.appSubtitle)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        statusBadge
                    }
                    .padding(.horizontal)

                    // 모델 미배치 시 원클릭 인앱 자동 다운로더 카드
                    if !engine.isModelLoaded {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .top, spacing: 10) {
                                Text("📦")
                                    .font(.title2)
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(UiStrings.isKo ? "Google Gemma AI 모델 설치" : "Install Google Gemma AI Model")
                                            .font(.system(size: 13, weight: .bold))
                                            .foregroundColor(.primary)
                                        Spacer()
                                        Text(UiStrings.isKo ? "오프라인 저장 (~1.6GB)" : "Offline Store (~1.6GB)")
                                            .font(.system(size: 9, weight: .semibold))
                                            .foregroundColor(.accentColor)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color.accentColor.opacity(0.12))
                                            .cornerRadius(4)
                                    }
                                    Text(UiStrings.isKo ? "외부 도구 설치 없이 앱 내부에서 100% 온디바이스 Gemma 신경망을 원클릭으로 다운로드하여 즉시 실시간 글쓰기 교정을 활성화합니다." : "Download the 100% on-device Gemma neural network directly inside the app with a single click to instantly activate real-time writing refinement.")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                        .lineLimit(nil)
                                }
                            }

                            if downloader.isDownloading {
                                VStack(alignment: .leading, spacing: 6) {
                                    ProgressView(value: downloader.progress, total: 1.0)
                                        .progressViewStyle(.linear)

                                    HStack {
                                        Text(downloader.statusMessage)
                                            .font(.system(size: 11, weight: .medium))
                                            .foregroundColor(.accentColor)
                                        Spacer()
                                        Button(UiStrings.isKo ? "취소" : "Cancel") {
                                            downloader.cancelDownload()
                                        }
                                        .font(.system(size: 11))
                                        .foregroundColor(.red)
                                    }
                                }
                                .padding(.top, 4)
                            } else {
                                HStack(spacing: 10) {
                                    Button(action: {
                                        downloader.startDownload()
                                    }) {
                                        HStack(spacing: 6) {
                                            Image(systemName: "arrow.down.circle.fill")
                                            Text(UiStrings.isKo ? "🚀 온디바이스 AI 모델 다운로드" : "🚀 Download On-Device AI Model")
                                        }
                                        .font(.system(size: 11, weight: .bold))
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(Color.accentColor)
                                        .foregroundColor(.white)
                                        .cornerRadius(8)
                                    }

                                    Button(action: {
                                        engine.detectAndInitOnDeviceModel()
                                    }) {
                                        HStack(spacing: 4) {
                                            Image(systemName: "arrow.clockwise")
                                            Text(UiStrings.isKo ? "로컬 감지 새로고침" : "Refresh Local")
                                        }
                                        .font(.system(size: 11))
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 8)
                                        .background(Color.primary.opacity(0.06))
                                        .foregroundColor(.primary)
                                        .cornerRadius(8)
                                    }
                                }
                                .padding(.top, 2)
                            }

                            if let error = downloader.errorMessage {
                                Text(error)
                                    .font(.system(size: 10))
                                    .foregroundColor(.red)
                            }
                        }
                        .padding(14)
                        .background(Color.primary.opacity(0.04))
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.accentColor.opacity(0.3), lineWidth: 1)
                        )
                        .padding(.horizontal)
                    }

                    // Tone selector pills
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(toneManager.allTones) { tone in
                                toneButton(for: tone)
                            }
                        }
                        .padding(.horizontal)
                    }

                    // Input Text Area
                    VStack(alignment: .leading, spacing: 8) {
                        Text(UiStrings.originalBadge)
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.secondary)

                        TextEditor(text: $inputText)
                            .frame(minHeight: 120)
                            .padding(8)
                            .background(Color.primary.opacity(0.04))
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )
                    }
                    .padding(.horizontal)

                    // Action Button
                    Button(action: runInference) {
                        HStack {
                            if engine.isProcessing {
                                ProgressView()
                                    .padding(.trailing, 4)
                            }
                            Image(systemName: "wand.and.stars")
                            Text(engine.isProcessing ? UiStrings.analyzing : UiStrings.refineButton)
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Color.gray : Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || engine.isProcessing)
                    .padding(.horizontal)

                    // Output Text Area
                    if !outputText.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(UiStrings.aiBadge)
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.accentColor)
                                Spacer()
                                Button(action: copyToClipboard) {
                                    HStack(spacing: 4) {
                                        Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                                        Text(isCopied ? UiStrings.copied : UiStrings.copyResult)
                                            .font(.caption)
                                    }
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(Color.gray.opacity(0.15))
                                    .cornerRadius(8)
                                }
                            }

                            Text(outputText)
                                .font(.body)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.primary.opacity(0.06))
                                .cornerRadius(12)
                        }
                        .padding(.horizontal)
                    }

                    if let err = errorMessage {
                        Text(err)
                            .font(.caption)
                            .foregroundColor(.orange)
                            .padding(.horizontal)
                    }
                }
                .padding(.vertical)
            }
            .navigationTitle(UiStrings.sandboxTitle)
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
        }
    }

    private func toneButton(for tone: CustomTone) -> some View {
        let isSelected = (selectedToneId == tone.id)
        return Button(action: {
            selectedToneId = tone.id
        }) {
            Text(tone.title)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.accentColor : Color.gray.opacity(0.15))
                .foregroundColor(isSelected ? .white : .primary)
                .clipShape(Capsule())
        }
    }

    private var statusBadge: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(engine.isModelLoaded ? Color.green : Color.gray)
                .frame(width: 8, height: 8)
            Text(engine.isModelLoaded ? "Gemma Ready" : "Fallback")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.gray.opacity(0.12))
        .clipShape(Capsule())
    }

    private func runInference() {
        guard !inputText.isEmpty else { return }
        errorMessage = nil
        let selectedTone = toneManager.allTones.first { $0.id == selectedToneId }
        Task {
            let result = await engine.process(text: inputText, tone: selectedTone?.instruction)
            await MainActor.run {
                self.outputText = result.text
                if case .error(_, let err) = result {
                    self.errorMessage = err
                }
            }
        }
    }

    private func copyToClipboard() {
        #if canImport(UIKit)
        UIPasteboard.general.string = outputText
        isCopied = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isCopied = false
        }
        #elseif canImport(AppKit)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(outputText, forType: .string)
        isCopied = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isCopied = false
        }
        #endif
    }
}
