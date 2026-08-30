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
        intentEngine = DearTalkIntentEngine(null)
    }

    @Test
    fun `언어_감지_유틸_테스트`() {
        assertTrue(DearTalkIntentEngine.hasKorean("안녕하세요"))
        assertTrue(DearTalkIntentEngine.hasKorean("Hello 안녕하세요"))
        assertFalse(DearTalkIntentEngine.hasKorean("Hello world"))
        assertFalse(DearTalkIntentEngine.hasKorean("12345!@#$"))

        assertTrue(DearTalkIntentEngine.isEnglish("Hello how are you"))
        assertTrue(DearTalkIntentEngine.isEnglish("Where is the conference room?"))
        assertTrue(DearTalkIntentEngine.hasKorean("나는 AI를 이용해서 생성했어요"))
        assertFalse(DearTalkIntentEngine.isEnglish("나는 AI를 이용해서 생성했어요"))
    }

    @Test
    fun `영한_혼용_입력_및_모델미로드_무왜곡_원문보존_테스트`() = runBlocking {
        val mixedInput = "나는 AI를 이용해서 생성했어요"
        val result = intentEngine.process(mixedInput)
        assertTrue(result is IntentResult.Success)
        val text = (result as IntentResult.Success).text
        assertEquals(mixedInput, text)
    }

    @Test
    fun `모델_미로드시_인위적_왜곡_없이_정직한_원문_보존_테스트`() = runBlocking {
        val input = "내일 아침 10시 약속 만나자고 하는 거를 공산화 툴로 좀 말해 줄래"
        val result = intentEngine.process(input)
        assertTrue(result is IntentResult.Success)
        val text = (result as IntentResult.Success).text

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

    @Test
    fun `모델_감지_및_초기화_메소드와_Flow_갱신_테스트`() = runBlocking {
        assertFalse(intentEngine.isModelLoaded)
        assertFalse(intentEngine.isModelLoadedFlow.value)
        intentEngine.detectAndInitOnDeviceModel()
        assertFalse(intentEngine.isModelLoadedFlow.value)
    }
}
