import Foundation

/// macOS용 다국어 UI 문자열 관리 싱글톤
/// 시스템 언어(Locale.current)를 감지하여 한국어(ko) 및 영어(default)를 지원합니다.
public enum UiStrings {
    private static var isKo: Bool {
        let lang = Locale.current.language.languageCode?.identifier ?? "en"
        return lang.hasPrefix("ko")
    }

    // MARK: - App & MenuBar
    public static var appName: String { "DearTalk AI" }
    public static var menuBarTitle: String { isKo ? "DearTalk AI 실시간 글쓰기 비서" : "DearTalk AI Writing Assistant" }
    public static var enabled: String { isKo ? "실시간 DIFF 보조 켜기" : "Enable Real-time DIFF" }
    public static var disabled: String { isKo ? "실시간 DIFF 보조 끄기" : "Disable Real-time DIFF" }
    public static var statusActive: String { isKo ? "🟢 실시간 감지 중" : "🟢 Active & Monitoring" }
    public static var statusPaused: String { isKo ? "⏸️ 일시 정지됨" : "⏸️ Paused" }
    public static var openSandbox: String { isKo ? "🧪 실시간 테스트 샌드박스 열기" : "🧪 Open Sandbox Playground" }
    public static var settings: String { isKo ? "⚙️ 환경설정..." : "⚙️ Settings..." }
    public static var accessibilityPermission: String { isKo ? "🔑 손쉬운 사용(Accessibility) 권한 필요" : "🔑 Accessibility Permission Required" }
    public static var grantPermission: String { isKo ? "권한 설정 열기" : "Open Permission Settings" }
    public static var quit: String { isKo ? "종료" : "Quit DearTalk AI" }

    // MARK: - Floating Diff Overlay
    public static var diffTitle: String { isKo ? "✨ 온디바이스 AI 제안" : "✨ On-Device AI Suggestion" }
    public static var applySuggestionShortcut: String { isKo ? "Tab 키를 눌러 교체" : "Press Tab to Apply" }
    public static var dismissShortcut: String { isKo ? "Esc 키로 닫기" : "Esc to Dismiss" }
    public static var copySuggestion: String { isKo ? "복사" : "Copy" }
    public static var applyNow: String { isKo ? "적용" : "Apply" }
    public static var analyzing: String { isKo ? "🔒 온디바이스 AI가 분석 중..." : "🔒 On-device AI analyzing..." }
    public static var originalText: String { isKo ? "작성 중 원문" : "Original" }
    public static var suggestedText: String { isKo ? "AI 다듬기" : "AI Refined" }
    public static var noDiffFound: String { isKo ? "완벽한 문장입니다!" : "Looks great as is!" }

    // MARK: - Tones (톤앤매너)
    public static var toneRefine: String { isKo ? "기본다듬기" : "Refine" }
    public static var tonePolite: String { isKo ? "공손하게" : "Polite" }
    public static var toneCasual: String { isKo ? "친근하게" : "Casual" }
    public static var toneBusiness: String { isKo ? "비즈니스" : "Business" }
    public static var toneFunny: String { isKo ? "재미있게" : "Funny" }
    public static var toneCheeky: String { isKo ? "건방지게" : "Cheeky" }

    // MARK: - Translations (번역 대상)
    public static var langEnglish: String { isKo ? "영어" : "English" }
    public static var langIndonesian: String { isKo ? "인도네시아어" : "Indonesian" }
    public static var langJapanese: String { isKo ? "일본어" : "Japanese" }
    public static var langChinese: String { isKo ? "중국어" : "Chinese" }
    public static var langSpanish: String { isKo ? "스페인어" : "Spanish" }
    public static var langFrench: String { isKo ? "프랑스어" : "French" }
    public static var langGerman: String { isKo ? "독일어" : "German" }
    public static var langVietnamese: String { isKo ? "베트남어" : "Vietnamese" }

    // MARK: - Sandbox View
    public static var sandboxTitle: String { isKo ? "DearTalk AI 실시간 DIFF 샌드박스" : "DearTalk AI Real-time DIFF Sandbox" }
    public static var sandboxPlaceholder: String { isKo ? "여기에 텍스트를 입력하면 잠시 후 실시간으로 AI가 문맥을 분석하여 DIFF를 생성합니다..." : "Type here to see real-time on-device AI diff suggestions as you pause..." }
    public static var testPresets: String { isKo ? "💡 원클릭 테스트 프리셋" : "💡 One-Click Test Presets" }
    public static var currentTone: String { isKo ? "적용할 톤:" : "Target Tone:" }
    public static var targetLanguage: String { isKo ? "번역 언어:" : "Target Language:" }
    public static var diffLegendAdded: String { isKo ? "추가/개선된 표현" : "Added / Improved" }
    public static var diffLegendRemoved: String { isKo ? "제거/어색한 표현" : "Removed / Awkward" }
    public static var debounceSensitivity: String { isKo ? "반응 대기 시간:" : "Debounce Delay:" }
    public static var privacyNotice: String { isKo ? "🔒 100% 온디바이스 추론 · 외부 네트워크 통신 0% · 데이터 미수집" : "🔒 100% On-Device · Zero Network · No Data Collected" }
    public static var modelNotLoaded: String { isKo ? "온디바이스 LLM 모델 준비 중 (원문 보존)" : "On-device LLM model preparing (original preserved)" }
    public static var modelLoadedNotice: String { isKo ? "온디바이스 LLM 실시간 신경망 추론 완료" : "On-device LLM neural inference complete" }
}

