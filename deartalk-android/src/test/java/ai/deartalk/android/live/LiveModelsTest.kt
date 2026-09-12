package ai.deartalk.android.live

import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import ai.deartalk.android.live.data.LiveSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 🧪 DearTalk Live 데이터 모델 및 비즈니스 로직 단위 테스트
 */
class LiveModelsTest {

    @Test
    fun testLiveSessionCreation() {
        val session = LiveSession(myLang = "KO", partnerLang = "EN")
        assertNotNull(session.id)
        assertTrue(session.title.contains("Live Voice"))
        assertEquals("KO", session.myLang)
        assertEquals("EN", session.partnerLang)
        assertTrue(session.createdAt > 0)
    }

    @Test
    fun testLiveMessageCreation() {
        val message = LiveMessage(
            sessionId = "test-session-123",
            sender = LiveSender.ME,
            rawText = "안녕하세요",
            refinedText = "Hello",
            sourceLang = "KO",
            targetLang = "EN",
            tone = "✨ 기본다듬기"
        )

        assertEquals("test-session-123", message.sessionId)
        assertEquals(LiveSender.ME, message.sender)
        assertEquals("안녕하세요", message.rawText)
        assertEquals("Hello", message.refinedText)
        assertEquals("KO", message.sourceLang)
        assertEquals("EN", message.targetLang)
        assertEquals("✨ 기본다듬기", message.tone)
    }

    @Test
    fun testPartnerMessageWithoutTone() {
        val partnerMsg = LiveMessage(
            sessionId = "test-session-123",
            sender = LiveSender.PARTNER,
            rawText = "Nice to meet you",
            refinedText = "만나서 반갑습니다",
            sourceLang = "EN",
            targetLang = "KO"
        )

        assertEquals(LiveSender.PARTNER, partnerMsg.sender)
        assertEquals(null, partnerMsg.tone)
        assertEquals("EN", partnerMsg.sourceLang)
        assertEquals("KO", partnerMsg.targetLang)
    }

    @Test
    fun testSessionMarkdownExportFormat() {
        val session = LiveSession(id = "test-session-abc", myLang = "KO", partnerLang = "EN")
        val msg1 = LiveMessage(
            sessionId = session.id,
            sender = LiveSender.ME,
            rawText = "얼마인가요?",
            refinedText = "How much is it?",
            sourceLang = "KO",
            targetLang = "EN",
            tone = "✨ 기본다듬기"
        )
        val msg2 = LiveMessage(
            sessionId = session.id,
            sender = LiveSender.PARTNER,
            rawText = "It is ten dollars",
            refinedText = "10달러입니다",
            sourceLang = "EN",
            targetLang = "KO"
        )

        val messages = listOf(msg1, msg2)
        val sb = StringBuilder()
        sb.appendLine("# 🎙️ DearTalk Live 대화 세션 기록")
        sb.appendLine("- **세션 ID**: `${session.id}`")
        sb.appendLine("- **언어 설정**: ${session.myLang} ⇄ ${session.partnerLang}")
        messages.forEachIndexed { idx, m ->
            val label = if (m.sender == LiveSender.ME) "🙋 나" else "👤 상대방"
            sb.appendLine("### ${idx + 1}. $label")
            sb.appendLine("- **원문 (${m.sourceLang})**: ${m.rawText}")
            sb.appendLine("- **AI 번역/정제 (${m.targetLang})**: ${m.refinedText}")
        }

        val output = sb.toString()
        assertTrue(output.contains("DearTalk Live 대화 세션 기록"))
        assertTrue(output.contains("얼마인가요?"))
        assertTrue(output.contains("How much is it?"))
        assertTrue(output.contains("It is ten dollars"))
        assertTrue(output.contains("10달러입니다"))
    }

    @Test
    fun testLanguageModelStatusHierarchy() {
        val states: List<ai.deartalk.android.stt.LanguageModelStatus> = listOf(
            ai.deartalk.android.stt.LanguageModelStatus.Idle,
            ai.deartalk.android.stt.LanguageModelStatus.Checking,
            ai.deartalk.android.stt.LanguageModelStatus.Scheduled,
            ai.deartalk.android.stt.LanguageModelStatus.Downloading(45),
            ai.deartalk.android.stt.LanguageModelStatus.Installed,
            ai.deartalk.android.stt.LanguageModelStatus.SupportedOnline,
            ai.deartalk.android.stt.LanguageModelStatus.Error(13)
        )

        assertEquals(7, states.size)
        val downloading = states[3] as ai.deartalk.android.stt.LanguageModelStatus.Downloading
        assertEquals(45, downloading.progress)
        val error = states[6] as ai.deartalk.android.stt.LanguageModelStatus.Error
        assertEquals(13, error.code)
    }
}
