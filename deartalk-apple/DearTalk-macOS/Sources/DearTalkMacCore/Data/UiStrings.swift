import Foundation

/// Centralized multilingual UI string manager for macOS
/// Detects system locale (Korean vs English fallback) for consistent cross-platform behavior.
public enum UiStrings {
    public static var isKo: Bool {
        if let preferred = Locale.preferredLanguages.first, preferred.lowercased().hasPrefix("ko") {
            return true
        }
        let currentCode = Locale.current.language.languageCode?.identifier ?? Locale.current.identifier
        return currentCode.lowercased().hasPrefix("ko")
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
    public static var permissionNeeded: String { isKo ? "⚠️ 권한 필요" : "⚠️ Permission Required" }
    public static var grantPermission: String { isKo ? "권한 설정 열기" : "Open Permission Settings" }
    public static var quit: String { isKo ? "종료" : "Quit DearTalk AI" }

    // MARK: - Floating Diff Overlay
    public static var diffTitle: String { isKo ? "✨ 온디바이스 AI 제안" : "✨ On-Device AI Suggestion" }
    public static var applySuggestionShortcut: String { isKo ? "Tab 키를 눌러 교체" : "Press Tab to Apply" }
    public static var dismissShortcut: String { isKo ? "Esc 키로 닫기" : "Esc to Dismiss" }
    public static var copySuggestion: String { isKo ? "복사" : "Copy" }
    public static var copied: String { isKo ? "복사됨!" : "Copied!" }
    public static var regenerate: String { isKo ? "다시 생성" : "Regenerate" }
    public static var applyNow: String { isKo ? "적용하기" : "Apply" }
    public static var applyNowWithTab: String { isKo ? "적용하기 (Tab)" : "Apply (Tab)" }
    public static var tabApplyOn: String { isKo ? "Tab 적용 ON" : "Tab Apply ON" }
    public static var tabApplyOff: String { isKo ? "Tab 적용 OFF" : "Tab Apply OFF" }
    public static var tabApplyTooltipOn: String { isKo ? "Tab 키 입력 시 자동으로 완성 문장을 적용합니다 (클릭하여 끄기)" : "Press Tab to auto-apply suggested text (Click to disable)" }
    public static var tabApplyTooltipOff: String { isKo ? "Tab 키 자동 완성이 꺼져 있습니다. [적용하기] 버튼을 클릭해 적용하세요 (클릭하여 켜기)" : "Tab auto-apply is disabled. Click [Apply] to replace text (Click to enable)" }
    public static var analyzing: String { isKo ? "🔒 온디바이스 AI가 분석 중..." : "🔒 On-device AI analyzing..." }
    public static var originalBadge: String { isKo ? "원문" : "Original" }
    public static var aiBadge: String { isKo ? "AI" : "AI" }
    public static var originalText: String { isKo ? "작성 중 원문" : "Original" }
    public static var suggestedText: String { isKo ? "AI 다듬기" : "AI Refined" }
    public static var noDiffFound: String { isKo ? "완벽한 문장입니다!" : "Looks great as is!" }

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

    // MARK: - Onboarding Guide
    public static var onboardingReadyTitle: String { isKo ? "🎉 준비 완료!" : "🎉 All Set!" }
    public static var onboardingReadyDesc: String {
        isKo ? "이제 카카오톡, 슬랙, 메모장, 브라우저 어디서든\n글을 쓰시면 실시간으로 AI 교정 패널이 나타납니다."
             : "Start typing anywhere—Slack, Notes, or Browser.\nThe real-time AI correction panel will assist you instantly."
    }
    public static var onboardingStartBtn: String { isKo ? "시작하기" : "Get Started" }
    public static var onboardingHeaderTitle: String { isKo ? "DearTalk 실시간 AI 활성화" : "Enable DearTalk Real-time AI" }
    public static var onboardingHeaderDesc: String { isKo ? "타이핑 중 실시간 문맥 분석을 위해 1초 설정이 필요합니다" : "Requires a quick 1-second accessibility setup to analyze typing context" }
    public static var onboardingStep1Title: String { isKo ? "아래 버튼을 눌러 시스템 설정을 엽니다" : "Open System Settings below" }
    public static var onboardingStep1Desc: String { isKo ? "macOS 손쉬운 사용(Accessibility) 설정 창이 자동으로 열립니다." : "Opens macOS Accessibility settings panel automatically." }
    public static var onboardingStep2Title: String { isKo ? "목록에서 'DearTalk' 스위치를 켭니다" : "Enable the 'DearTalk' toggle in the list" }
    public static var onboardingStep2Desc: String { isKo ? "스위치를 켜면 이 창이 자동으로 완료 상태로 바뀝니다." : "Turning it on automatically transitions to completion." }
    public static var onboardingOpenSettingsBtn: String { isKo ? "🔑 손쉬운 사용 설정 열기" : "🔑 Open Accessibility Settings" }
    public static var onboardingPrivacyDesc: String { isKo ? "🔒 100% 온디바이스 작동 · 외부 서버 통신 0% · 키로그 미수집" : "🔒 100% On-Device · Zero Network · No Keystrokes Collected" }

    // MARK: - Settings View
    public static var settingsSectionPermission: String { isKo ? "🔐 접근성 권한 상태" : "🔐 Accessibility Permission" }
    public static var settingsPermissionGranted: String { isKo ? "손쉬운 사용 권한 승인됨" : "Accessibility Permission Granted" }
    public static var settingsSectionDetection: String { isKo ? "⚙️ 실시간 감지 설정" : "⚙️ Real-time Detection Settings" }
    public static var settingsSectionPhilosophy: String { isKo ? "🔒 온디바이스 AI 철학" : "🔒 On-Device AI Philosophy" }
    public static var settingsPhilosophyContent: String {
        isKo ? "• 100% 기기 내부 NPU/GPU 온디바이스 추론\n• 외부 네트워크 통신 0% (완벽한 프라이버시 보장)\n• 꼼수 및 가짜 하드코딩 대체 로직 0%"
             : "• 100% On-Device Neural Inference (NPU/GPU/CPU)\n• 0% External Network Traffic (Complete Privacy)\n• Zero Fake Heuristic Fallback Rules"
    }

    // MARK: - Sandbox View
    public static var sandboxTitle: String { isKo ? "DearTalk AI 실시간 DIFF 샌드박스" : "DearTalk AI Real-time DIFF Sandbox" }
    public static var sandboxModelActive: String {
        isKo ? "🔒 온디바이스 LLM 신경망 활성화 · 100% 오프라인 실시간 추론"
             : "🔒 On-Device LLM Neural Network Active · 100% Offline Real-time Inference"
    }
    public static var sandboxModelWaiting: String {
        isKo ? "🔒 온디바이스 LLM 모델 준비 대기 중 · 원문 100% 보존 모드"
             : "🔒 On-Device LLM Model Standby · 100% Original Text Preserved"
    }
    public static var sandboxShowPanelBtn: String { isKo ? "플로팅 패널 띄우기" : "Show Floating Panel" }
    public static var sandboxMonitoringTooltipActive: String { isKo ? "클릭하여 실시간 모니터링 일시 정지" : "Click to pause real-time monitoring" }
    public static var sandboxMonitoringTooltipPaused: String { isKo ? "클릭하여 실시간 모니터링 시작 (손쉬운 사용 권한 필요)" : "Click to start real-time monitoring (Accessibility permission required)" }
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

