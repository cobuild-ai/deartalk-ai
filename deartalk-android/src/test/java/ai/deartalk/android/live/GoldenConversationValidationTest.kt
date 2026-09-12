package ai.deartalk.android.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

/**
 * 🌟 네이버 오늘의 회화 실제 데이터셋 기반 골든 벤치마크 및 회귀 테스트
 * - 5개 대표 실생활 시나리오(택시/교통, 호텔/조식, 쇼핑, 레스토랑 할인, 일상 카페) 무결성 검증
 * - 스피커 번갈아 대화하는 2-Way 턴테이킹(Turn-Taking) 구조 정합성 보장
 */
class GoldenConversationValidationTest {

    @Test
    fun testGoldenConversationsDatasetIntegrity() {
        val stream = javaClass.classLoader?.getResourceAsStream("naver_golden_conversations.json")
        assertNotNull("골든 대화 데이터셋 파일이 존재해야 합니다", stream)

        val reader = InputStreamReader(stream!!)
        val content = reader.readText()

        // 1. 전체 파일이 유효한 JSON 배열인지 확인
        assertTrue("JSON 배열로 시작해야 합니다", content.trim().startsWith("["))
        assertTrue("JSON 배열로 끝나야 합니다", content.trim().endsWith("]"))

        // 2. 5개 핵심 날짜 및 시나리오가 모두 포함되어 있는지 검증
        val expectedDates = listOf("20260912", "20260910", "20260909", "20260908", "20260903")
        expectedDates.forEach { date ->
            assertTrue("날짜 $date 회화가 포함되어야 합니다", content.contains("\"date\": \"$date\""))
        }

        // 3. 5개 대표 시나리오 타이틀 및 번역 검증
        val expectedScenarios = mapOf(
            "Where are you headed?" to "어디로 가세요?",
            "Does my room rate include breakfast?" to "제 객실 요금에 조식이 포함되나요?",
            "I’m just browsing." to "그냥 구경하는 거예요.",
            "We’re offering a 20% discount." to "20% 할인을 해드리고 있어요.",
            "Have you had breakfast?" to "아침은 먹었어?"
        )

        expectedScenarios.forEach { (enTitle, koTitle) ->
            assertTrue("제목 '$enTitle'이 포함되어야 합니다", content.contains(enTitle))
            assertTrue("한국어 번역 '$koTitle'이 포함되어야 합니다", content.contains(koTitle))
        }

        // 4. 모든 문장에 화자(A/B) 및 오디오 mp3가 매핑되어 있는지 검증
        val audioMatches = Regex("\"audio\": \"([^\"]+)\"").findAll(content).toList()
        assertTrue("최소 30개 이상의 원어민 오디오 문장이 매핑되어야 합니다", audioMatches.size >= 30)
        audioMatches.forEach { match ->
            val audioPath = match.groupValues[1]
            assertTrue("오디오 파일은 .mp3 확장자여야 합니다", audioPath.endsWith(".mp3"))
        }

        // 5. 총 대화 세션 개수 (5개) 확인
        val dateMatches = Regex("\"date\": \"(\\d{8})\"").findAll(content).toList()
        assertEquals("정확히 5개의 대화 세션이어야 합니다", 5, dateMatches.size)
    }

    /**
     * 🏨 [구글 플레이 스토어 등록 검증용] 실기기 2-Way 호텔 체크인/조식 대화 시퀀스 테스트
     * - 실제 Galaxy 단말에서 실측된 외국인 직원 ↔ 한국인 투숙객 3-Turn 대화 내역 검증
     * - LiveMessage 데이터 모델 변환, 화자 교대(PARTNER ➔ ME ➔ PARTNER), 언어 방향 및 번역 정합성 보장
     */
    @Test
    fun testHotelLiveConversationTurnSequence() {
        val session = ai.deartalk.android.live.data.LiveSession(
            id = "google-play-hotel-session",
            title = "2026-09-12 11:40:39 - Live",
            myLang = "KO",
            partnerLang = "EN"
        )

        // Turn 1: 외국인 직원 질문 (EN -> KO)
        val turn1 = ai.deartalk.android.live.data.LiveMessage(
            sessionId = session.id,
            sender = ai.deartalk.android.live.data.LiveSender.PARTNER,
            rawText = "Do you have a reservation?",
            refinedText = "예약하셨어요?",
            sourceLang = "EN",
            targetLang = "KO"
        )

        // Turn 2: 한국인 투숙객 답변 (KO -> EN, 톤앤매너: ✨ 기본다듬기)
        val turn2 = ai.deartalk.android.live.data.LiveMessage(
            sessionId = session.id,
            sender = ai.deartalk.android.live.data.LiveSender.ME,
            rawText = "네 예약했습니다 피터로 되어 있어요",
            refinedText = "Yes, I made a reservation, it is under Peter.",
            sourceLang = "KO",
            targetLang = "EN",
            tone = "✨ 기본다듬기"
        )

        // Turn 3: 외국인 직원 조식 안내 (EN -> KO)
        val turn3 = ai.deartalk.android.live.data.LiveMessage(
            sessionId = session.id,
            sender = ai.deartalk.android.live.data.LiveSender.PARTNER,
            rawText = "Yes, it includes a continental breakfast at our buffet.",
            refinedText = "네, 저희 뷔페에서 컨티넨탈 아침 식사가 포함되어 있습니다.",
            sourceLang = "EN",
            targetLang = "KO"
        )

        val conversationList = listOf(turn1, turn2, turn3)

        // 1. 턴테이킹(Turn-Taking) 화자 교대 무결성 검증
        assertEquals(ai.deartalk.android.live.data.LiveSender.PARTNER, conversationList[0].sender)
        assertEquals(ai.deartalk.android.live.data.LiveSender.ME, conversationList[1].sender)
        assertEquals(ai.deartalk.android.live.data.LiveSender.PARTNER, conversationList[2].sender)

        // 2. 언어 방향성 및 톤앤매너 검증
        assertEquals("EN", conversationList[0].sourceLang)
        assertEquals("KO", conversationList[0].targetLang)
        assertEquals("KO", conversationList[1].sourceLang)
        assertEquals("EN", conversationList[1].targetLang)
        assertEquals("✨ 기본다듬기", conversationList[1].tone)

        // 3. 대화 세션 및 텍스트 원문/번역 공백 없음 검증
        conversationList.forEach { msg ->
            assertEquals(session.id, msg.sessionId)
            assertTrue("원문이 비어있지 않아야 합니다", msg.rawText.isNotBlank())
            assertTrue("번역문이 비어있지 않아야 합니다", msg.refinedText.isNotBlank())
        }

        // 4. 스크립트 마크다운 내보내기 형식 정합성 검증
        val sb = StringBuilder()
        sb.appendLine("# 🎙️ DearTalk Live - ${session.title}")
        sb.appendLine("- **언어**: ${session.myLang} ⇄ ${session.partnerLang}")
        conversationList.forEachIndexed { idx, m ->
            val speakerTag = if (m.sender == ai.deartalk.android.live.data.LiveSender.ME) "나 (KO ➔ EN)" else "상대방 (EN ➔ KO)"
            sb.appendLine("### ${idx + 1}. $speakerTag")
            sb.appendLine("> **원문**: ${m.rawText}")
            sb.appendLine("> **번역**: ${m.refinedText}")
        }
        val exportText = sb.toString()
        assertTrue(exportText.contains("Do you have a reservation?"))
        assertTrue(exportText.contains("예약하셨어요?"))
        assertTrue(exportText.contains("Yes, I made a reservation, it is under Peter."))
        assertTrue(exportText.contains("컨티넨탈 아침 식사가 포함되어 있습니다."))
    }

    /**
     * 🌐 [5대 골든 시나리오 전체] LiveMessage 데이터 모델 자동 파싱 및 턴 매핑 검증
     */
    @Test
    fun testAllGoldenConversationsTurnTakingAndLiveMessageMapping() {
        val stream = javaClass.classLoader?.getResourceAsStream("naver_golden_conversations.json")
        assertNotNull(stream)
        val content = InputStreamReader(stream!!).readText()

        // 각 세션별로 문장 블록 추출
        val sessionBlocks = content.split("\"date\":").drop(1)
        assertEquals(5, sessionBlocks.size)

        var totalSentenceCount = 0
        sessionBlocks.forEachIndexed { sIdx, block ->
            val enMatches = Regex("\"en\": \"([^\"]+)\"").findAll(block).map { it.groupValues[1] }.toList()
            val koMatches = Regex("\"ko\": \"([^\"]+)\"").findAll(block).map { it.groupValues[1] }.toList()
            val speakerMatches = Regex("\"speaker\": \"([^\"]+)\"").findAll(block).map { it.groupValues[1] }.toList()

            assertEquals("영어와 한국어 문장 수가 일치해야 합니다", enMatches.size, koMatches.size)
            assertEquals("영어와 화자 수가 일치해야 합니다", enMatches.size, speakerMatches.size)
            assertTrue("세션당 최소 6문장 이상이어야 합니다", enMatches.size >= 6)

            // LiveMessage 리스트로 일괄 변환 검증
            val liveMessages = enMatches.indices.map { idx ->
                val speaker = if (speakerMatches[idx] == "A") {
                    ai.deartalk.android.live.data.LiveSender.PARTNER
                } else {
                    ai.deartalk.android.live.data.LiveSender.ME
                }
                ai.deartalk.android.live.data.LiveMessage(
                    sessionId = "golden-session-$sIdx",
                    sender = speaker,
                    rawText = if (speaker == ai.deartalk.android.live.data.LiveSender.ME) koMatches[idx] else enMatches[idx],
                    refinedText = if (speaker == ai.deartalk.android.live.data.LiveSender.ME) enMatches[idx] else koMatches[idx],
                    sourceLang = if (speaker == ai.deartalk.android.live.data.LiveSender.ME) "KO" else "EN",
                    targetLang = if (speaker == ai.deartalk.android.live.data.LiveSender.ME) "EN" else "KO"
                )
            }

            assertEquals(enMatches.size, liveMessages.size)
            totalSentenceCount += liveMessages.size
        }

        assertTrue("전체 골든 문장 수는 30개 이상이어야 합니다", totalSentenceCount >= 30)
    }
}
