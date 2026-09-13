package ai.deartalk.android.agent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DearTalkIntentEngineTest {

    private lateinit var intentEngine: DearTalkIntentEngine

    @Before
    fun setUp() {
        intentEngine = DearTalkIntentEngine(null)
    }

    @Test
    fun `언어_감지_유틸_테스트`() {
        assertTrue(DearTalkIntentEngine.hasKorean("안녕하세요"))
        assertTrue(DearTalkIntentEngine.hasKorean("Hello 안녕하세요"))
        assertFalse(DearTalkIntentEngine.hasKorean("Hello world"))
        assertFalse(DearTalkIntentEngine.hasKorean("12345!@#$"))

        assertTrue(DearTalkIntentEngine.isEnglish("Hello how are you"))
        assertTrue(DearTalkIntentEngine.isEnglish("Where is the conference room?"))
        assertTrue(DearTalkIntentEngine.hasKorean("나는 AI를 이용해서 생성했어요"))
        assertFalse(DearTalkIntentEngine.isEnglish("나는 AI를 이용해서 생성했어요"))
    }

    @Test
    fun `영한_혼용_입력_및_모델미로드_무왜곡_원문보존_테스트`() = runBlocking {
        val mixedInput = "나는 AI를 이용해서 생성했어요"
        val result = intentEngine.process(mixedInput)
        assertTrue(result is IntentResult.Success)
        val text = (result as IntentResult.Success).text
        assertEquals(mixedInput, text)
    }

    @Test
    fun `모델_미로드시_인위적_왜곡_없이_정직한_원문_보존_테스트`() = runBlocking {
        val input = "내일 아침 10시 약속 만나자고 하는 거를 공산화 툴로 좀 말해 줄래"
        val result = intentEngine.process(input)
        assertTrue(result is IntentResult.Success)
        val text = (result as IntentResult.Success).text

        assertFalse(text.endsWith("부탁드립니다."))
        assertEquals(input, text)
    }

    @Test
    fun `빈_문자열_처리_테스트`() = runBlocking {
        val result = intentEngine.process("   ")
        assertTrue(result is IntentResult.Success)
        assertEquals("", (result as IntentResult.Success).text)
    }

    @Test
    fun `다국어_번역_빈문자열_및_안전_처리_테스트`() = runBlocking {
        val target = ai.deartalk.android.data.pref.TranslationTarget(
            id = "test_en",
            name = "영어",
            targetLanguage = "영어(English)",
            flag = "🇺🇸"
        )
        val resultEmpty = intentEngine.processWithTranslation("   ", target)
        assertTrue(resultEmpty is IntentResult.Success)
        assertEquals("", (resultEmpty as IntentResult.Success).text)

        val input = "안녕하세요 반갑습니다"
        val result = intentEngine.processWithTranslation(input, target)
        assertTrue(result is IntentResult.Success)
        assertEquals(input, (result as IntentResult.Success).text)
    }

    @Test
    fun `톤앤매너_다국어_입력_및_모델미로드_원문보존_테스트`() = runBlocking {
        val tone = ai.deartalk.android.data.pref.CustomTone(
            id = "tone_polite",
            name = "Polite",
            instruction = "Be polite"
        )
        
        // 영어 입력 테스트
        val englishInput = "Do you want to have lunch?"
        val resultEn = intentEngine.processWithTone(englishInput, tone)
        assertTrue(resultEn is IntentResult.Success)
        assertEquals(englishInput, (resultEn as IntentResult.Success).text)

        // 일본어 입력 테스트
        val japaneseInput = "ご飯一緒に食べますか？"
        val resultJa = intentEngine.processWithTone(japaneseInput, tone)
        assertTrue(resultJa is IntentResult.Success)
        assertEquals(japaneseInput, (resultJa as IntentResult.Success).text)
    }

    @Test
    fun `모델_감지_및_초기화_메소드와_Flow_갱신_테스트`() = runBlocking {
        assertFalse(intentEngine.isModelLoaded)
        assertFalse(intentEngine.isModelLoadedFlow.value)
        intentEngine.detectAndInitOnDeviceModel()
        assertFalse(intentEngine.isModelLoadedFlow.value)
    }

    @Test
    fun `cleanLlmOutput_Gemma_및_Qwen_ChatML_특수토큰_정제_테스트`() {
        // Gemma 토큰 테스트
        val gemmaRaw = "<start_of_turn>model\n내일 뵙겠습니다.<end_of_turn>"
        assertEquals("내일 뵙겠습니다.", intentEngine.cleanLlmOutput(gemmaRaw))

        // Qwen ChatML 토큰 테스트
        val qwenRaw = "<|im_start|>assistant\nSampai jumpa besok.<|im_end|>"
        assertEquals("Sampai jumpa besok.", intentEngine.cleanLlmOutput(qwenRaw))

        // 접두어 제거 테스트
        val prefixRaw = "최종 문장: 안녕하세요!"
        assertEquals("안녕하세요!", intentEngine.cleanLlmOutput(prefixRaw))

        // 복합 마크다운 및 따옴표 제거 테스트
        val complexRaw = "<|im_start|>assistant\n\"Terima kasih banyak atas bantuannya!\"<|im_end|>"
        assertEquals("Terima kasih banyak atas bantuannya!", intentEngine.cleanLlmOutput(complexRaw))
    }

    @Test
    fun `인도네시아어_톤변환_원문보존_테스트`() = runBlocking {
        val tone = ai.deartalk.android.data.pref.CustomTone(
            id = "tone_polite",
            name = "Sopan",
            instruction = "Gunakan bahasa sopan"
        )
        val indonesianInput = "Besok jam berapa kita bisa bertemu?"
        val result = intentEngine.processWithTone(indonesianInput, tone)
        assertTrue(result is IntentResult.Success)
        assertEquals(indonesianInput, (result as IntentResult.Success).text)
    }

    @Test
    fun `명시적_의문문_폴백_물음표_자동부착_테스트`() = runBlocking {
        // 1. 명백한 의문사/의문어미("언제 출발해", "내일 몇 시에 만날까") -> 모델 미로드 폴백 시에도 '?' 자동 부착
        val result1 = intentEngine.process("언제 출발해")
        assertTrue(result1 is IntentResult.Success)
        assertEquals("언제 출발해?", (result1 as IntentResult.Success).text)

        val result2 = intentEngine.process("내일 몇 시에 만날까")
        assertTrue(result2 is IntentResult.Success)
        assertEquals("내일 몇 시에 만날까?", (result2 as IntentResult.Success).text)

        // 2. 평서문("나 괜찮아", "밥 먹었어", "지금 출발했습니다") -> 물음표 부착하지 않고 평문 보존
        val result3 = intentEngine.process("나 괜찮아")
        assertTrue(result3 is IntentResult.Success)
        assertEquals("나 괜찮아", (result3 as IntentResult.Success).text)

        val result4 = intentEngine.process("밥 먹었어")
        assertTrue(result4 is IntentResult.Success)
        assertEquals("밥 먹었어", (result4 as IntentResult.Success).text)

        val result5 = intentEngine.process("지금 출발했습니다")
        assertTrue(result5 is IntentResult.Success)
        assertEquals("지금 출발했습니다", (result5 as IntentResult.Success).text)
    }

    @Test
    fun `의도_재작성_폴백_구두점_및_의도_변환_테스트`() = runBlocking {
        // 모델 미로드 폴백 환경에서 의도에 따른 기본 부호 및 변환 검증
        val rewrittenQuestion = intentEngine.rewriteSentenceIntent(
            text = "이거 복잡한 문제야",
            langCode = "KO",
            targetIntent = ai.deartalk.android.live.data.SpeechIntent.QUESTION
        )
        assertEquals("이거 복잡한 문제야?", rewrittenQuestion)

        val rewrittenStatement = intentEngine.rewriteSentenceIntent(
            text = "이거 너는 복잡한 문제라고 생각하니?",
            langCode = "KO",
            targetIntent = ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        )
        assertEquals("이거 너는 복잡한 문제라고 생각하니.", rewrittenStatement)

        // rephraseMessageWithIntent 통합 파이프라인 테스트
        val (raw, refined) = intentEngine.rephraseMessageWithIntent(
            rawSourceText = "이거 복잡한 문제야",
            sourceLangCode = "KO",
            targetLangCode = "KO",
            targetIntent = ai.deartalk.android.live.data.SpeechIntent.QUESTION
        )
        assertEquals("이거 복잡한 문제야?", raw)
        assertEquals("이거 복잡한 문제야?", refined)
    }

    @Test
    fun `cleanLlmOutput_말미_스파클_및_장식_기호_정제_테스트`() {
        val rawWithSparkle = "I am at home?✨"
        assertEquals("I am at home?", intentEngine.cleanLlmOutput(rawWithSparkle))

        val rawWithStars = "Are you going to eat today? ✨⭐"
        assertEquals("Are you going to eat today?", intentEngine.cleanLlmOutput(rawWithStars))
    }

    @Test
    fun `영어_의도_재작성_폴백_부호_보존_테스트`() = runBlocking {
        val question = intentEngine.rewriteSentenceIntent(
            text = "You are at home",
            langCode = "EN",
            targetIntent = ai.deartalk.android.live.data.SpeechIntent.QUESTION
        )
        assertEquals("You are at home?", question)

        val statement = intentEngine.rewriteSentenceIntent(
            text = "Are you at home?",
            langCode = "EN",
            targetIntent = ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        )
        assertEquals("You are at home.", statement)
    }

    @Test
    fun `parseIntentTagAndClean_의도_태그_파싱_및_본문_분리_테스트`() {
        // 1. QUESTION 태그 파싱
        val rawQuestion = "[INTENT: QUESTION]\n오늘은 며칠이야?"
        val (intentQ, textQ) = DearTalkIntentEngine.parseIntentTagAndClean(rawQuestion)
        assertEquals(ai.deartalk.android.live.data.SpeechIntent.QUESTION, intentQ)
        assertEquals("오늘은 며칠이야?", textQ)

        // 2. REQUEST 태그 파싱 (대소문자 무시)
        val rawRequest = "[intent: request] 이 문서 좀 검토해줘."
        val (intentR, textR) = DearTalkIntentEngine.parseIntentTagAndClean(rawRequest)
        assertEquals(ai.deartalk.android.live.data.SpeechIntent.REQUEST, intentR)
        assertEquals("이 문서 좀 검토해줘.", textR)

        // 3. CONFIRM 태그 파싱
        val rawConfirm = "[INTENT: CONFIRM]\n내일 3시 맞지?"
        val (intentC, textC) = DearTalkIntentEngine.parseIntentTagAndClean(rawConfirm)
        assertEquals(ai.deartalk.android.live.data.SpeechIntent.CONFIRM, intentC)
        assertEquals("내일 3시 맞지?", textC)

        // 4. STATEMENT 태그 파싱
        val rawStatement = "[INTENT: STATEMENT] 나 지금 가고 있어."
        val (intentS, textS) = DearTalkIntentEngine.parseIntentTagAndClean(rawStatement)
        assertEquals(ai.deartalk.android.live.data.SpeechIntent.STATEMENT, intentS)
        assertEquals("나 지금 가고 있어.", textS)

        // 5. 태그 부재 시 fallback 반환
        val rawNoTag = "그냥 일반 텍스트입니다."
        val (intentFallback, textFallback) = DearTalkIntentEngine.parseIntentTagAndClean(
            rawNoTag,
            fallbackIntent = ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        )
        assertEquals(ai.deartalk.android.live.data.SpeechIntent.STATEMENT, intentFallback)
        assertEquals("그냥 일반 텍스트입니다.", textFallback)
    }

    @Test
    fun `cleanLlmOutput_태그_포함_문장_완벽_정제_테스트`() {
        val raw = "[INTENT: QUESTION]\n오늘은 며칠이야?"
        val cleaned = intentEngine.cleanLlmOutput(raw)
        assertEquals("오늘은 며칠이야?", cleaned)
    }

    @Test
    fun `translateWithIntent_단일패스_화행_분류_및_원문보존_테스트`() = runBlocking {
        val (output, intent) = intentEngine.translateWithIntent(
            voiceInput = "너는 어떻게 생각해",
            targetLangCode = "EN",
            sourceLangCode = "KO"
        )
        assertEquals("너는 어떻게 생각해", output)
        assertEquals(ai.deartalk.android.live.data.SpeechIntent.QUESTION, intent)
    }
}

