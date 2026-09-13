package ai.deartalk.android.agent.language

import ai.deartalk.android.live.data.SpeechIntent

/**
 * 🎯 언어별 특정 화행(Speech Intent)에 대한 프롬프트 지침 및 종결 부호 스펙
 */
data class SpeechIntentRule(
    val directive: String,
    val endingPunctuation: String = "",
    val examples: List<Pair<String, String>> = emptyList()
)

/**
 * 🌐 개별 언어별 화행(Pragmatics), 문자 체계(Script), 출력 정제 규칙 및 후처리 불변 프로필 (SSOT)
 */
data class LanguageProfile(
    val code: String, // ISO 639-1 (e.g. "KO", "EN", "JA", "ZH", "ID", "ES", "FR", "DE", "VI")
    val englishName: String, // e.g. "English", "Japanese", "Korean"
    val localizedName: String, // e.g. "한국어", "English", "日本語"
    val flag: String, // e.g. "🇰🇷", "🇺🇸", "🇯🇵"
    val scriptGuidelines: String = "", // e.g. 한자/가나 구어체, 후리가나 제외 등
    val intentRules: Map<SpeechIntent, SpeechIntentRule> = emptyMap(),
    val cleaningPrefixes: List<String> = emptyList(), // 언어별 모델 출력 불필요 접두어
    val quotationMarks: List<Char> = listOf('"', '\'', '`', '“', '”', '‘', '’'),
    val postProcessor: ((rawOutput: String, intent: SpeechIntent) -> String)? = null
) {
    fun getIntentRule(intent: SpeechIntent): SpeechIntentRule? = intentRules[intent]

    fun applyPostProcessing(output: String, intent: SpeechIntent): String {
        return postProcessor?.invoke(output, intent) ?: output
    }
}
