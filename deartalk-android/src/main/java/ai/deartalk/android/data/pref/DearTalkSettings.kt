package ai.deartalk.android.data.pref

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flag: String
)

object DearTalkSettings {
    private const val PREF_NAME = "deartalk_preferences"
    private const val KEY_USE_AUTO_LANGUAGE = "key_use_auto_language"
    private const val KEY_SELECTED_LANGUAGE_CODE = "key_selected_language_code"

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("ko", "한국어", "Korean", "🇰🇷"),
        SupportedLanguage("en", "English", "English", "🇺🇸"),
        SupportedLanguage("id", "Indonesia", "Indonesian", "🇮🇩"),
        SupportedLanguage("ja", "日本語", "Japanese", "🇯🇵"),
        SupportedLanguage("zh-CN", "简体中文", "Simplified Chinese", "🇨🇳"),
        SupportedLanguage("zh-TW", "繁體中文", "Traditional Chinese", "🇹🇼"),
        SupportedLanguage("es", "Español", "Spanish", "🇪🇸"),
        SupportedLanguage("fr", "Français", "French", "🇫🇷"),
        SupportedLanguage("de", "Deutsch", "German", "🇩🇪")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoLanguage(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_USE_AUTO_LANGUAGE, true)
    }

    fun setAutoLanguage(context: Context, auto: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_AUTO_LANGUAGE, auto).apply()
    }

    fun getSelectedLanguageCode(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_LANGUAGE_CODE, "ko") ?: "ko"
    }

    fun setSelectedLanguageCode(context: Context, code: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_LANGUAGE_CODE, code).apply()
    }

    /**
     * 🌐 기본 언어 연동:
     * - 자동 모드(기본값): 기기 시스템 기본 언어(Locale.getDefault()) 100% 자동 연동
     * - 수동 모드: 사용자가 직접 선택한 언어(ko, en, id, ja, zh 등) 적용
     */
    fun getEffectiveLocale(context: Context? = null): Locale {
        if (context != null && !isAutoLanguage(context)) {
            val code = getSelectedLanguageCode(context)
            return Locale.forLanguageTag(code)
        }
        return Locale.getDefault()
    }

    private const val KEY_SPEECH_SILENCE_MILLIS = "key_speech_silence_millis"

    fun getSilenceTimeoutMillis(context: Context): Long {
        return getPrefs(context).getLong(KEY_SPEECH_SILENCE_MILLIS, 3500L)
    }

    fun setSilenceTimeoutMillis(context: Context, millis: Long) {
        getPrefs(context).edit().putLong(KEY_SPEECH_SILENCE_MILLIS, millis).apply()
    }

    fun getLanguageDisplayTitle(context: Context): String {
        val locale = getEffectiveLocale(context)
        val isAuto = isAutoLanguage(context)
        val targetLang = SUPPORTED_LANGUAGES.firstOrNull { it.code.startsWith(locale.language) }
        val name = targetLang?.let { "${it.flag} ${it.nativeName}" } ?: locale.displayLanguage
        val isKorean = UiStrings.isKo
        val isIndonesian = UiStrings.isId
        return if (isAuto) {
            if (isKorean) "$name (시스템 언어 자동 연동)"
            else if (isIndonesian) "$name (Otomatis Sistem)"
            else "$name (System Auto)"
        } else {
            if (isKorean) "$name (수동 선택)"
            else if (isIndonesian) "$name (Dipilih Manual)"
            else "$name (Manual)"
        }
    }

    private const val KEY_KOREAN_KEYBOARD_TYPE = "key_korean_keyboard_type"

    fun getKoreanKeyboardType(context: Context): KoreanKeyboardType {
        val name = getPrefs(context).getString(KEY_KOREAN_KEYBOARD_TYPE, KoreanKeyboardType.DUBEOLSIK.name)
        return try {
            KoreanKeyboardType.valueOf(name ?: KoreanKeyboardType.DUBEOLSIK.name)
        } catch (e: Exception) {
            KoreanKeyboardType.DUBEOLSIK
        }
    }

    fun setKoreanKeyboardType(context: Context, type: KoreanKeyboardType) {
        getPrefs(context).edit().putString(KEY_KOREAN_KEYBOARD_TYPE, type.name).apply()
    }
}

enum class KoreanKeyboardType {
    DUBEOLSIK,
    CHEONJIIN
}

