package ai.deartalk.android.live

import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import ai.deartalk.android.live.data.LiveSession
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.live.data.formatSourcePunctuation
import ai.deartalk.android.live.data.getLabel
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
        assertTrue(session.title.contains("DearTalk-Live"))
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

    @Test
    fun testSpeechIntentValuesAndLabels() {
        val intents = ai.deartalk.android.live.data.SpeechIntent.values()
        assertEquals(5, intents.size)

        assertEquals("auto", ai.deartalk.android.live.data.SpeechIntent.AUTO.id)
        assertEquals("question", ai.deartalk.android.live.data.SpeechIntent.QUESTION.id)
        assertEquals("statement", ai.deartalk.android.live.data.SpeechIntent.STATEMENT.id)
        assertEquals("request", ai.deartalk.android.live.data.SpeechIntent.REQUEST.id)
        assertEquals("confirm", ai.deartalk.android.live.data.SpeechIntent.CONFIRM.id)

        intents.forEach { intent ->
            val label = intent.getLabel()
            assertTrue(label.isNotBlank())
        }

        // 🌐 다국어 지원 검증 (KO, EN, ID, JA, ZH)
        assertEquals("✨ 스마트", ai.deartalk.android.live.data.SpeechIntent.AUTO.getLabel("KO"))
        assertEquals("✨ Smart", ai.deartalk.android.live.data.SpeechIntent.AUTO.getLabel("EN"))
        assertEquals("✨ Cerdas", ai.deartalk.android.live.data.SpeechIntent.AUTO.getLabel("ID"))
        assertEquals("✨ スマート", ai.deartalk.android.live.data.SpeechIntent.AUTO.getLabel("JA"))
        assertEquals("✨ 智能", ai.deartalk.android.live.data.SpeechIntent.AUTO.getLabel("ZH"))

        assertEquals("❓ 질문?", ai.deartalk.android.live.data.SpeechIntent.QUESTION.getLabel("KO"))
        assertEquals("❓ Question", ai.deartalk.android.live.data.SpeechIntent.QUESTION.getLabel("EN"))
        assertEquals("❓ Tanya?", ai.deartalk.android.live.data.SpeechIntent.QUESTION.getLabel("ID"))

        assertEquals("💬 설명.", ai.deartalk.android.live.data.SpeechIntent.STATEMENT.getLabel("KO"))
        assertEquals("💬 Statement", ai.deartalk.android.live.data.SpeechIntent.STATEMENT.getLabel("EN"))
        assertEquals("💬 Jawaban.", ai.deartalk.android.live.data.SpeechIntent.STATEMENT.getLabel("ID"))

        assertEquals("🙏 부탁!", ai.deartalk.android.live.data.SpeechIntent.REQUEST.getLabel("KO"))
        assertEquals("🙏 Request", ai.deartalk.android.live.data.SpeechIntent.REQUEST.getLabel("EN"))
        assertEquals("🙏 Mohon!", ai.deartalk.android.live.data.SpeechIntent.REQUEST.getLabel("ID"))

        assertEquals("🔁 확인?", ai.deartalk.android.live.data.SpeechIntent.CONFIRM.getLabel("KO"))
        assertEquals("🔁 Confirm", ai.deartalk.android.live.data.SpeechIntent.CONFIRM.getLabel("EN"))
        assertEquals("🔁 Benarkah?", ai.deartalk.android.live.data.SpeechIntent.CONFIRM.getLabel("ID"))
    }

    @Test
    fun testLiveUiHelperLocalization() {
        val helper = ai.deartalk.android.live.data.LiveUiHelper

        // Title
        assertEquals("🗣️ 말하기", helper.getSpeakTitle("KO"))
        assertEquals("🗣️ Speak", helper.getSpeakTitle("EN"))
        assertEquals("🗣️ Bicara", helper.getSpeakTitle("ID"))
        assertEquals("🗣️ 話す", helper.getSpeakTitle("JA"))
        assertEquals("🗣️ 说话", helper.getSpeakTitle("ZH"))

        // Finish
        assertEquals("🛑 완료 (전송)", helper.getFinishTitle("KO"))
        assertEquals("🛑 Finish (Send)", helper.getFinishTitle("EN"))
        assertEquals("🛑 Selesai (Kirim)", helper.getFinishTitle("ID"))

        // Processing
        assertEquals("⏳ 번역/보정 중", helper.getProcessingTitle("KO"))
        assertEquals("⏳ Processing", helper.getProcessingTitle("EN"))
        assertEquals("⏳ Menerjemahkan", helper.getProcessingTitle("ID"))
    }

    @Test
    fun testFormatSourcePunctuation() {
        val format = ::formatSourcePunctuation

        // 1. SLM 문맥 판별 결과가 의문문(translationHasQuestion = true)인 경우:
        //    (모호한 문장에서 의문문 인텐트가 반영되었거나 자연스러운 질문인 경우 원문 카드에도 '?' 동기화)
        assertEquals("밥 먹었어?", format("밥 먹었어", SpeechIntent.QUESTION, true))
        assertEquals("식사하셨어요?", format("식사하셨어요", SpeechIntent.QUESTION, true))
        assertEquals("어디 가?", format("어디 가", SpeechIntent.AUTO, true))
        assertEquals("몇 시예요?", format("몇 시예요", SpeechIntent.AUTO, true))

        // 2. 사용자가 실수로 질문 버튼을 눌렀으나 명백한 평서문이라 의문문이 필요하지 않은 경우(translationHasQuestion = false):
        //    (문장 맥락상 의문문이 아니므로 억지로 '?'를 붙이지 않음)
        assertEquals("오늘 날씨가 정말 좋습니다", format("오늘 날씨가 정말 좋습니다", SpeechIntent.QUESTION, false))
        assertEquals("나 지금 도서관에 가고 있어", format("나 지금 도서관에 가고 있어", SpeechIntent.QUESTION, false))

        // 3. 평서문 스마트(AUTO) 모드 역시 '?' 없이 원문 보존
        assertEquals("진짜 맛있어", format("진짜 맛있어", SpeechIntent.AUTO, false))
        assertEquals("집에 가는 중이야", format("집에 가는 중이야", SpeechIntent.AUTO, false))

        // 4. 💬 설명. (STATEMENT) 인텐트 선택 시 '.' 자동 부가
        assertEquals("밥 먹었어.", format("밥 먹었어", SpeechIntent.STATEMENT, false))

        // 5. 이미 종결 부호(?, !, .)가 있으면 원형 100% 보존
        assertEquals("밥 먹었어?", format("밥 먹었어?", SpeechIntent.QUESTION, true))
        assertEquals("정말 대단해!", format("정말 대단해!", SpeechIntent.QUESTION, false))
        assertEquals("끝났습니다.", format("끝났습니다.", SpeechIntent.QUESTION, false))
    }
}
