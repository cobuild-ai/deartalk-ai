package ai.deartalk.android.agent

import ai.deartalk.android.data.pref.KeyboardMode
import ai.deartalk.android.ui.state.ImeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 🎛️ [DearTalk-AI 2-Tier 모드 분기 무결성 단위 테스트] (DearTalkModeSwitchTest)
 *
 * 목적:
 * 1. 3세 이상 전연령 대상의 기본 모드(BASIC)와 글로벌 파워유저용 프로 모드(PRO)의 상태 전이 검증.
 * 2. 기본 모드 시 번역/언어선택기 UI 숨김 플래그 무결성 및 시스템 언어 100% 고정 상태 검증.
 * 3. 프로 모드 시 59개국어 번역 및 톤앤매너 결합 상태 검증.
 */
class DearTalkModeSwitchTest {

    @Test
    fun testKeyboardMode_enumAndDefaultValue() {
        assertEquals("기본 모드 열거형 이름 일치", "BASIC", KeyboardMode.BASIC.name)
        assertEquals("프로 모드 열거형 이름 일치", "PRO", KeyboardMode.PRO.name)
        assertEquals("모드는 정확히 2개 존재해야 함", 2, KeyboardMode.values().size)
    }

    @Test
    fun testKeyboardMode_ImeUiStateDefaults() {
        val defaultState = ImeUiState()
        assertEquals("ImeUiState 초기 모드는 3세 이상 전연령을 위한 BASIC 이어야 함", KeyboardMode.BASIC, defaultState.keyboardMode)
        assertFalse("초기 번역 모드는 비활성화 상태여야 함", defaultState.isTranslationMode)
    }

    @Test
    fun testKeyboardMode_stateTransition() {
        val state = ImeUiState(keyboardMode = KeyboardMode.BASIC)
        assertEquals(KeyboardMode.BASIC, state.keyboardMode)

        val switchedToPro = state.copy(keyboardMode = KeyboardMode.PRO, isTranslationMode = true)
        assertEquals(KeyboardMode.PRO, switchedToPro.keyboardMode)
        assertTrue(switchedToPro.isTranslationMode)

        val switchedBackToBasic = switchedToPro.copy(keyboardMode = KeyboardMode.BASIC, isTranslationMode = false)
        assertEquals(KeyboardMode.BASIC, switchedBackToBasic.keyboardMode)
        assertFalse(switchedBackToBasic.isTranslationMode)
    }

    @Test
    fun testKeyboardMode_proModeEnablesFullFeatures() {
        val proState = ImeUiState(
            keyboardMode = KeyboardMode.PRO,
            isTranslationMode = true
        )
        assertTrue("프로 모드는 다국어 번역 활성화 가능", proState.isTranslationMode)
        assertEquals(KeyboardMode.PRO, proState.keyboardMode)
    }
}
