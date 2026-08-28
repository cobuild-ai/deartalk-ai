package ai.deartalk.android.agent

import ai.deartalk.android.data.pref.CustomTone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DearTalkIntentEngine 언어 감지 및 추론 파이프라인 단위 테스트
 * - Given / When / Then 패턴을 준수하여 언어 판별, 원문 보존 불변식, 톤앤매너 변환 안정성을 검증합니다.
 */
class DearTalkIntentEngineTest {

    private lateinit var intentEngine: DearTalkIntentEngine

    @Before
    fun setUp() {
        intentEngine = DearTalkIntentEngine(null, null)
    }

    @Test
    fun `한글_문자열_포함_여부를_정확히_감지한다`() {
        // Given
        val koreanSample = "안녕하세요"
        val mixedSample = "Hello 안녕하세요"
        val englishOnly = "Hello world"
        val symbolOnly = "12345!@#$"

        // When & Then
        assertTrue("순수 한글은 true를 반환해야 합니다.", DearTalkIntentEngine.hasKorean(koreanSample))
        assertTrue("혼합 문장은 true를 반환해야 합니다.", DearTalkIntentEngine.hasKorean(mixedSample))
        assertFalse("순수 영문은 false를 반환해야 합니다.", DearTalkIntentEngine.hasKorean(englishOnly))
        assertFalse("숫자 및 특수기호는 false를 반환해야 합니다.", DearTalkIntentEngine.hasKorean(symbolOnly))
    }

    @Test
    fun `순수_영문_문자열_여부를_정확히_감지한다`() {
        // Given
        val englishSentence = "Hello how are you"
        val englishQuestion = "Where is the conference room?"
        val mixedText = "Hello 안녕하세요"
        val koreanOnly = "안녕하세요"

        // When & Then
        assertTrue("순수 영문 문장은 true를 반환해야 합니다.", DearTalkIntentEngine.isEnglish(englishSentence))
        assertTrue("특수문자가 포함된 영문 질문은 true를 반환해야 합니다.", DearTalkIntentEngine.isEnglish(englishQuestion))
        assertFalse("한글이 섞인 문장은 false를 반환해야 합니다.", DearTalkIntentEngine.isEnglish(mixedText))
        assertFalse("순수 한글 문장은 false를 반환해야 합니다.", DearTalkIntentEngine.isEnglish(koreanOnly))
    }

    @Test
    fun `모델_미로드시_인위적_왜곡_없이_정직한_원문을_보존한다`() = runBlocking {
        // Given
        val rawInput = "내일 아침 10시 약속 만나자고 하는 거를 공산화 툴로 좀 말해 줄래"

        // When
        val result = intentEngine.process(rawInput)

        // Then
        assertTrue("처리 결과는 성공이어야 합니다.", result is IntentResult.Success)
        val text = (result as IntentResult.Success).text
        assertFalse("하드코딩된 어미 접미사가 추가되어서는 안 됩니다.", text.endsWith("부탁드립니다."))
        assertEquals("입력 원문이 정확히 보존되어야 합니다.", rawInput, text)
    }

    @Test
    fun `공백_입력_시_빈_문자열_결과를_반환한다`() = runBlocking {
        // Given
        val whitespaceInput = "   "

        // When
        val result = intentEngine.process(whitespaceInput)

        // Then
        assertTrue("처리 결과는 성공이어야 합니다.", result is IntentResult.Success)
        assertEquals("빈 문자열이 반환되어야 합니다.", "", (result as IntentResult.Success).text)
    }

    @Test
    fun `영문_입력과_톤_적용_시_모델_미로드_상태에서_원문_영어를_보존한다`() = runBlocking {
        // Given
        val politeTone = CustomTone(
            id = "tone_polite",
            name = "Polite",
            instruction = "Be polite"
        )
        val englishInput = "Do you want to have lunch?"

        // When
        val result = intentEngine.processWithTone(englishInput, politeTone)

        // Then
        assertTrue("처리 결과는 성공이어야 합니다.", result is IntentResult.Success)
        assertEquals("모델 미로드 시 원문 영어가 보존되어야 합니다.", englishInput, (result as IntentResult.Success).text)
    }

    @Test
    fun `외국어_입력_시_모델_미로드_상태에서_원문_언어를_보존한다`() = runBlocking {
        // Given
        val politeTone = CustomTone(
            id = "tone_polite",
            name = "Polite",
            instruction = "Be polite"
        )
        val japaneseInput = "ご飯一緒に食べますか？"

        // When
        val result = intentEngine.processWithTone(japaneseInput, politeTone)

        // Then
        assertTrue("처리 결과는 성공이어야 합니다.", result is IntentResult.Success)
        assertEquals("모델 미로드 시 외국어 원문이 보존되어야 합니다.", japaneseInput, (result as IntentResult.Success).text)
    }

    @Test
    fun `모델_감지_및_초기화_호출_시_로딩_상태_Flow가_안전하게_유지된다`() = runBlocking {
        // Given
        val initialLoaded = intentEngine.isModelLoaded

        // When
        intentEngine.detectAndInitOnDeviceModel()
        val currentFlowValue = intentEngine.isModelLoadedFlow.value

        // Then
        assertFalse("초기 모델 로드 상태는 false여야 합니다.", initialLoaded)
        assertFalse("단위 테스트 환경(모델 파일 부재)에서는 로딩 완료 상태가 false여야 합니다.", currentFlowValue)
    }
}
