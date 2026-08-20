import SwiftUI

public struct IOSSettingsView: View {
    @StateObject private var toneManager = CustomToneManager.shared
    @StateObject private var engine = DearTalkIntentEngine.shared
    @StateObject private var downloader = ModelDownloader.shared

    @State private var newToneTitle: String = ""
    @State private var newToneInstruction: String = ""
    @State private var showingAddToneSheet: Bool = false

    public init() {}

    public var body: some View {
        Form {
            Section(header: Text(UiStrings.isKo ? "온디바이스 AI 모델 상태" : "On-Device AI Model Status")) {
                HStack {
                    Text(UiStrings.isKo ? "모델 로드 상태" : "Model Status")
                    Spacer()
                    Text(engine.isModelLoaded ? UiStrings.modelReady : UiStrings.modelNotLoaded)
                        .font(.subheadline)
                        .foregroundColor(engine.isModelLoaded ? .green : .secondary)
                }

                if let path = engine.loadedModelPath {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(UiStrings.isKo ? "로드된 모델 경로" : "Model Path")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(path)
                            .font(.caption2)
                            .foregroundColor(.primary)
                    }
                }
            }

            Section(header: Text(UiStrings.isKo ? "기본 톤앤매너" : "Default Tones")) {
                ForEach(toneManager.systemTones) { tone in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(tone.title)
                            .font(.headline)
                        Text(tone.instruction)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 2)
                }
            }

            Section(header: Text(UiStrings.isKo ? "사용자 커스텀 톤앤매너" : "Custom Tones")) {
                ForEach(toneManager.customTones) { tone in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(tone.title)
                            .font(.headline)
                        Text(tone.instruction)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 2)
                }
                .onDelete { indexSet in
                    for index in indexSet {
                        let tone = toneManager.customTones[index]
                        toneManager.removeTone(id: tone.id)
                    }
                }

                Button(action: { showingAddToneSheet = true }) {
                    Label(UiStrings.isKo ? "새로운 커스텀 톤 추가" : "Add Custom Tone", systemImage: "plus.circle")
                }
            }
        }
        .navigationTitle(UiStrings.isKo ? "환경설정" : "Settings")
        .sheet(isPresented: $showingAddToneSheet) {
            NavigationStack {
                Form {
                    Section(header: Text(UiStrings.isKo ? "톤 이름" : "Tone Name")) {
                        TextField(UiStrings.isKo ? "예: 정중한 교수님 톤" : "e.g., Courteous Professor", text: $newToneTitle)
                    }
                    Section(header: Text(UiStrings.isKo ? "프롬프트 지시사항" : "Prompt Instruction")) {
                        TextEditor(text: $newToneInstruction)
                            .frame(minHeight: 100)
                    }
                }
                .navigationTitle(UiStrings.isKo ? "커스텀 톤 생성" : "Create Custom Tone")
                #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
                #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(UiStrings.isKo ? "취소" : "Cancel") {
                            showingAddToneSheet = false
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(UiStrings.isKo ? "추가" : "Add") {
                            toneManager.addTone(title: newToneTitle, instruction: newToneInstruction)
                            newToneTitle = ""
                            newToneInstruction = ""
                            showingAddToneSheet = false
                        }
                        .disabled(newToneTitle.isEmpty || newToneInstruction.isEmpty)
                    }
                }
            }
        }
    }
}
