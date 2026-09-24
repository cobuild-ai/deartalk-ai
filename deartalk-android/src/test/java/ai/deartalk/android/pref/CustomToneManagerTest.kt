package ai.deartalk.android.pref

import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.CustomToneManager
import ai.deartalk.android.data.pref.TranslationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomToneManagerTest {

    @Test
    fun testDefaultTones_integrityAndOrder() {
        val defaultTones = CustomToneManager.DEFAULT_TONES
        assertTrue(defaultTones.isNotEmpty())
        assertEquals(6, defaultTones.size)

        val toneIds = defaultTones.map { it.id }
        assertEquals(
            listOf("tone_refine", "tone_polite", "tone_casual", "tone_business", "tone_funny", "tone_cheeky"),
            toneIds
        )

        defaultTones.forEach { tone ->
            assertTrue(tone.name.isNotBlank())
            assertTrue(tone.instruction.isNotBlank())
            assertTrue(tone.icon.isNotBlank())
        }
    }

    @Test
    fun testDefaultTranslations_containsCoreLanguages() {
        val translations = CustomToneManager.DEFAULT_TRANSLATIONS
        assertTrue(translations.isNotEmpty())

        val codes = translations.map { it.code }
        assertTrue(codes.contains("EN"))
        assertTrue(codes.contains("ID"))
        assertTrue(codes.contains("JA"))
        assertTrue(codes.contains("ZH"))

        translations.forEach { trans ->
            assertTrue(trans.name.isNotBlank())
            assertTrue(trans.targetLanguage.isNotBlank())
            assertTrue(trans.flag.isNotBlank())
        }
    }

    @Test
    fun testCustomTone_dataModel() {
        val custom = CustomTone(
            name = "테스트 톤",
            instruction = "친구에게 장난치듯이 답해줘",
            icon = "🎨"
        )
        assertNotNull(custom.id)
        assertEquals("테스트 톤", custom.name)
        assertEquals("🎨", custom.icon)
    }

    @Test
    fun testTranslationTarget_dataModel() {
        val target = TranslationTarget(
            code = "KO",
            name = "한국어",
            targetLanguage = "한국어(Korean)",
            flag = "🇰🇷"
        )
        assertNotNull(target.id)
        assertEquals("KO", target.code)
        assertEquals("🇰🇷", target.flag)
    }
}
