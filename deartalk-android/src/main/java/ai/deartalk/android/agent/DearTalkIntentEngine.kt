package ai.deartalk.android.agent

import android.content.Context
import android.util.Log
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
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
import java.util.Locale

sealed interface IntentResult {
    data class Success(val text: String, val message: String = "") : IntentResult
    data class Error(val fallbackText: String, val error: String) : IntentResult
}

/**
 * 100% 온디바이스 초경량 순수 신경망 추론 엔진 (LiteRT-LM)
 * - 입력 언어(한국어, 영어 등)를 자동으로 감지하여 원문 언어를 100% 보존하며 톤 변환/문장 교정 수행
 */
class DearTalkIntentEngine(
    private val context: Context?
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
         * 입력 텍스트에 한글이 포함되어 있는지 판별
         */
        fun hasKorean(text: String): Boolean {
            return text.any { ch ->
                (ch in '\uAC00'..'\uD7A3') || (ch in '\u1100'..'\u11FF') || (ch in '\u3130'..'\u318F')
            }
        }

        /**
         * 입력 텍스트가 순수 영문 위주인지 판별
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
        if (sharedLoaded && sharedLiteRtEngine != null) {
            _isModelLoadedFlow.value = true
            return
        }

        val qwenManager = ai.deartalk.android.data.ModelLifecycleManager(appContext)
        val qwenPaths = qwenManager.resolveModelPaths()
        val qwenLlmPath = qwenPaths[ai.deartalk.android.data.ModelLifecycleManager.KEY_LLM]

        val candidatePaths = mutableListOf<String>()

        // 🌟 1순위: 다운로드/설치된 Qwen 고성능 1.7B LLM 모델 우선 바인딩
        if (!qwenLlmPath.isNullOrBlank()) {
            candidatePaths.add(qwenLlmPath)
        }
        candidatePaths.add(File(appContext.filesDir, "models/qwen/qwen3-1.7b-it.bin").absolutePath)
        candidatePaths.add("/data/local/tmp/llm/qwen3-1.7b-it.bin")

        // 🌟 2순위: 기본 Gemma 2B LiteRT 및 로컬 모델
        candidatePaths.addAll(
            listOf(
                "/data/local/tmp/llm/model.litertlm",
                "/data/local/tmp/llm/gemma-2b-it.litertlm",
                "/data/local/tmp/llm/model.bin",
                "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin",
                "/data/local/tmp/llm/gemma-2b-it-cpu-int4.bin",
                File(appContext.filesDir, "models/model.litertlm").absolutePath,
                File(appContext.filesDir, "models/model.bin").absolutePath
            )
        )

        val llmDir = File("/data/local/tmp/llm/")
        if (llmDir.exists() && llmDir.isDirectory) {
            llmDir.listFiles()?.filter { it.isFile && it.name.endsWith(".litertlm", ignoreCase = true) }?.forEach {
                if (!candidatePaths.contains(it.absolutePath)) {
                    candidatePaths.add(0, it.absolutePath)
                }
            }
        }

        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                val backends: List<Backend> = listOf(Backend.CPU(), Backend.GPU())
                for (backend in backends) {
                    try {
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
                        Log.d(TAG, "✅ [초경량 온디바이스 LLM 로드 완료 ($backend)]: $path (${file.length() / 1024 / 1024}MB)")
                        return
                    } catch (e: Throwable) {
                        Log.e(TAG, "⚠️ 온디바이스 LLM 초기화 실패 ($path, $backend): ${e.message}")
                    }
                }
            }
        }
    }

    fun detectAndInitOnDeviceModel() {
        if (context == null) return
        synchronized(DearTalkIntentEngine::class.java) {
            if (sharedInitJob?.isActive == true) return
            sharedInitJob = initScope.launch {
                initMutex.withLock {
                    if (sharedLoaded && sharedLiteRtEngine != null) {
                        _isModelLoadedFlow.value = true
                        return@withLock
                    }
                    sharedLoaded = false
                    _isModelLoadedFlow.value = false
                    initOnDeviceModel(context.applicationContext)
                }
            }
        }
    }

    /**
     * 🔄 모델 핫 리로드: Qwen 패키지 다운로드 완료 또는 삭제 시 새 모델 경로 즉시 재바인딩
     */
    fun reloadModel() {
        if (context == null) return
        synchronized(DearTalkIntentEngine::class.java) {
            sharedInitJob = initScope.launch {
                initMutex.withLock {
                    try {
                        sharedLiteRtEngine = null
                    } catch (_: Throwable) {}
                    sharedLoaded = false
                    _isModelLoadedFlow.value = false
                    initOnDeviceModel(context.applicationContext)
                }
            }
        }
    }

    /**
     * 1. 실시간 음성 문장 교정 (오탈자 수정, 물음표/느낌표/마침표 문맥 부착)
     * - 입력 텍스트의 언어(한국어 / 영어 / 다국어)를 감지하여 원문 언어를 100% 유지
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

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try {
                sharedInitJob?.join()
            } catch (_: Throwable) {}
        }

        if (isModelLoaded) {
            try {
                val isInputKorean = hasKorean(trimmed)
                val isInputEnglish = isEnglish(trimmed)
                val isIndonesianLocale = context?.let {
                    val lang = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(it).language.lowercase()
                    lang == "id" || lang == "in"
                } ?: false

                val prompt = if (isInputKorean) {
                    "<start_of_turn>user\n" +
                            "당신은 모바일 키보드의 '실시간 음성 문장 교정 및 다듬기 AI'입니다.\n" +
                            "⚠️ 중요: 당신은 챗봇이 아니므로 절대로 사용자의 말에 대답하거나 대화를 나누지 마세요!\n" +
                            "당신의 유일한 임무는 사용자가 말한 거칠거나 불완전한 음성 내용을, 상대방에게 즉시 보낼 수 있도록 맞춤법/오탈자를 완벽히 교정하고, 문맥에 부합하는 올바른 문장 부호('?', '!', '.')를 반드시 완성하여 자연스럽고 정돈된 문장으로 세련되게 다듬어 주는 것입니다.\n\n" +
                            "[변환 예시]\n" +
                            "- \"금요일 제외한 매일 11시에서 11시30분까지는 Privacy 스크럼이니 절대로 잊지마\" -> 금요일을 제외한 매일 11시부터 11시 30분까지는 Privacy 스크럼 일정이니 꼭 기억해 주세요.\n" +
                            "- \"지금 가고 있는데 차 막혀서 늦을듯 미안\" -> 지금 이동 중인데 도로가 정체되어 조금 늦을 것 같습니다.\n" +
                            "- \"자료 보냈으니 확인해보고 알려줘\" -> 송부드린 자료 확인 후 회신 부탁드립니다.\n" +
                            "- \"내일 몇 시에 만날까\" -> 내일 몇 시에 만날까요?\n" +
                            "- \"혹시 언제 시간 괜찮으세요\" -> 혹시 언제 시간 괜찮으신가요?\n" +
                            "- \"이 방향 어떻게 생각해\" -> 이 방향에 대해 어떻게 생각하시나요?\n" +
                            "- \"오늘 정말 고마웠어\" -> 오늘 정말 감사했습니다!\n\n" +
                            "[출력 규칙]\n" +
                            "1. 질문 문장이더라도 절대 답하지 말고, 질문 문장 자체를 정돈하여 출력하세요.\n" +
                            "2. 의문문(질문/확인)에는 반드시 물음표('?'), 감사/감탄에는 느낌표('!'), 서술문에는 마침표('.') 등 문맥에 부합하는 올바른 문장 부호를 부착하세요.\n" +
                            "3. 외래어, 고유명사 및 보편적 약어는 문맥에 부합하는 표준 표기법을 준수하여 정돈하세요.\n" +
                            "4. 설명, 따옴표, 마크다운 기호 없이 오직 상대방에게 전송할 한 줄의 다듬어진 한국어 문장만 평문으로 출력하세요.\n\n" +
                            "음성 원문: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else if (isIndonesianLocale) {
                    "<start_of_turn>user\n" +
                            "Anda adalah AI perapih dan pengoreksi tata bahasa pesan teks suara untuk papan ketik ponsel.\n" +
                            "⚠️ PENTING: Anda BUKAN chatbot. JANGAN menjawab pertanyaan atau mengobrol dengan pengguna!\n" +
                            "Tugas tunggal Anda adalah memperbaiki kesalahan ketik/tata bahasa, memberikan tanda baca yang tepat ('?', '!', '.', ','), dan merapikan kalimat masukan menjadi bahasa Indonesia yang baik, alami, dan sopan agar siap dikirim sebagai pesan.\n\n" +
                            "[Contoh]\n" +
                            "- \"saya lagi di jalan tapi macet bgt mungkin telat 15 menit maaf ya\" -> Saya sedang di jalan tetapi lalu lintas sangat macet, mungkin terlambat 15 menit. Maaf ya.\n" +
                            "- \"proposal yg udah diperbarui udh dikirim tolong dicek ya\" -> Proposal yang sudah diperbarui sudah saya kirim, tolong dicek ya.\n" +
                            "- \"besok makan siang jam berapa enaknya tolong kabari\" -> Besok makan siang jam berapa enaknya? Tolong kabari ya.\n" +
                            "- \"terima kasih banyak atas bantuannya hari ini\" -> Terima kasih banyak atas bantuannya hari ini!\n\n" +
                            "[Aturan Output]\n" +
                            "1. Jika masukan berupa pertanyaan, JANGAN jawab pertanyaan tersebut, cukup rapikan kalimat pertanyaannya.\n" +
                            "2. Keluarkan HANYA satu baris kalimat hasil perapian dalam bahasa Indonesia tanpa tanda kutip, markdown, atau salam pembuka.\n\n" +
                            "Masukan: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else if (isInputEnglish) {
                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's 'speech-to-text grammar & punctuation corrector'.\n" +
                            "⚠️ CRITICAL: You are NOT a chatbot. Do NOT answer questions or converse with the user!\n" +
                            "⚠️ ABSOLUTE RULE: The input is in English. Keep it strictly in ENGLISH. Do NOT translate to Korean or any other language!\n" +
                            "Your ONLY duty is to correct typos, fix grammar, and attach appropriate punctuation marks ('?', '!', '.', ',') in English so the user can send it as a clean message.\n\n" +
                            "[Examples]\n" +
                            "- \"what time should we meet tomorrow\" -> What time should we meet tomorrow?\n" +
                            "- \"i just arrived safely\" -> I just arrived safely.\n" +
                            "- \"thank you so much for your help\" -> Thank you so much for your help!\n" +
                            "- \"where is the meeting room please tell me\" -> Where is the meeting room? Please tell me.\n\n" +
                            "[Output Rules]\n" +
                            "1. If the input is a question, do NOT answer it. Just refine the English question itself.\n" +
                            "2. Output ONLY the refined single-line English text without quotes, markdown, or greetings.\n\n" +
                            "Input: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else {
                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's 'speech-to-text punctuation corrector'.\n" +
                            "⚠️ CRITICAL: Do NOT answer questions. Keep the original language of the input.\n" +
                            "Attach appropriate punctuation marks and output ONLY the single-line refined text.\n\n" +
                            "Input: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                }

                var output = ""
                sharedLiteRtEngine?.let { engine ->
                    val session = engine.createSession()
                    val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
                    try { session.close() } catch (_: Throwable) {}
                    output = cleanLlmOutput(response)
                }

                if (output.isNotBlank()) {
                    try {
                        Log.d(TAG, "✨ [온디바이스 LLM 생성 완료]: '$trimmed' ➔ '$output'")
                    } catch (_: Throwable) {}
                    return@withContext IntentResult.Success(output, UiStrings.aiGenerationComplete)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 온디바이스 LLM 추론 오류: ${e.message}")
            }
        }

        return@withContext IntentResult.Success(trimmed, UiStrings.sttRawResult)
    }

    /**
     * 2. 4대 톤앤매너 변환 (기본다듬기, 공손하게, 친근하게, 비즈니스 등)
     * - 원문이 영문이면 영어 톤으로 변환, 한글이면 한글 톤으로 변환 (언어 보존)
     */
    suspend fun processWithTone(
        voiceInput: String,
        tone: CustomTone,
        currentEditorText: String = "",
        packageName: String = ""
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext IntentResult.Success("")

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        if (isModelLoaded) {
            try {
                val isInputKorean = hasKorean(trimmed)
                val isInputEnglish = isEnglish(trimmed)

                val examples = if (isInputKorean) {
                    when (tone.id) {
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
                        "tone_cheeky", "건방지게", "당당하게" -> """
                            [변환 예시]
                            - 원문: "식사 같이 하실래요?" -> 오늘 밥은 내가 같이 먹어주는 거니까 영광인 줄 알아 😼
                            - 원문: "오늘 재밌었어" -> 오늘 나랑 놀았으니 넌 복 받은 거야 😼
                        """.trimIndent()
                        else -> """
                            [변환 예시]
                            - 원문: "식사 같이 하실래요?" -> 식사 같이 하실래요?
                        """.trimIndent()
                    }
                } else {
                    when (tone.id) {
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

                val prompt = if (isInputKorean) {
                    "<start_of_turn>user\n" +
                            "당신은 모바일 키보드의 '텍스트 어조/톤 변환기'입니다.\n" +
                            "⚠️ 중요: 당신은 챗봇이 아니므로 절대로 질문에 대답하거나 대화를 시도하지 마세요!\n" +
                            "화자의 핵심 의도와 내용을 100% 보존하면서, 텍스트의 어조만 '${tone.name}'(${tone.instruction}) 스타일로 다시 작성하세요.\n\n" +
                            "$examples\n\n" +
                            "[출력 규칙]\n" +
                            "1. 원문이 질문이더라도 절대 답하지 말고, 원문 자체를 ${tone.name} 어조로 변환한 한 줄의 문장만 출력하세요.\n" +
                            "2. 원문이 의문문(질문/확인)인 경우 물음표('?')를 반드시 부착하고, 문맥에 부합하는 올바른 문장 부호('?', '!', '.')를 완성하세요.\n" +
                            "3. 외래어, 고유명사 및 보편적 약어는 문맥에 부합하는 표준 표기법을 준수하여 정돈하세요.\n" +
                            "4. 설명, 인사말, 따옴표, 라벨 접두어 없이 오직 변환된 한국어 텍스트만 평문으로 출력하세요.\n\n" +
                            "변환할 원문: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else {
                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's 'tone & style transformer'.\n" +
                            "⚠️ CRITICAL: You are NOT a chatbot. Do NOT answer questions or converse with the user!\n" +
                            "⚠️ ABSOLUTE RULE: Keep the original language (English) of the input text. Do NOT translate it into Korean or other languages!\n" +
                            "Convert the tone of the English text into the target style '${tone.name}' (${tone.instruction}) while preserving the original meaning.\n\n" +
                            "$examples\n\n" +
                            "[Output Rules]\n" +
                            "1. Even if the input is a question, do NOT answer it. Just refine and convert the question itself in English.\n" +
                            "2. Output ONLY the refined English text without explanations, greetings, quotes, or markdown.\n\n" +
                            "Input: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                }

                var output = ""
                sharedLiteRtEngine?.let { engine ->
                    val session = engine.createSession()
                    val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
                    try { session.close() } catch (_: Throwable) {}
                    output = cleanLlmOutput(response)
                }

                if (output.isNotBlank()) {
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
        target: ai.deartalk.android.data.pref.TranslationTarget,
        currentEditorText: String = "",
        packageName: String = ""
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext IntentResult.Success("")

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        if (isModelLoaded) {
            try {
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

                val prompt = "<start_of_turn>user\n" +
                        "Translate the following text into $targetLangInstruction.\n" +
                        "CRITICAL RULES:\n" +
                        "1. You are a translator. Do NOT answer questions or converse with the user.\n" +
                        "2. Output ONLY the direct translated sentence in the target language.\n" +
                        "3. Do NOT include quotes, pronunciation guides, explanations, markdown, or greetings.\n\n" +
                        "Text: \"$trimmed\"<end_of_turn>\n" +
                        "<start_of_turn>model\n"

                var output = ""
                sharedLiteRtEngine?.let { engine ->
                    val session = engine.createSession()
                    val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
                    try { session.close() } catch (_: Throwable) {}
                    output = cleanLlmOutput(response)
                }

                if (output.isNotBlank()) {
                    try {
                        Log.d(TAG, "✨ [온디바이스 LLM 다국어 번역 완료]: '$trimmed' ➔ '$output' (${target.name})")
                    } catch (_: Throwable) {}
                    return@withContext IntentResult.Success(output, UiStrings.translationComplete(target.flag, target.name))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 다국어 번역 실행 오류: ${e.message}")
            }
        }

        return@withContext process(voiceInput, currentEditorText, packageName)
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

        text = text
            .replace(Regex("""^(최종\s*문장|수정된\s*문장|다듬은\s*문장|변환된\s*문장|결과|답변|제안|답|문장|Output|Result|Sentence|model|assistant|AI|Translation|Translated Text|Japanese|English|Chinese|Spanish|French|German|Indonesian|Vietnamese|Thai|Tagalog|Malay|日本語|英語|中国語)\s*[:：]\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .removePrefix(">")
            .removePrefix("-")
            .removePrefix("*")
            .removeSurrounding("\"")
            .removeSurrounding("`")
            .removeSurrounding("```")
            .trim()

        return text.trim()
    }

    suspend fun processIntent(voiceInput: String): IntentResult = process(voiceInput)

    suspend fun applyTone(voiceInput: String, toneName: String): IntentResult {
        val tone = CustomTone(
            id = toneName,
            name = toneName,
            instruction = toneName,
            icon = "✨"
        )
        return processWithTone(voiceInput, tone)
    }

    /**
     * 🌐 12개 글로벌/동남아 다국어 특화 실시간 동적 통역 엔진 (Zero Hardcoding)
     */
    suspend fun translate(
        voiceInput: String,
        targetLangCode: String,
        sourceLangCode: String = "KO",
        tone: String? = null
    ): String = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext ""

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        val targetLangName = when (targetLangCode.uppercase()) {
            "EN" -> "English"
            "ES" -> "Spanish (Español)"
            "FR" -> "French (Français)"
            "DE" -> "German (Deutsch)"
            "JA" -> "Japanese (日本語)"
            "ZH" -> "Simplified Chinese (简体中文)"
            "KO" -> "Korean (한국어)"
            "ID" -> "Indonesian (Bahasa Indonesia)"
            "VI" -> "Vietnamese (Tiếng Việt)"
            "TL", "FIL" -> "Filipino/Tagalog (Wikang Filipino)"
            "TH" -> "Thai (ภาษาไทย)"
            "MS" -> "Malay (Bahasa Melayu)"
            else -> targetLangCode
        }

        val sourceLangName = when (sourceLangCode.uppercase()) {
            "EN" -> "English"
            "ES" -> "Spanish"
            "FR" -> "French"
            "DE" -> "German"
            "JA" -> "Japanese"
            "ZH" -> "Chinese"
            "KO" -> "Korean"
            "ID" -> "Indonesian"
            "VI" -> "Vietnamese"
            "TL", "FIL" -> "Filipino"
            "TH" -> "Thai"
            "MS" -> "Malay"
            else -> sourceLangCode
        }

        val toneInstruction = if (!tone.isNullOrBlank()) " Adapt the translated sentence to have a '$tone' tone." else ""

        if (isModelLoaded) {
            try {
                val prompt = "<start_of_turn>user\n" +
                        "You are an expert real-time simultaneous interpreter.\n" +
                        "Translate the spoken speech from $sourceLangName into natural, accurate $targetLangName.$toneInstruction\n" +
                        "CRITICAL INSTRUCTIONS:\n" +
                        "1. Output ONLY the single-line translated sentence in $targetLangName.\n" +
                        "2. Do NOT add notes, explanations, romanization, conversational fillers, or quotes.\n\n" +
                        "Input text: \"$trimmed\"<end_of_turn>\n" +
                        "<start_of_turn>model\n"

                var output = ""
                sharedLiteRtEngine?.let { engine ->
                    val session = engine.createSession()
                    val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
                    try { session.close() } catch (_: Throwable) {}
                    output = cleanLlmOutput(response)
                }

                if (output.isNotBlank()) {
                    Log.d(TAG, "✨ [온디바이스 번역 성공] '$trimmed' ($sourceLangName) ➔ '$output' ($targetLangName)")
                    return@withContext output
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ translate 오류: ${e.message}")
            }
        }

        return@withContext trimmed
    }

    suspend fun processCustomPrompt(promptText: String): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }
        if (isModelLoaded) {
            try {
                val formatted = "<start_of_turn>user\n$promptText<end_of_turn>\n<start_of_turn>model\n"
                var output = ""
                sharedLiteRtEngine?.let { engine ->
                    val session = engine.createSession()
                    val response = session.generateContent(listOf(InputData.Text(formatted))).trim()
                    try { session.close() } catch (_: Throwable) {}
                    output = cleanLlmOutput(response)
                }
                if (output.isNotBlank()) return@withContext output
            } catch (e: Throwable) {
                Log.e(TAG, "❌ processCustomPrompt 오류: ${e.message}")
            }
        }
        return@withContext ""
    }
}
