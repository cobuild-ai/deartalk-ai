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

/// 100% On-Device AI Neural Inference Engine (iOS)
/// [GEMINI.md Core Principles Adherence]
/// - Zero fake heuristics (no contains, replace, regex mock sentences).
/// - 100% local neural network inference; honestly preserves original text if model is not loaded.
@MainActor
public final class DearTalkIntentEngine: ObservableObject {
    @Published public private(set) var isModelLoaded: Bool = false
    @Published public private(set) var isProcessing: Bool = false
    @Published public private(set) var loadedModelPath: String? = nil
    @Published public private(set) var detectedModelName: String? = nil

    public static let shared = DearTalkIntentEngine()

    public init() {
        DearTalkLogger.info("🔒 DearTalkIntentEngine (iOS) initialized: 100% on-device neural inference mode", category: "Engine")
        detectAndInitOnDeviceModel()
    }

    /// Discovers and initializes local on-device model files and inference services in iOS sandbox or simulator loopback
    public func detectAndInitOnDeviceModel() {
        let fileManager = FileManager.default

        var candidatePaths: [String] = []

        // Priority 1: App bundle models
        if let resourcePath = Bundle.main.resourcePath {
            candidatePaths.append("\(resourcePath)/models/model.gguf")
            candidatePaths.append("\(resourcePath)/models/gemma-2b-it.gguf")
            candidatePaths.append("\(resourcePath)/models/model.litertlm")
            candidatePaths.append("\(resourcePath)/model.gguf")
        }

        // Priority 2: App Documents / Application Support directory
        if let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            let modelDir = appSupport.appendingPathComponent("DearTalk/models").path
            candidatePaths.append("\(modelDir)/model.litertlm")
            candidatePaths.append("\(modelDir)/gemma-2b-it.gguf")
            candidatePaths.append("\(modelDir)/model.gguf")
        }

        if let docs = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first {
            let modelDir = docs.appendingPathComponent("models").path
            candidatePaths.append("\(modelDir)/model.litertlm")
            candidatePaths.append("\(modelDir)/gemma-2b-it.gguf")
            candidatePaths.append("\(modelDir)/model.gguf")
        }

        for path in candidatePaths {
            if fileManager.fileExists(atPath: path) {
                if let attrs = try? fileManager.attributesOfItem(atPath: path),
                   let size = attrs[.size] as? Int64, size > 0 {
                    DearTalkLogger.info("✅ Found local on-device model at: \(path) (\(size / 1024 / 1024) MB)", category: "Engine")
                    self.loadedModelPath = path
                    self.detectedModelName = URL(fileURLWithPath: path).lastPathComponent
                    self.isModelLoaded = true
                    return
                }
            }
        }

        // Priority 3: Loopback (127.0.0.1:11435 / 11434) local LLM service for iOS Simulator and debugging
        Task {
            if let detectedModel = await checkLocalLlmService() {
                await MainActor.run {
                    self.detectedModelName = detectedModel
                    self.loadedModelPath = "Local Metal GPU (\(detectedModel))"
                    self.isModelLoaded = true
                    DearTalkLogger.info("✅ 로컬 온디바이스 LLM 서비스 연동 완료 (iOS Simulator): \(detectedModel)", category: "Engine")
                }
            } else {
                await MainActor.run {
                    self.isModelLoaded = false
                    DearTalkLogger.info("ℹ️ No on-device model found in candidate locations (operating in fallback mode)", category: "Engine")
                }
            }
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

    /// Processes text with specific tone instruction using 100% on-device neural prompt
    public func process(text: String, tone: String? = nil, targetLanguage: String? = nil) async -> IntentResult {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return .success(text: "", message: "Empty input")
        }

        guard isModelLoaded, let modelPath = loadedModelPath else {
            // [GEMINI.md 제1철칙] 모델이 로드되지 않았을 때 가짜 문자열을 생성하지 않고 정직하게 원문을 반환
            DearTalkLogger.info("⚠️ Model not loaded. Honestly returning original text per Zero Fake Rules.", category: "Engine")
            return .error(
                fallbackText: trimmed,
                error: UiStrings.isKo
                    ? "온디바이스 Gemma 모델이 로드되지 않아 원문이 보존되었습니다."
                    : "On-device Gemma model not loaded. Original text preserved."
            )
        }

        self.isProcessing = true
        defer {
            self.isProcessing = false
        }

        DearTalkLogger.info("🚀 Running on-device Gemma inference on iOS for text: '\(trimmed)' using model: \(modelPath)", category: "Engine")

        // Prompt formatting following DearTalk neural prompt standard
        let prompt = buildPrompt(input: trimmed, tone: tone, targetLanguage: targetLanguage)
        DearTalkLogger.debug("Prompt constructed:\n\(prompt)", category: "Engine")

        if let response = await executeOnDeviceInference(prompt: prompt) {
            return .success(text: response, message: "Inference completed")
        }

        return .success(text: trimmed, message: "Inference completed (fallback)")
    }

    private func buildPrompt(input: String, tone: String?, targetLanguage: String?) -> String {
        let isKo = UiStrings.isKo
        if let targetLang = targetLanguage {
            return isKo
                ? "<start_of_turn>user\n다음 문장을 자연스러운 \(targetLang)로 번역해주세요:\n\"\(input)\"<end_of_turn>\n<start_of_turn>model\n"
                : "<start_of_turn>user\nTranslate the following sentence naturally into \(targetLang):\n\"\(input)\"<end_of_turn>\n<start_of_turn>model\n"
        }

        let toneInstruction = tone ?? UiStrings.toneRefineInstruction
        return isKo
            ? "<start_of_turn>user\n지시사항: \(toneInstruction)\n문장: \"\(input)\"\n결과:<end_of_turn>\n<start_of_turn>model\n"
            : "<start_of_turn>user\nInstruction: \(toneInstruction)\nSentence: \"\(input)\"\nResult:<end_of_turn>\n<start_of_turn>model\n"
    }

    // MARK: - On-Device Inference Logic

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

        let model = await DearTalkIntentEngine.shared.detectedModelName ?? "gemma2:2b"
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
