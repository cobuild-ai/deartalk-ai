package ai.deartalk.android.agent

import ai.deartalk.android.data.pref.CustomToneManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 안드로이드 코어 AI 엔진 표준 검증 테스트 스위트
 * - Given / When / Then 구조를 준수하여 제1철칙(0% Fake Rules), 어조 프리셋, 공백 안전성, LLM 태그 정제를 검증합니다.
 */
class CommonCoreEngineTest {

    private lateinit var intentEngine: DearTalkIntentEngine

    @Before
    fun setUp() {
        intentEngine = DearTalkIntentEngine(null, null)
    }

    @Test
    fun `기본_어조_프리셋은_정확히_6개와_지정된_ID를_가진다`() {
        // Given
        val expectedCount = 6
        val expectedIds = listOf(
            "tone_refine",
            "tone_polite",
            "tone_casual",
            "tone_business",
            "tone_funny",
            "tone_cheeky"
        )

        // When
        val tones = CustomToneManager.DEFAULT_TONES

        // Then
        assertEquals("기본 어조 프리셋 개수는 6개여야 합니다.", expectedCount, tones.size)
        assertEquals("기본 어조 프리셋 ID 목록이 일치해야 합니다.", expectedIds, tones.map { it.id })
    }

    @Test
    fun `모델이_로드되지_않았을_때_입력_원문을_왜곡_없이_정직하게_보존한다`() = runBlocking {
        // Given
        val rawInput = "부탁할께 너의 고양이를 가져와"

        // When
        val result = intentEngine.process(rawInput)

        // Then
        assertTrue("처리 결과는 성공 상태여야 합니다.", result is IntentResult.Success)
        val text = (result as IntentResult.Success).text
        assertFalse("인위적인 어미 변조 하드코딩이 포함되어서는 안 됩니다.", text.endsWith("부탁드리겠습니다."))
        assertEquals("모델 부재 시 입력 원문이 그대로 보존되어야 합니다.", rawInput, text)
    }

    @Test
    fun `공백이나_빈_문자열_입력_시_빈_결과를_안전하게_반환한다`() = runBlocking {
        // Given
        val blankInput = "   "

        // When
        val result = intentEngine.process(blankInput)

        // Then
        assertTrue("처리 결과는 성공 상태여야 합니다.", result is IntentResult.Success)
        assertEquals("공백 입력 시 빈 문자열이 반환되어야 합니다.", "", (result as IntentResult.Success).text)
    }

    @Test
    fun `특수_기호_식별자_입력_시_크래시_없이_원문을_보존한다`() = runBlocking {
        // Given
        val symbolInput = "@smilelife"

        // When
        val result = intentEngine.process(symbolInput)

        // Then
        assertTrue("처리 결과는 성공 상태여야 합니다.", result is IntentResult.Success)
        assertEquals("특수 기호 입력 원문이 온전히 보존되어야 합니다.", symbolInput, (result as IntentResult.Success).text)
    }

    @Test
    fun `LLM_출력에서_태그와_라벨_접두사를_깨끗하게_제거한다`() {
        // Given
        val rawLlmOutput = "<start_of_turn>model\n최종 문장: 내일 아침 9시에 뵙겠습니다.<end_of_turn>"
        val expectedCleaned = "내일 아침 9시에 뵙겠습니다."

        // When
        val cleaned = intentEngine.cleanLlmOutput(rawLlmOutput)

        // Then
        assertEquals("LLM 특수 태그 및 '최종 문장:' 라벨이 깔끔하게 정제되어야 합니다.", expectedCleaned, cleaned)
    }
}
