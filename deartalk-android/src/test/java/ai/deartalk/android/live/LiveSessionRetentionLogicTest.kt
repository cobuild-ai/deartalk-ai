package ai.deartalk.android.live

import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import ai.deartalk.android.live.data.LiveSession
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.live.data.formatSourcePunctuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class LiveSessionRetentionLogicTest {



    @Test
    fun testFormatSourcePunctuation() {
        // translationHasQuestion = true -> 물음표 부착
        assertEquals("밥 먹었어?", formatSourcePunctuation("밥 먹었어", SpeechIntent.QUESTION, translationHasQuestion = true))
        assertEquals("밥 먹었어?", formatSourcePunctuation("밥 먹었어?", SpeechIntent.QUESTION, translationHasQuestion = true))

        // STATEMENT intent -> 마침표 부착
        assertEquals("공부 중이야.", formatSourcePunctuation("공부 중이야", SpeechIntent.STATEMENT))
        assertEquals("공부 중이야.", formatSourcePunctuation("공부 중이야.", SpeechIntent.STATEMENT))

        // REQUEST intent -> 원문 보존
        assertEquals("도와주세요", formatSourcePunctuation("도와주세요", SpeechIntent.REQUEST))
    }

    @Test
    fun testLiveMessageCreation() {
        val msg = LiveMessage(
            id = "msg_1",
            sessionId = "session_1",
            sender = LiveSender.ME,
            rawText = "Hello",
            refinedText = "Hello.",
            sourceLang = "EN",
            targetLang = "KO",
            tone = "정중한"
        )
        assertEquals("msg_1", msg.id)
        assertEquals(LiveSender.ME, msg.sender)
        assertEquals("정중한", msg.tone)
    }
}
