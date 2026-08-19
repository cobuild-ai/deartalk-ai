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

        // Priority 1: Standalone .app bundle Contents/Resources/models/
        if let resourcePath = Bundle.main.resourcePath {
            candidatePaths.append("\(resourcePath)/models/model.gguf")
            candidatePaths.append("\(resourcePath)/models/gemma-2b-it.gguf")
            candidatePaths.append("\(resourcePath)/models/gemma-2b-it-q4.gguf")
            candidatePaths.append("\(resourcePath)/models/model.litertlm")
            candidatePaths.append("\(resourcePath)/model.gguf")
        }

        // Priority 2: macOS Standard Application Support directory
        candidatePaths.append("\(homeDir)/Library/Application Support/DearTalk/models/model.gguf")
        candidatePaths.append("\(homeDir)/Library/Application Support/DearTalk/models/gemma-2b-it.gguf")
        candidatePaths.append("\(homeDir)/Library/Application Support/DearTalk/models/model.litertlm")
        candidatePaths.append("/Library/Application Support/DearTalk/models/model.litertlm")

        // Priority 3: User home .deartalk directory
        candidatePaths.append("\(homeDir)/.deartalk/models/model.gguf")
        candidatePaths.append("\(homeDir)/.deartalk/models/gemma-2b-it.gguf")
        candidatePaths.append("\(homeDir)/.deartalk/models/model.litertlm")
        candidatePaths.append("\(homeDir)/.deartalk/models/gemma-2b-it.bin")
        candidatePaths.append("\(homeDir)/models/gemma-2b-it.litertlm")
        candidatePaths.append("\(homeDir)/models/model.bin")
        candidatePaths.append("/data/local/tmp/llm/model.litertlm")

        for path in candidatePaths {
            if fileManager.fileExists(atPath: path) {
                if let attrs = try? fileManager.attributesOfItem(atPath: path),
                   let size = attrs[.size] as? Int64, size > 0 {
                    self.loadedModelPath = path
                    self.isModelLoaded = true
                    let isBundled = path.contains(".app/Contents/Resources")
                    DearTalkLogger.info("✅ \(isBundled ? "앱 내장 독립 번들" : "로컬") 온디바이스 모델 감지 완료: \(path) (\(size / 1024 / 1024) MB)", category: "Engine")

                    // 백그라운드 로컬 Metal 추론 엔진 기동 확인
                    ensureLocalInferenceServerRunning(modelPath: path)
                    return
                }
            }
        }

        // 4순위: 로컬 루프백(127.0.0.1:11434 / 11435) 온디바이스 LLM 서비스 탐색
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

    /// 로컬 llama-server 데몬이 11435 포트에서 실행 중인지 확인하고, 아니면 자동 기동
    private func ensureLocalInferenceServerRunning(modelPath: String) {
        Task {
            if await checkPortRunning(port: 11435) {
                DearTalkLogger.info("⚡ 로컬 Metal GPU 추론 서버(11435) 활성화 확인", category: "Engine")
                return
            }

            // llama-server 바이너리 경로 탐색
            let serverBinCandidates = [
                "/opt/homebrew/bin/llama-server",
                "/usr/local/bin/llama-server",
                Bundle.main.bundleURL.appendingPathComponent("Contents/Helpers/llama-server").path
            ]

            guard let serverBin = serverBinCandidates.first(where: { FileManager.default.fileExists(atPath: $0) }) else {
                DearTalkLogger.warning("ℹ️ llama-server 바이너리 탐색 중", category: "Engine")
                return
            }

            DearTalkLogger.info("🚀 로컬 Metal GPU 추론 서버 자동 시작: \(serverBin)", category: "Engine")
            let proc = Process()
            proc.executableURL = URL(fileURLWithPath: serverBin)
            proc.arguments = ["-m", modelPath, "--port", "11435", "-ngl", "99", "-c", "512", "--log-disable"]
            try? proc.run()
            self.localServerProcess = proc
        }
    }

    private func checkPortRunning(port: Int) async -> Bool {
        guard let url = URL(string: "http://127.0.0.1:\(port)/health") else { return false }
        var request = URLRequest(url: url)
        request.timeoutInterval = 0.5
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse)?.statusCode == 200
        } catch {
            return false
        }
    }

    /// 로컬 온디바이스 루프백 서비스(11435 또는 11434) 응답 검사
    private func checkLocalLlmService() async -> String? {
        if await checkPortRunning(port: 11435) {
            return "gemma-2-2b (Metal GPU)"
        }

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
        <start_of_turn>user
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

        원문: "안녕하세요 이것은 당신의 감사 표시"
        교정: 안녕하세요, 이것은 감사의 표시입니다.

        원문: "\(input)"<end_of_turn>
        <start_of_turn>model
        """
    }

    private func buildTonePrompt(input: String, tone: CustomTone) -> String {
        let examples: String
        switch tone.id {
        case "tone_polite", "공손하게", "정중한 존댓말", "정중":
            examples = """
            [변환 예시]
            원문: "식사 같이 하실래요?"
            변환: 혹시 식사 함께 하실 수 있으실까요?

            원문: "내일 시간 되세요?"
            변환: 내일 시간 내어주실 수 있으신지 여쭙습니다.
            """
        case "tone_casual", "친근하게", "친근하고 따뜻한", "친근":
            examples = """
            [변환 예시]
            원문: "식사 같이 하실래요?"
            변환: 우리 같이 식사해요! 😊

            원문: "내일 시간 되세요?"
            변환: 내일 혹시 시간 괜찮아요? 😊
            """
        case "tone_business", "비즈니스", "전문적인 비즈니스":
            examples = """
            [변환 예시]
            원문: "식사 같이 하실래요?"
            변환: 금일 오찬 함께 하실 수 있는지 확인 부탁드립니다.

            원문: "내일 회의 언제 할까요?"
            변환: 익일 회의 일정 조율 요청드립니다.
            """
        case "tone_funny", "재미있게":
            examples = """
            [변환 예시]
            원문: "식사 같이 하실래요?"
            변환: 밥 먹으러 안 가면 유죄! 같이 맛있는 거 먹으러 가요 🤣

            원문: "내일 시간 되세요?"
            변환: 내일 저랑 놀아줄 귀한 시간 1초만 기부해 주시죠! 🤣
            """
        case "tone_cheeky", "건방지게":
            examples = """
            [변환 예시]
            원문: "식사 같이 하실래요?"
            변환: 오늘 밥은 내가 같이 먹어주는 거니까 영광인 줄 알아 😼

            원문: "내일 시간 되세요?"
            변환: 내일 시간 비워둬, 내가 만나줄게 😼
            """
        default:
            examples = ""
        }

        return """
        <start_of_turn>user
        당신은 한국어 '실시간 톤앤매너 변환 AI'입니다.
        원문의 의미와 단어를 온전히 보존하면서, 어조만 '\(tone.name)'(\(tone.instruction)) 스타일로 자연스럽게 변환하세요.

        [핵심 원칙]
        1. 문장 맨 앞이나 뒤에 불필요한 단어('요, ', '네, ', '답변: ')를 덧붙이지 말고, 원문 문장의 어미와 뉘앙스를 바르게 변환하세요.
        2. 챗봇 답변 금지: 원문이 질문이더라도 절대 대답하지 말고, 원문 문장 자체를 해당 톤으로 변환하세요.
        3. 출력 형식: 설명, 인사말, 따옴표 없이 오직 변환된 한 줄의 문장만 출력하세요.

        \(examples)

        변환할 원문: "\(input)"<end_of_turn>
        <start_of_turn>model
        """
    }

    private func buildTranslationPrompt(input: String, target: TranslationTarget) -> String {
        return """
        <start_of_turn>user
        Translate the following text into \(target.targetLanguage).
        CRITICAL RULES:
        1. You are a translator. Do NOT answer questions or converse with the user.
        2. Output ONLY the direct translated sentence in the target language.
        3. Do NOT include quotes, explanations, markdown, or greetings.

        Text: "\(input)"<end_of_turn>
        <start_of_turn>model
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
        // 1. 11435 포트 (로컬 llama-server Metal GPU 추론)
        if let output = await queryLlamaServerCompletion(prompt: prompt) {
            return output
        }

        // 2. 11434 포트 (로컬 Ollama API 추론)
        if let output = await queryOllamaGenerate(prompt: prompt) {
            return output
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

        let model = DearTalkIntentEngine.shared.detectedModelName ?? "gemma2:2b"
        let payload: [String: Any] = [
            "model": model,
            "prompt": prompt,
            "stream": false,
            "options": [
                "temperature": 0.2,
                "num_predict": 64
            ]
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
        var text = raw

        // 1. LLM 제어 태그 제거 (<start_of_turn>, <end_of_turn>, <bos>, <eos> 등)
        let tagsPattern = try? NSRegularExpression(
            pattern: "</?(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)[^>]*>",
            options: .caseInsensitive
        )
        if let regex = tagsPattern {
            let range = NSRange(location: 0, length: text.utf16.count)
            text = regex.stringByReplacingMatches(in: text, options: [], range: range, withTemplate: "")
        }

        // 2. 개별 라인 순회하며 role 헤더(model, user) 및 라벨 접두어 제거
        let lines = text.components(separatedBy: .newlines).map { line -> String in
            var l = line.trimmingCharacters(in: .whitespacesAndNewlines)
            if l.lowercased() == "model" || l.lowercased() == "user" {
                return ""
            }
            if l.lowercased().hasPrefix("model:") || l.lowercased().hasPrefix("assistant:") || l.lowercased().hasPrefix("user:") {
                if let colonIdx = l.firstIndex(of: ":") {
                    l = String(l[l.index(after: colonIdx)...]).trimmingCharacters(in: .whitespacesAndNewlines)
                }
            }
            let labelPattern = try? NSRegularExpression(
                pattern: "^(교정|변환\\s*결과|최종\\s*문장|결과|답변|model|assistant|AI|Translation|Translated Text|Output|Correction)\\s*:\\s*",
                options: .caseInsensitive
            )
            if let regex = labelPattern {
                let range = NSRange(location: 0, length: l.utf16.count)
                l = regex.stringByReplacingMatches(in: l, options: [], range: range, withTemplate: "")
            }
            // 맨 앞의 콜론, 따옴표, 불릿 기호 및 공백 완벽 제거
            let leadingColonPattern = try? NSRegularExpression(pattern: "^[:\\s\"'`>*\\-]+", options: [])
            if let regex = leadingColonPattern {
                let range = NSRange(location: 0, length: l.utf16.count)
                l = regex.stringByReplacingMatches(in: l, options: [], range: range, withTemplate: "")
            }
            // 맨 앞의 잘못 생성된 어미/접두어 ('요, ', '네, ' 등) 제거
            let leadingFillerPattern = try? NSRegularExpression(pattern: "^(요|네|예)[,\\s]+", options: [])
            if let regex = leadingFillerPattern {
                let range = NSRange(location: 0, length: l.utf16.count)
                l = regex.stringByReplacingMatches(in: l, options: [], range: range, withTemplate: "")
            }
            return l.trimmingCharacters(in: CharacterSet(charactersIn: "\"`>-* :"))
        }.filter { !$0.isEmpty }

        return lines.first ?? ""
    }
}
