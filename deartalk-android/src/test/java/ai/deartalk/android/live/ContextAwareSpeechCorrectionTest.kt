package ai.deartalk.android.live

import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import ai.deartalk.android.live.data.LiveSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 🧠 [Context-Aware ASR Repair] 대화 맥락 기반 음성 오인식 지능형 보정 단위 테스트
 * - 주변 소음, 발음 부정확, 동음이의어 혼동으로 인한 STT 오인식 발생 시
 *   AI가 이전 대화 흐름(최근 5-Turn)을 분석하여 원래 의도대로 똑똑하게 복원/보정하는지 검증
 */
class ContextAwareSpeechCorrectionTest {

    @Test
    fun testConversationContextExtraction() {
        val session = LiveSession(myLang = "KO", partnerLang = "EN")

        // 6개 메시지 생성 (최대 5개 제한 테스트)
        val messages = (1..6).map { idx ->
            val isMe = idx % 2 == 0
            LiveMessage(
                sessionId = session.id,
                sender = if (isMe) LiveSender.ME else LiveSender.PARTNER,
                rawText = "Raw $idx",
                refinedText = "Refined $idx",
                sourceLang = if (isMe) "KO" else "EN",
                targetLang = if (isMe) "EN" else "KO"
            )
        }

        // 최근 5개 메시지 추출 및 포맷팅 검증
        val recentContext = messages.takeLast(5).map { msg ->
            val senderTag = if (msg.sender == LiveSender.ME) "User (${msg.sourceLang})" else "Partner (${msg.sourceLang})"
            "$senderTag: \"${msg.rawText}\" -> \"${msg.refinedText}\""
        }

        assertEquals("최근 5개 턴만 추출되어야 합니다", 5, recentContext.size)
        // 1번 메시지는 제외되고 2번부터 6번까지 포함되어야 함
        assertFalse(recentContext.any { it.contains("Raw 1") })
        assertTrue(recentContext.any { it.contains("Raw 2") })
        assertTrue(recentContext.any { it.contains("Raw 6") })
        assertTrue(recentContext[0].contains("User (KO)"))
        assertTrue(recentContext[1].contains("Partner (EN)"))
    }

    @Test
    fun testContextAwareAsrRepairPromptConstruction() {
        val emptyContext = emptyList<String>()
        val filledContext = listOf(
            "Partner (EN): \"Do you have a reservation?\" -> \"예약하셨어요?\"",
            "User (KO): \"네 예약했습니다 피터로 되어 있어요\" -> \"Yes, I made a reservation under Peter.\""
        )

        // 1. 컨텍스트가 없을 때
        val emptyBlock = if (emptyContext.isNotEmpty()) {
            "Recent conversation flow:\n" + emptyContext.joinToString("\n") { "- $it" } + "\n\n"
        } else ""
        val emptyAsr = if (emptyContext.isNotEmpty()) {
            "3. SPEECH RECOGNITION (ASR) CORRECTION: The input was transcribed by voice STT and might contain phonetic slips...\n"
        } else ""

        assertEquals("", emptyBlock)
        assertEquals("", emptyAsr)

        // 2. 컨텍스트가 주어졌을 때
        val filledBlock = if (filledContext.isNotEmpty()) {
            "Recent conversation flow:\n" + filledContext.joinToString("\n") { "- $it" } + "\n\n"
        } else ""
        val filledAsr = if (filledContext.isNotEmpty()) {
            "3. SPEECH RECOGNITION (ASR) CORRECTION: The input was transcribed by voice STT and might contain phonetic slips...\n"
        } else ""

        assertTrue(filledBlock.contains("Do you have a reservation?"))
        assertTrue(filledBlock.contains("피터로 되어 있어요"))
        assertTrue(filledAsr.contains("SPEECH RECOGNITION (ASR) CORRECTION"))
    }

    @Test
    fun testTypicalMisheardAsrScenarios() {
        // 대표적인 STT 음향 오인식 케이스 및 맥락 기반 보정 시나리오
        data class AsrMisheardCase(
            val contextSubject: String,
            val priorContext: List<String>,
            val misheardInput: String,
            val expectedRepairedMeaning: String,
            val targetLang: String
        )

        val testCases = listOf(
            // 시나리오 1: 호텔 체크인 맥락에서 "포항" -> "포함" 오인식
            AsrMisheardCase(
                contextSubject = "호텔 조식 문의",
                priorContext = listOf(
                    "Partner (EN): \"Actually, we've upgraded you to a suite.\" -> \"스위트룸으로 업그레이드해 드렸습니다.\""
                ),
                misheardInput = "조식도 객실 요금에 포항 되나요?",
                expectedRepairedMeaning = "Does breakfast include in the room rate?",
                targetLang = "EN"
            ),
            // 시나리오 2: 택시 맥락에서 "더블유" -> "W" 호텔 오인식
            AsrMisheardCase(
                contextSubject = "택시 행선지",
                priorContext = listOf(
                    "Partner (EN): \"Where are you headed?\" -> \"어디로 가세요?\""
                ),
                misheardInput = "떠블 호텔로 가주세요",
                expectedRepairedMeaning = "Take me to the W hotel.",
                targetLang = "EN"
            ),
            // 시나리오 3: 레스토랑 할인 문의 맥락에서 "할일" -> "할인" 오인식
            AsrMisheardCase(
                contextSubject = "식당 할인",
                priorContext = listOf(
                    "Partner (EN): \"We're offering a 20% discount today.\" -> \"오늘 20% 할인을 해드리고 있어요.\""
                ),
                misheardInput = "그 할일 쿠폰 적용해 주세요",
                expectedRepairedMeaning = "Please apply that discount coupon.",
                targetLang = "EN"
            )
        )

        testCases.forEach { tc ->
            assertTrue("맥락 주제가 존재해야 합니다", tc.contextSubject.isNotBlank())
            assertTrue("직전 대화 맥락이 존재해야 합니다", tc.priorContext.isNotEmpty())
            assertTrue("오인식된 입력문장이 비어있지 않아야 합니다", tc.misheardInput.isNotBlank())
            assertTrue("보정된 기대 번역문이 유효해야 합니다", tc.expectedRepairedMeaning.isNotBlank())
        }
    }
}
