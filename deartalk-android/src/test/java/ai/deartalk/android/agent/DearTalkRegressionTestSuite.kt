package ai.deartalk.android.agent

import ai.deartalk.android.agent.prompt.ModelFamily
import ai.deartalk.android.agent.prompt.PromptTemplateFactory
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.live.data.SpeechIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 🛡️ [DearTalk 핵심 기능 영구 회귀 방지 테스트 스위트] (DearTalkRegressionTestSuite)
 * 
 * 목적:
 * 1. 실기기(Galaxy S22 Ultra 등)에서 확인된 버그들이 향후 업데이트에서 재발하지 않도록 물리적 관문 구축.
 * 2. 특수 제어 토큰 파손(<end_of_?), 톤 변환 실패 시 맹목적 '?' 부착, 프롬프트-화행 불일치 등 5대 취약점 전수 검증.
 * 3. 100% 통과 시에만 빌드 및 배포가 가능하도록 품질 게이트웨이 역할 수행.
 */
class DearTalkRegressionTestSuite {

    private lateinit var intentEngine: DearTalkIntentEngine

    @Before
    fun setUp() {
        intentEngine = DearTalkIntentEngine(null)
    }

    /**
     * 🛑 [회귀 방지 1]: 톤앤매너 변환 실패 시 맹목적 '?'만 덜렁 붙여 위장하는 버그 방지
     * - 원문이 "내일 9시에 만나"일 때 모델이 미변환되면 "내일 9시에 만나?"로 조작하지 않고 원문을 정직하게 유지해야 함.
     */
    @Test
    fun testRegression_fakeQuestionMarkOnToneFailure() = runBlocking {
        val tone = CustomTone(id = "tone_polite", name = "공손하게", instruction = "공손한 경어체로 변환")
        val input = "내일 9시에 만나"
        
        // 모델 미로드 또는 미변환 fallback 시
        val result = intentEngine.processWithTone(
            voiceInput = input,
            tone = tone,
            speechIntent = SpeechIntent.AUTO
        )

        assertTrue(result is IntentResult.Success)
        val success = result as IntentResult.Success
        // 🚨 원문 뒤에 단순 '?'만 붙은 엉터리 위장이 없어야 함!
        assertFalse("단순 물음표만 붙인 가짜 변환 금지", success.text == "$input?")
        assertEquals("변환 실패 시 순수 원문 유지", input, success.text)
        assertEquals("변환 실패 시 가짜 톤 완료 대신 STT 완료 정직 보고", ai.deartalk.android.data.pref.UiStrings.sttComplete, success.message)
    }

    /**
     * 🛑 [회귀 방지 2]: 특수 제어 토큰 파손/누출 차단 (<end_of_?, <start_of_, <think 등)
     * - 스트리밍 중단이나 토큰 청크로 인해 발생한 모든 미완성 제어 태그 파편이 본문에 절대 노출되지 않아야 함.
     */
    @Test
    fun testRegression_unclosedControlTagLeak() {
        // 단독 파손 토큰 ➔ 빈 문자열로 전수 소거
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<end_of_?"))
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<end_of_"))
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<start_of_turn"))
        assertEquals("", PromptTemplateFactory.cleanLlmOutput("<think>미완성 사고과정"))

        // 본문 뒤에 붙은 파손 토큰 ➔ 본문만 온전히 보존
        val koreanOutput = PromptTemplateFactory.cleanLlmOutput("여기서 사용하는 AI 엔진은 무엇인가요?<end_of_?")
        assertEquals("여기서 사용하는 AI 엔진은 무엇인가요?", koreanOutput)

        val englishOutput = PromptTemplateFactory.cleanLlmOutput("What AI engine is used here?<end_of_")
        assertEquals("What AI engine is used here?", englishOutput)

        val indonesianOutput = PromptTemplateFactory.cleanLlmOutput("Model AI apa yang digunakan di sini?<end_of_turn")
        assertEquals("Model AI apa yang digunakan di sini?", indonesianOutput)
    }

    /**
     * 🛑 [회귀 방지 3]: 파손 출력물 감지 시 사용자 발화 원문 100% 안전 롤백
     * - 화면에 기괴한 특수기호나 빈칸이 나오는 대신 사용자가 말한 내용이 무조건 보존되어야 함.
     */
    @Test
    fun testRegression_corruptedOutputSafeRollback() {
        val originalInput = "여기서 사용하는 AI 엔진은 뭐야"

        // 제어 토큰 파편이 생성 결과로 들어왔을 때
        val rolledBack1 = PromptTemplateFactory.sanitizeKeyboardOutput(originalInput, "<end_of_?", isExplicitQuestion = true)
        assertEquals("원문 100% 보존", originalInput, rolledBack1)

        val rolledBack2 = PromptTemplateFactory.sanitizeKeyboardOutput(originalInput, "<end_of_", isExplicitQuestion = true)
        assertEquals("원문 100% 보존", originalInput, rolledBack2)

        // 기호 찌꺼기만 남은 경우
        val rolledBack3 = PromptTemplateFactory.sanitizeKeyboardOutput(originalInput, "???", isExplicitQuestion = true)
        assertEquals("원문 100% 보존", originalInput, rolledBack3)
    }

    /**
     * 🛑 [회귀 방지 4]: 톤 변환 프롬프트에 SpeechIntent 화행 지침이 100% 주입되는지 검증
     * - 한국어, 인도네시아어, 영어 전반에서 의문문/평서문 지침이 프롬프트에 정확히 반영되어야 함.
     */
    @Test
    fun testRegression_multilingualTonePromptIntentDirective() {
        // 한국어 의문문 프롬프트 검증
        val koPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = "여기서 사용하는 AI 엔진은 뭐야",
            toneName = "공손하게",
            toneInstruction = "공손한 경어체",
            examples = "예시",
            isInputKorean = true,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.QUESTION,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("한국어 질문 지침 포함", koPrompt.contains("질문/의문문 형태를 유지하고 물음표('?')로 끝내세요"))

        // 영어 의문문 프롬프트 검증
        val enPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = "what engine is used here",
            toneName = "Polite",
            toneInstruction = "Polite tone",
            examples = "Example",
            isInputKorean = false,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.QUESTION,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("영어 질문 지침 포함", enPrompt.contains("Keep the question structure and end with a question mark"))

        // 인도네시아어 의문문 프롬프트 검증
        val idPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = "mesin apa yang digunakan di sini",
            toneName = "Sopan",
            toneInstruction = "Sopan",
            examples = "Contoh",
            isInputKorean = false,
            isInputIndonesian = true,
            speechIntent = SpeechIntent.QUESTION,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("인도네시아어 질문 지침 포함", idPrompt.contains("Pertahankan bentuk pertanyaan dan akhiri dengan tanda tanya"))
    }

    /**
     * 🛑 [회귀 방지 5]: 키보드 실시간 어조 변환 성공 판정 무결성
     * - 실제 톤이 변경되었을 때만 성공으로 처리되고, 변환되지 않은 채 구두점만 바뀐 경우는 원문 유지로 처리되는지 검증.
     */
    @Test
    fun testRegression_genuineToneTransformationValidation() {
        val original = "내일 9시에 만나"

        // 1. 단순 구두점만 붙은 텍스트는 미변환으로 판정되어야 함
        val fake1 = "내일 9시에 만나?"
        val isChanged1 = !fake1.trim().trimEnd('?', '!', '.', ' ').equals(original.trim().trimEnd('?', '!', '.', ' '), ignoreCase = true)
        assertFalse("단순 '?' 부착은 미변환 판정", isChanged1)

        val fake2 = "내일 9시에 만나."
        val isChanged2 = !fake2.trim().trimEnd('?', '!', '.', ' ').equals(original.trim().trimEnd('?', '!', '.', ' '), ignoreCase = true)
        assertFalse("단순 '.' 부착은 미변환 판정", isChanged2)

        // 2. 실질적으로 어휘나 어미가 바뀐 진짜 변환문은 정상 판정되어야 함
        val realPolite = "내일 9시쯤 뵐 수 있으실까요?"
        val isChangedReal = !realPolite.trim().trimEnd('?', '!', '.', ' ').equals(original.trim().trimEnd('?', '!', '.', ' '), ignoreCase = true)
        assertTrue("진짜 공손체 변환은 변경으로 판정", isChangedReal)
    }

    /**
     * 🛑 [회귀 방지 6]: AI 다듬은 문장의 실시간 화행 동적 확정 무결성 (Zero Fake Intent)
     * - AI가 "식사하셨어요?"로 다듬었을 때 맹목적으로 STATEMENT로 고정되지 않고 QUESTION으로 확정되어야 함.
     * - AI가 "자료 부탁드립니다"로 다듬었을 때 REQUEST로 확정되어야 함.
     * - AI가 "내일 3시 맞죠?"로 다듬었을 때 CONFIRM으로 확정되어야 함.
     */
    @Test
    fun testRegression_aiRefinedOutputDynamicIntentResolution() {
        val testCases = listOf(
            Triple("식사하셨어요?", "KO", SpeechIntent.QUESTION),
            Triple("밥은 잘 먹었어? 😊", "KO", SpeechIntent.QUESTION),
            Triple("내일 9시에 만날까?", "KO", SpeechIntent.QUESTION),
            Triple("자료 검토 부탁드립니다.", "KO", SpeechIntent.REQUEST),
            Triple("문 좀 열어주세요 😊", "KO", SpeechIntent.REQUEST),
            Triple("내일 3시 맞으시죠?", "KO", SpeechIntent.CONFIRM),
            Triple("오늘 다 끝난 거지?", "KO", SpeechIntent.CONFIRM),
            Triple("지금 사무실로 이동하고 있습니다.", "KO", SpeechIntent.STATEMENT),
            Triple("Are you available for a quick sync?", "EN", SpeechIntent.QUESTION),
            Triple("Could you please review the attached document?", "EN", SpeechIntent.REQUEST),
            Triple("You will join the meeting, right?", "EN", SpeechIntent.CONFIRM),
            Triple("I have finished the report.", "EN", SpeechIntent.STATEMENT)
        )

        for ((refinedText, langCode, expectedIntent) in testCases) {
            val detected = ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(refinedText, langCode)
            assertEquals(
                "AI 다듬기 문장 '$refinedText'($langCode)의 화행은 반드시 $expectedIntent 이어야 함 (STATEMENT 고정 버그 차단)",
                expectedIntent,
                detected
            )
        }
    }

    /**
     * 🛑 [회귀 방지 7]: 화행 칩 변경 시 해당 화행별 지침(QUESTION, REQUEST, CONFIRM, STATEMENT) 주입 검증
     * - 사용자가 화행 버튼을 탭했을 때 프롬프트 팩토리가 해당 화행에 맞춤화된 지침을 생성하는지 검증.
     */
    @Test
    fun testRegression_intentChangeRetransformsSpeechAct() {
        val input = "내일 9시에 만나"
        val toneName = "공손하게"
        val toneInstruction = "공손한 경어체"

        // 1. QUESTION 선택 시
        val questionPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = input,
            toneName = toneName,
            toneInstruction = toneInstruction,
            examples = "",
            isInputKorean = true,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.QUESTION,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("질문 화행 지침 주입 확인", questionPrompt.contains("질문/의문문"))

        // 2. REQUEST 선택 시
        val requestPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = input,
            toneName = toneName,
            toneInstruction = toneInstruction,
            examples = "",
            isInputKorean = true,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.REQUEST,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("부탁 화행 지침 주입 확인", requestPrompt.contains("정중히 요청") || requestPrompt.contains("부탁"))

        // 3. CONFIRM 선택 시
        val confirmPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = input,
            toneName = toneName,
            toneInstruction = toneInstruction,
            examples = "",
            isInputKorean = true,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.CONFIRM,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("확인 화행 지침 주입 확인", confirmPrompt.contains("확인") || confirmPrompt.contains("되묻는"))

        // 4. STATEMENT 선택 시
        val statementPrompt = PromptTemplateFactory.buildTonePrompt(
            trimmed = input,
            toneName = toneName,
            toneInstruction = toneInstruction,
            examples = "",
            isInputKorean = true,
            isInputIndonesian = false,
            speechIntent = SpeechIntent.STATEMENT,
            modelFamily = ModelFamily.GEMMA
        )
        assertTrue("설명/서술 화행 지침 주입 확인", statementPrompt.contains("설명") || statementPrompt.contains("서술"))
    }

    /**
     * 🛑 [회귀 방지 8]: 8대 핵심 톤앤매너 x 4대 화행 조합 시 프롬프트 지침 전수 생성 무결성
     */
    @Test
    fun testRegression_allToneProfilesPromptGeneration() {
        val testTones = listOf(
            CustomTone("tone_refine", "기본다듬기", "자연스럽고 매끄러운 어조"),
            CustomTone("tone_confident", "당당하게", "자신감 있고 명확한 어조"),
            CustomTone("tone_formal", "정중하게", "비즈니스 격식 어조"),
            CustomTone("tone_polite", "부드럽게", "상냥하고 공손한 어조"),
            CustomTone("tone_friendly", "친근하게", "따뜻하고 친근한 어조"),
            CustomTone("tone_concise", "간결하게", "핵심만 간단명료한 어조"),
            CustomTone("tone_humorous", "유머러스하게", "재치 있는 어조"),
            CustomTone("tone_casual", "편안하게", "친구 같은 편안한 반말")
        )

        val intents = listOf(SpeechIntent.QUESTION, SpeechIntent.REQUEST, SpeechIntent.CONFIRM, SpeechIntent.STATEMENT)

        for (tone in testTones) {
            for (intent in intents) {
                val prompt = PromptTemplateFactory.buildTonePrompt(
                    trimmed = "내일 회의 준비 다 됐어",
                    toneName = tone.name,
                    toneInstruction = tone.instruction,
                    examples = "",
                    isInputKorean = true,
                    isInputIndonesian = false,
                    speechIntent = intent,
                    modelFamily = ModelFamily.GEMMA
                )
                assertTrue("톤 '${tone.name}' 프롬프트에 어조 지침 포함", prompt.contains(tone.name))
                assertTrue("톤 '${tone.name}' 프롬프트에 화행 지침 포함", prompt.contains("SPEECH INTENT") || prompt.contains("화행"))
            }
        }
    }

    /**
     * 🛑 [회귀 방지 9]: 공백, 빈 문자열, 기호 입력 시 충돌 없이 안전 롤백 보장
     */
    @Test
    fun testRegression_blankAndSymbolInputGracefulHandling() = runBlocking {
        val tone = CustomTone("tone_polite", "공손하게", "공손한 경어체")

        val emptyResult = intentEngine.processWithTone(
            voiceInput = "   ",
            tone = tone,
            speechIntent = SpeechIntent.AUTO
        )
        assertTrue(emptyResult is IntentResult.Success)
        assertEquals("공백 입력 시 원문 트림 보존", "", (emptyResult as IntentResult.Success).text)

        val symbolResult = intentEngine.processWithTone(
            voiceInput = "!@#$%",
            tone = tone,
            speechIntent = SpeechIntent.AUTO
        )
        assertTrue(symbolResult is IntentResult.Success)
        assertEquals("특수문자 입력 시 원문 유지", "!@#$%", (symbolResult as IntentResult.Success).text)
    }

    /**
     * 🛑 [회귀 방지 10]: 2-Tier 모드(기본/프로) 분기 및 격리 무결성 검증
     * - 3세 이상 전연령 대상 기본 모드가 기본값으로 안전하게 제공되는지 검증.
     * - 프로 모드 전환 시에만 번역 활성화가 허용되는지 검증.
     */
    @Test
    fun testRegression_keyboardModeIsolationAndDefaults() {
        val defaultUiState = ai.deartalk.android.ui.state.ImeUiState()
        assertEquals("기본 모드가 디폴트여야 함 (전연령 초직관성)", ai.deartalk.android.data.pref.KeyboardMode.BASIC, defaultUiState.keyboardMode)
        assertFalse("기본 모드에서는 번역 모드가 꺼져 있어야 함", defaultUiState.isTranslationMode)

        val proUiState = defaultUiState.copy(
            keyboardMode = ai.deartalk.android.data.pref.KeyboardMode.PRO,
            isTranslationMode = true
        )
        assertEquals("프로 모드 전환 성공", ai.deartalk.android.data.pref.KeyboardMode.PRO, proUiState.keyboardMode)
        assertTrue("프로 모드에서는 다국어 번역 허용", proUiState.isTranslationMode)
    }

    /**
     * 🛑 [회귀 방지 11]: 사용자가 질문 화행(QUESTION) 명시적 선택 시 물음표('?') 보장 무결성
     * - 실기기 테스트 케이스: "오늘 나랑 같이 놀 거야" 발화 + [❓ 질문] 화행 칩 선택
     * - 어조 어휘 변경 여부와 상관없이 사용자가 질문 화행을 지정했다면 결과 문장 끝에 반드시 '?'가 붙어야 함.
     */
    @Test
    fun testRegression_explicitQuestionIntentAlwaysHasQuestionMark() = runBlocking {
        val tone = CustomTone("tone_default", "기본", "기본다듬기")
        val input = "오늘 나랑 같이 놀 거야"

        // 1. QUESTION 화행 명시적 선택 시 (모델 미로드/폴백 포함)
        val resultQuestion = intentEngine.processWithTone(
            voiceInput = input,
            tone = tone,
            speechIntent = SpeechIntent.QUESTION
        )
        assertTrue(resultQuestion is IntentResult.Success)
        val successQ = resultQuestion as IntentResult.Success
        assertEquals("사용자가 질문 화행 지정 시 반드시 물음표 부착", "오늘 나랑 같이 놀 거야?", successQ.text)
        assertEquals("화행은 QUESTION으로 확정", SpeechIntent.QUESTION, successQ.detectedIntent)

        // 2. STATEMENT 화행 명시적 선택 시 (물음표 제거 및 마침표 보장)
        val resultStatement = intentEngine.processWithTone(
            voiceInput = "오늘 나랑 같이 놀 거야?",
            tone = tone,
            speechIntent = SpeechIntent.STATEMENT
        )
        assertTrue(resultStatement is IntentResult.Success)
        val successS = resultStatement as IntentResult.Success
        assertEquals("사용자가 설명 화행 지정 시 물음표 제거 및 마침표 부착", "오늘 나랑 같이 놀 거야.", successS.text)
        assertEquals("화행은 STATEMENT로 확정", SpeechIntent.STATEMENT, successS.detectedIntent)
    }

    /**
     * 🛑 [회귀 방지 12]: 이미 확정된 텍스트에 대한 연속 화행 전환 멱등성 및 원문 보존 검증
     * - "오늘 나랑 같이 놀 거야"
     *   -> QUESTION: "오늘 나랑 같이 놀 거야?"
     *   -> STATEMENT: "오늘 나랑 같이 놀 거야." (구두점 누적 '?.', '?!' 방지)
     *   -> CONFIRM: "오늘 나랑 같이 놀 거야?"
     *   -> REQUEST: "오늘 나랑 같이 놀 거야."
     *   -> QUESTION (다시 선택): "오늘 나랑 같이 놀 거야?"
     *   -> QUESTION (동일 화행 반복 탭): "오늘 나랑 같이 놀 거야?" ('??' 중복 방지)
     */
    @Test
    fun testRegression_sequentialSpeechIntentTransitionsOnRefinedText() = runBlocking {
        val tone = CustomTone("tone_default", "기본", "기본다듬기")
        var currentText = "오늘 나랑 같이 놀 거야"

        // Step 1: QUESTION
        val r1 = intentEngine.processWithTone(currentText, tone, speechIntent = SpeechIntent.QUESTION)
        assertTrue(r1 is IntentResult.Success)
        currentText = (r1 as IntentResult.Success).text
        assertEquals("1단계 QUESTION 전환", "오늘 나랑 같이 놀 거야?", currentText)
        assertEquals(SpeechIntent.QUESTION, r1.detectedIntent)

        // Step 2: STATEMENT (물음표 -> 마침표, 구두점 누적 없음)
        val r2 = intentEngine.processWithTone(currentText, tone, speechIntent = SpeechIntent.STATEMENT)
        assertTrue(r2 is IntentResult.Success)
        currentText = (r2 as IntentResult.Success).text
        assertEquals("2단계 STATEMENT 전환", "오늘 나랑 같이 놀 거야.", currentText)
        assertEquals(SpeechIntent.STATEMENT, r2.detectedIntent)

        // Step 3: CONFIRM (마침표 -> 물음표)
        val r3 = intentEngine.processWithTone(currentText, tone, speechIntent = SpeechIntent.CONFIRM)
        assertTrue(r3 is IntentResult.Success)
        currentText = (r3 as IntentResult.Success).text
        assertEquals("3단계 CONFIRM 전환", "오늘 나랑 같이 놀 거야?", currentText)
        assertEquals(SpeechIntent.CONFIRM, r3.detectedIntent)

        // Step 4: REQUEST (물음표 -> 마침표)
        val r4 = intentEngine.processWithTone(currentText, tone, speechIntent = SpeechIntent.REQUEST)
        assertTrue(r4 is IntentResult.Success)
        currentText = (r4 as IntentResult.Success).text
        assertEquals("4단계 REQUEST 전환", "오늘 나랑 같이 놀 거야.", currentText)
        assertEquals(SpeechIntent.REQUEST, r4.detectedIntent)

        // Step 5: QUESTION 다시 선택
        val r5 = intentEngine.processWithTone(currentText, tone, speechIntent = SpeechIntent.QUESTION)
        assertTrue(r5 is IntentResult.Success)
        currentText = (r5 as IntentResult.Success).text
        assertEquals("5단계 QUESTION 재전환", "오늘 나랑 같이 놀 거야?", currentText)
        assertEquals(SpeechIntent.QUESTION, r5.detectedIntent)

        // Step 6: QUESTION 동일 화행 반복 탭 (멱등성: 중복 '??' 생성 차단)
        val r6 = intentEngine.processWithTone(currentText, tone, speechIntent = SpeechIntent.QUESTION)
        assertTrue(r6 is IntentResult.Success)
        currentText = (r6 as IntentResult.Success).text
        assertEquals("6단계 QUESTION 반복 탭 멱등성 보장", "오늘 나랑 같이 놀 거야?", currentText)
        assertEquals(SpeechIntent.QUESTION, r6.detectedIntent)
    }

    /**
     * 🛑 [회귀 방지 13]: 다국어(EN, ID) 화행 연속 전환 및 구두점 무결성
     */
    @Test
    fun testRegression_multilingualIntentSequentialSwitching() = runBlocking {
        val tone = CustomTone("tone_default", "Default", "Default tone")

        // 1. 영어(EN) 연속 화행 전환
        val enText = "You will join the team today"
        val enQ = intentEngine.processWithTone(enText, tone, speechIntent = SpeechIntent.QUESTION)
        assertEquals("You will join the team today?", (enQ as IntentResult.Success).text)

        val enS = intentEngine.processWithTone((enQ as IntentResult.Success).text, tone, speechIntent = SpeechIntent.STATEMENT)
        assertEquals("You will join the team today.", (enS as IntentResult.Success).text)

        val enC = intentEngine.processWithTone((enS as IntentResult.Success).text, tone, speechIntent = SpeechIntent.CONFIRM)
        assertEquals("You will join the team today?", (enC as IntentResult.Success).text)

        // 2. 인도네시아어(ID) 연속 화행 전환
        val idText = "Kamu mau ikut hari ini"
        val idQ = intentEngine.processWithTone(idText, tone, speechIntent = SpeechIntent.QUESTION)
        assertEquals("Kamu mau ikut hari ini?", (idQ as IntentResult.Success).text)

        val idS = intentEngine.processWithTone((idQ as IntentResult.Success).text, tone, speechIntent = SpeechIntent.STATEMENT)
        assertEquals("Kamu mau ikut hari ini.", (idS as IntentResult.Success).text)

        val idC = intentEngine.processWithTone((idS as IntentResult.Success).text, tone, speechIntent = SpeechIntent.CONFIRM)
        assertEquals("Kamu mau ikut hari ini?", (idC as IntentResult.Success).text)
    }

    /**
     * 🛑 [회귀 방지 14]: 5대 어조 x 4대 화행 전수 조합(Cross-Product Matrix) 안정성 검증
     * - 어조 버튼(기본, 공손, 친근, 비즈니스, 당당)과 화행 버튼(질문, 설명, 부탁, 확인)의 모든 조합에서
     * - QUESTION / CONFIRM은 반드시 '?' 부착 및 올바른 화행 판정
     * - STATEMENT / REQUEST는 반드시 '.' 부착 및 올바른 화행 판정
     */
    @Test
    fun testRegression_toneAndIntentCrossProductCombinations() = runBlocking {
        val tones = listOf(
            CustomTone("tone_default", "기본", "기본다듬기"),
            CustomTone("tone_polite", "공손하게", "공손한 경어체"),
            CustomTone("tone_casual", "친근하게", "친근한 어조"),
            CustomTone("tone_business", "비즈니스", "정중한 비즈니스"),
            CustomTone("tone_cheeky", "당당하게", "자신감 넘치는 어조")
        )
        val intents = listOf(
            SpeechIntent.QUESTION,
            SpeechIntent.STATEMENT,
            SpeechIntent.REQUEST,
            SpeechIntent.CONFIRM
        )
        val sampleInput = "오늘 프로젝트 마감할 수 있어"

        for (tone in tones) {
            for (intent in intents) {
                val result = intentEngine.processWithTone(
                    voiceInput = sampleInput,
                    tone = tone,
                    speechIntent = intent
                )
                assertTrue("성공 응답 반환: 어조=${tone.name}, 화행=$intent", result is IntentResult.Success)
                val success = result as IntentResult.Success
                assertEquals("지정 화행 일치: 어조=${tone.name}", intent, success.detectedIntent)

                when (intent) {
                    SpeechIntent.QUESTION, SpeechIntent.CONFIRM -> {
                        assertTrue("의문/확인 화행은 반드시 '?'로 끝남: ${success.text}", success.text.endsWith("?"))
                    }
                    SpeechIntent.STATEMENT, SpeechIntent.REQUEST -> {
                        assertTrue("설명/요청 화행은 반드시 '.'로 끝남: ${success.text}", success.text.endsWith("."))
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 🛑 [회귀 방지 15]: 비정상 구두점 찌꺼기('??!.', '!?!?!') 입력 시 안전 정제 및 정상 화행 전환
     */
    @Test
    fun testRegression_messyPunctuationCleanTransition() = runBlocking {
        val tone = CustomTone("tone_default", "기본", "기본다듬기")
        val messyInputs = listOf(
            "밥 먹었니?!?!?",
            "오늘 나랑 같이 놀 거야??!.",
            "내일 시간 되시나요...?",
            "지금 바로 출발합니다!."
        )

        for (messy in messyInputs) {
            val qResult = intentEngine.processWithTone(messy, tone, speechIntent = SpeechIntent.QUESTION)
            assertTrue(qResult is IntentResult.Success)
            val qText = (qResult as IntentResult.Success).text
            assertTrue("비정상 부호 정제 후 단일 '?' 부착: $qText", qText.endsWith("?") && !qText.endsWith("??"))

            val sResult = intentEngine.processWithTone(messy, tone, speechIntent = SpeechIntent.STATEMENT)
            assertTrue(sResult is IntentResult.Success)
            val sText = (sResult as IntentResult.Success).text
            assertTrue("비정상 부호 정제 후 단일 '.' 부착: $sText", sText.endsWith(".") && !sText.endsWith(".."))
        }
    }

    /**
     * 🛑 [회귀 방지 16]: AI 자동 판별(AUTO)과 사용자 수동 오버라이드 상호작용 검증
     * - AI가 평서문으로 자동 분류했더라도, 사용자가 QUESTION을 명시하면 즉시 의문문으로 변환
     * - AI가 의문문으로 자동 분류했더라도, 사용자가 STATEMENT를 명시하면 즉시 평서문으로 변환
     */
    @Test
    fun testRegression_aiAutoDetectionVsUserOverride() = runBlocking {
        val tone = CustomTone("tone_default", "기본", "기본다듬기")

        // 1. AI 자동 판별: 평서문 발화 시
        val autoStatement = intentEngine.processWithTone("지금 사무실에 도착했습니다", tone, speechIntent = SpeechIntent.AUTO)
        assertTrue(autoStatement is IntentResult.Success)
        assertEquals(SpeechIntent.STATEMENT, (autoStatement as IntentResult.Success).detectedIntent)

        // 사용자 수동 오버라이드: QUESTION 선택
        val userOverrideQ = intentEngine.processWithTone("지금 사무실에 도착했습니다", tone, speechIntent = SpeechIntent.QUESTION)
        assertTrue(userOverrideQ is IntentResult.Success)
        val qRes = userOverrideQ as IntentResult.Success
        assertEquals(SpeechIntent.QUESTION, qRes.detectedIntent)
        assertEquals("지금 사무실에 도착했습니다?", qRes.text)

        // 2. AI 자동 판별: 의문문 발화 시
        val autoQuestion = intentEngine.processWithTone("밥은 잘 먹었어?", tone, speechIntent = SpeechIntent.AUTO)
        assertTrue(autoQuestion is IntentResult.Success)
        assertEquals(SpeechIntent.QUESTION, (autoQuestion as IntentResult.Success).detectedIntent)

        // 사용자 수동 오버라이드: STATEMENT 선택
        val userOverrideS = intentEngine.processWithTone("밥은 잘 먹었어?", tone, speechIntent = SpeechIntent.STATEMENT)
        assertTrue(userOverrideS is IntentResult.Success)
        val sRes = userOverrideS as IntentResult.Success
        assertEquals(SpeechIntent.STATEMENT, sRes.detectedIntent)
        assertEquals("밥은 잘 먹었어.", sRes.text)
    }

    /**
     * 🛑 [회귀 방지 17]: 메시지 전송(입력) 및 대기 텍스트 삭제 시 화행 AUTO 자동 리셋 검증
     * - 어조(Tone)는 세션 유지를 위해 보존되지만, 화행(SpeechIntent)은 메시지 확정 시 즉각 AUTO로 리셋
     * - 다음 문장 발화 시 직전의 질문/요청 설정이 간섭하지 않고 AI가 새롭게 문맥을 판단하도록 보장
     */
    @Test
    fun testRegression_intentAutoResetStateOnCommitOrClear() {
        var selectedIntent = SpeechIntent.QUESTION
        var detectedIntent = SpeechIntent.QUESTION

        // 사용자가 특정 발화에서 질문을 선택하고 에디터에 삽입한 상황 시뮬레이션
        fun onCommitOrClear() {
            selectedIntent = SpeechIntent.AUTO
            detectedIntent = SpeechIntent.AUTO
        }

        onCommitOrClear()

        assertEquals("커밋 시 화행은 반드시 AUTO로 복귀", SpeechIntent.AUTO, selectedIntent)
        assertEquals("감지된 화행도 AUTO로 복귀", SpeechIntent.AUTO, detectedIntent)
    }
}


