package ai.deartalk.android.ui.state

import ai.deartalk.android.data.ActiveAiTier
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.data.SpeechIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {

    @Test
    fun testMainUiState_defaultsAndCopy() {
        val defaultState = MainUiState()
        assertFalse(defaultState.isImeEnabled)
        assertFalse(defaultState.isImeSelected)
        assertFalse(defaultState.isModelLoaded)
        assertTrue(defaultState.isAutoLanguage)
        assertEquals("ko", defaultState.selectedLanguageCode)
        assertEquals(KoreanKeyboardType.DUBEOLSIK, defaultState.selectedKoreanKeyboardType)

        val updated = defaultState.copy(
            isImeEnabled = true,
            isModelLoaded = true,
            isAutoLanguage = false,
            selectedLanguageCode = "en",
            recognizedLiveText = "안녕하세요"
        )
        assertTrue(updated.isImeEnabled)
        assertTrue(updated.isModelLoaded)
        assertFalse(updated.isAutoLanguage)
        assertEquals("en", updated.selectedLanguageCode)
        assertEquals("안녕하세요", updated.recognizedLiveText)
    }

    @Test
    fun testImeUiState_defaultsAndCopy() {
        val defaultIme = ImeUiState()
        assertEquals(MicUiState.IDLE, defaultIme.micUiState)
        assertEquals(ActiveAiTier.STT_ONLY, defaultIme.activeTier)
        assertEquals(SpeechIntent.AUTO, defaultIme.selectedSpeechIntent)

        val updated = defaultIme.copy(
            micUiState = MicUiState.LISTENING,
            selectedSpeechIntent = SpeechIntent.QUESTION
        )
        assertEquals(MicUiState.LISTENING, updated.micUiState)
        assertEquals(SpeechIntent.QUESTION, updated.selectedSpeechIntent)
    }

    @Test
    fun testImeUiState_resetSelections_resetsToneAndSpeechIntent() {
        // 사용자가 '정중하게' 톤과 '질문' 화행을 선택한 상태
        val politeTone = ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES[1] // 👔 정중하게
        val customizedState = ImeUiState(
            selectedTone = politeTone,
            selectedSpeechIntent = SpeechIntent.QUESTION,
            detectedSpeechIntent = SpeechIntent.QUESTION,
            aiText = "식사하셨습니까?",
            recognizedText = "밥 먹었어"
        )
        assertEquals("tone_polite", customizedState.selectedTone.id)
        assertEquals(SpeechIntent.QUESTION, customizedState.selectedSpeechIntent)

        // 메시지 전송 또는 삭제 시 초기화 수행
        val resetState = customizedState.resetSelections()

        // 🌟 [UX 검증]: 톤앤매너는 기본다듬기(✨), 화행은 AUTO로 100% 원복되어야 함
        assertEquals(ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES.first().id, resetState.selectedTone.id)
        assertEquals(SpeechIntent.AUTO, resetState.selectedSpeechIntent)
        assertEquals(SpeechIntent.AUTO, resetState.detectedSpeechIntent)
    }



    @Test
    fun testMockLlmEngine_generatesOutput() = kotlinx.coroutines.runBlocking {
        val engine = ai.deartalk.android.agent.engine.MockLlmEngine("테스트 변환 성공")
        assertTrue(engine.isModelLoaded)
        val result = engine.generate("프롬프트")
        assertEquals("테스트 변환 성공", result)
    }

    @Test
    fun testImeUiEvent_typesInstantiable() {
        val event1 = ai.deartalk.android.ime.ui.ImeUiEvent.MainMicClick
        val event2 = ai.deartalk.android.ime.ui.ImeUiEvent.ApplyAiText("테스트")
        assertTrue(event1 is ai.deartalk.android.ime.ui.ImeUiEvent)
        assertEquals("테스트", event2.text)
    }

    @Test
    fun testMainUiEvent_typesInstantiable() {
        val event1 = ai.deartalk.android.ui.main.MainUiEvent.ToggleMic
        val event2 = ai.deartalk.android.ui.main.MainUiEvent.TestPreset("예시")
        assertTrue(event1 is ai.deartalk.android.ui.main.MainUiEvent)
        assertEquals("예시", event2.preset)
    }
}
