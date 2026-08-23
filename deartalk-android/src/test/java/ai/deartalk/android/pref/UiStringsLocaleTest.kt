package ai.deartalk.android.pref

import ai.deartalk.android.data.pref.UiStrings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UiStringsLocaleTest {

    @Test
    fun testLocaleOverride() {
        // 1. 영어 오버라이드 테스트
        UiStrings.setLocale(Locale.ENGLISH)
        assertEquals("Refine", UiStrings.toneRefine)
        assertEquals("Polite", UiStrings.tonePolite)
        assertEquals("Apply", UiStrings.apply)
        assertEquals("Cancel", UiStrings.cancel)
        assertEquals("ℹ️ About & Help", UiStrings.settingsTabAbout)
        assertEquals("App Version", UiStrings.appVersionLabel)
        assertEquals("Build Timestamp", UiStrings.buildTimestampLabel)
        assertEquals("📖 DearTalk AI User Guide", UiStrings.userGuideTitle)

        // 2. 한국어 오버라이드 테스트
        UiStrings.setLocale(Locale.KOREAN)
        assertEquals("기본다듬기", UiStrings.toneRefine)
        assertEquals("공손하게", UiStrings.tonePolite)
        assertEquals("입력", UiStrings.apply)
        assertEquals("취소", UiStrings.cancel)
        assertEquals("ℹ️ 정보 및 도움말", UiStrings.settingsTabAbout)
        assertEquals("앱 버전", UiStrings.appVersionLabel)
        assertEquals("빌드 타임스탬프", UiStrings.buildTimestampLabel)
        assertEquals("📖 DearTalk AI 사용 설명서", UiStrings.userGuideTitle)
    }
}

