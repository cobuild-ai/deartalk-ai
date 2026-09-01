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
        assertEquals("Last Updated", UiStrings.buildTimestampLabel)
        assertEquals("📖 DearTalk AI Quick Guide", UiStrings.userGuideTitle)
        assertEquals("🎙️ DearTalk Voice Studio", UiStrings.voiceStudioTitle)
        assertEquals("✨ Tone Transformation", UiStrings.modeToneTransform)
        assertEquals("🌐 Live Interpretation", UiStrings.modeLiveTranslation)
        assertEquals("👩 Female Voice", UiStrings.voiceFemale)
        assertEquals("👨 Male Voice", UiStrings.voiceMale)
        assertEquals("What You Said (STT)", UiStrings.rawSttTitle)
        assertEquals("✨ AI Refined & Translated", UiStrings.aiResultTitle)
        assertEquals("🌟 Qwen 1.7B Pro", UiStrings.tierBadgeHigh)
        assertEquals("🟢 Gemma 2B Base", UiStrings.tierBadgeBase)
        assertEquals("⚡ STT Only (Needs AI Pack)", UiStrings.tierBadgeSttOnly)
        assertEquals("Polish into a natural, fluent, and well-structured complete sentence preserving original intent.", UiStrings.instRefine)

        // 2. 한국어 오버라이드 테스트
        UiStrings.setLocale(Locale.KOREAN)
        assertEquals("기본다듬기", UiStrings.toneRefine)
        assertEquals("공손하게", UiStrings.tonePolite)
        assertEquals("입력", UiStrings.apply)
        assertEquals("취소", UiStrings.cancel)
        assertEquals("ℹ️ 앱 정보 및 도움말", UiStrings.settingsTabAbout)
        assertEquals("앱 버전", UiStrings.appVersionLabel)
        assertEquals("업데이트 일시", UiStrings.buildTimestampLabel)
        assertEquals("📖 DearTalk AI 쉽게 쓰는 법", UiStrings.userGuideTitle)
        assertEquals("🎙️ DearTalk 보이스 스튜디오", UiStrings.voiceStudioTitle)
        assertEquals("✨ 고운말 톤 변환", UiStrings.modeToneTransform)
        assertEquals("🌐 실시간 다국어 통역", UiStrings.modeLiveTranslation)
        assertEquals("👩 여성 음성", UiStrings.voiceFemale)
        assertEquals("👨 남성 음성", UiStrings.voiceMale)
        assertEquals("내가 말한 내용 (STT)", UiStrings.rawSttTitle)
        assertEquals("✨ AI 조율 및 번역 결과", UiStrings.aiResultTitle)
        assertEquals("🌟 Qwen 1.7B Pro", UiStrings.tierBadgeHigh)
        assertEquals("🟢 Gemma 2B Base", UiStrings.tierBadgeBase)
        assertEquals("⚡ STT 모드 (AI팩 필요)", UiStrings.tierBadgeSttOnly)
        assertEquals("문맥을 살려 중복과 어색한 끊김 없이 자연스럽고 유려한 완성형 문장으로 다듬어 작성하세요.", UiStrings.instRefine)

        // 3. 인도네시아어 오버라이드 테스트
        UiStrings.setLocale(Locale("id", "ID"))
        assertEquals("Rapikan", UiStrings.toneRefine)
        assertEquals("Sopan", UiStrings.tonePolite)
        assertEquals("🎙️ Studio Suara DearTalk", UiStrings.voiceStudioTitle)
        assertEquals("✨ Transformasi Gaya Bicara", UiStrings.modeToneTransform)
        assertEquals("🌐 Penerjemah Langsung", UiStrings.modeLiveTranslation)
        assertEquals("👩 Suara Wanita", UiStrings.voiceFemale)
        assertEquals("👨 Suara Pria", UiStrings.voiceMale)
        assertEquals("Yang Anda Katakan (STT)", UiStrings.rawSttTitle)
        assertEquals("✨ Hasil AI & Terjemahan", UiStrings.aiResultTitle)
        assertEquals("🌟 Qwen 1.7B Pro", UiStrings.tierBadgeHigh)
        assertEquals("🟢 Gemma 2B Base", UiStrings.tierBadgeBase)
        assertEquals("⚡ Mode STT (Perlu Paket AI)", UiStrings.tierBadgeSttOnly)
        assertEquals("Rapikan menjadi satu kalimat lengkap yang alami dan lancar tanpa pengulangan kata yang janggal.", UiStrings.instRefine)
    }
}
