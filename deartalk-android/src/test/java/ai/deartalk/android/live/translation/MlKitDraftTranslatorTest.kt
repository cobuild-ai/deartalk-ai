package ai.deartalk.android.live.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MlKitDraftTranslatorTest {

    @Test
    fun testLanguageTagResolution() {
        assertEquals("ko", MlKitDraftTranslator.resolveLanguageTag("KO"))
        assertEquals("ko", MlKitDraftTranslator.resolveLanguageTag("ko-KR"))
        assertEquals("id", MlKitDraftTranslator.resolveLanguageTag("ID"))
        assertEquals("id", MlKitDraftTranslator.resolveLanguageTag("id_ID"))
        assertEquals("en", MlKitDraftTranslator.resolveLanguageTag("EN"))
        assertEquals("ja", MlKitDraftTranslator.resolveLanguageTag("JA"))
        assertEquals("zh", MlKitDraftTranslator.resolveLanguageTag("ZH"))
        assertEquals("es", MlKitDraftTranslator.resolveLanguageTag("ES"))
        assertEquals("fr", MlKitDraftTranslator.resolveLanguageTag("FR"))
        assertEquals("de", MlKitDraftTranslator.resolveLanguageTag("DE"))
        assertEquals("vi", MlKitDraftTranslator.resolveLanguageTag("VI"))
        assertEquals("th", MlKitDraftTranslator.resolveLanguageTag("TH"))
        assertEquals("ar", MlKitDraftTranslator.resolveLanguageTag("AR"))
        assertEquals("ru", MlKitDraftTranslator.resolveLanguageTag("RU"))
    }

    @Test
    fun testSupportedLanguages() {
        assertTrue(MlKitDraftTranslator.isSupported("KO"))
        assertTrue(MlKitDraftTranslator.isSupported("EN"))
        assertTrue(MlKitDraftTranslator.isSupported("ID"))
        assertTrue(MlKitDraftTranslator.isSupported("JA"))
        assertTrue(MlKitDraftTranslator.isSupported("ES"))
        assertTrue(MlKitDraftTranslator.isSupported("FR"))
        assertTrue(MlKitDraftTranslator.isSupported("DE"))

        assertFalse(MlKitDraftTranslator.isSupported("INVALID_LANG_CODE_XYZ"))
    }
}
