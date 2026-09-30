package ai.deartalk.android.agent

import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ui.state.ImeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 🌐 [DearTalk-AI 키보드 시스템 언어 자동 연동 및 통합 모드 검증 테스트]
 *
 * 목적:
 * 1. 프로/베이직 분리 철폐 확인: 단일 통합 키보드로써 ImeUiState 기본값 무결성 검증.
 * 2. 키보드 앱 특성에 따른 시스템 언어 100% 자동 맞춤 검증.
 */
class DearTalkModeSwitchTest {

    @Test
    fun testUnifiedKeyboard_ImeUiStateDefaults() {
        val defaultState = ImeUiState()
        assertEquals("초기 텍스트는 빈 문자열이어야 함", "", defaultState.recognizedText)
        assertEquals("초기 AI 텍스트는 빈 문자열이어야 함", "", defaultState.aiText)
    }

    @Test
    fun testUnifiedKeyboard_toneSelectionFreedom() {
        val defaultState = ImeUiState()
        val customTone = ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES[1]
        val updated = defaultState.copy(selectedTone = customTone)
        assertEquals("톤 변경 즉시 반영", customTone, updated.selectedTone)
    }

    @Test
    fun testSystemLocaleAutoAdaptation() {
        // 시스템 로케일이 한국어일 때
        Locale.setDefault(Locale.KOREAN)
        UiStrings.setLocale(Locale.getDefault())
        assertTrue("시스템 언어가 한국어일 때 isKo는 true", UiStrings.isKo)
        assertFalse("시스템 언어가 한국어일 때 isId는 false", UiStrings.isId)

        // 시스템 로케일이 영어일 때
        Locale.setDefault(Locale.ENGLISH)
        UiStrings.setLocale(Locale.getDefault())
        assertFalse("시스템 언어가 영어일 때 isKo는 false", UiStrings.isKo)
        assertFalse("시스템 언어가 영어일 때 isId는 false", UiStrings.isId)

        // 시스템 로케일이 인도네시아어일 때
        Locale.setDefault(Locale("id", "ID"))
        UiStrings.setLocale(Locale.getDefault())
        assertTrue("시스템 언어가 인도네시아어일 때 isId는 true", UiStrings.isId)
        assertFalse("시스템 언어가 인도네시아어일 때 isKo는 false", UiStrings.isKo)

        // 테스트 종료 후 기본 한국어로 복원
        Locale.setDefault(Locale.KOREAN)
        UiStrings.setLocale(Locale.KOREAN)
    }
}
