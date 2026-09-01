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
}
