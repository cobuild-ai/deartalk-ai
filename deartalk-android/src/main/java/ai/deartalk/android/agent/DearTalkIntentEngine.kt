package ai.deartalk.android.agent

import android.content.Context
import android.util.Log
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.data.repository.ContextRepository
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * - 복잡한 레거시/거대 모델 분기를 모두 걷어내고, 키보드에 최적화된 초경량 온디바이스 신경망 추론 전담
 * - 실시간 텍스트 교정(오탈자/문장부호) 및 4대 톤앤매너 변환
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

        @Volatile
        var sharedLoaded: Boolean = false
            private set
    }

    val isModelLoaded: Boolean
        get() = sharedLoaded

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
        val candidatePaths = mutableListOf(
            "/data/local/tmp/llm/model.litertlm",
            "/data/local/tmp/llm/gemma-2b-it.litertlm",
            "/data/local/tmp/llm/model.bin",
            "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin",
            "/data/local/tmp/llm/gemma-2b-it-cpu-int4.bin",
            File(appContext.filesDir, "models/model.litertlm").absolutePath,
            File(appContext.filesDir, "models/model.bin").absolutePath
        )

        // /data/local/tmp/llm/ 디렉토리 내의 모든 .litertlm 파일 자동 탐색
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
                        Log.d(TAG, "✅ [초경량 온디바이스 LLM 로드 완료 ($backend)]: $path (${file.length() / 1024 / 1024}MB)")
                        return
                    } catch (e: Throwable) {
                        Log.e(TAG, "⚠️ 온디바이스 LLM 초기화 실패 ($path, $backend): ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 1. 실시간 음성 문장 교정 (오탈자 수정, 물음표/느낌표/마침표 문맥 부착)
     * - 한국어 및 다국어(영어, 일본어, 중국어 등) 시스템 언어에 맞춘 유연한 온디바이스 프롬프트
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
                val targetLocale = context?.let { DearTalkSettings.getEffectiveLocale(it) } ?: Locale.getDefault()
                val isKo = targetLocale.language == "ko"
                val langName = targetLocale.getDisplayLanguage(targetLocale).ifBlank { targetLocale.displayLanguage }

                val prompt = if (isKo) {
                    "<start_of_turn>user\n" +
                            "당신은 모바일 키보드의 '실시간 음성 문장 교정기'입니다.\n" +
                            "⚠️ 중요: 당신은 챗봇이 아니므로 절대로 사용자의 말에 대답하거나 답변하지 마세요!\n" +
                            "당신의 유일한 임무는 사용자가 말한 음성 내용을 상대방에게 보낼 수 있도록, 오탈자를 교정하고 문맥에 맞는 문장부호(의문문은 '?', 감탄은 '!', 마침표, 쉼표)를 자연스럽게 부착하여 원문 메시지를 완성하는 것입니다.\n\n" +
                            "[변환 예시]\n" +
                            "- \"내일 몇 시에 만날까\" -> 내일 몇 시에 만날까?\n" +
                            "- \"지금 출발해도 괜찮아\" -> 지금 출발해도 괜찮아?\n" +
                            "- \"오늘 정말 고마웠어\" -> 오늘 정말 고마웠어!\n" +
                            "- \"확인 후 연락 드릴게요\" -> 확인 후 연락드리겠습니다.\n\n" +
                            "[출력 규칙]\n" +
                            "1. 사용자가 질문을 했더라도 그 질문에 대답하지 말고, 사용자가 한 질문 문장 자체를 다듬어 출력하세요.\n" +
                            "2. 설명, 따옴표, 마크다운 기호 없이 오직 상대방에게 전송할 한 줄의 $langName 문장만 평문으로 출력하세요.\n\n" +
                            "음성 원문: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else {
                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's 'speech-to-text grammar & punctuation corrector'.\n" +
                            "⚠️ CRITICAL: You are NOT a chatbot. Do NOT answer questions or converse with the user!\n" +
                            "Your ONLY duty is to correct typos, fix grammar, and attach appropriate punctuation marks ('?', '!', '.', ',') in $langName so the user can send it as a clean message.\n\n" +
                            "[Examples]\n" +
                            "- \"what time should we meet tomorrow\" -> What time should we meet tomorrow?\n" +
                            "- \"i just arrived safely\" -> I just arrived safely.\n" +
                            "- \"thank you so much for your help\" -> Thank you so much for your help!\n\n" +
                            "[Output Rules]\n" +
                            "1. If the input is a question, do NOT answer it. Just refine the question itself.\n" +
                            "2. Output ONLY the refined single-line $langName text without quotes, markdown, or greetings.\n\n" +
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
                    contextRepository?.recordInteraction(packageName, trimmed, output, "ON_DEVICE_LLM")
                    return@withContext IntentResult.Success(output, UiStrings.aiGenerationComplete)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 온디바이스 LLM 추론 오류: ${e.message}")
            }
        }

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

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        if (isModelLoaded) {
            try {
                val examples = when (tone.id) {
                    "tone_polite", "공손하게" -> """
                        [변환 예시]
                        - 원문: "식사 같이 하실래요?" -> 혹시 식사 함께 하실 수 있으실까요?
                        - 원문: "내일 시간 되세요?" -> 내일 시간 내어주실 수 있으신지 여쭙습니다.
                        - 원문: "자료 보내줘" -> 요청하신 자료를 송부해 드립니다.
                    """.trimIndent()
                    "tone_casual", "친근하게" -> """
                        [변환 예시]
                        - 원문: "식사 같이 하실래요?" -> 우리 같이 식사해요! 😊
                        - 원문: "내일 시간 되세요?" -> 내일 혹시 시간 괜찮아요? 😊
                        - 원문: "오늘 재밌었어" -> 오늘 너무 즐거웠어! 😊
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
                        - 원문: "내일 시간 되세요?" -> 내일 저랑 놀아줄 귀한 시간 1초만 기부해 주시죠! 🤣
                        - 원문: "오늘 재밌었어" -> 오늘 너무 재밌어서 배꼽 가출할 뻔했잖아 🤣
                    """.trimIndent()
                    "tone_cheeky", "건방지게" -> """
                        [변환 예시]
                        - 원문: "식사 같이 하실래요?" -> 오늘 밥은 내가 같이 먹어주는 거니까 영광인 줄 알아 😼
                        - 원문: "내일 시간 되세요?" -> 내일 시간 비워둬, 내가 만나줄게 😼
                        - 원문: "오늘 재밌었어" -> 오늘 나랑 놀았으니 넌 복 받은 거야 😼
                    """.trimIndent()
                    else -> """
                        [변환 예시]
                        - 원문: "식사 같이 하실래요?" -> 식사 같이 하실래요?
                        - 원문: "내일 몇 시에 만날까?" -> 내일 몇 시에 만날까요?
                    """.trimIndent()
                }

                val prompt = "<start_of_turn>user\n" +
                        "당신은 모바일 키보드의 '텍스트 어조/톤 변환기'입니다.\n" +
                        "⚠️ 중요: 당신은 챗봇이 아니므로 절대로 질문에 대답하거나 대화를 시도하지 마세요!\n" +
                        "화자의 핵심 의도와 내용을 100% 보존하면서, 텍스트의 어조만 '${tone.name}'(${tone.instruction}) 스타일로 다시 작성하세요.\n\n" +
                        "$examples\n\n" +
                        "[출력 규칙]\n" +
                        "1. 원문이 질문이더라도 절대 답하지 말고, 원문 자체를 ${tone.name} 어조로 변환한 한 줄의 문장만 출력하세요.\n" +
                        "2. 설명, 인사말, 따옴표, 라벨 접두어 없이 오직 변환된 텍스트만 평문으로 출력하세요.\n\n" +
                        "변환할 원문: \"$trimmed\"<end_of_turn>\n" +
                        "<start_of_turn>model\n"

                var output = ""
                sharedLiteRtEngine?.let { engine ->
                    val session = engine.createSession()
                    val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
                    try { session.close() } catch (_: Throwable) {}
                    output = cleanLlmOutput(response)
                }

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
     * 3. 실시간 온디바이스 다국어 번역 (한국어 -> 영어, 일본어, 중국어, 인도네시아어, 스페인어, 프랑스어, 독일어, 베트남어 등)
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
                    contextRepository?.recordInteraction(packageName, trimmed, output, "TRANSLATION_${target.id}")
                    return@withContext IntentResult.Success(output, UiStrings.translationComplete(target.flag, target.name))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 다국어 번역 실행 오류: ${e.message}")
            }
        }

        return@withContext process(voiceInput, currentEditorText, packageName)
    }

    private fun cleanLlmOutput(raw: String): String {
        var text = raw
            // 1. LLM 특수 제어 토큰 및 턴 태그 제거
            .replace(Regex("""<(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)[^>]*>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)>""", RegexOption.IGNORE_CASE), "")
            // 2. 잔여 HTML/XML 태그 제거
            .replace(Regex("""</?[a-zA-Z0-9_-]+(\s+[^>]*)?>"""), "")
            // 3. 인위적 라벨 접두어 제거 ("최종 문장:", "답변:", "model:", "AI:" 등)
            .replace(Regex("""^(최종\s*문장|결과|답변|model|assistant|AI|Translation|Translated Text)\s*:\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
            // 4. 마크다운 인용 기호, 불릿, 따옴표 제거
            .removePrefix(">")
            .removePrefix("-")
            .removePrefix("*")
            .removeSurrounding("\"")
            .removeSurrounding("`")
            .removeSurrounding("```")
            .trim()

        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                text = lines.first()
            }
        }
        return text.trim()
    }
}
