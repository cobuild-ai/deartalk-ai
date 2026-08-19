import Foundation

public enum AiModeType: String, Codable {
    case defaultMode = "DEFAULT"
    case customTone = "CUSTOM_TONE"
    case translation = "TRANSLATION"
}

public struct CustomTone: Identifiable, Hashable, Codable {
    public let id: String
    public var name: String
    public var instruction: String
    public var icon: String

    public init(id: String, name: String, instruction: String, icon: String) {
        self.id = id
        self.name = name
        self.instruction = instruction
        self.icon = icon
    }
}

public struct TranslationTarget: Identifiable, Hashable, Codable {
    public let id: String
    public var name: String
    public var targetLanguage: String
    public var flag: String

    public init(id: String, name: String, targetLanguage: String, flag: String) {
        self.id = id
        self.name = name
        self.targetLanguage = targetLanguage
        self.flag = flag
    }
}

public struct AiModeItem: Identifiable, Hashable {
    public let id: String
    public let name: String
    public let icon: String
    public let type: AiModeType

    public init(id: String, name: String, icon: String, type: AiModeType) {
        self.id = id
        self.name = name
        self.icon = icon
        self.type = type
    }
}

public final class CustomToneManager: ObservableObject {
    public static let shared = CustomToneManager()

    @Published public var selectedToneId: String = "tone_refine"
    @Published public var selectedTranslationId: String = "trans_en"
    @Published public var isTranslationMode: Bool = false
    @Published public var isTabCompletionEnabled: Bool {
        didSet {
            UserDefaults.standard.set(isTabCompletionEnabled, forKey: "deartalk_tab_completion_enabled")
        }
    }

    public init() {
        if UserDefaults.standard.object(forKey: "deartalk_tab_completion_enabled") == nil {
            self.isTabCompletionEnabled = true
        } else {
            self.isTabCompletionEnabled = UserDefaults.standard.bool(forKey: "deartalk_tab_completion_enabled")
        }
    }

    public var defaultTones: [CustomTone] {
        [
            CustomTone(
                id: "tone_refine",
                name: UiStrings.toneRefine,
                instruction: "문맥을 살려 중복과 어색한 끊김 없이 자연스럽고 유려한 완성형 문장으로 다듬어 작성하세요.",
                icon: "✨"
            ),
            CustomTone(
                id: "tone_polite",
                name: UiStrings.tonePolite,
                instruction: "상대방에게 정중하고 예의 바른 비즈니스 경어체로 다듬어 완성형 문장 하나로 작성하세요.",
                icon: "👔"
            ),
            CustomTone(
                id: "tone_casual",
                name: UiStrings.toneCasual,
                instruction: "친구에게 대화하듯 부드럽고 친근한 톤으로 자연스러운 완성형 문장 하나로 작성하세요.",
                icon: "😊"
            ),
            CustomTone(
                id: "tone_business",
                name: UiStrings.toneBusiness,
                instruction: "격식 있는 이메일/업무 메신저에 어울리는 명확하고 신뢰감 있는 문장으로 작성하세요.",
                icon: "💼"
            ),
            CustomTone(
                id: "tone_funny",
                name: UiStrings.toneFunny,
                instruction: "재치 있고 위트와 유머가 넘치며 빵 터지는 센스 있는 유쾌한 어조로 작성하세요.",
                icon: "🤣"
            ),
            CustomTone(
                id: "tone_cheeky",
                name: UiStrings.toneCheeky,
                instruction: "자신만만하고 쿨하며 살짝 얄밉고 거만하지만 밉지 않은 도도한 반말 어조로 작성하세요.",
                icon: "😼"
            )
        ]
    }

    public var defaultTranslations: [TranslationTarget] {
        [
            TranslationTarget(id: "trans_en", name: UiStrings.langEnglish, targetLanguage: "영어(English)", flag: "🇺🇸"),
            TranslationTarget(id: "trans_id", name: UiStrings.langIndonesian, targetLanguage: "인도네시아어(Bahasa Indonesia)", flag: "🇮🇩"),
            TranslationTarget(id: "trans_ja", name: UiStrings.langJapanese, targetLanguage: "일본어(日本語)", flag: "🇯🇵"),
            TranslationTarget(id: "trans_zh", name: UiStrings.langChinese, targetLanguage: "중국어(中文)", flag: "🇨🇳"),
            TranslationTarget(id: "trans_es", name: UiStrings.langSpanish, targetLanguage: "스페인어(Español)", flag: "🇪🇸"),
            TranslationTarget(id: "trans_fr", name: UiStrings.langFrench, targetLanguage: "프랑스어(Français)", flag: "🇫🇷"),
            TranslationTarget(id: "trans_de", name: UiStrings.langGerman, targetLanguage: "독일어(Deutsch)", flag: "🇩🇪"),
            TranslationTarget(id: "trans_vi", name: UiStrings.langVietnamese, targetLanguage: "베트남어(Tiếng Việt)", flag: "🇻🇳")
        ]
    }

    public var currentTone: CustomTone {
        defaultTones.first { $0.id == selectedToneId } ?? defaultTones[0]
    }

    public var currentTranslation: TranslationTarget {
        defaultTranslations.first { $0.id == selectedTranslationId } ?? defaultTranslations[0]
    }
}
