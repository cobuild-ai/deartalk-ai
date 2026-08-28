package ai.deartalk.android.data.pref

import java.util.Locale

/**
 * 중앙집중식 다국어 UI 문자열 관리 오브젝트
 * - 애플/구글 스타일의 프리미엄하면서도 누구나 쉽게 이해할 수 있는 명확한 표현 적용
 * - 시스템 Locale 기반으로 한국어(ko) / 영어(default) 자동 분기
 */
object UiStrings {
    private var overrideLocale: Locale? = null

    fun setLocale(locale: Locale) {
        overrideLocale = locale
    }

    private val isKo: Boolean
        get() {
            val locale = overrideLocale ?: Locale.getDefault()
            return locale.language == "ko"
        }

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 마이크 버튼 상태
    // ═══════════════════════════════════════════════════
    val micPreparing get() = if (isKo) "⏳ 마이크 준비 중..." else "⏳ Preparing mic..."
    val micListening get() = if (isKo) "🔴 듣고 있어요 (말씀이 끝나면 터치)" else "🔴 Listening (tap when finished)"
    val micProcessingAi get() = if (isKo) "🔒 AI가 문장을 다듬는 중..." else "🔒 Polishing sentence with AI..."
    val micIdle get() = if (isKo) "🎙️ AI 음성 입력" else "🎙️ AI Voice Input"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 키보드/설정 버튼
    // ═══════════════════════════════════════════════════
    val keyboard get() = if (isKo) "자판" else "Keys"
    val keyboardContentDesc get() = if (isKo) "일반 자판으로 전환" else "Switch to standard keyboard"
    val settingsContentDesc get() = if (isKo) "DearTalk AI 설정 및 가이드" else "DearTalk AI Settings & Guide"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 스마트 DIFF 캔버스
    // ═══════════════════════════════════════════════════
    val micConnecting get() = if (isKo) "⏳ 마이크 연결 중..." else "⏳ Connecting mic..."
    val micConnectingDesc get() = if (isKo) "음성 인식 준비 중입니다. 잠시만 기다려 주세요." else "Getting ready to listen. Please wait a moment."
    val listeningLabel get() = if (isKo) "🎙️ 실시간으로 듣고 있어요..." else "🎙️ Listening to your voice..."
    val speakNowHint get() = if (isKo) "편하게 말씀하시면 AI가 문맥에 맞게 다듬어 드려요." else "Speak naturally, and AI will polish your sentence."
    val sttRaw get() = if (isKo) "🎤 내가 말한 내용" else "🎤 What You Said"
    val aiRefine get() = if (isKo) "✨ AI가 다듬은 문장" else "✨ AI Polished"
    val aiRefiningContext get() = if (isKo) "🔒 AI가 문맥을 분석하여 다듬는 중..." else "🔒 Refining context with on-device AI..."
    val canvasPlaceholder get() = if (isKo) {
        "🎙️ 상단 마이크를 누르고 말씀하시면 [내가 말한 내용]과 [AI가 다듬은 문장]이 여기에 표시됩니다."
    } else {
        "🎙️ Tap the mic above to speak. [What You Said] and [AI Polished] will appear here."
    }

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 입력/취소 버튼
    // ═══════════════════════════════════════════════════
    val apply get() = if (isKo) "입력" else "Apply"
    val applyContentDesc get() = if (isKo) "메시지 창에 입력" else "Insert into message"
    val cancel get() = if (isKo) "취소" else "Cancel"
    val cancelContentDesc get() = if (isKo) "입력 취소" else "Cancel"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 번역 셀렉트박스
    // ═══════════════════════════════════════════════════
    fun translationLabel(name: String) = if (isKo) "$name 번역" else "Translate to $name"
    val selectTranslationTarget get() = if (isKo) "🌐 번역할 언어 선택" else "🌐 Select Target Language"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 유틸리티 바
    // ═══════════════════════════════════════════════════
    val deleteCharContentDesc get() = if (isKo) "한 글자 지우기" else "Delete character"
    val deleteSentence get() = if (isKo) "문장삭제" else "Del Sent"
    val deleteSentenceContentDesc get() = if (isKo) "방금 입력한 문장 전체 삭제" else "Delete current sentence"
    val enterContentDesc get() = if (isKo) "줄바꿈 또는 전송" else "Enter or send"

    // ═══════════════════════════════════════════════════
    // StandardKeyboardView.kt
    // ═══════════════════════════════════════════════════
    val standardKeyboardMode get() = if (isKo) "⌨️ 일반 키보드" else "⌨️ Standard Keyboard"
    val aiVoiceMode get() = if (isKo) "✨ AI 음성 모드" else "✨ AI Voice Mode"
    val korEngToggle get() = if (isKo) "한/영" else "KO/EN"

    // ═══════════════════════════════════════════════════
    // CustomToneManager.kt — 톤앤매너 이름
    // ═══════════════════════════════════════════════════
    val toneRefine get() = if (isKo) "기본다듬기" else "Refine"
    val tonePolite get() = if (isKo) "공손하게" else "Polite"
    val toneCasual get() = if (isKo) "친근하게" else "Casual"
    val toneBusiness get() = if (isKo) "비즈니스" else "Business"
    val toneFunny get() = if (isKo) "재미있게" else "Humorous"
    val toneCheeky get() = if (isKo) "당당하게" else "Cheeky"

    // ═══════════════════════════════════════════════════
    // CustomToneManager.kt — 언어 이름
    // ═══════════════════════════════════════════════════
    val langEnglish get() = if (isKo) "영어" else "English"
    val langIndonesian get() = if (isKo) "인도네시아어" else "Indonesian"
    val langJapanese get() = if (isKo) "일본어" else "Japanese"
    val langChinese get() = if (isKo) "중국어" else "Chinese"
    val langSpanish get() = if (isKo) "스페인어" else "Spanish"
    val langFrench get() = if (isKo) "프랑스어" else "French"
    val langGerman get() = if (isKo) "독일어" else "German"
    val langVietnamese get() = if (isKo) "베트남어" else "Vietnamese"

    val defaultAi get() = if (isKo) "기본 AI" else "Default AI"

    // ═══════════════════════════════════════════════════
    // DearTalkIME.kt — 상태 메시지
    // ═══════════════════════════════════════════════════
    val aiProcessing get() = if (isKo) "🔒 AI가 문장을 다듬는 중..." else "🔒 Polishing sentence with AI..."
    val errorOccurred get() = if (isKo) "준비 완료" else "Ready"
    val aiTextComplete get() = if (isKo) "✨ 문장이 완성되었습니다 ([입력]을 눌러 전송)" else "✨ Ready to send (tap [Apply])"
    val sttComplete get() = if (isKo) "🎤 음성 인식 완료 ([입력]을 눌러 전송)" else "🎤 Speech recognized (tap [Apply])"
    val noTextToTransform get() = if (isKo) "⚠️ 다듬을 내용이 없습니다." else "⚠️ No text provided"
    fun modeProcessing(icon: String, name: String) = if (isKo) "🔒 $icon $name 적용 중..." else "🔒 Applying $name..."
    fun modeComplete(name: String) = if (isKo) "✨ $name 적용 완료 ([입력] 터치)" else "✨ $name ready (tap [Apply])"
    val transformFailed get() = if (isKo) "⚠️ 변환 실패 (원문이 유지됩니다)" else "⚠️ Transformation failed (original kept)"
    fun toneConverting(icon: String, name: String) = if (isKo) "🔒 $icon $name 말투로 바꾸는 중..." else "🔒 Changing tone to $name..."
    fun toneApplied(name: String) = if (isKo) "✨ $name 말투 적용 완료 ([입력] 터치)" else "✨ $name tone ready (tap [Apply])"
    val textApplied get() = if (isKo) "✅ 입력창에 입력되었습니다" else "✅ Inserted into message"
    val aiTextCleared get() = if (isKo) "🗑️ 내용을 지웠습니다" else "🗑️ Text cleared"
    val lastSentenceDeleted get() = if (isKo) "✂️ 방금 입력한 문장이 삭제되었습니다" else "✂️ Previous sentence deleted"
    val editorSentenceDeleted get() = if (isKo) "✂️ 문장이 삭제되었습니다" else "✂️ Sentence deleted"

    // ═══════════════════════════════════════════════════
    // DearTalkIntentEngine.kt — 결과 메시지
    // ═══════════════════════════════════════════════════
    val aiGenerationComplete get() = if (isKo) "🔒 온디바이스 AI 다듬기 완료" else "🔒 On-Device AI Refinement Complete"
    val sttRawResult get() = if (isKo) "🎤 음성 인식 내용" else "🎤 Recognized Speech"
    fun toneComplete(icon: String, name: String) = if (isKo) "🔒 $icon $name 완료" else "🔒 $icon $name Complete"
    fun translationComplete(flag: String, name: String) = if (isKo) "🔒 $flag $name 번역 완료" else "🔒 $flag $name Translation Complete"

    // ═══════════════════════════════════════════════════
    // MainActivity.kt — 설정 및 안내 화면
    // ═══════════════════════════════════════════════════
    val settingsListening get() = if (isKo) "말씀을 듣고 있어요... (편하게 말씀하세요)" else "Listening to your voice... (Speak now)"
    val settingsAiAnalyzing get() = if (isKo) "🔒 AI가 문맥을 분석하여 문장을 다듬고 있습니다..." else "🔒 Analyzing context to polish your sentence..."
    val settingsAiComplete get() = if (isKo) "🔒 AI 문장 다듬기 완료!" else "🔒 AI Refinement Complete!"
    fun settingsSttError(code: Int) = if (isKo) "마이크 대기 중 (코드: $code)" else "Microphone ready (code: $code)"
    val settingsAiRefining get() = if (isKo) "AI가 문장을 다듬는 중..." else "Polishing with On-Device AI..."
    val settingsAiRefined get() = if (isKo) "🔒 AI 문장 다듬기 완료" else "🔒 AI Refinement Complete"

    // ═══════════════════════════════════════════════════
    // MainActivity.kt — 정보 및 도움말
    // ═══════════════════════════════════════════════════
    val settingsTabAbout get() = if (isKo) "ℹ️ 앱 정보 및 도움말" else "ℹ️ About & Help"
    val appVersionLabel get() = if (isKo) "앱 버전" else "App Version"
    val buildTimestampLabel get() = if (isKo) "업데이트 일시" else "Last Updated"
    val userGuideTitle get() = if (isKo) "📖 DearTalk AI 쉽게 쓰는 법" else "📖 DearTalk AI Quick Guide"
    val userGuideHowToUseTitle get() = if (isKo) "💡 이렇게 사용해 보세요" else "💡 How to Use"
    val userGuideHowToUseContent get() = if (isKo) {
        "1. [1단계] 및 [2단계] 버튼을 눌러 DearTalk AI 키보드를 켜고 기본 키보드로 선택합니다.\n" +
        "2. 카카오톡, 메시지 등 대화창을 열고 DearTalk AI 키보드를 엽니다.\n" +
        "3. 키보드 상단의 마이크(🎙️) 버튼을 누르고 편하게 말씀하세요.\n" +
        "4. [공손하게], [친근하게], [비즈니스] 칩을 누르면 원하는 말투로 바로 바뀝니다.\n" +
        "5. 마음에 드는 문장을 확인하고 오른쪽 [입력] 버튼을 누르면 대화창에 바로 입력됩니다."
    } else {
        "1. Tap Step 1 and Step 2 buttons above to enable and set DearTalk AI as your default keyboard.\n" +
        "2. Open any messaging app and bring up the DearTalk AI keyboard.\n" +
        "3. Tap the mic (🎙️) button and speak naturally.\n" +
        "4. Tap tone chips like [Polite], [Casual], or [Business] to transform your style instantly.\n" +
        "5. Tap the [Apply] button on the right to insert the polished sentence into your chat."
    }
}
