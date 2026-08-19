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
                instruction: UiStrings.toneRefineInstruction,
                icon: "✨"
            ),
            CustomTone(
                id: "tone_polite",
                name: UiStrings.tonePolite,
                instruction: UiStrings.tonePoliteInstruction,
                icon: "👔"
            ),
            CustomTone(
                id: "tone_casual",
                name: UiStrings.toneCasual,
                instruction: UiStrings.toneCasualInstruction,
                icon: "😊"
            ),
            CustomTone(
                id: "tone_business",
                name: UiStrings.toneBusiness,
                instruction: UiStrings.toneBusinessInstruction,
                icon: "💼"
            ),
            CustomTone(
                id: "tone_funny",
                name: UiStrings.toneFunny,
                instruction: UiStrings.toneFunnyInstruction,
                icon: "🤣"
            ),
            CustomTone(
                id: "tone_cheeky",
                name: UiStrings.toneCheeky,
                instruction: UiStrings.toneCheekyInstruction,
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
