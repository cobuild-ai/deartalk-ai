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
        val isKorean = UiStrings.isKo
        val isIndonesian = UiStrings.isId
        return if (isAuto) {
            if (isKorean) "$name (시스템 기본)" else if (isIndonesian) "$name (Otomatis)" else "$name (Auto)"
        } else {
            if (isKorean) "$name (사용자 지정)" else if (isIndonesian) "$name (Manual)" else "$name (Custom)"
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

    private const val KEY_KEYBOARD_MODE = "key_keyboard_mode"
    private const val KEY_ONBOARDING_MODE_SHOWN = "key_onboarding_mode_shown"

    fun getKeyboardMode(context: Context): KeyboardMode {
        val name = getPrefs(context).getString(KEY_KEYBOARD_MODE, KeyboardMode.BASIC.name)
        return try {
            KeyboardMode.valueOf(name ?: KeyboardMode.BASIC.name)
        } catch (_: Exception) {
            KeyboardMode.BASIC
        }
    }

    fun setKeyboardMode(context: Context, mode: KeyboardMode) {
        getPrefs(context).edit().putString(KEY_KEYBOARD_MODE, mode.name).apply()
    }

    fun isOnboardingModeShown(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_MODE_SHOWN, false)
    }

    fun setOnboardingModeShown(context: Context, shown: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_MODE_SHOWN, shown).apply()
    }
}

enum class KoreanKeyboardType {
    DUBEOLSIK,
    CHEONJIIN
}

/**
 * 🎛️ 2-Tier 키보드 경험 모드
 * - BASIC (기본 모드): 언어 고정, 번역 숨김, 톤앤매너/화행 보정에 집중 (3세 이상 전연령 초직관 경험)
 * - PRO (프로 모드): 다국어 실시간 번역 + 톤앤매너 + 2-Track 통역 결합 (글로벌 파워 유저)
 */
enum class KeyboardMode {
    BASIC,
    PRO
}
