package ai.deartalk.android.data

/**
 * 🎯 실시간 발화 의도 (Speech Pragmatics / 화행)
 * - 사용자가 탭하여 1회성(One-shot)으로 지정할 수 있는 발화 유형
 * - 발화 및 번역/변환이 완료되면 자동으로 AUTO(스마트)로 복귀
 */
enum class SpeechIntent(val id: String) {
    AUTO("auto"),           // ✨ 스마트 (AI 자율 맥락 판단)
    QUESTION("question"),   // ❓ 질문? (의문문 강제)
    STATEMENT("statement"), // 💬 설명. (평서문/답변 강제)
    REQUEST("request"),     // 🙏 부탁! (정중한 요청/부탁 강제)
    CONFIRM("confirm")      // 🔁 확인? (되묻기/동의 구하기 강제)
}

fun SpeechIntent.getLabel(langCode: String? = null): String {
    val code = (langCode ?: ai.deartalk.android.data.pref.UiStrings.currentLocale.language).uppercase()
    return when (this) {
        SpeechIntent.AUTO -> when {
            code.startsWith("KO") -> "✨ 스마트"
            code.startsWith("ID") -> "✨ Cerdas"
            else -> "✨ Smart"
        }
        SpeechIntent.QUESTION -> when {
            code.startsWith("KO") -> "❓ 질문?"
            code.startsWith("ID") -> "❓ Tanya?"
            else -> "❓ Question?"
        }
        SpeechIntent.STATEMENT -> when {
            code.startsWith("KO") -> "💬 설명."
            code.startsWith("ID") -> "💬 Pernyataan."
            else -> "💬 Statement."
        }
        SpeechIntent.REQUEST -> when {
            code.startsWith("KO") -> "🙏 부탁!"
            code.startsWith("ID") -> "🙏 Tolong!"
            else -> "🙏 Request!"
        }
        SpeechIntent.CONFIRM -> when {
            code.startsWith("KO") -> "🔁 확인?"
            code.startsWith("ID") -> "🔁 Konfirmasi?"
            else -> "🔁 Confirm?"
        }
    }
}
