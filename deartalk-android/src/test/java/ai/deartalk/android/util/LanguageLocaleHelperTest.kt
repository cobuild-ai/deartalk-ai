package ai.deartalk.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LanguageLocaleHelperTest {

    @Test
    fun testHasKorean_withVariousScripts() {
        assertTrue(LanguageLocaleHelper.hasKorean("안녕하세요"))
        assertTrue(LanguageLocaleHelper.hasKorean("Hello 안녕"))
        assertTrue(LanguageLocaleHelper.hasKorean("ㅋㅋㅋ"))
        assertTrue(LanguageLocaleHelper.hasKorean("ㄱㅏ"))
        assertFalse(LanguageLocaleHelper.hasKorean("Hello world! 1234"))
        assertFalse(LanguageLocaleHelper.hasKorean("こんにちは"))
        assertFalse(LanguageLocaleHelper.hasKorean("你好"))
    }

    @Test
    fun testHasJapanese() {
        assertTrue(LanguageLocaleHelper.hasJapanese("こんにちは"))
        assertTrue(LanguageLocaleHelper.hasJapanese("テスト"))
        assertFalse(LanguageLocaleHelper.hasJapanese("Hello"))
        assertFalse(LanguageLocaleHelper.hasJapanese("안녕하세요"))
    }

    @Test
    fun testHasChinese() {
        assertTrue(LanguageLocaleHelper.hasChinese("你好世界"))
        assertTrue(LanguageLocaleHelper.hasChinese("谢谢"))
        // 한글이나 일어에 포함된 한자는 배제되는 규칙 검증
        assertFalse(LanguageLocaleHelper.hasChinese("안녕하세요"))
        assertFalse(LanguageLocaleHelper.hasChinese("日本語です"))
    }

    @Test
    fun testHasThai() {
        assertTrue(LanguageLocaleHelper.hasThai("สวัสดี"))
        assertFalse(LanguageLocaleHelper.hasThai("Hello"))
    }

    @Test
    fun testIsEnglish() {
        assertTrue(LanguageLocaleHelper.isEnglish("Hello world"))
        assertTrue(LanguageLocaleHelper.isEnglish("Can you please help me?"))
        assertTrue(LanguageLocaleHelper.isEnglish("AI 2.0"))
        assertFalse(LanguageLocaleHelper.isEnglish("안녕하세요 Hello"))
        assertFalse(LanguageLocaleHelper.isEnglish("12345!@#$"))
        assertFalse(LanguageLocaleHelper.isEnglish(""))
    }

    @Test
    fun testDetectLanguageCode() {
        assertEquals("KO", LanguageLocaleHelper.detectLanguageCode("오늘 회의 몇 시야?"))
        assertEquals("EN", LanguageLocaleHelper.detectLanguageCode("What time is our meeting?"))
        assertEquals("JA", LanguageLocaleHelper.detectLanguageCode("会議は何時ですか？"))
        assertEquals("ZH", LanguageLocaleHelper.detectLanguageCode("会议几点开始？"))
        assertEquals("TH", LanguageLocaleHelper.detectLanguageCode("การประชุมกี่โมง"))
        assertEquals("ID", LanguageLocaleHelper.detectLanguageCode("12345", fallback = "ID"))
    }

    @Test
    fun testGetLocaleForCode() {
        assertEquals(Locale.US, LanguageLocaleHelper.getLocaleForCode("EN"))
        assertEquals(Locale.FRANCE, LanguageLocaleHelper.getLocaleForCode("FR"))
        assertEquals(Locale.GERMANY, LanguageLocaleHelper.getLocaleForCode("DE"))
        assertEquals(Locale.JAPAN, LanguageLocaleHelper.getLocaleForCode("JA"))
        assertEquals(Locale.KOREAN, LanguageLocaleHelper.getLocaleForCode("UNKNOWN"))
    }

    @Test
    fun testGetLanguageTag() {
        assertEquals("ko-KR", LanguageLocaleHelper.getLanguageTag(Locale.KOREAN))
        assertEquals("en-US", LanguageLocaleHelper.getLanguageTag(Locale.US))
        assertEquals("ja-JP", LanguageLocaleHelper.getLanguageTag(Locale.JAPAN))
    }
}
