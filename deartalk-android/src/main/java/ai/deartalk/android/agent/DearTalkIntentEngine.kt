package ai.deartalk.android.agent

import android.content.Context
import android.util.Log
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.TranslationTarget
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.data.repository.ContextRepository
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed interface IntentResult {
    data class Success(val text: String, val message: String = "") : IntentResult
    data class Error(val fallbackText: String, val error: String) : IntentResult
}

/**
 * 100% 온디바이스 초경량 순수 신경망 추론 엔진 (LiteRT-LM)
 * - Clean Code 원칙: 단일 책임, 프롬프트 빌더 분리, 일관된 세션 라이프사이클 관리, 원문 언어 100% 보존.
 */
class DearTalkIntentEngine(
    private val context: Context?,
    private val contextRepository: ContextRepository?
) {
    companion object {
        private const val TAG = "DearTalkAI"
        private val initScope = CoroutineScope(Dispatchers.IO)
        private val initMutex = Mutex()
        private var sharedLiteRtEngine: Engine? = null
        private var sharedInitJob: Job? = null

        private val _isModelLoadedFlow = MutableStateFlow(false)
        val isModelLoadedFlow: StateFlow<Boolean> = _isModelLoadedFlow.asStateFlow()

        @Volatile
        var sharedLoaded: Boolean = false
            private set

        /**
         * 입력 텍스트에 한글 유니코드(완성형, 자모, 호환자모)가 포함되어 있는지 판별합니다.
         */
        fun hasKorean(text: String): Boolean {
            return text.any { ch ->
                (ch in '\uAC00'..'\uD7A3') || (ch in '\u1100'..'\u11FF') || (ch in '\u3130'..'\u318F')
            }
        }

        /**
         * 입력 텍스트가 순수 영문 위주인지 판별합니다.
         */
        fun isEnglish(text: String): Boolean {
            if (hasKorean(text)) return false
            val letters = text.filter { it.isLetter() }
            if (letters.isEmpty()) return false
            return letters.all { it in 'a'..'z' || it in 'A'..'Z' }
        }
    }

    val isModelLoaded: Boolean
        get() = sharedLoaded

    val isModelLoadedFlow: StateFlow<Boolean>
        get() = DearTalkIntentEngine.isModelLoadedFlow

    init {
        ensureModelLoaded()
    }

    private fun ensureModelLoaded() {
        if (isModelLoaded || context == null) return
        synchronized(DearTalkIntentEngine::class.java) {
            if (sharedInitJob == null || sharedInitJob?.isCompleted == true) {
                sharedInitJob = initScope.launch {
                    initMutex.withLock {
                        if (!isModelLoaded) {
                            initOnDeviceModel(context.applicationContext)
                        }
                    }
                }
            }
        }
    }

    private fun initOnDeviceModel(appContext: Context) {
        val candidatePaths = resolveCandidateModelPaths(appContext)

        for (path in candidatePaths) {
            val file = File(path)
            if (!file.exists() || file.length() <= 0) continue

            val backends = listOf(Backend.CPU(), Backend.GPU())
            for (backend in backends) {
                if (tryInitializeEngine(path, backend, appContext, file.length())) {
                    return
                }
            }
        }
    }

    private fun resolveCandidateModelPaths(appContext: Context): List<String> {
        val paths = mutableListOf(
            "/data/local/tmp/llm/model.litertlm",
            "/data/local/tmp/llm/gemma-2b-it.litertlm",
            "/data/local/tmp/llm/model.bin",
            "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin",
            "/data/local/tmp/llm/gemma-2b-it-cpu-int4.bin",
            File(appContext.filesDir, "models/model.litertlm").absolutePath,
            File(appContext.filesDir, "models/model.bin").absolutePath
        )

        val llmDir = File("/data/local/tmp/llm/")
        if (llmDir.exists() && llmDir.isDirectory) {
            llmDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".litertlm", ignoreCase = true) }
                ?.forEach { file ->
                    if (!paths.contains(file.absolutePath)) {
                        paths.add(0, file.absolutePath)
                    }
                }
        }
        return paths
    }

    private fun tryInitializeEngine(
        path: String,
        backend: Backend,
        appContext: Context,
        fileSize: Long
    ): Boolean {
        return try {
            Log.d(TAG, "🔄 온디바이스 LLM 초기화 시도 ($backend): $path")
            val config = EngineConfig(
                modelPath = path,
                backend = backend,
                cacheDir = appContext.cacheDir.absolutePath
            )
            val engine = Engine(config)
            engine.initialize()

            // Warm-up 검증
            val session = engine.createSession()
            val testResp = session.generateContent(listOf(InputData.Text("Hello"))).trim()
            session.close()
            Log.d(TAG, "🧪 [온디바이스 LLM Warm-up 성공 ($backend)]: '$testResp'")

            sharedLiteRtEngine = engine
            sharedLoaded = true
            _isModelLoadedFlow.value = true
            Log.d(TAG, "✅ [초경량 온디바이스 LLM 로드 완료 ($backend)]: $path (${fileSize / 1024 / 1024}MB)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "⚠️ 온디바이스 LLM 초기화 실패 ($path, $backend): ${e.message}")
            false
        }
    }

    fun detectAndInitOnDeviceModel() {
        if (context == null) return
        synchronized(DearTalkIntentEngine::class.java) {
            sharedInitJob = initScope.launch {
                initMutex.withLock {
                    sharedLoaded = false
                    _isModelLoadedFlow.value = false
                    initOnDeviceModel(context.applicationContext)
                }
            }
        }
    }

    /**
     * 1. 실시간 음성 문장 교정 (오탈자 수정, 물음표/느낌표/마침표 문맥 부착)
     */
    suspend fun process(
        voiceInput: String,
        currentEditorText: String = "",
        packageName: String = ""
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) {
            return@withContext IntentResult.Success("")
        }

        awaitModelInitializationIfActive()

        if (isModelLoaded) {
            try {
                val prompt = PromptTemplateBuilder.buildRefinePrompt(trimmed)
                val output = executeInference(prompt)

                if (output.isNotBlank()) {
                    Log.d(TAG, "✨ [온디바이스 LLM 생성 완료]: '$trimmed' ➔ '$output'")
                    contextRepository?.recordInteraction(packageName, trimmed, output, "ON_DEVICE_LLM")
                    return@withContext IntentResult.Success(output, UiStrings.aiGenerationComplete)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 온디바이스 LLM 추론 오류: ${e.message}")
            }
        }

        // Fallback: 0% Fake Rule - 원문 정직 보존
        contextRepository?.recordInteraction(packageName, trimmed, trimmed, "STT_RAW")
        return@withContext IntentResult.Success(trimmed, UiStrings.sttRawResult)
    }

    /**
     * 2. 4대 톤앤매너 변환 (기본다듬기, 공손하게, 친근하게, 비즈니스 등)
     */
    suspend fun processWithTone(
        voiceInput: String,
        tone: CustomTone,
        currentEditorText: String = "",
        packageName: String = ""
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext IntentResult.Success("")

        awaitModelInitializationIfActive()

        if (isModelLoaded) {
            try {
                val prompt = PromptTemplateBuilder.buildTonePrompt(trimmed, tone)
                val output = executeInference(prompt)

                if (output.isNotBlank()) {
                    contextRepository?.recordInteraction(packageName, trimmed, output, "CUSTOM_TONE")
                    return@withContext IntentResult.Success(output, UiStrings.toneComplete(tone.icon, tone.name))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 톤앤매너 실행 오류: ${e.message}")
            }
        }

        return@withContext process(voiceInput, currentEditorText, packageName)
    }

    /**
     * 3. 실시간 온디바이스 다국어 번역
     */
    suspend fun processWithTranslation(
        voiceInput: String,
        target: TranslationTarget,
        currentEditorText: String = "",
        packageName: String = ""
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext IntentResult.Success("")

        awaitModelInitializationIfActive()

        if (isModelLoaded) {
            try {
                val prompt = PromptTemplateBuilder.buildTranslationPrompt(trimmed, target)
                val output = executeInference(prompt)

                if (output.isNotBlank()) {
                    Log.d(TAG, "✨ [온디바이스 LLM 다국어 번역 완료]: '$trimmed' ➔ '$output' (${target.name})")
                    contextRepository?.recordInteraction(packageName, trimmed, output, "TRANSLATION_${target.id}")
                    return@withContext IntentResult.Success(output, UiStrings.translationComplete(target.flag, target.name))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 다국어 번역 실행 오류: ${e.message}")
            }
        }

        return@withContext process(voiceInput, currentEditorText, packageName)
    }

    private suspend fun awaitModelInitializationIfActive() {
        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try {
                sharedInitJob?.join()
            } catch (_: Throwable) {}
        }
    }

    private fun executeInference(prompt: String): String {
        val engine = sharedLiteRtEngine ?: return ""
        val session = engine.createSession()
        return try {
            val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
            cleanLlmOutput(response)
        } finally {
            try {
                session.close()
            } catch (_: Throwable) {}
        }
    }

    internal fun cleanLlmOutput(raw: String): String {
        var text = raw
            .replace(Regex("""<(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)[^>]*>\s*(model|user|assistant)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</?[a-zA-Z0-9_-]+(\s+[^>]*)?>"""), "")
            .trim()

        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                text = lines.first()
            }
        }

        return text
            .replace(Regex("""^(최종\s*문장|수정된\s*문장|다듬은\s*문장|변환된\s*문장|결과|답변|제안|답|문장|Output|Result|Sentence|model|assistant|AI|Translation|Translated Text)\s*[:：]\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .removePrefix(">")
            .removePrefix("-")
            .removePrefix("*")
            .removeSurrounding("\"")
            .removeSurrounding("`")
            .removeSurrounding("```")
            .trim()
    }
}

/**
 * 온디바이스 LLM 프롬프트 템플릿 빌더
 * - 언어 보존, 챗봇 모드 방지, 단일 행 출력 규칙을 캡슐화합니다.
 */
internal object PromptTemplateBuilder {

    fun buildRefinePrompt(input: String): String {
        val isInputKorean = DearTalkIntentEngine.hasKorean(input)
        val isInputEnglish = DearTalkIntentEngine.isEnglish(input)

        return when {
            isInputKorean -> """
                <start_of_turn>user
                당신은 모바일 키보드의 '실시간 음성 문장 교정 및 다듬기 AI'입니다.
                ⚠️ 중요: 당신은 챗봇이 아니므로 절대로 사용자의 말에 대답하거나 대화를 나누지 마세요!
                당신의 유일한 임무는 사용자가 말한 거칠거나 불완전한 음성 내용을, 상대방에게 즉시 보낼 수 있도록 맞춤법/오탈자를 완벽히 교정하고 자연스럽고 정돈된 문장으로 세련되게 다듬어 주는 것입니다.

                [변환 예시]
                - "금요일 제외한 매일 11시에서 11시30분까지는 Privacy 스크럼이니 절대로 잊지마" -> 금요일을 제외한 매일 11시부터 11시 30분까지는 Privacy 스크럼 일정이니 꼭 기억해 주세요.
                - "지금 가고 있는데 차 막혀서 늦을듯 미안" -> 지금 이동 중인데 도로가 정체되어 조금 늦을 것 같습니다.
                - "자료 보냈으니 확인해보고 알려줘" -> 송부드린 자료 확인 후 회신 부탁드립니다.
                - "내일 몇 시에 만날까" -> 내일 몇 시에 만날까요?
                - "오늘 정말 고마웠어" -> 오늘 정말 감사했습니다!

                [출력 규칙]
                1. 질문 문장이더라도 절대 답하지 말고, 질문 문장 자체를 정돈하여 출력하세요.
                2. 설명, 따옴표, 마크다운 기호 없이 오직 상대방에게 전송할 한 줄의 다듬어진 한국어 문장만 평문으로 출력하세요.

                음성 원문: "$input"<end_of_turn>
                <start_of_turn>model
            """.trimIndent() + "\n"

            isInputEnglish -> """
                <start_of_turn>user
                You are a mobile keyboard's 'speech-to-text grammar & punctuation corrector'.
                ⚠️ CRITICAL: You are NOT a chatbot. Do NOT answer questions or converse with the user!
                ⚠️ ABSOLUTE RULE: The input is in English. Keep it strictly in ENGLISH. Do NOT translate to Korean or any other language!
                Your ONLY duty is to correct typos, fix grammar, and attach appropriate punctuation marks ('?', '!', '.', ',') in English so the user can send it as a clean message.

                [Examples]
                - "what time should we meet tomorrow" -> What time should we meet tomorrow?
                - "i just arrived safely" -> I just arrived safely.
                - "thank you so much for your help" -> Thank you so much for your help!
                - "where is the meeting room please tell me" -> Where is the meeting room? Please tell me.

                [Output Rules]
                1. If the input is a question, do NOT answer it. Just refine the English question itself.
                2. Output ONLY the refined single-line English text without quotes, markdown, or greetings.

                Input: "$input"<end_of_turn>
                <start_of_turn>model
            """.trimIndent() + "\n"

            else -> """
                <start_of_turn>user
                You are a mobile keyboard's 'speech-to-text punctuation corrector'.
                ⚠️ CRITICAL: Do NOT answer questions. Keep the original language of the input.
                Attach appropriate punctuation marks and output ONLY the single-line refined text.

                Input: "$input"<end_of_turn>
                <start_of_turn>model
            """.trimIndent() + "\n"
        }
    }

    fun buildTonePrompt(input: String, tone: CustomTone): String {
        val isInputKorean = DearTalkIntentEngine.hasKorean(input)
        val examples = if (isInputKorean) getKoreanToneExamples(tone.id) else getEnglishToneExamples(tone.id)

        return if (isInputKorean) {
            """
                <start_of_turn>user
                당신은 모바일 키보드의 '텍스트 어조/톤 변환기'입니다.
                ⚠️ 중요: 당신은 챗봇이 아니므로 절대로 질문에 대답하거나 대화를 시도하지 마세요!
                화자의 핵심 의도와 내용을 100% 보존하면서, 텍스트의 어조만 '${tone.name}'(${tone.instruction}) 스타일로 다시 작성하세요.

                $examples

                [출력 규칙]
                1. 원문이 질문이더라도 절대 답하지 말고, 원문 자체를 ${tone.name} 어조로 변환한 한 줄의 문장만 출력하세요.
                2. 설명, 인사말, 따옴표, 라벨 접두어 없이 오직 변환된 한국어 텍스트만 평문으로 출력하세요.

                변환할 원문: "$input"<end_of_turn>
                <start_of_turn>model
            """.trimIndent() + "\n"
        } else {
            """
                <start_of_turn>user
                You are a mobile keyboard's 'tone & style transformer'.
                ⚠️ CRITICAL: You are NOT a chatbot. Do NOT answer questions or converse with the user!
                ⚠️ ABSOLUTE RULE: Keep the original language (English) of the input text. Do NOT translate it into Korean or other languages!
                Convert the tone of the English text into the target style '${tone.name}' (${tone.instruction}) while preserving the original meaning.

                $examples

                [Output Rules]
                1. Even if the input is a question, do NOT answer it. Just refine and convert the question itself in English.
                2. Output ONLY the refined English text without explanations, greetings, quotes, or markdown.

                Input: "$input"<end_of_turn>
                <start_of_turn>model
            """.trimIndent() + "\n"
        }
    }

    fun buildTranslationPrompt(input: String, target: TranslationTarget): String {
        val targetLangInstruction = when {
            target.id.contains("en") || target.name.contains("영어") || target.name.contains("English") -> "natural, fluent English"
            target.id.contains("ja") || target.name.contains("일본어") || target.name.contains("Japanese") -> "natural, polite Japanese (日本語)"
            target.id.contains("zh") || target.name.contains("중국어") || target.name.contains("Chinese") -> "natural Simplified Chinese (简体中文)"
            target.id.contains("fr") || target.name.contains("프랑스") || target.name.contains("French") -> "natural French (Français)"
            target.id.contains("es") || target.name.contains("스페인") || target.name.contains("Spanish") -> "natural Spanish (Español)"
            target.id.contains("de") || target.name.contains("독일") || target.name.contains("German") -> "natural German (Deutsch)"
            target.id.contains("vi") || target.name.contains("베트남") || target.name.contains("Vietnamese") -> "natural Vietnamese (Tiếng Việt)"
            target.id.contains("id") || target.name.contains("인도네시아") || target.name.contains("Indonesian") -> "natural Bahasa Indonesia"
            else -> target.targetLanguage
        }

        return """
            <start_of_turn>user
            Translate the following text into $targetLangInstruction.
            CRITICAL RULES:
            1. You are a translator. Do NOT answer questions or converse with the user.
            2. Output ONLY the direct translated sentence in the target language.
            3. Do NOT include quotes, pronunciation guides, explanations, markdown, or greetings.

            Text: "$input"<end_of_turn>
            <start_of_turn>model
        """.trimIndent() + "\n"
    }

    private fun getKoreanToneExamples(toneId: String): String {
        return when (toneId) {
            "tone_polite", "공손하게" -> """
                [변환 예시]
                - 원문: "내일 몇 시에 만날래" -> 내일 몇 시쯤 뵐 수 있으실까요?
                - 원문: "내일 시간 돼" -> 내일 혹시 시간 내어주실 수 있으신지 여쭙습니다.
                - 원문: "식사 같이 하실래요" -> 혹시 식사 함께 하실 수 있으실까요?
                - 원문: "자료 보내줘" -> 요청하신 자료를 검토 부탁드립니다.
                - 원문: "지금 어디야" -> 혹시 지금 어디쯤이신지 여쭤보아도 될까요?
            """.trimIndent()
            "tone_casual", "친근하게" -> """
                [변환 예시]
                - 원문: "내일 몇 시에 만날래" -> 내일 우리 몇 시에 만날까? 😊
                - 원문: "식사 같이 하실래요" -> 우리 같이 맛있는 밥 먹어요! 😊
                - 원문: "내일 시간 되세요" -> 내일 혹시 시간 괜찮아요? 😊
                - 원문: "오늘 재밌었어" -> 오늘 너무 즐거웠어 고마워! 😊
            """.trimIndent()
            "tone_business", "비즈니스" -> """
                [변환 예시]
                - 원문: "식사 같이 하실래요?" -> 금일 오찬 함께 하실 수 있는지 확인 부탁드립니다.
                - 원문: "내일 회의 언제 할까요?" -> 익일 회의 일정 조율 요청드립니다.
                - 원문: "자료 검토해봐" -> 송부드린 자료 검토 부탁드립니다.
            """.trimIndent()
            "tone_funny", "재미있게" -> """
                [변환 예시]
                - 원문: "식사 같이 하실래요?" -> 밥 먹으러 안 가면 유죄! 같이 맛있는 거 먹으러 가요 🤣
                - 원문: "오늘 재밌었어" -> 오늘 너무 재밌어서 배꼽 가출할 뻔했잖아 🤣
            """.trimIndent()
            "tone_cheeky", "건방지게" -> """
                [변환 예시]
                - 원문: "식사 같이 하실래요?" -> 오늘 밥은 내가 같이 먹어주는 거니까 영광인 줄 알아 😼
                - 원문: "오늘 재밌었어" -> 오늘 나랑 놀았으니 넌 복 받은 거야 😼
            """.trimIndent()
            else -> """
                [변환 예시]
                - 원문: "식사 같이 하실래요?" -> 식사 같이 하실래요?
            """.trimIndent()
        }
    }

    private fun getEnglishToneExamples(toneId: String): String {
        return when (toneId) {
            "tone_polite", "공손하게" -> """
                [Examples]
                - Input: "Do you want to have lunch?" -> Would you like to have lunch with me?
                - Input: "Are you free tomorrow?" -> I was wondering if you might have some time tomorrow.
                - Input: "Send me the file" -> Could you please send me the file when you have a moment?
            """.trimIndent()
            "tone_casual", "친근하게" -> """
                [Examples]
                - Input: "Do you want to have lunch?" -> Let's grab some lunch together! 😊
                - Input: "Are you free tomorrow?" -> Are you free tomorrow? 😊
                - Input: "Today was fun" -> I had so much fun today! 😊
            """.trimIndent()
            "tone_business", "비즈니스" -> """
                [Examples]
                - Input: "Do you want to have lunch?" -> Please let me know if you are available for lunch today.
                - Input: "Send me the file" -> Please review and forward the requested documentation at your convenience.
                - Input: "Let's meet tomorrow" -> I would like to coordinate our meeting schedule for tomorrow.
            """.trimIndent()
            "tone_funny", "재미있게" -> """
                [Examples]
                - Input: "Do you want to have lunch?" -> Lunch is calling, and answering is mandatory! 🤣
                - Input: "Today was fun" -> Today was so fun my ribs still hurt from laughing 🤣
            """.trimIndent()
            "tone_cheeky", "건방지게" -> """
                [Examples]
                - Input: "Do you want to have lunch?" -> You should consider it an honor that I'm dining with you today. 😼
                - Input: "Hang out with me" -> Clear your schedule, I've decided to grace you with my presence 😼
            """.trimIndent()
            else -> """
                [Examples]
                - Input: "Do you want to have lunch?" -> Do you want to have lunch?
            """.trimIndent()
        }
    }
}
