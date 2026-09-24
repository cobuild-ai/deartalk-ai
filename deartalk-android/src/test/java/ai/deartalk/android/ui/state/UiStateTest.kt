package ai.deartalk.android.ui.state

import ai.deartalk.android.data.ActiveAiTier
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.live.data.SpeechIntent
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
        assertEquals("ko", defaultState.selectedLanguageCode)
        assertEquals(KoreanKeyboardType.DUBEOLSIK, defaultState.selectedKoreanKeyboardType)

        val updated = defaultState.copy(
            isImeEnabled = true,
            isModelLoaded = true,
            recognizedLiveText = "안녕하세요"
        )
        assertTrue(updated.isImeEnabled)
        assertTrue(updated.isModelLoaded)
        assertEquals("안녕하세요", updated.recognizedLiveText)
    }

    @Test
    fun testImeUiState_defaultsAndCopy() {
        val defaultIme = ImeUiState()
        assertEquals(MicUiState.IDLE, defaultIme.micUiState)
        assertEquals(ActiveAiTier.STT_ONLY, defaultIme.activeTier)
        assertEquals(SpeechIntent.AUTO, defaultIme.selectedSpeechIntent)
        assertFalse(defaultIme.isTranslationMode)

        val updated = defaultIme.copy(
            micUiState = MicUiState.LISTENING,
            isTranslationMode = true,
            selectedSpeechIntent = SpeechIntent.QUESTION
        )
        assertEquals(MicUiState.LISTENING, updated.micUiState)
        assertTrue(updated.isTranslationMode)
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
    fun testLiveUiState_defaultsAndCopy() {
        val defaultLive = LiveUiState()
        assertEquals(ai.deartalk.android.live.ActiveSpeaker.NONE, defaultLive.activeSpeaker)
        assertEquals("KO", defaultLive.myLang)
        assertEquals("EN", defaultLive.partnerLang)
        assertEquals(SpeechIntent.AUTO, defaultLive.myIntent)
        assertFalse(defaultLive.isProcessing)
        assertFalse(defaultLive.isFlipViewEnabled)

        val updated = defaultLive.copy(
            activeSpeaker = ai.deartalk.android.live.ActiveSpeaker.ME,
            myLang = "ID",
            partnerLang = "KO",
            myIntent = SpeechIntent.REQUEST,
            isProcessing = true,
            isFlipViewEnabled = true,
            streamingText = "Bisa tolong bantu saya?"
        )
        assertEquals(ai.deartalk.android.live.ActiveSpeaker.ME, updated.activeSpeaker)
        assertEquals("ID", updated.myLang)
        assertEquals("KO", updated.partnerLang)
        assertEquals(SpeechIntent.REQUEST, updated.myIntent)
        assertTrue(updated.isProcessing)
        assertTrue(updated.isFlipViewEnabled)
        assertEquals("Bisa tolong bantu saya?", updated.streamingText)
    }
}
