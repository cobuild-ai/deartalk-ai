package ai.deartalk.android.pref

import ai.deartalk.android.data.pref.UiStrings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * 다국어 UI 리소스 및 로케일 동적 전환 단위 테스트
 * - Given / When / Then 패턴을 준수하여 영어 및 한국어 로케일 문자열 일관성을 검증합니다.
 */
class UiStringsLocaleTest {

    @Test
    fun `영문_로케일_설정_시_영문_UI_문자열이_정확히_반환된다`() {
        // Given
        val targetLocale = Locale.ENGLISH

        // When
        UiStrings.setLocale(targetLocale)

        // Then
        assertEquals("Refine", UiStrings.toneRefine)
        assertEquals("Polite", UiStrings.tonePolite)
        assertEquals("Apply", UiStrings.apply)
        assertEquals("Cancel", UiStrings.cancel)
        assertEquals("ℹ️ About & Help", UiStrings.settingsTabAbout)
        assertEquals("App Version", UiStrings.appVersionLabel)
        assertEquals("Last Updated", UiStrings.buildTimestampLabel)
        assertEquals("📖 DearTalk AI Quick Guide", UiStrings.userGuideTitle)
    }

    @Test
    fun `한국어_로케일_설정_시_한국어_UI_문자열이_정확히_반환된다`() {
        // Given
        val targetLocale = Locale.KOREAN

        // When
        UiStrings.setLocale(targetLocale)

        // Then
        assertEquals("기본다듬기", UiStrings.toneRefine)
        assertEquals("공손하게", UiStrings.tonePolite)
        assertEquals("입력", UiStrings.apply)
        assertEquals("취소", UiStrings.cancel)
        assertEquals("ℹ️ 앱 정보 및 도움말", UiStrings.settingsTabAbout)
        assertEquals("앱 버전", UiStrings.appVersionLabel)
        assertEquals("업데이트 일시", UiStrings.buildTimestampLabel)
        assertEquals("📖 DearTalk AI 쉽게 쓰는 법", UiStrings.userGuideTitle)
    }
}
