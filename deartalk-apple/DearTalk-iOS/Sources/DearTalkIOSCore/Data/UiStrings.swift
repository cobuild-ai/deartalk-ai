import Foundation

/// Centralized multilingual UI string manager for iOS
public enum UiStrings {
    public static var isKo: Bool {
        if let preferred = Locale.preferredLanguages.first, preferred.lowercased().hasPrefix("ko") {
            return true
        }
        let currentCode = Locale.current.language.languageCode?.identifier ?? Locale.current.identifier
        return currentCode.lowercased().hasPrefix("ko")
    }

    // MARK: - App
    public static var appName: String { "DearTalk AI" }
    public static var appSubtitle: String { isKo ? "100% 온디바이스 AI 글쓰기 비서" : "100% On-Device AI Writing Assistant" }

    // MARK: - UI & Sandbox
    public static var sandboxTitle: String { isKo ? "🧪 AI 글쓰기 샌드박스" : "🧪 AI Writing Sandbox" }
    public static var inputPlaceholder: String { isKo ? "여기에 다듬을 문장을 입력하세요..." : "Type text to refine here..." }
    public static var refineButton: String { isKo ? "✨ 온디바이스 AI로 다듬기" : "✨ Refine with On-Device AI" }
    public static var copyResult: String { isKo ? "결과 복사" : "Copy Result" }
    public static var copied: String { isKo ? "복사 완료!" : "Copied!" }
    public static var originalBadge: String { isKo ? "원문" : "Original" }
    public static var aiBadge: String { isKo ? "AI 제안" : "AI Suggestion" }
    public static var analyzing: String { isKo ? "🔒 온디바이스 Gemma AI가 분석 중..." : "🔒 On-device Gemma AI analyzing..." }
    public static var modelReady: String { isKo ? "🟢 온디바이스 LLM 준비 완료" : "🟢 On-Device LLM Ready" }
    public static var modelNotLoaded: String { isKo ? "⚪ 온디바이스 모델 미로드 (원문 보존 모드)" : "⚪ Model Not Loaded (Fallback Mode)" }

    // MARK: - Tones (톤앤매너)
    public static var toneRefine: String { isKo ? "기본다듬기" : "Refine" }
    public static var toneRefineInstruction: String {
        isKo ? "문맥을 살려 중복과 어색한 끊김 없이 자연스럽고 유려한 완성형 문장으로 다듬어 작성하세요."
             : "Refine and polish the sentence naturally with clear context, fixing repetitions and awkward phrasing."
    }

    public static var tonePolite: String { isKo ? "공손하게" : "Polite" }
    public static var tonePoliteInstruction: String {
        isKo ? "상대방에게 정중하고 예의 바른 비즈니스 경어체로 다듬어 완성형 문장 하나로 작성하세요."
             : "Rewrite politely with formal and respectful tone suitable for courteous communication."
    }

    public static var toneCasual: String { isKo ? "친근하게" : "Casual" }
    public static var toneCasualInstruction: String {
        isKo ? "친구에게 대화하듯 부드럽고 친근한 톤으로 자연스러운 완성형 문장 하나로 작성하세요."
             : "Rewrite in a friendly, warm, and casual conversational tone."
    }

    public static var toneBusiness: String { isKo ? "비즈니스" : "Business" }
    public static var toneBusinessInstruction: String {
        isKo ? "격식 있는 이메일/업무 메신저에 어울리는 명확하고 신뢰감 있는 문장으로 작성하세요."
             : "Refine into a professional, concise, and trustworthy tone for business emails and workplace messages."
    }

    public static var toneFunny: String { isKo ? "재미있게" : "Funny" }
    public static var toneFunnyInstruction: String {
        isKo ? "재치 있고 위트와 유머가 넘치며 빵 터지는 센스 있는 유쾌한 어조로 작성하세요."
             : "Rewrite in a humorous, witty, and fun tone full of positive energy."
    }

    public static var toneCheeky: String { isKo ? "건방지게" : "Cheeky" }
    public static var toneCheekyInstruction: String {
        isKo ? "자신만만하고 쿨하며 살짝 얄밉고 거만하지만 밉지 않은 도도한 반말 어조로 작성하세요."
             : "Rewrite in a confident, sassy, and playfully cheeky tone."
    }

    // MARK: - Translations (번역 대상)
    public static var langEnglish: String { isKo ? "영어" : "English" }
    public static var langIndonesian: String { isKo ? "인도네시아어" : "Indonesian" }
    public static var langJapanese: String { isKo ? "일본어" : "Japanese" }
    public static var langChinese: String { isKo ? "중국어" : "Chinese" }
    public static var langSpanish: String { isKo ? "스페인어" : "Spanish" }
    public static var langFrench: String { isKo ? "프랑스어" : "French" }
    public static var langGerman: String { isKo ? "독일어" : "German" }
    public static var langVietnamese: String { isKo ? "베트남어" : "Vietnamese" }

    // MARK: - About & Help
    public static var settingsTabGeneral: String { isKo ? "⚙️ 일반 설정" : "⚙️ General" }
    public static var settingsTabAbout: String { isKo ? "ℹ️ 정보 및 도움말" : "ℹ️ About & Help" }
    public static var appVersionLabel: String { isKo ? "앱 버전" : "App Version" }
    public static var buildTimestampLabel: String { isKo ? "빌드 타임스탬프" : "Build Timestamp" }
    public static var userGuideTitle: String { isKo ? "📖 DearTalk AI 사용 설명서" : "📖 DearTalk AI User Guide" }
    public static var userGuideHowToUseTitle: String { isKo ? "💡 사용 방법" : "💡 How to Use" }
    public static var userGuideHowToUseContent: String {
        isKo ? "1. 앱 메인 화면에서 텍스트를 입력하거나 샌드박스를 사용합니다.\n2. 상단 마이크 버튼을 통해 온디바이스 AI 음성 인식을 시작할 수 있습니다.\n3. 키보드로 타이핑을 하거나 음성 입력을 진행한 뒤, 원하는 대화 톤이나 번역 타깃 언어를 선택하여 다듬기 결과를 확인합니다.\n4. 결과물의 복사 버튼을 눌러 손쉽게 복사해 사용할 수 있습니다."
             : "1. Type text on the main screen or try the playground sandbox.\n2. Tap the top mic button to start on-device AI speech-to-text recognition.\n3. After entering text, tap one of the tone or translation target buttons to see the refined AI suggestion.\n4. Copy the result with one tap using the copy button."
    }
}

