package ai.deartalk.android.data.pref

import java.util.Locale

/**
 * 중앙집중식 다국어 UI 문자열 관리 오브젝트
 * - 시스템 Locale 기반으로 한국어(ko) / 영어(default) 자동 분기
 * - 모든 사용자 대면 UI 문자열을 한 곳에서 관리
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
    val micListening get() = if (isKo) "🔴 지금 말씀하세요 (터치 시 완료)" else "🔴 Speak now (tap to finish)"
    val micProcessingAi get() = if (isKo) "🔒 온디바이스 AI 다듬는 중..." else "🔒 On-device AI refining..."
    val micIdle get() = if (isKo) "🎙️ AI 음성 입력" else "🎙️ AI Voice Input"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 키보드/설정 버튼
    // ═══════════════════════════════════════════════════
    val keyboard get() = if (isKo) "자판" else "Keys"
    val keyboardContentDesc get() = if (isKo) "자판" else "Keyboard"
    val settingsContentDesc get() = if (isKo) "DearTalkAI 설정" else "DearTalkAI Settings"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 스마트 DIFF 캔버스
    // ═══════════════════════════════════════════════════
    val micConnecting get() = if (isKo) "⏳ 마이크 연결 중..." else "⏳ Connecting mic..."
    val micConnectingDesc get() = if (isKo) "잠시만 기다려 주세요. 마이크가 곧 열립니다." else "Please wait. Mic will open shortly."
    val listeningLabel get() = if (isKo) "🎙️ 음성 듣는 중..." else "🎙️ Listening..."
    val speakNowHint get() = if (isKo) "지금 말씀하시면 실시간 변환됩니다..." else "Speak now for real-time transcription..."
    val sttRaw get() = if (isKo) "STT 원문" else "STT Raw"
    val aiRefine get() = if (isKo) "AI 다듬기" else "AI Refined"
    val aiRefiningContext get() = if (isKo) "🔒 온디바이스 AI 문맥 다듬는 중..." else "🔒 On-device AI refining context..."
    val canvasPlaceholder get() = if (isKo) {
        "🎙️ 상단 마이크를 누르고 말씀하시면, [STT 원문]과 [AI 다듬기] 비교가 여기에 표시됩니다."
    } else {
        "🎙️ Tap the mic above and speak. [STT Raw] vs [AI Refined] comparison will appear here."
    }

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 입력/취소 버튼
    // ═══════════════════════════════════════════════════
    val apply get() = if (isKo) "입력" else "Apply"
    val applyContentDesc get() = if (isKo) "입력" else "Apply"
    val cancel get() = if (isKo) "취소" else "Cancel"
    val cancelContentDesc get() = if (isKo) "취소" else "Cancel"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 번역 셀렉트박스
    // ═══════════════════════════════════════════════════
    fun translationLabel(name: String) = if (isKo) "$name 번역" else "Translate to $name"
    val selectTranslationTarget get() = if (isKo) "🌐 번역할 목표 언어 선택" else "🌐 Select target language"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 유틸리티 바
    // ═══════════════════════════════════════════════════
    val deleteCharContentDesc get() = if (isKo) "1글자 지우기" else "Delete character"
    val deleteSentence get() = if (isKo) "문장삭제" else "Del Sent"
    val deleteSentenceContentDesc get() = if (isKo) "문장 삭제" else "Delete sentence"
    val enterContentDesc get() = if (isKo) "전송" else "Enter"

    // ═══════════════════════════════════════════════════
    // StandardKeyboardView.kt
    // ═══════════════════════════════════════════════════
    val standardKeyboardMode get() = if (isKo) "⌨️ 일반 자판 모드" else "⌨️ Standard Keyboard"
    val aiVoiceMode get() = if (isKo) "✨ AI 음성 모드" else "✨ AI Voice Mode"
    val korEngToggle get() = if (isKo) "한/영" else "KO/EN"

    // ═══════════════════════════════════════════════════
    // CustomToneManager.kt — 기본 톤앤매너 이름
    // ═══════════════════════════════════════════════════
    val toneRefine get() = if (isKo) "기본다듬기" else "Refine"
    val tonePolite get() = if (isKo) "공손하게" else "Polite"
    val toneCasual get() = if (isKo) "친근하게" else "Casual"
    val toneBusiness get() = if (isKo) "비즈니스" else "Business"
    val toneFunny get() = if (isKo) "재미있게" else "Funny"
    val toneCheeky get() = if (isKo) "건방지게" else "Cheeky"

    // ═══════════════════════════════════════════════════
    // CustomToneManager.kt — 기본 번역 대상 언어 이름
    // ═══════════════════════════════════════════════════
    val langEnglish get() = if (isKo) "영어" else "English"
    val langIndonesian get() = if (isKo) "인도네시아어" else "Indonesian"
    val langJapanese get() = if (isKo) "일본어" else "Japanese"
    val langChinese get() = if (isKo) "중국어" else "Chinese"
    val langSpanish get() = if (isKo) "스페인어" else "Spanish"
    val langFrench get() = if (isKo) "프랑스어" else "French"
    val langGerman get() = if (isKo) "독일어" else "German"
    val langVietnamese get() = if (isKo) "베트남어" else "Vietnamese"

    // CustomToneManager.kt — 기본 AI 모드
    val defaultAi get() = if (isKo) "기본 AI" else "Default AI"

    // ═══════════════════════════════════════════════════
    // DearTalkIME.kt — 상태 메시지
    // ═══════════════════════════════════════════════════
    val aiProcessing get() = if (isKo) "🔒 온디바이스 AI 변환 중..." else "🔒 On-device AI processing..."
    val errorOccurred get() = if (isKo) "오류 발생" else "Error occurred"
    val aiTextComplete get() = if (isKo) "✨ AI Text 완성 (우측 [입력] 클릭)" else "✨ AI Text ready (tap [Apply])"
    val sttComplete get() = if (isKo) "🎤 음성 인식 완료 (우측 [입력] 클릭)" else "🎤 Speech recognized (tap [Apply])"
    val noTextToTransform get() = if (isKo) "⚠️ 변환할 텍스트가 없습니다" else "⚠️ No text to transform"
    fun modeProcessing(icon: String, name: String) = if (isKo) "🔒 $icon $name 처리 중..." else "🔒 $icon $name processing..."
    fun modeComplete(name: String) = if (isKo) "✨ $name 완료 (우측 [입력] 클릭)" else "✨ $name done (tap [Apply])"
    val transformFailed get() = if (isKo) "⚠️ 변환 실패" else "⚠️ Transform failed"
    fun toneConverting(icon: String, name: String) = if (isKo) "🔒 $icon $name 변환 중..." else "🔒 $icon $name converting..."
    fun toneApplied(name: String) = if (isKo) "✨ $name 적용 완료 (우측 [입력] 클릭)" else "✨ $name applied (tap [Apply])"
    val textApplied get() = if (isKo) "✅ 텍스트박스 입력 완료" else "✅ Text applied"
    val aiTextCleared get() = if (isKo) "🗑️ AI Text 비움" else "🗑️ AI Text cleared"
    val lastSentenceDeleted get() = if (isKo) "✂️ 마지막 1문장 삭제됨" else "✂️ Last sentence deleted"
    val editorSentenceDeleted get() = if (isKo) "✂️ 에디터 1문장 삭제 완료" else "✂️ Editor sentence deleted"

    // ═══════════════════════════════════════════════════
    // DearTalkIntentEngine.kt — 결과 메시지
    // ═══════════════════════════════════════════════════
    val aiGenerationComplete get() = if (isKo) "🔒 온디바이스 AI 생성 완료" else "🔒 On-device AI generation complete"
    val sttRawResult get() = if (isKo) "🎤 음성 인식 원문" else "🎤 Speech recognized (raw)"
    fun toneComplete(icon: String, name: String) = if (isKo) "🔒 $icon $name 완료" else "🔒 $icon $name complete"
    fun translationComplete(flag: String, name: String) = if (isKo) "🔒 $flag $name 번역 완료" else "🔒 $flag $name translation complete"

    // ═══════════════════════════════════════════════════
    // MainActivity.kt — 설정 및 안내 화면
    // ═══════════════════════════════════════════════════
    val settingsListening get() = if (isKo) "음성을 듣고 있습니다... (말씀하세요)" else "Listening to your voice..."
    val settingsAiAnalyzing get() = if (isKo) "🔒 온디바이스 Gemma AI가 문맥을 분석하여 조율 중입니다..." else "🔒 On-device Gemma AI is analyzing context..."
    val settingsAiComplete get() = if (isKo) "🔒 온디바이스 AI 조율 완료!" else "🔒 On-device AI refinement complete!"
    fun settingsSttError(code: Int) = if (isKo) "음성 인식 대기 중 (코드: $code)" else "Waiting for speech (code: $code)"
    val settingsAiRefining get() = if (isKo) "온디바이스 AI 조율 중..." else "Refining with On-Device AI..."
    val settingsAiRefined get() = if (isKo) "🔒 온디바이스 AI 조율 완료" else "🔒 On-Device AI Completed"
}
