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
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
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

    fun setAutoLanguage(context: Context, isAuto: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_AUTO_LANGUAGE, isAuto).apply()
    }

    fun getSelectedLanguageCode(context: Context): String {
        val defaultLang = Locale.getDefault().language
        return getPrefs(context).getString(KEY_SELECTED_LANGUAGE_CODE, if (defaultLang == "ko") "ko" else "en") ?: "ko"
    }

    fun setSelectedLanguageCode(context: Context, code: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_LANGUAGE_CODE, code).apply()
    }

    fun getEffectiveLocale(context: Context): Locale {
        if (isAutoLanguage(context)) {
            return Locale.getDefault()
        }
        val code = getSelectedLanguageCode(context)
        return when {
            code.contains("-") -> {
                val parts = code.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(code)
        }
    }

    private const val KEY_SPEECH_SILENCE_MILLIS = "key_speech_silence_millis"

    fun getSilenceTimeoutMillis(context: Context): Long {
        return getPrefs(context).getLong(KEY_SPEECH_SILENCE_MILLIS, 3500L)
    }

    fun setSilenceTimeoutMillis(context: Context, millis: Long) {
        getPrefs(context).edit().putLong(KEY_SPEECH_SILENCE_MILLIS, millis).apply()
    }

    fun getLanguageDisplayTitle(context: Context): String {
        val isAuto = isAutoLanguage(context)
        val locale = getEffectiveLocale(context)
        val targetLang = SUPPORTED_LANGUAGES.firstOrNull { it.code.startsWith(locale.language) }
        val name = targetLang?.let { "${it.flag} ${it.nativeName}" } ?: locale.displayLanguage
        val isKorean = Locale.getDefault().language == "ko"
        return if (isAuto) {
            if (isKorean) "$name (시스템 기본)" else "$name (System Auto)"
        } else {
            if (isKorean) "$name (사용자 지정)" else "$name (Custom)"
        }
    }
}
