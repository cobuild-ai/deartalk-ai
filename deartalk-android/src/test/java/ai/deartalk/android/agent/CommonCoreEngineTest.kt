package ai.deartalk.android.agent

import ai.deartalk.android.data.pref.CustomToneManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cross-Platform Common Standard Test Suite (Android)
 * - Mirrors the exact same test scenarios and assertions as macOS (DearTalkMacRunner).
 */
class CommonCoreEngineTest {

    private lateinit var intentEngine: DearTalkIntentEngine

    @Before
    fun setUp() {
        intentEngine = DearTalkIntentEngine(null, null)
    }

    // [Test 1] 6 Unified Tone Presets Verification
    @Test
    fun testUnifiedTonePresetsCountAndIds() {
        val tones = CustomToneManager.DEFAULT_TONES
        assertEquals("Default tone presets count must be 6", 6, tones.size)
        val expectedIds = listOf("tone_refine", "tone_polite", "tone_casual", "tone_business", "tone_funny", "tone_cheeky")
        assertEquals(expectedIds, tones.map { it.id })
    }

    // [Test 2] Zero Fake Rules & Original Preservation Verification (The 1st Principle)
    @Test
    fun testOriginalTextPreservationWithoutFakeHardcoding() = runBlocking {
        val input = "부탁할께 너의 고양이를 가져와"
        val result = intentEngine.process(input)
        assertTrue(result is IntentResult.Success)
        val text = (result as IntentResult.Success).text

        assertFalse("Must never append hardcoded suffix hacks", text.endsWith("부탁드리겠습니다."))
        assertEquals("Must honestly preserve original text when model is not loaded", input, text)
    }

    // [Test 3] Empty & Whitespace Input Safety
    @Test
    fun testEmptyAndWhitespaceInputHandling() = runBlocking {
        val emptyResult = intentEngine.process("   ")
        assertTrue(emptyResult is IntentResult.Success)
        assertEquals("", (emptyResult as IntentResult.Success).text)
    }

    // [Test 4] Special Symbol & Single Identifier Safety (@smilelife)
    @Test
    fun testSpecialSymbolHandling() = runBlocking {
        val symbolInput = "@smilelife"
        val result = intentEngine.process(symbolInput)
        assertTrue(result is IntentResult.Success)
        assertEquals(symbolInput, (result as IntentResult.Success).text)
    }

    // [Test 5] LLM Output Tag Cleansing Filter
    @Test
    fun testLlmOutputCleansingFilter() {
        val rawLlm = "<start_of_turn>model\n최종 문장: 내일 아침 9시에 뵙겠습니다.<end_of_turn>"
        val cleaned = intentEngine.cleanLlmOutput(rawLlm)
        assertEquals("내일 아침 9시에 뵙겠습니다.", cleaned)
    }
}
