package ai.deartalk.android.agent

import android.content.Context
import android.util.Log
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.agent.language.LanguageProfileRegistry
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
    data class Success(
        val text: String,
        val message: String = "",
        val detectedIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.STATEMENT
    ) : IntentResult
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
         * 입력 텍스트에 한글이 포함되어 있는지 판별 (LanguageLocaleHelper 단일 소스 위임)
         */
        fun hasKorean(text: String): Boolean = ai.deartalk.android.util.LanguageLocaleHelper.hasKorean(text)

        /**
         * 입력 텍스트가 순수 영문 위주인지 판별 (LanguageLocaleHelper 단일 소스 위임)
         */
        fun isEnglish(text: String): Boolean = ai.deartalk.android.util.LanguageLocaleHelper.isEnglish(text)

        /**
         * 🎯 SLM 출력 텍스트에서 [INTENT: ...] 메타 태그를 파싱하고 본문 텍스트를 분리 정제합니다.
         */
        fun parseIntentTagAndClean(
            rawOutput: String,
            fallbackIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        ): Pair<ai.deartalk.android.live.data.SpeechIntent, String> {
            val intentTagRegex = Regex("""\[INTENT:\s*(STATEMENT|QUESTION|REQUEST|CONFIRM)\]""", RegexOption.IGNORE_CASE)
            val match = intentTagRegex.find(rawOutput)
            val parsedIntent = if (match != null) {
                val tagStr = match.groupValues[1].uppercase()
                try {
                    ai.deartalk.android.live.data.SpeechIntent.valueOf(tagStr)
                } catch (_: Throwable) {
                    fallbackIntent
                }
            } else {
                fallbackIntent
            }

            val cleaned = rawOutput.replace(intentTagRegex, "").trim()
            return Pair(parsedIntent, cleaned)
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

        // 🌟 1순위: 다운로드/설치된 Qwen 0.5B 초경량 / 1.7B LLM 모델 우선 바인딩
        if (!qwenLlmPath.isNullOrBlank()) {
            candidatePaths.add(qwenLlmPath)
        }
        candidatePaths.add(File(appContext.filesDir, "models/qwen/qwen2.5-0.5b-it.bin").absolutePath)
        candidatePaths.add(File(appContext.filesDir, "models/qwen/qwen-0.5b-it.bin").absolutePath)
        candidatePaths.add("/data/local/tmp/llm/qwen2.5-0.5b-it.bin")
        candidatePaths.add("/data/local/tmp/llm/qwen-0.5b-it.bin")
        candidatePaths.add("/data/local/tmp/llm/qwen2.5-0.5b-it.litertlm")
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
     * 🛡️ 단일 인스턴스/세션 안전 추론 실행기
     */
    private suspend fun executeInference(prompt: String): String? = withContext(Dispatchers.IO) {
        sharedLiteRtEngine?.let { engine ->
            val session = engine.createSession()
            try {
                val response = session.generateContent(listOf(InputData.Text(prompt))).trim()
                val cleaned = cleanLlmOutput(response)
                cleaned.takeIf { it.isNotBlank() }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ [온디바이스 LLM 추론 오류]: ${e.message}")
                null
            } finally {
                try {
                    session.close()
                } catch (e: Throwable) {
                    Log.w(TAG, "⚠️ [LiteRT 세션 종료 예외]: ${e.message}")
                }
            }
        }
    }

    /**
     * 1. 실시간 음성 문장 교정 (오탈자 수정, 물음표/느낌표/마침표 문맥 부착)
     * - 말끝 피치 억양(음의 높낮이 상승) 및 문맥 분석을 기반으로 의문문('?')을 자동 판별하여 완성
     */
    suspend fun process(
        voiceInput: String,
        currentEditorText: String = "",
        packageName: String = "",
        speechIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.AUTO
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

        val isInputKorean = hasKorean(trimmed)
        val isIndonesianLocale = context?.let {
            val lang = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(it).language.lowercase()
            lang == "id" || lang == "in"
        } ?: false
        val detectedLangCode = if (isInputKorean) "KO" else if (isIndonesianLocale) "ID" else "EN"
        val isExplicitQuestion = ai.deartalk.android.stt.IntonationAnalyzer.isLikelyQuestion(
            text = trimmed,
            languageCode = detectedLangCode
        )

        if (isModelLoaded) {
            try {
                val isInputEnglish = isEnglish(trimmed)

                // 🌟 앱별 격리 대화 맥락 캐시 조회
                val priorContext = if (packageName.isNotBlank()) {
                    AppScopedUtteranceCache.shared.getRecentContext(packageName)
                } else emptyList()

                val contextBlockKorean = if (priorContext.isNotEmpty()) {
                    "[직전 대화 맥락 (교정 대상 아님, 대명사/주어/동음이의어 참고용)]:\n" +
                            priorContext.joinToString("\n") { "- $it" } + "\n\n"
                } else ""

                val contextBlockIndonesian = if (priorContext.isNotEmpty()) {
                    "[Konteks Percakapan Sebelumnya (Hanya referensi subjek/kata ganti, JANGAN dijawab)]:\n" +
                            priorContext.joinToString("\n") { "- $it" } + "\n\n"
                } else ""

                val contextBlockEnglish = if (priorContext.isNotEmpty()) {
                    "[Prior Conversation Context (For pronoun/subject resolution only, do NOT answer)]:\n" +
                            priorContext.joinToString("\n") { "- $it" } + "\n\n"
                } else ""

                val prompt = if (isInputKorean) {
                    val intentRuleSection = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        "[화행 자동 분류 및 출력 형식]\n" +
                        "1. 문맥과 의도를 분석하여 화행을 [STATEMENT(설명), QUESTION(질문), REQUEST(부탁/요청), CONFIRM(확인/되묻기)] 중 하나로 분류하세요.\n" +
                        "2. 출력 첫 줄에 반드시 `[INTENT: 분류된화행]` 태그를 출력하고, 다음 줄에 교정된 문장만 출력하세요.\n" +
                        "예:\n" +
                        "[INTENT: QUESTION]\n" +
                        "오늘은 며칠이야?\n\n"
                    } else {
                        "[지정 화행 및 출력 형식]\n" +
                        "1. 지정된 화행: ${speechIntent.name}\n" +
                        "2. 출력 첫 줄에 반드시 `[INTENT: ${speechIntent.name}]` 태그를 출력하고, 다음 줄에 교정된 문장만 출력하세요.\n\n"
                    }

                    "<start_of_turn>user\n" +
                            "당신은 모바일 키보드의 '실시간 음성 문장 교정 AI'입니다.\n" +
                            "⚠️ 3대 불변 원칙:\n" +
                            "1. [원형 보존]: 사용자의 어조와 말투(반말은 반말로, 존댓말은 존댓말로)를 100% 유지하세요. 원문에 없는 새로운 완곡어/질문사를 덧붙이거나 문장을 다른 의미로 치환하지 마세요.\n" +
                            "2. [최소 교정]: 오직 오탈자, 맞춤법, 띄어쓰기, 문맥에 맞는 문장부호('.', '?', '!')만 교정하세요.\n" +
                            "3. [문장부호 판별]: 명백한 질문/의문사가 있는 경우만 물음표('?')를 붙이고, 의견/추측(~것 같아, ~듯), 허용/단정(~돼), 서술문은 반드시 평서문('.')으로 마침표를 찍으세요.\n\n" +
                            (if (currentEditorText.isNotBlank()) "[입력창 이전 맥락]: $currentEditorText\n\n" else "") +
                            contextBlockKorean +
                            intentRuleSection +
                            "[교정 예시]\n" +
                            "- \"너도 괜찬을 것 같아\" -> 너도 괜찮을 것 같아.\n" +
                            "- \"넌 몰라도 돼\" -> 넌 몰라도 돼.\n" +
                            "- \"나 지금 밥 머것어\" -> 나 지금 밥 먹었어.\n" +
                            "- \"너 지금 밥 머것어\" -> 너 지금 밥 먹었어?\n" +
                            "- \"오늘 날씨 진짜 조타\" -> 오늘 날씨 진짜 좋다!\n" +
                            "- \"지금 어디 가고 계신가요\" -> 지금 어디 가고 계신가요?\n" +
                            "- \"자료 보냈으니 확인해보고 알려줘\" -> 자료 보냈으니 확인해보고 알려줘.\n" +
                            "- \"금요일 제외한 매일 11시에서 11시30분까지는 Privacy 스크럼이니 잊지마\" -> 금요일 제외한 매일 11시부터 11시 30분까지는 Privacy 스크럼 일정이니 잊지 마.\n\n" +
                            "음성 원문: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else if (isIndonesianLocale) {
                    val intentRuleSection = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        "[Aturan Klasifikasi Niat & Format Output]\n" +
                        "1. Analisis maksud masukan: [INTENT: STATEMENT | QUESTION | REQUEST | CONFIRM].\n" +
                        "2. Baris 1: `[INTENT: NIAT]`, Baris 2: teks hasil koreksi.\n\n"
                    } else {
                        "[Format Output]\n" +
                        "1. Niat: ${speechIntent.name}\n" +
                        "2. Baris 1: `[INTENT: ${speechIntent.name}]`, Baris 2: teks hasil koreksi.\n\n"
                    }

                    "<start_of_turn>user\n" +
                            "Anda adalah AI perapih tata bahasa dan tanda baca pesan suara untuk papan ketik ponsel.\n" +
                            "⚠️ 3 ATURAN UTAMA:\n" +
                            "1. [Pertahankan Bentuk Asli]: Pertahankan nada bahasa pengguna (santai tetap santai, sopan tetap sopan). JANGAN menambahkan kata-kata baru atau mengganti kalimat dengan kata lain.\n" +
                            "2. [Koreksi Minimal]: Hanya perbaiki salah ketik, spasi, dan tambahkan tanda baca yang tepat ('.', '?', '!').\n" +
                            "3. [Tanda Tanya]: Gunakan '?' HANYA jika masukan berupa pertanyaan atau memiliki kata tanya. Kalimat opini/pernyataan harus diakhiri titik ('.').\n\n" +
                            (if (currentEditorText.isNotBlank()) "[Konteks Input]: $currentEditorText\n\n" else "") +
                            contextBlockIndonesian +
                            intentRuleSection +
                            "[Contoh Koreksi]\n" +
                            "- \"kamu juga bakal oke kok\" -> Kamu juga bakal oke kok.\n" +
                            "- \"kamu gak perlu tahu\" -> Kamu gak perlu tahu.\n" +
                            "- \"saya lagi di jalan\" -> Saya lagi di jalan.\n" +
                            "- \"kamu sudah makan siang\" -> Kamu sudah makan siang?\n" +
                            "- \"terima kasih banyak atas bantuannya\" -> Terima kasih banyak atas bantuannya!\n\n" +
                            "Teks Suara: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else if (isInputEnglish) {
                    val intentRuleSection = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        "[Intent Classification & Output Format]\n" +
                        "1. Classify intent: [INTENT: STATEMENT | QUESTION | REQUEST | CONFIRM].\n" +
                        "2. Line 1: `[INTENT: <INTENT>]`, Line 2: corrected text.\n\n"
                    } else {
                        "[Output Format]\n" +
                        "1. Intent: ${speechIntent.name}\n" +
                        "2. Line 1: `[INTENT: ${speechIntent.name}]`, Line 2: corrected text.\n\n"
                    }

                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's speech-to-text sentence refinement & punctuation AI.\n" +
                            "⚠️ 3 CORE RULES:\n" +
                            "1. [Preserve Tone & Register]: Keep the user's exact words and tone (informal remains informal, formal remains formal). Do NOT substitute with other phrases.\n" +
                            "2. [Minimal Edit]: Correct ONLY typos, spelling, spacing, and appropriate punctuation ('.', '?', '!').\n" +
                            "3. [Punctuation Rule]: Apply '?' ONLY if it is an explicit question. Opinions, estimations, and statements must end with a period ('.').\n\n" +
                            (if (currentEditorText.isNotBlank()) "[Editor Context]: $currentEditorText\n\n" else "") +
                            contextBlockEnglish +
                            intentRuleSection +
                            "[Correction Examples]\n" +
                            "- \"i think you will be fine too\" -> I think you will be fine too.\n" +
                            "- \"you dont need to know\" -> You don't need to know.\n" +
                            "- \"i ate lunch already\" -> I ate lunch already.\n" +
                            "- \"did you eat lunch\" -> Did you eat lunch?\n" +
                            "- \"what time are we meeting\" -> What time are we meeting?\n" +
                            "- \"thank you so much for your help\" -> Thank you so much for your help!\n\n" +
                            "Input: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else {
                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's 'speech-to-text punctuation corrector'.\n" +
                            "⚠️ CRITICAL: Do NOT answer questions. Keep the original language of the input.\n" +
                            "Attach appropriate punctuation marks and output ONLY the single-line refined text.\n\n" +
                            contextBlockEnglish +
                            "Input: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                }

                val output = executeInference(prompt)

                if (!output.isNullOrBlank()) {
                    val defaultFallbackIntent = if (isExplicitQuestion) ai.deartalk.android.live.data.SpeechIntent.QUESTION else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
                    val expectedFallback = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else defaultFallbackIntent
                    val (detectedIntent, cleanOutput) = parseIntentTagAndClean(output, expectedFallback)
                    var refined = cleanLlmOutput(cleanOutput, detectedLangCode)
                    if (detectedIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION && !refined.endsWith("?")) {
                        refined = refined.removeSuffix(".").removeSuffix("!").trim() + "?"
                    }
                    try {
                        Log.d(TAG, "✨ [온디바이스 LLM 생성 완료]: '$trimmed' ➔ '$refined' (화행: $detectedIntent)")
                    } catch (_: Throwable) {}
                    if (packageName.isNotBlank()) {
                        AppScopedUtteranceCache.shared.addUtterance(packageName, refined)
                    }
                    return@withContext IntentResult.Success(refined, UiStrings.aiGenerationComplete, detectedIntent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 온디바이스 LLM 추론 오류: ${e.message}")
            }
        }

        // 🌟 LLM 미로드 또는 폴백 시: 의문사/의문어미 판별 기반 폴백
        val fallbackIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            speechIntent
        } else if (isExplicitQuestion) {
            ai.deartalk.android.live.data.SpeechIntent.QUESTION
        } else {
            ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        }

        val fallbackText = if (fallbackIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION && !trimmed.endsWith("?")) {
            trimmed.removeSuffix(".").removeSuffix("!").trim() + "?"
        } else {
            trimmed
        }

        if (packageName.isNotBlank()) {
            AppScopedUtteranceCache.shared.addUtterance(packageName, fallbackText)
        }
        return@withContext IntentResult.Success(fallbackText, UiStrings.sttRawResult, fallbackIntent)
    }

    /**
     * 2. 4대 톤앤매너 변환 (기본다듬기, 공손하게, 친근하게, 비즈니스 등)
     * - 원문이 영문이면 영어 톤으로 변환, 한글이면 한글 톤으로 변환 (언어 보존)
     */
    suspend fun processWithTone(
        voiceInput: String,
        tone: CustomTone,
        currentEditorText: String = "",
        packageName: String = "",
        speechIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.AUTO
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext IntentResult.Success("")

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        if (isModelLoaded) {
            try {
                val isInputKorean = hasKorean(trimmed)
                val isIndonesianLocale = context?.let {
                    val lang = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(it).language.lowercase()
                    lang == "id" || lang == "in"
                } ?: false
                val isInputIndonesian = !isInputKorean && (isIndonesianLocale || ai.deartalk.android.util.LanguageLocaleHelper.detectLanguageCode(trimmed) == "ID")

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
                } else if (isInputIndonesian) {
                    when (tone.id) {
                        "tone_polite", "공손하게", "sopan" -> """
                            [Contoh]
                            - Masukan: "Besok jam berapa ketemu?" -> Besok kira-kira kita bisa bertemu jam berapa ya?
                            - Masukan: "Kirim filenya ya" -> Mohon kirimkan dokumen yang diminta jika ada waktu luang.
                            - Masukan: "Mau makan siang bareng?" -> Apakah berkenan untuk makan siang bersama hari ini?
                        """.trimIndent()
                        "tone_casual", "친근하게", "santai" -> """
                            [Contoh]
                            - Masukan: "Mau makan siang bareng?" -> Yuk makan siang bareng! 😊
                            - Masukan: "Besok ada waktu?" -> Besok kamu senggang nggak? 😊
                            - Masukan: "Hari ini seru banget" -> Hari ini seru banget makasih ya! 😊
                        """.trimIndent()
                        "tone_business", "비즈니스", "formal" -> """
                            [Contoh]
                            - Masukan: "Mau makan siang bareng?" -> Mohon konfirmasi apakah Anda berkenan untuk makan siang bersama hari ini.
                            - Masukan: "Kirim filenya ya" -> Mohon tinjau dan kirimkan dokumen tersebut pada kesempatan pertama.
                            - Masukan: "Besok meeting jam berapa?" -> Mohon koordinasi mengenai jadwal rapat besok.
                        """.trimIndent()
                        "tone_funny", "재미있게", "lucu" -> """
                            [Contoh]
                            - Masukan: "Mau makan siang bareng?" -> Perut udah demo nih, nggak ikut makan siang awas ya! 🤣
                            - Masukan: "Hari ini seru banget" -> Seru banget hari ini, ketawa mulu sampai sakit perut 🤣
                        """.trimIndent()
                        "tone_cheeky", "건방지게", "당당하게", "percaya diri" -> """
                            [Contoh]
                            - Masukan: "Mau makan siang bareng?" -> Makan siang bareng aku itu kesempatan langka lho, bangga dong 😼
                            - Masukan: "Temani aku besok" -> Kosongkan jadwalmu, besok aku izinkan kamu menemani aku 😼
                        """.trimIndent()
                        else -> """
                            [Contoh]
                            - Masukan: "Mau makan siang bareng?" -> Mau makan siang bareng?
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

                val intentDirective = when (speechIntent) {
                    ai.deartalk.android.live.data.SpeechIntent.QUESTION -> "화행 목표: [질문/의문문] 문맥에 맞게 질문 어미로 바꾸고 물음표('?')로 끝내세요."
                    ai.deartalk.android.live.data.SpeechIntent.STATEMENT -> "화행 목표: [설명/평서문] 서술/설명 형태로 바꾸고 마침표('.')로 끝내세요."
                    ai.deartalk.android.live.data.SpeechIntent.REQUEST -> "화행 목표: [부탁/요청] 공손하게 부탁하거나 요청하는 형태로 바꾸세요."
                    ai.deartalk.android.live.data.SpeechIntent.CONFIRM -> "화행 목표: [확인/되묻기] 확인이나 동의를 구하는 형태로 바꾸고 물음표('?')로 끝내세요."
                    ai.deartalk.android.live.data.SpeechIntent.AUTO -> ""
                }

                val intentInstruction = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                    "[화행 자동 분류 및 출력 규칙]\n" +
                    "1. 원문의 문맥에 맞는 화행(STATEMENT, QUESTION, REQUEST, CONFIRM)을 스스로 판단하세요.\n" +
                    "2. 출력 첫 줄에 반드시 `[INTENT: 분류된화행]` 태그를 출력하고, 둘째 줄에 ${tone.name} 어조로 변환된 문장만 출력하세요.\n\n"
                } else {
                    val intentName = speechIntent.name
                    "[지정 화행 지침 및 출력 규칙]\n" +
                    "1. 지정 화행: $intentName ($intentDirective)\n" +
                    "2. 출력 첫 줄에 반드시 `[INTENT: $intentName]` 태그를 출력하고, 둘째 줄에 ${tone.name} 어조로 변환된 문장만 출력하세요.\n\n"
                }

                val prompt = if (isInputKorean) {
                    "<start_of_turn>user\n" +
                            "당신은 모바일 키보드의 '텍스트 어조/톤 변환기'입니다.\n" +
                            "⚠️ 중요: 당신은 챗봇이 아니므로 절대로 질문에 대답하거나 대화를 시도하지 마세요!\n" +
                            "화자의 핵심 의도와 내용을 100% 보존하면서, 텍스트의 어조만 '${tone.name}'(${tone.instruction}) 스타일로 다시 작성하세요.\n" +
                            intentInstruction +
                            "$examples\n\n" +
                            "[출력 규칙]\n" +
                            "1. 원문이 질문이더라도 절대 답하지 말고, 원문 자체를 ${tone.name} 어조로 변환하세요.\n" +
                            "2. 원문이 의문문(질문/확인)인 경우 물음표('?')를 반드시 부착하고, 문맥에 부합하는 올바른 문장 부호('?', '!', '.')를 완성하세요.\n" +
                            "3. 외래어, 고유명사 및 보편적 약어는 문맥에 부합하는 표준 표기법을 준수하여 정돈하세요.\n" +
                            "4. 설명, 인사말, 따옴표, 라벨 접두어 없이 오직 첫 줄에 [INTENT: ...] 태그, 둘째 줄에 변환된 한국어 텍스트만 출력하세요.\n\n" +
                            "변환할 원문: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else if (isInputIndonesian) {
                    val intentInstructionId = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        "[Aturan Klasifikasi Niat & Format Output]\n" +
                        "1. Analisis maksud masukan: [INTENT: STATEMENT | QUESTION | REQUEST | CONFIRM].\n" +
                        "2. Baris 1: `[INTENT: NIAT]`, Baris 2: teks hasil pengubahan gaya.\n\n"
                    } else {
                        "[Format Output]\n" +
                        "1. Niat: ${speechIntent.name}\n" +
                        "2. Baris 1: `[INTENT: ${speechIntent.name}]`, Baris 2: teks hasil pengubahan gaya.\n\n"
                    }

                    "<start_of_turn>user\n" +
                            "Anda adalah 'pengubah nada & gaya pesan teks' untuk papan ketik ponsel.\n" +
                            "⚠️ PENTING: Anda BUKAN chatbot. JANGAN menjawab pertanyaan atau mengobrol dengan pengguna!\n" +
                            "⚠️ ATURAN MUTLAK: Tetap gunakan bahasa Indonesia. JANGAN menerjemahkannya ke bahasa lain!\n" +
                            "Ubah nada pesan dalam bahasa Indonesia agar sesuai dengan gaya '${tone.name}' (${tone.instruction}) tanpa mengubah maksud asli kalimat.\n\n" +
                            intentInstructionId +
                            "$examples\n\n" +
                            "Masukan: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                } else {
                    val intentInstructionEn = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        "[Intent Classification & Output Format]\n" +
                        "1. Classify intent: [INTENT: STATEMENT | QUESTION | REQUEST | CONFIRM].\n" +
                        "2. Line 1: `[INTENT: <INTENT>]`, Line 2: converted text.\n\n"
                    } else {
                        "[Output Format]\n" +
                        "1. Intent: ${speechIntent.name}\n" +
                        "2. Line 1: `[INTENT: ${speechIntent.name}]`, Line 2: converted text.\n\n"
                    }

                    "<start_of_turn>user\n" +
                            "You are a mobile keyboard's 'tone & style transformer'.\n" +
                            "⚠️ CRITICAL: You are NOT a chatbot. Do NOT answer questions or converse with the user!\n" +
                            "⚠️ ABSOLUTE RULE: Keep the original language (English) of the input text. Do NOT translate it into Korean or other languages!\n" +
                            "Convert the tone of the English text into the target style '${tone.name}' (${tone.instruction}) while preserving the original meaning.\n\n" +
                            intentInstructionEn +
                            "$examples\n\n" +
                            "Input: \"$trimmed\"<end_of_turn>\n" +
                            "<start_of_turn>model\n"
                }

                val output = executeInference(prompt)

                if (!output.isNullOrBlank()) {
                    val defaultFallbackIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
                    val (detectedIntent, cleanOutput) = parseIntentTagAndClean(output, defaultFallbackIntent)
                    var refined = cleanLlmOutput(cleanOutput)
                    if (detectedIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION && !refined.endsWith("?")) {
                        refined = refined.removeSuffix(".").removeSuffix("!").trim() + "?"
                    }
                    return@withContext IntentResult.Success(refined, UiStrings.toneComplete(tone.icon, tone.name), detectedIntent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 톤앤매너 실행 오류: ${e.message}")
            }
        }

        val fallbackIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        return@withContext IntentResult.Success(trimmed, UiStrings.toneComplete(tone.icon, tone.name), fallbackIntent)
    }

    /**
     * 3. 실시간 온디바이스 다국어 번역 (TranslationTarget 기반)
     * - 내부적으로 핵심 번역 엔진 translate()를 단일 위임 호출하여 중복 제거
     */
    suspend fun processWithTranslation(
        voiceInput: String,
        target: ai.deartalk.android.data.pref.TranslationTarget,
        currentEditorText: String = "",
        packageName: String = "",
        tone: String? = null,
        speechIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.AUTO
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext IntentResult.Success("")

        val targetLangCode = LanguageProfileRegistry.resolveCode(target)

        val translated = translate(
            voiceInput = trimmed,
            targetLangCode = targetLangCode,
            sourceLangCode = "AUTO",
            tone = tone,
            packageName = packageName,
            speechIntent = speechIntent
        )

        val effectiveIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            speechIntent
        } else if (translated.trim().endsWith("?")) {
            ai.deartalk.android.live.data.SpeechIntent.QUESTION
        } else {
            ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        }

        if (translated.isNotBlank() && translated != trimmed) {
            try {
                Log.d(TAG, "✨ [온디바이스 LLM 다국어 번역 완료]: '$trimmed' ➔ '$translated' (${target.name})")
            } catch (_: Throwable) {}
            IntentResult.Success(translated, UiStrings.translationComplete(target.flag, target.name), effectiveIntent)
        } else {
            process(voiceInput, currentEditorText, packageName, speechIntent)
        }
    }

    internal fun cleanLlmOutput(raw: String, targetLangCode: String = ""): String {
        var text = raw
            .replace(Regex("""<(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)[^>]*>\s*(model|user|assistant)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<\|(im_start|im_end|endoftext)[^|>]*\|>\s*(assistant|user|system)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</?[a-zA-Z0-9_-]+(\s+[^>]*)?>"""), "")
            .replace(Regex("""\[INTENT:\s*(STATEMENT|QUESTION|REQUEST|CONFIRM)\]""", RegexOption.IGNORE_CASE), "")
            .trim()

        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                text = lines.first()
            }
        }

        text = text
            .replace(LanguageProfileRegistry.allCleaningPrefixesRegex, "")
            .trim()
            .removePrefix(">")
            .removePrefix("-")
            .removePrefix("*")

        val profile = if (targetLangCode.isNotBlank()) LanguageProfileRegistry.get(targetLangCode) else null
        val quotes = profile?.quotationMarks ?: listOf('"', '\'', '`', '“', '”', '‘', '’', '「', '」', '『', '』')
        for (q in quotes) {
            text = text.trim(q)
        }

        text = text
            .replace(Regex("""[\u2728\u2729\u2b50\u2b51\u2747\u2748\u2749\u2733\u2734\u2744]+$"""), "")
            .trim()

        return text.trim()
    }

    suspend fun processIntent(
        voiceInput: String,
        packageName: String = ""
    ): IntentResult =
        process(voiceInput = voiceInput, packageName = packageName)

    suspend fun applyTone(voiceInput: String, toneName: String, packageName: String = ""): IntentResult {
        val tone = CustomTone(
            id = toneName,
            name = toneName,
            instruction = toneName,
            icon = "✨"
        )
        return processWithTone(voiceInput = voiceInput, tone = tone, packageName = packageName)
    }

    /**
     * 🔄 문장 발화 의도(Intent) 변환기 (Whole-Sentence Pragmatic Rewrite)
     * - 온디바이스 SLM을 통해 단순 구두점 변경이 아닌, 문장의 어순, 종결 어미, 억양을 완벽히 재작성합니다.
     * - 예: "이거 복잡한 문제야" ➔ (QUESTION) "이거 복잡한 문제야?" / "이거 복잡한 문제인가요?"
     * - 예: "너는 복잡한 문제라고 생각하니" ➔ (STATEMENT) "이건 복잡한 문제라고 생각해."
     * - 예: "This is a complicated issue" ➔ (QUESTION) "Is this a complicated issue?" / "Do you think this is a complicated issue?"
     */
    suspend fun rewriteSentenceIntent(
        text: String,
        langCode: String = "KO",
        targetIntent: ai.deartalk.android.live.data.SpeechIntent,
        packageName: String = ""
    ): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || targetIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) return@withContext trimmed

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        val effectiveLang = if (langCode.equals("AUTO", ignoreCase = true)) {
            ai.deartalk.android.util.LanguageLocaleHelper.detectLanguageCode(trimmed, fallback = "KO")
        } else langCode.uppercase()

        if (isModelLoaded || sharedInitJob?.isActive == true) {
            try {
                val profile = LanguageProfileRegistry.get(effectiveLang)
                val intentRule = profile.getIntentRule(targetIntent)
                val intentDesc = intentRule?.directive ?: ""

                val scriptGuideline = if (profile.scriptGuidelines.isNotBlank()) "${profile.scriptGuidelines}\n" else ""
                val prompt = "<start_of_turn>user\n" +
                        "You are an expert real-time conversational sentence rewrite AI.\n" +
                        "Rewrite the sentence to match the target speech intent while preserving the core meaning and tone.\n" +
                        scriptGuideline +
                        "Target Intent: $intentDesc\n\n" +
                        "[Output Rule]\n" +
                        "Output ONLY the single-line rewritten sentence in ${profile.englishName} without quotes, markdown, or explanations.\n\n" +
                        "Input text: \"$trimmed\"<end_of_turn>\n" +
                        "<start_of_turn>model\n"

                val output = executeInference(prompt)
                if (!output.isNullOrBlank()) {
                    var clean = cleanLlmOutput(output, profile.code)
                    clean = profile.applyPostProcessing(clean, targetIntent)
                    Log.d(TAG, "✨ [의도 재작성 완료]: '$trimmed' ➔ '$clean' ($targetIntent, ${profile.code})")
                    return@withContext clean
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ rewriteSentenceIntent 오류: ${e.message}")
            }
        }

        // 🛡️ 휴리스틱 폴백: 부호 및 기본 어미 보정
        val clean = trimmed.trimEnd('?', '.', '!', ',', '"', '\'', '`')
        when (targetIntent) {
            ai.deartalk.android.live.data.SpeechIntent.QUESTION -> "$clean?"
            ai.deartalk.android.live.data.SpeechIntent.STATEMENT -> {
                if (effectiveLang == "EN") {
                    ai.deartalk.android.stt.IntonationAnalyzer.convertToDeclarativeEnglish(clean)
                } else {
                    "$clean."
                }
            }
            ai.deartalk.android.live.data.SpeechIntent.REQUEST -> "$clean."
            ai.deartalk.android.live.data.SpeechIntent.CONFIRM -> "$clean?"
            ai.deartalk.android.live.data.SpeechIntent.AUTO -> trimmed
        }
    }

    /**
     * 🎯 [DearTalk Live & AI 공통] 원문 및 번역문 통합 의도 재작성 및 재번역 파이프라인
     * - 원문을 새 의도에 맞추어 온디바이스 SLM으로 재작성한 후,
     * - 번역 대상 언어가 다를 경우 목표 언어로 완전한 문장 구조를 갖추어 재번역합니다.
     */
    suspend fun rephraseMessageWithIntent(
        rawSourceText: String,
        sourceLangCode: String,
        targetLangCode: String,
        targetIntent: ai.deartalk.android.live.data.SpeechIntent,
        tone: String? = null,
        packageName: String = "",
        conversationContext: List<String> = emptyList()
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val rewrittenSource = rewriteSentenceIntent(
            text = rawSourceText,
            langCode = sourceLangCode,
            targetIntent = targetIntent,
            packageName = packageName
        )

        val rewrittenTranslation = if (sourceLangCode.equals(targetLangCode, ignoreCase = true)) {
            rewrittenSource
        } else {
            translate(
                voiceInput = rewrittenSource,
                targetLangCode = targetLangCode,
                sourceLangCode = sourceLangCode,
                tone = tone,
                packageName = packageName,
                conversationContext = conversationContext,
                speechIntent = targetIntent
            )
        }

        Pair(rewrittenSource, rewrittenTranslation)
    }

    /**
     * 🌐 12개 글로벌/동남아 다국어 특화 실시간 동적 통역 & 화행 단일 패스 분류 엔진 (Single-Pass Semantic Tagging)
     * - Zero Hardcoding 원칙에 따라 온디바이스 SLM이 번역과 동시에 발화 의도([INTENT: ...])를 원샷 태깅합니다.
     */
    suspend fun translateWithIntent(
        voiceInput: String,
        targetLangCode: String,
        sourceLangCode: String = "KO",
        tone: String? = null,
        packageName: String = "",
        conversationContext: List<String> = emptyList(),
        speechIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.AUTO
    ): Pair<String, ai.deartalk.android.live.data.SpeechIntent> = withContext(Dispatchers.IO) {
        val trimmed = voiceInput.trim()
        if (trimmed.isBlank()) return@withContext Pair("", ai.deartalk.android.live.data.SpeechIntent.STATEMENT)

        if (!isModelLoaded && sharedInitJob?.isActive == true) {
            try { sharedInitJob?.join() } catch (_: Throwable) {}
        }

        val targetProfile = LanguageProfileRegistry.get(targetLangCode)
        val targetLangName = targetProfile.englishName

        val effectiveSourceLangCode = if (sourceLangCode.equals("AUTO", ignoreCase = true)) {
            ai.deartalk.android.util.LanguageLocaleHelper.detectLanguageCode(trimmed, fallback = "KO")
        } else {
            sourceLangCode
        }
        val sourceProfile = LanguageProfileRegistry.get(effectiveSourceLangCode)
        val sourceLangName = sourceProfile.englishName

        val toneInstruction = if (!tone.isNullOrBlank()) " Adapt the translated sentence to have a '$tone' tone." else ""

        // 🌟 1. 실시간 대화 세션 맥락 (DearTalk Live) 우선, 없으면 2. 앱별 격리 캐시 활용
        val combinedContext = if (conversationContext.isNotEmpty()) {
            conversationContext
        } else if (packageName.isNotBlank()) {
            AppScopedUtteranceCache.shared.getRecentContext(packageName)
        } else emptyList()

        val contextBlock = if (combinedContext.isNotEmpty()) {
            "Recent conversation flow (use this context to understand situation, pronouns, and repair misheard words):\n" +
                    combinedContext.joinToString("\n") { "- $it" } + "\n\n"
        } else ""

        val asrRepairInstruction = if (combinedContext.isNotEmpty()) {
            "3. SPEECH RECOGNITION (ASR) CORRECTION: The input was transcribed by voice STT and might contain phonetic slips, misheard words, homophones, or noise corruptions (e.g., mishearing '포함' as '포항', 'W hotel' as 'double hotel', numbers, or names). Analyze the preceding conversation flow carefully. If an obvious transcription mistake or contextual mismatch is present, SMARTLY REPAIR the intended meaning into natural $targetLangName.\n"
        } else ""

        val linguisticRule = if (targetProfile.scriptGuidelines.isNotBlank()) {
            "3. LINGUISTIC SPECIFICATION: ${targetProfile.scriptGuidelines}\n"
        } else ""

        // 🎯 4. 명시적 발화 의도 (Speech Pragmatics) 및 단일 패스 분류 지침 주입 (Zero Hardcoding)
        val isExplicitQuestion = ai.deartalk.android.stt.IntonationAnalyzer.isLikelyQuestion(
            text = trimmed,
            languageCode = effectiveSourceLangCode
        )
        val defaultFallbackIntent = if (isExplicitQuestion) ai.deartalk.android.live.data.SpeechIntent.QUESTION else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        val expectedFallback = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else defaultFallbackIntent

        val intentRuleSection = if (speechIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            "4. INTENT CLASSIFICATION & OUTPUT FORMAT:\n" +
            "- Analyze the pragmatic intent: [INTENT: STATEMENT | QUESTION | REQUEST | CONFIRM].\n" +
            "- Line 1: Output `[INTENT: <CLASSIFIED_INTENT>]`\n" +
            "- Line 2: Output ONLY the single-line translated sentence in $targetLangName.\n\n"
        } else {
            val intentRule = targetProfile.getIntentRule(speechIntent)
            val intentDirective = intentRule?.directive ?: ""
            "4. SPECIFIED INTENT & OUTPUT FORMAT:\n" +
            (if (intentDirective.isNotBlank()) "- $intentDirective\n" else "") +
            "- Specified Intent: ${speechIntent.name}\n" +
            "- Line 1: Output `[INTENT: ${speechIntent.name}]`\n" +
            "- Line 2: Output ONLY the single-line translated sentence in $targetLangName.\n\n"
        }

        if (isModelLoaded || sharedInitJob?.isActive == true) {
            try {
                val prompt = "<start_of_turn>user\n" +
                        "You are an expert real-time simultaneous interpreter and conversational speech repair assistant.\n" +
                        "Translate the spoken speech from $sourceLangName into natural, accurate $targetLangName.$toneInstruction\n" +
                        "CRITICAL INSTRUCTIONS:\n" +
                        "1. Follow the two-line output format exactly.\n" +
                        "2. Do NOT add notes, explanations, romanization, conversational fillers, or quotes.\n" +
                        linguisticRule +
                        asrRepairInstruction +
                        intentRuleSection +
                        contextBlock +
                        "Input text to translate: \"$trimmed\"<end_of_turn>\n" +
                        "<start_of_turn>model\n"

                val output = executeInference(prompt)

                if (!output.isNullOrBlank()) {
                    val (detectedIntent, cleanOutput) = parseIntentTagAndClean(output, expectedFallback)
                    var processed = cleanLlmOutput(cleanOutput, targetProfile.code)
                    processed = targetProfile.applyPostProcessing(processed, if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else detectedIntent)
                    Log.d(TAG, "✨ [온디바이스 번역 성공] '$trimmed' ($sourceLangName) ➔ '$processed' ($targetLangName, 화행: $detectedIntent)")
                    if (packageName.isNotBlank()) {
                        AppScopedUtteranceCache.shared.addUtterance(packageName, processed)
                    }
                    return@withContext Pair(processed, detectedIntent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ translateWithIntent 오류: ${e.message}")
            }
        }

        if (packageName.isNotBlank()) {
            AppScopedUtteranceCache.shared.addUtterance(packageName, trimmed)
        }
        Pair(trimmed, expectedFallback)
    }

    /**
     * 🌐 12개 글로벌/동남아 다국어 특화 실시간 동적 통역 엔진 (하위 호환성 래퍼)
     */
    suspend fun translate(
        voiceInput: String,
        targetLangCode: String,
        sourceLangCode: String = "KO",
        tone: String? = null,
        packageName: String = "",
        conversationContext: List<String> = emptyList(),
        speechIntent: ai.deartalk.android.live.data.SpeechIntent = ai.deartalk.android.live.data.SpeechIntent.AUTO
    ): String = translateWithIntent(
        voiceInput = voiceInput,
        targetLangCode = targetLangCode,
        sourceLangCode = sourceLangCode,
        tone = tone,
        packageName = packageName,
        conversationContext = conversationContext,
        speechIntent = speechIntent
    ).first

    suspend fun processCustomPrompt(promptText: String): String = withContext(Dispatchers.IO) {
        val formatted = "<start_of_turn>user\n$promptText<end_of_turn>\n<start_of_turn>model\n"
        val output = executeInference(formatted)
        return@withContext output ?: ""
    }
}
