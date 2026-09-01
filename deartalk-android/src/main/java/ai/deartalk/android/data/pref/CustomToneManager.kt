package ai.deartalk.android.data.pref

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

enum class AiModeType {
    DEFAULT,
    TONE,
    TRANSLATION
}

data class AiModeItem(
    val id: String,
    val name: String,
    val icon: String,
    val type: AiModeType,
    val customTone: CustomTone? = null,
    val translationTarget: TranslationTarget? = null
)

data class CustomTone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val instruction: String,
    val icon: String = "✨"
)

data class TranslationTarget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetLanguage: String,
    val flag: String = "🌐"
)

object CustomToneManager {
    private const val PREF_NAME = "deartalk_custom_presets"
    private const val KEY_TONES = "saved_custom_tones"
    private const val KEY_TRANSLATIONS = "saved_translations"

    val DEFAULT_TONES: List<CustomTone>
        get() = listOf(
            CustomTone(
                id = "tone_refine",
                name = UiStrings.toneRefine,
                instruction = UiStrings.instRefine,
                icon = "✨"
            ),
            CustomTone(
                id = "tone_polite",
                name = UiStrings.tonePolite,
                instruction = UiStrings.instPolite,
                icon = "👔"
            ),
            CustomTone(
                id = "tone_casual",
                name = UiStrings.toneCasual,
                instruction = UiStrings.instCasual,
                icon = "😊"
            ),
            CustomTone(
                id = "tone_business",
                name = UiStrings.toneBusiness,
                instruction = UiStrings.instBusiness,
                icon = "💼"
            ),
            CustomTone(
                id = "tone_funny",
                name = UiStrings.toneFunny,
                instruction = UiStrings.instFunny,
                icon = "🤣"
            ),
            CustomTone(
                id = "tone_cheeky",
                name = UiStrings.toneCheeky,
                instruction = UiStrings.instCheeky,
                icon = "😼"
            )
        )

    val DEFAULT_TRANSLATIONS: List<TranslationTarget>
        get() = listOf(
            TranslationTarget(
                id = "trans_en",
                name = UiStrings.langEnglish,
                targetLanguage = "영어(English)",
                flag = "🇺🇸"
            ),
            TranslationTarget(
                id = "trans_id",
                name = UiStrings.langIndonesian,
                targetLanguage = "인도네시아어(Bahasa Indonesia)",
                flag = "🇮🇩"
            ),
            TranslationTarget(
                id = "trans_ja",
                name = UiStrings.langJapanese,
                targetLanguage = "일본어(日本語)",
                flag = "🇯🇵"
            ),
            TranslationTarget(
                id = "trans_zh",
                name = UiStrings.langChinese,
                targetLanguage = "중국어(中文)",
                flag = "🇨🇳"
            ),
            TranslationTarget(
                id = "trans_es",
                name = UiStrings.langSpanish,
                targetLanguage = "스페인어(Español)",
                flag = "🇪🇸"
            ),
            TranslationTarget(
                id = "trans_fr",
                name = UiStrings.langFrench,
                targetLanguage = "프랑스어(Français)",
                flag = "🇫🇷"
            ),
            TranslationTarget(
                id = "trans_de",
                name = UiStrings.langGerman,
                targetLanguage = "독일어(Deutsch)",
                flag = "🇩🇪"
            ),
            TranslationTarget(
                id = "trans_vi",
                name = UiStrings.langVietnamese,
                targetLanguage = "베트남어(Tiếng Việt)",
                flag = "🇻🇳"
            )
        )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getAllAiModes(context: Context): List<AiModeItem> {
        val list = mutableListOf<AiModeItem>()
        // 1. 기본 모드
        list.add(
            AiModeItem(
                id = "default_ai",
                name = UiStrings.defaultAi,
                icon = "✨",
                type = AiModeType.DEFAULT
            )
        )
        // 2. 톤앤매너 모드들
        getTones(context).forEach { tone ->
            list.add(
                AiModeItem(
                    id = tone.id,
                    name = tone.name,
                    icon = tone.icon,
                    type = AiModeType.TONE,
                    customTone = tone
                )
            )
        }
        // 3. 번역 모드들
        getTranslations(context).forEach { trans ->
            list.add(
                AiModeItem(
                    id = trans.id,
                    name = trans.name,
                    icon = trans.flag,
                    type = AiModeType.TRANSLATION,
                    translationTarget = trans
                )
            )
        }
        return list
    }

    // 1. 톤앤매너 관리
    fun getTones(context: Context): List<CustomTone> {
        return DEFAULT_TONES
    }

    fun saveTones(context: Context, tones: List<CustomTone>) {
        val jsonArray = JSONArray()
        tones.forEach { tone ->
            val obj = JSONObject().apply {
                put("id", tone.id)
                put("name", tone.name)
                put("instruction", tone.instruction)
                put("icon", tone.icon)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_TONES, jsonArray.toString()).apply()
    }

    fun addTone(context: Context, tone: CustomTone) {
        val list = getTones(context).toMutableList()
        list.add(tone)
        saveTones(context, list)
    }

    fun deleteTone(context: Context, toneId: String) {
        val list = getTones(context).filter { it.id != toneId }
        saveTones(context, list)
    }

    // 2. 번역 대상 언어 관리
    fun getTranslations(context: Context): List<TranslationTarget> {
        val jsonString = getPrefs(context).getString(KEY_TRANSLATIONS, null) ?: return DEFAULT_TRANSLATIONS
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<TranslationTarget>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TranslationTarget(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        targetLanguage = obj.getString("targetLanguage"),
                        flag = obj.optString("flag", "🌐")
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_TRANSLATIONS else list
        } catch (_: Exception) {
            DEFAULT_TRANSLATIONS
        }
    }

    fun saveTranslations(context: Context, list: List<TranslationTarget>) {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("targetLanguage", item.targetLanguage)
                put("flag", item.flag)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_TRANSLATIONS, jsonArray.toString()).apply()
    }

    fun addTranslation(context: Context, item: TranslationTarget) {
        val list = getTranslations(context).toMutableList()
        list.add(item)
        saveTranslations(context, list)
    }

    fun deleteTranslation(context: Context, id: String) {
        val list = getTranslations(context).filter { it.id != id }
        saveTranslations(context, list)
    }
}
