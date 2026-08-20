package ai.deartalk.android.agent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DearTalkIntentEngineTest {

    private lateinit var intentEngine: DearTalkIntentEngine

    @Before
    fun setUp() {
        intentEngine = DearTalkIntentEngine(null, null)
    }

    @Test
    fun `모델_미로드시_인위적_왜곡_없이_정직한_원문_보존_테스트`() = runBlocking {
        val input = "내일 아침 10시 약속 만나자고 하는 거를 공산화 툴로 좀 말해 줄래"
        val result = intentEngine.process(input)
        assertTrue(result is IntentResult.Success)
        val text = (result as IntentResult.Success).text

        // 하드코딩으로 "부탁드립니다"가 붙거나 깨지는 기괴한 왜곡이 없어야 함
        assertFalse(text.endsWith("부탁드립니다."))
        assertEquals(input, text)
    }

    @Test
    fun `빈_문자열_처리_테스트`() = runBlocking {
        val result = intentEngine.process("   ")
        assertTrue(result is IntentResult.Success)
        assertEquals("", (result as IntentResult.Success).text)
    }

    @Test
    fun `다국어_번역_빈문자열_및_안전_처리_테스트`() = runBlocking {
        val target = ai.deartalk.android.data.pref.TranslationTarget(
            id = "test_en",
            name = "영어",
            targetLanguage = "영어(English)",
            flag = "🇺🇸"
        )
        val resultEmpty = intentEngine.processWithTranslation("   ", target)
        assertTrue(resultEmpty is IntentResult.Success)
        assertEquals("", (resultEmpty as IntentResult.Success).text)

        val input = "안녕하세요 반갑습니다"
        val result = intentEngine.processWithTranslation(input, target)
        assertTrue(result is IntentResult.Success)
        // 모델 미로드 시 왜곡 없이 원문 반환
        assertEquals(input, (result as IntentResult.Success).text)
    }

    @Test
    fun `톤앤매너_다국어_입력_및_모델미로드_원문보존_테스트`() = runBlocking {
        val tone = ai.deartalk.android.data.pref.CustomTone(
            id = "tone_polite",
            name = "Polite",
            instruction = "Be polite"
        )
        
        // 영어 입력 테스트
        val englishInput = "Do you want to have lunch?"
        val resultEn = intentEngine.processWithTone(englishInput, tone)
        assertTrue(resultEn is IntentResult.Success)
        assertEquals(englishInput, (resultEn as IntentResult.Success).text)

        // 일본어 입력 테스트
        val japaneseInput = "ご飯一緒に食べますか？"
        val resultJa = intentEngine.processWithTone(japaneseInput, tone)
        assertTrue(resultJa is IntentResult.Success)
        assertEquals(japaneseInput, (resultJa as IntentResult.Success).text)
    }
}
