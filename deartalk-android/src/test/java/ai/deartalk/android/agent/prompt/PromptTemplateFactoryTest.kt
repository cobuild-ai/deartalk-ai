package ai.deartalk.android.agent.prompt

import ai.deartalk.android.live.data.SpeechIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplateFactoryTest {

    @Test
    fun testParseIntentTagAndClean() {
        val raw = "[INTENT: QUESTION]\n오늘 날씨 어때?"
        val (intent, clean) = PromptTemplateFactory.parseIntentTagAndClean(raw)
        assertEquals(SpeechIntent.QUESTION, intent)
        assertEquals("오늘 날씨 어때?", clean)
    }

    @Test
    fun testParseIntentTagAndClean_fallback() {
        val raw = "안녕하세요 반갑습니다."
        val (intent, clean) = PromptTemplateFactory.parseIntentTagAndClean(raw, SpeechIntent.STATEMENT)
        assertEquals(SpeechIntent.STATEMENT, intent)
        assertEquals("안녕하세요 반갑습니다.", clean)
    }

    @Test
    fun testParseIntentTagAndClean_intentWrappingSentence() {
        val raw = "[INTENT: REQUEST]\n[INTENT: 你今天中午一起吃饭吗？]"
        val (intent, clean) = PromptTemplateFactory.parseIntentTagAndClean(raw, SpeechIntent.REQUEST)
        assertEquals(SpeechIntent.REQUEST, intent)
        assertEquals("你今天中午一起吃饭吗？", clean)
    }

    @Test
    fun testCleanLlmOutput_specialTokensAndThinking() {
        val raw = "<start_of_turn>model\n<think>Let me think about how to refine this sentence.</think>\n오늘 날씨가 정말 좋습니다.<end_of_turn>"
        val cleaned = PromptTemplateFactory.cleanLlmOutput(raw)
        assertEquals("오늘 날씨가 정말 좋습니다.", cleaned)
    }

    @Test
    fun testCleanLlmOutput_reasoningLinesSkipped() {
        val raw = "Okay, let's see the user's input.\nThe user wants to refine: \"밥 머것어\"\n밥 먹었어?"
        val cleaned = PromptTemplateFactory.cleanLlmOutput(raw)
        assertEquals("밥 먹었어?", cleaned)
    }

    @Test
    fun testCleanLlmOutput_quotesAndPrefixes() {
        val raw = "\"안녕하세요, 반갑습니다!\""
        val cleaned = PromptTemplateFactory.cleanLlmOutput(raw)
        assertEquals("안녕하세요, 반갑습니다!", cleaned)
    }

    @Test
    fun testCleanLlmOutput_emptyInput() {
        assertEquals("", PromptTemplateFactory.cleanLlmOutput(""))
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("   "))
    }

    @Test
    fun testCleanLlmOutput_unclosedAndCorruptedControlTags() {
        // 1. 단독 파손된 제어 토큰 조각 (<end_of_?, <end_of_, <start_of_)
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<end_of_?"))
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<end_of_"))
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<start_of_turn"))

        // 2. 다국어 본문 뒤에 닫히지 않은 제어 토큰이 붙은 경우 (한국어, 영어, 인도네시아어)
        assertEquals("여기서 사용하는 AI 엔진은 무엇인가요?", PromptTemplateFactory.cleanLlmOutput("여기서 사용하는 AI 엔진은 무엇인가요?<end_of_?"))
        assertEquals("What AI engine is used here?", PromptTemplateFactory.cleanLlmOutput("What AI engine is used here?<end_of_"))
        assertEquals("Model AI apa yang digunakan di sini?", PromptTemplateFactory.cleanLlmOutput("Model AI apa yang digunakan di sini?<end_of_turn"))

        // 3. <think> 블록 제거 (닫히지 않은 경우 전체 소거, 닫힌 경우 본문 보존)
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<think>Let me reason without closing tag"))
        assertEquals("정상 문장입니다.", PromptTemplateFactory.cleanLlmOutput("<think>사고과정...</think>정상 문장입니다."))
    }

    @Test
    fun testSanitizeKeyboardOutput_corruptedTagRollback() {
        // 단독 제어 태그 파편이 남아있는 경우 ➔ 사용자의 원문 100% 안전 복원
        val koreanInput = "여기서 사용하는 AI 엔진은 뭐야"
        assertEquals(koreanInput, PromptTemplateFactory.sanitizeKeyboardOutput(koreanInput, "<end_of_?", isExplicitQuestion = true))
        assertEquals(koreanInput, PromptTemplateFactory.sanitizeKeyboardOutput(koreanInput, "<end_of_", isExplicitQuestion = true))

        val englishInput = "What AI engine is used here"
        assertEquals(englishInput, PromptTemplateFactory.sanitizeKeyboardOutput(englishInput, "<end_of_?", isExplicitQuestion = true))

        val indonesianInput = "Mesin AI apa yang dipakai di sini"
        assertEquals(indonesianInput, PromptTemplateFactory.sanitizeKeyboardOutput(indonesianInput, "???", isExplicitQuestion = true))
    }

    @Test
    fun testSanitizeKeyboardOutput_antiChatbot() {
        // 원문이 의문문인데 모델이 챗봇처럼 답변한 경우 ➔ 원문 의문문 구조로 안전 복원
        val chatbotAnswer = "네, 저는 방금 식사를 마쳤습니다. 맛있는 점심 드세요!"
        val sanitized = PromptTemplateFactory.sanitizeKeyboardOutput("밥 먹었어?", chatbotAnswer, isExplicitQuestion = true)
        assertEquals("밥 먹었어?", sanitized)

        // 원문이 정상적으로 의문문 어조로 다듬어진 경우 ➔ 정상 통과
        val validRefinement = "식사는 맛있게 하셨습니까?"
        val passed = PromptTemplateFactory.sanitizeKeyboardOutput("밥 먹었어?", validRefinement, isExplicitQuestion = true)
        assertEquals("식사는 맛있게 하셨습니까?", passed)

        // 실기기 이슈: 모델이 "~라고 하면, 내가 ~ 바꿔줄게" 처럼 메타 설명/챗봇형 응답을 한 경우 ➔ 원문 안전 복원
        val metatalkKo = "\"써니업 스타일로 부탁해\"라고 하면, 내가 친구한테 말하듯이 부드럽고 친근하게 바꿔줄게."
        val sanitizedMetatalkKo = PromptTemplateFactory.sanitizeKeyboardOutput("어 나는 써니업 스타일로 부탁해", metatalkKo, isExplicitQuestion = false)
        assertEquals("어 나는 써니업 스타일로 부탁해", sanitizedMetatalkKo)

        // 다국어(영어/인니) 메타 해설 구조도 하드코딩 없이 동일하게 안전 복원
        val metatalkEn = "\"Please do sunny up style\", in a casual tone you can say it like this to your friend."
        val sanitizedMetatalkEn = PromptTemplateFactory.sanitizeKeyboardOutput("Please do sunny up style", metatalkEn, isExplicitQuestion = false)
        assertEquals("Please do sunny up style", sanitizedMetatalkEn)

        val metatalkId = "\"tolong gaya sunny up\", artinya Anda bisa mengatakannya dengan gaya santai kepada teman."
        val sanitizedMetatalkId = PromptTemplateFactory.sanitizeKeyboardOutput("tolong gaya sunny up", metatalkId, isExplicitQuestion = false)
        assertEquals("tolong gaya sunny up", sanitizedMetatalkId)

        // 하드코딩 제거 검증: 사용자의 정상 발화("제가 도와드릴게요", "친구한테 말하듯이 편하게 해")는 오탐(False Positive) 없이 정상 통과
        val userHelpSentence = "제가 도와드릴게요"
        val userHelpRefined = "제가 도와드리겠습니다"
        assertEquals(userHelpRefined, PromptTemplateFactory.sanitizeKeyboardOutput(userHelpSentence, userHelpRefined, isExplicitQuestion = false))

        val userFriendSentence = "친구한테 말하듯이 편하게 해"
        val userFriendRefined = "친구에게 말하듯이 편하게 하세요"
        assertEquals(userFriendRefined, PromptTemplateFactory.sanitizeKeyboardOutput(userFriendSentence, userFriendRefined, isExplicitQuestion = false))
    }

    @Test
    fun testCleanLlmOutput_promptEchoAndArrows() {
        // 모델이 프롬프트의 '-> 변환: ' 구조를 에코한 경우 본문만 깔끔하게 추출
        val echo1 = "원문: \"밥 먹었어\" -> 변환: \"식사하셨습니까?\""
        assertEquals("식사하셨습니까?", PromptTemplateFactory.cleanLlmOutput(echo1))

        val echo2 = "Input: \"hello\" ➔ Output: \"Halo\""
        assertEquals("Halo", PromptTemplateFactory.cleanLlmOutput(echo2))
    }

    @Test
    fun testBuildCorrectionPrompt_korean_gemmaDefault() {
        // 단일 표준 모델 Gemma 4 E2B 기본값 검증
        val prompt = PromptTemplateFactory.buildCorrectionPrompt(
            trimmed = "밥 먹었어",
            isInputKorean = true,
            isIndonesianLocale = false,
            isInputEnglish = false,
            speechIntent = SpeechIntent.QUESTION
        )
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("모바일 키보드의 문장 교정 및 다듬기 엔진"))
        assertTrue(prompt.contains("원문 왜곡 금지 및 내용 생략 금지"))
        assertTrue(prompt.contains("밥 먹었어"))
        assertTrue(prompt.contains("<end_of_turn>"))
        assertTrue(prompt.contains("<start_of_turn>model"))
    }

    @Test
    fun testBuildCorrectionPrompt_english() {
        val prompt = PromptTemplateFactory.buildCorrectionPrompt(
            trimmed = "what time is it",
            isInputKorean = false,
            isIndonesianLocale = false,
            isInputEnglish = true,
            speechIntent = SpeechIntent.AUTO
        )
        assertTrue(prompt.contains("sentence refinement engine"))
        assertTrue(prompt.contains("what time is it"))
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("<end_of_turn>"))
    }

    @Test
    fun testBuildCorrectionPrompt_indonesian() {
        val prompt = PromptTemplateFactory.buildCorrectionPrompt(
            trimmed = "kamu mau kemana",
            isInputKorean = false,
            isIndonesianLocale = true,
            isInputEnglish = false,
            speechIntent = SpeechIntent.AUTO
        )
        assertTrue(prompt.contains("mesin perapih kalimat"))
        assertTrue(prompt.contains("kamu mau kemana"))
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("<end_of_turn>"))
    }

    @Test
    fun testBuildTonePrompt_gemmaDefault() {
        // 단일 표준 모델 Gemma 4 E2B 기본값 검증
        val prompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = "식사 같이 하실래요",
            toneName = "공손하게",
            toneInstruction = "정중하고 예의 바르게",
            examples = "- 원문: 식사 같이 하실래요 -> 식사 함께 하실 수 있으실까요?",
            isInputKorean = true,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.QUESTION
        )
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("텍스트 어조/톤 변환기"))
        assertTrue(prompt.contains("원문 왜곡 금지"))
        assertTrue(prompt.contains("내용 생략 금지"))
        assertTrue(prompt.contains("억지 한자어 치환 절대 금지"))
        assertTrue(prompt.contains("공손하게"))
        assertTrue(prompt.contains("식사 같이 하실래요"))
        assertTrue(prompt.contains("<end_of_turn>"))
        assertTrue(prompt.contains("<start_of_turn>model"))
    }

    @Test
    fun testBuildCorrectionPrompt_koreanMultiClauseNoOmission() {
        // 이유와 요구가 결합된 복합 문장(예: 천원만 주세요 배고파요) 생략 방지 지침 검증
        val prompt = PromptTemplateFactory.buildCorrectionPrompt(
            trimmed = "천원만 주세요 배고파요",
            isInputKorean = true,
            isIndonesianLocale = false,
            isInputEnglish = false
        )
        assertTrue(prompt.contains("원문 왜곡 금지 및 내용 생략 금지"))
        assertTrue(prompt.contains("이유, 상황, 요구 등 원문의 모든 의미 요소를 빠짐없이 100% 온전히 포함"))
        assertTrue(prompt.contains("천원만 주세요 배고파요"))
    }



    @Test
    fun testBuildTranslationPrompt_zeroContext() {
        // ⚡ 대화 맥락 완전 제거 검증
        val prompt = PromptTemplateFactory.buildTranslationPrompt(
            trimmed = "지금 어디 가세요?",
            sourceLangName = "Korean",
            targetLangName = "Indonesian",
            toneInstruction = "",
            linguisticRule = "",
            contextBlock = "Context: \"이전 대화 발화 내용\"\n"
        )
        assertFalse(prompt.contains("Context:"))
        assertFalse(prompt.contains("이전 대화"))
        assertTrue(prompt.contains("Translate spoken speech from Korean to Indonesian"))
        assertTrue(prompt.contains("Input: \"지금 어디 가세요?\""))
    }
}
