package ai.deartalk.android.util

import java.util.Locale

/**
 * 전역 언어 코드 및 로케일 매핑 유틸리티 (Single Source of Truth)
 */
object LanguageLocaleHelper {

    val SUPPORTED_LANG_CODES = listOf(
        "KO", "EN", "JA", "ZH", "ES", "FR", "DE", "ID", "VI", "TL", "TH", "MS"
    )

    fun getLocaleForCode(code: String): Locale = when (code.uppercase()) {
        "EN" -> Locale.US
        "ES" -> Locale("es", "ES")
        "FR" -> Locale.FRANCE
        "DE" -> Locale.GERMANY
        "JA" -> Locale.JAPAN
        "ZH" -> Locale.CHINESE
        "ID" -> Locale("id", "ID")
        "VI" -> Locale("vi", "VN")
        "TL", "FIL" -> Locale("fil", "PH")
        "TH" -> Locale("th", "TH")
        "MS" -> Locale("ms", "MY")
        else -> Locale.KOREAN
    }

    fun getLanguageTag(locale: Locale): String = when (locale.language.lowercase()) {
        "ko" -> "ko-KR"
        "en" -> "en-US"
        "ja" -> "ja-JP"
        "zh" -> "zh-CN"
        "es" -> "es-ES"
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "id", "in" -> "id-ID"
        "vi" -> "vi-VN"
        "fil", "tl" -> "fil-PH"
        "th" -> "th-TH"
        "ms" -> "ms-MY"
        else -> locale.toLanguageTag().ifBlank { "ko-KR" }
    }

    fun hasKorean(text: String): Boolean = text.any { ch ->
        (ch in '\uAC00'..'\uD7A3') || (ch in '\u1100'..'\u11FF') || (ch in '\u3130'..'\u318F')
    }

    fun hasJapanese(text: String): Boolean = text.any { ch ->
        (ch in '\u3040'..'\u309F') || (ch in '\u30A0'..'\u30FF')
    }

    fun hasChinese(text: String): Boolean = text.any { ch ->
        (ch in '\u4E00'..'\u9FFF')
    } && !hasKorean(text) && !hasJapanese(text)

    fun hasThai(text: String): Boolean = text.any { ch ->
        (ch in '\u0E00'..'\u0E7F')
    }

    fun isEnglish(text: String): Boolean {
        if (hasKorean(text) || hasJapanese(text) || hasChinese(text) || hasThai(text)) return false
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.all { it in 'a'..'z' || it in 'A'..'Z' }
    }

    /**
     * 🔍 입력 문자열의 문자 체계(Script)를 감지하여 가장 유력한 언어 코드 반환
     */
    fun detectLanguageCode(text: String, fallback: String = "KO"): String = when {
        hasKorean(text) -> "KO"
        hasJapanese(text) -> "JA"
        hasThai(text) -> "TH"
        hasChinese(text) -> "ZH"
        isEnglish(text) -> "EN"
        else -> fallback
    }
}
