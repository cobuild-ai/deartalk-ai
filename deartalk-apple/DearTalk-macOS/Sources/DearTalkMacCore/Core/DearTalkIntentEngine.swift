import Foundation
import OSLog

public enum IntentResult: Equatable {
    case success(text: String, message: String)
    case error(fallbackText: String, error: String)

    public var text: String {
        switch self {
        case .success(let text, _): return text
        case .error(let fallbackText, _): return fallbackText
        }
    }
}

/// 100% On-Device AI Neural Inference Engine (macOS)
/// [GEMINI.md Core Principles Adherence]
/// - Zero fake heuristics (no contains, replace, regex mock sentences).
/// - 100% local neural network inference; honestly preserves original text if model is not loaded.
public final class DearTalkIntentEngine: ObservableObject {
    @Published public private(set) var isModelLoaded: Bool = false
    @Published public private(set) var isProcessing: Bool = false
    @Published public private(set) var loadedModelPath: String? = nil
    @Published public private(set) var detectedModelName: String? = nil

    private var localServerProcess: Process?
    public static let shared = DearTalkIntentEngine()

    public init() {
        DearTalkLogger.info("🔒 DearTalkIntentEngine initialized: 100% on-device neural inference mode", category: "Engine")
        detectAndInitOnDeviceModel()
    }

    /// Discovers and initializes local on-device model files and inference services
    public func detectAndInitOnDeviceModel() {
        let fileManager = FileManager.default
        let homeDir = fileManager.homeDirectoryForCurrentUser.path

        var candidatePaths: [String] = []

        // Priority 1: SkyBrain Shared Models directory (~/.skybrain/models/)
        candidatePaths.append("\(homeDir)/.skybrain/models/gemma-4-E4B-it-Q4_K_M.gguf")
        candidatePaths.append("\(homeDir)/.skybrain/models/gemma-2-2b-it.Q4_K_M.gguf")

        // Priority 2: Standalone .app bundle Contents/Resources/models/
        if let resourcePath = Bundle.main.resourcePath {
            candidatePaths.append("\(resourcePath)/models/model.gguf")
            candidatePaths.append("\(resourcePath)/models/gemma-2b-it.gguf")
            candidatePaths.append("\(resourcePath)/models/gemma-2b-it-q4.gguf")
            candidatePaths.append("\(resourcePath)/models/model.litertlm")
            candidatePaths.append("\(resourcePath)/model.gguf")
        }

        // Priority 3: macOS Standard Application Support directory
        candidatePaths.append("\(homeDir)/Library/Application Support/DearTalk/models/model.gguf")
        candidatePaths.append("\(homeDir)/Library/Application Support/DearTalk/models/gemma-2b-it.gguf")
        candidatePaths.append("\(homeDir)/Library/Application Support/DearTalk/models/model.litertlm")

        // Priority 4: User home .deartalk directory
        candidatePaths.append("\(homeDir)/.deartalk/models/model.gguf")
        candidatePaths.append("\(homeDir)/.deartalk/models/gemma-2b-it.gguf")
        candidatePaths.append("\(homeDir)/.deartalk/models/model.litertlm")

        for path in candidatePaths {
            if fileManager.fileExists(atPath: path) {
                if let attrs = try? fileManager.attributesOfItem(atPath: path),
                   let size = attrs[.size] as? Int64, size > 0 {
                    self.loadedModelPath = path
                    self.isModelLoaded = true
                    let isBundled = path.contains(".app/Contents/Resources")
                    DearTalkLogger.info("✅ \(isBundled ? "앱 내장 독립 번들" : "로컬") 온디바이스 모델 감지 완료: \(path) (\(size / 1024 / 1024) MB)", category: "Engine")
                    return
                }
            }
        }

        // 5순위: 로컬 루프백(127.0.0.1:8000 SkyBrain / 11434 Ollama) 온디바이스 LLM 서비스 탐색
        Task {
            if let detectedModel = await checkLocalLlmService() {
                await MainActor.run {
                    self.detectedModelName = detectedModel
                    self.loadedModelPath = "Local Metal GPU (\(detectedModel))"
                    self.isModelLoaded = true
                    DearTalkLogger.info("✅ 로컬 온디바이스 LLM 서비스 연동 완료: \(detectedModel)", category: "Engine")
                }
            } else {
                await MainActor.run {
                    self.isModelLoaded = false
                    DearTalkLogger.warning("ℹ️ 온디바이스 LLM 모델 준비 대기 중: 모델 미배치 상태 (원문 100% 보존 모드)", category: "Engine")
                }
            }
        }
    }

    private func checkPortRunning(port: Int, path: String = "/healthz") async -> Bool {
        guard let url = URL(string: "http://127.0.0.1:\(port)\(path)") else { return false }
        var request = URLRequest(url: url)
        request.timeoutInterval = 0.8
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse)?.statusCode == 200
        } catch {
            return false
        }
    }

    /// 로컬 온디바이스 루프백 서비스 (8000 SkyBrain / 11435 llama-server / 11434 Ollama) 응답 검사
    private func checkLocalLlmService() async -> String? {
        // 1. SkyBrain Universal Daemon (8000)
        if await checkPortRunning(port: 8000, path: "/healthz") {
            return "SkyBrain (Gemma 4 E4B / Metal GPU)"
        }

        // 2. llama-server (11435)
        if await checkPortRunning(port: 11435, path: "/health") {
            return "llama-server (Metal GPU)"
        }

        // 3. Ollama (11434)
        guard let url = URL(string: "http://127.0.0.1:11434/api/tags") else { return nil }
        var request = URLRequest(url: url)
        request.timeoutInterval = 1.0

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResp = response as? HTTPURLResponse, httpResp.statusCode == 200 else { return nil }

            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let models = json["models"] as? [[String: Any]], !models.isEmpty {
                let modelNames = models.compactMap { $0["name"] as? String }
                if let gemma = modelNames.first(where: { $0.lowercased().contains("gemma") }) {
                    return gemma
                }
                return modelNames.first
            }
        } catch {
            return nil
        }
        return nil
    }

    /// 기본 문맥 다듬기 / 오탈자 교정 추론
    public func process(
        textInput: String,
        contextText: String = ""
    ) async -> IntentResult {
        let trimmed = textInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return .error(fallbackText: "", error: "입력된 텍스트가 없습니다.")
        }

        await MainActor.run { isProcessing = true }
        defer { Task { @MainActor in self.isProcessing = false } }

        guard isModelLoaded else {
            return .success(text: trimmed, message: UiStrings.modelNotLoaded)
        }

        let prompt = buildRefinePrompt(input: trimmed)
        if let response = await executeOnDeviceInference(prompt: prompt) {
            return .success(text: response, message: UiStrings.suggestedText)
        }

        return .success(text: trimmed, message: UiStrings.modelNotLoaded)
    }

    /// 톤앤매너 지정 변환 추론
    public func processWithTone(
        textInput: String,
        tone: CustomTone,
        contextText: String = ""
    ) async -> IntentResult {
        let trimmed = textInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return .error(fallbackText: "", error: "입력된 텍스트가 없습니다.")
        }

        await MainActor.run { isProcessing = true }
        defer { Task { @MainActor in self.isProcessing = false } }

        guard isModelLoaded else {
            return .success(text: trimmed, message: UiStrings.modelNotLoaded)
        }

        let prompt = buildTonePrompt(input: trimmed, tone: tone)
        if let response = await executeOnDeviceInference(prompt: prompt) {
            return .success(text: response, message: "🔒 \(tone.icon) \(tone.name) 완료")
        }

        return .success(text: trimmed, message: UiStrings.modelNotLoaded)
    }

    /// 다국어 번역 추론
    public func processTranslation(
        textInput: String,
        target: TranslationTarget
    ) async -> IntentResult {
        let trimmed = textInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return .error(fallbackText: "", error: "입력된 텍스트가 없습니다.")
        }

        await MainActor.run { isProcessing = true }
        defer { Task { @MainActor in self.isProcessing = false } }

        guard isModelLoaded else {
            return .success(text: trimmed, message: UiStrings.modelNotLoaded)
        }

        let prompt = buildTranslationPrompt(input: trimmed, target: target)
        if let response = await executeOnDeviceInference(prompt: prompt) {
            return .success(text: response, message: "🔒 \(target.flag) \(target.name) 번역 완료")
        }

        return .success(text: trimmed, message: UiStrings.modelNotLoaded)
    }

    // MARK: - Prompt Builders (Gemma 온디바이스 표준 템플릿)

    private func buildRefinePrompt(input: String) -> String {
        return """
        당신은 한국어 '실시간 문장 교정 AI'입니다.
        사용자가 메신저나 문서에 작성 중인 원문의 오탈자와 맞춤법을 정확하게 교정하여 완성형 문장으로 출력하세요.

        [핵심 원칙]
        1. 원문 단어 보존: 원문에 포함된 주어('너', '나', '저' 등), 명사, 고유명사, 숫자 등 모든 단어와 내용을 절대로 생략하거나 삭제하지 마세요.
        2. 오탈자 및 맞춤법 교정: 오타, 띄어쓰기 오류, 어색한 조사 및 어미만 자연스럽게 바르게 고치세요.
        3. 챗봇 답변 금지: 사용자의 질문이나 말에 대답하지 말고, 원문 문장 자체만 바르게 고쳐서 출력하세요.
        4. 출력 형식: 부가 설명, 따옴표("), 마크다운 없이 오직 교정된 한 줄의 문장만 출력하세요.

        [교정 예시]
        원문: "너 점심 맛있거 먹었어?"
        교정: 너 점심 맛있게 먹었어?

        원문: "내일 3시 만날수있을까?"
        교정: 내일 3시에 만날 수 있을까?

        원문: "\(input)"
        """
    }

    private func buildTonePrompt(input: String, tone: CustomTone) -> String {
        return """
        당신은 한국어 '실시간 톤앤매너 변환 AI'입니다.
        원문의 의미와 단어를 온전히 보존하면서, 어조만 '\(tone.name)'(\(tone.instruction)) 스타일로 자연스럽게 변환하세요.

        [핵심 원칙]
        1. 문장 맨 앞이나 뒤에 불필요한 단어를 덧붙이지 말고, 원문 문장의 어미와 뉘앙스를 바르게 변환하세요.
        2. 챗봇 답변 금지: 원문이 질문이더라도 절대 대답하지 말고, 원문 문장 자체를 해당 톤으로 변환하세요.
        3. 출력 형식: 설명, 인사말, 따옴표 없이 오직 변환된 한 줄의 문장만 출력하세요.

        변환할 원문: "\(input)"
        """
    }

    private func buildTranslationPrompt(input: String, target: TranslationTarget) -> String {
        return """
        Translate the following text into \(target.targetLanguage).
        CRITICAL RULES:
        1. You are a translator. Do NOT answer questions or converse with the user.
        2. Output ONLY the direct translated sentence in the target language.
        3. Do NOT include quotes, explanations, markdown, or greetings.

        Text: "\(input)"
        """
    }

    // MARK: - 100% 온디바이스 로컬 모델 추론 실행부

    private func executeOnDeviceInference(prompt: String) async -> String? {
        return await Task.detached(priority: .userInitiated) {
            guard let rawOutput = await self.runLocalModelInference(prompt: prompt) else {
                return nil
            }
            return self.cleanLlmOutput(rawOutput)
        }.value
    }

    private nonisolated func runLocalModelInference(prompt: String) async -> String? {
        // 1. SkyBrain Universal Daemon (8000 / OpenAI Compatible API)
        if let output = await querySkyBrainChatCompletion(prompt: prompt) {
            return output
        }

        // 2. 11435 포트 (로컬 llama-server Metal GPU 추론)
        if let output = await queryLlamaServerCompletion(prompt: prompt) {
            return output
        }

        // 3. 11434 포트 (로컬 Ollama API 추론)
        if let output = await queryOllamaGenerate(prompt: prompt) {
            return output
        }

        return nil
    }

    private nonisolated func querySkyBrainChatCompletion(prompt: String) async -> String? {
        guard let url = URL(string: "http://127.0.0.1:8000/v1/chat/completions") else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 10.0

        let payload: [String: Any] = [
            "model": "gemma-4-e4b",
            "messages": [
                ["role": "user", "content": prompt]
            ],
            "temperature": 0.2,
            "max_tokens": 512
        ]

        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return nil }
        request.httpBody = body

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResp = response as? HTTPURLResponse, httpResp.statusCode == 200 else { return nil }
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let choices = json["choices"] as? [[String: Any]],
               let first = choices.first,
               let message = first["message"] as? [String: Any],
               let content = message["content"] as? String {
                return content
            }
        } catch {
            return nil
        }
        return nil
    }

    private nonisolated func queryLlamaServerCompletion(prompt: String) async -> String? {
        guard let url = URL(string: "http://127.0.0.1:11435/completion") else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 6.0

        let payload: [String: Any] = [
            "prompt": prompt,
            "n_predict": 64,
            "temperature": 0.2,
            "stop": ["<end_of_turn>", "\n\n"]
        ]

        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return nil }
        request.httpBody = body

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResp = response as? HTTPURLResponse, httpResp.statusCode == 200 else { return nil }
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let content = json["content"] as? String {
                return content
            }
        } catch {
            return nil
        }
        return nil
    }

    private nonisolated func queryOllamaGenerate(prompt: String) async -> String? {
        guard let url = URL(string: "http://127.0.0.1:11434/api/generate") else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 6.0

        let payload: [String: Any] = [
            "model": "gemma2:2b",
            "prompt": prompt,
            "stream": false,
            "options": ["temperature": 0.2]
        ]

        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return nil }
        request.httpBody = body

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResp = response as? HTTPURLResponse, httpResp.statusCode == 200 else { return nil }
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let responseText = json["response"] as? String {
                return responseText
            }
        } catch {
            return nil
        }
        return nil
    }

    public nonisolated func cleanLlmOutput(_ raw: String) -> String {
        var text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.contains("<start_of_turn>") {
            text = text.replacingOccurrences(of: "<start_of_turn>model\n", with: "")
            text = text.replacingOccurrences(of: "<start_of_turn>model", with: "")
            text = text.replacingOccurrences(of: "<start_of_turn>user\n", with: "")
            text = text.replacingOccurrences(of: "<start_of_turn>user", with: "")
            text = text.replacingOccurrences(of: "<end_of_turn>", with: "")
            text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if text.hasPrefix("\"") && text.hasSuffix("\"") && text.count >= 2 {
            text = String(text.dropFirst().dropLast())
        }
        if text.hasPrefix("최종 문장: ") { text = String(text.dropFirst(7)) }
        if text.hasPrefix("교정: ") { text = String(text.dropFirst(4)) }
        if text.hasPrefix("변환: ") { text = String(text.dropFirst(4)) }
        if text.hasPrefix("출력: ") { text = String(text.dropFirst(4)) }
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
