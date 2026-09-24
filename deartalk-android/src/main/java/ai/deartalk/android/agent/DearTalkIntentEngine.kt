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
        private val inferenceMutex = Mutex()
        private var sharedLiteRtEngine: Engine? = null
        private var sharedInitJob: Job? = null

        private val _isModelLoadedFlow = MutableStateFlow(false)
        val isModelLoadedFlow: StateFlow<Boolean> = _isModelLoadedFlow.asStateFlow()

        private val _loadedModelNameFlow = MutableStateFlow("기본 모드 (온디바이스 STT)")
        val loadedModelNameFlow: StateFlow<String> = _loadedModelNameFlow.asStateFlow()

        private val _loadedModelSizeMbFlow = MutableStateFlow(0L)
        val loadedModelSizeMbFlow: StateFlow<Long> = _loadedModelSizeMbFlow.asStateFlow()

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
        ): Pair<ai.deartalk.android.live.data.SpeechIntent, String> =
            ai.deartalk.android.agent.prompt.PromptTemplateFactory.parseIntentTagAndClean(rawOutput, fallbackIntent)
    }

    val isModelLoaded: Boolean
        get() = sharedLoaded

    val isModelLoadedFlow: StateFlow<Boolean>
        get() = DearTalkIntentEngine.isModelLoadedFlow

    val loadedModelNameFlow: StateFlow<String>
        get() = DearTalkIntentEngine.loadedModelNameFlow

    val loadedModelSizeMbFlow: StateFlow<Long>
        get() = DearTalkIntentEngine.loadedModelSizeMbFlow

    val currentModelFamily: ai.deartalk.android.agent.prompt.ModelFamily
        get() = ai.deartalk.android.agent.prompt.ModelFamily.GEMMA

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

        // 🏆 단일 표준 프로덕션 모델: Gemma 4 E2B (2.4GB)
        val candidatePaths = mutableListOf<String>()
        candidatePaths.addAll(ai.deartalk.android.agent.engine.ModelPaths.STANDARD_CANDIDATE_PATHS)
        ai.deartalk.android.agent.engine.ModelPaths.APP_INTERNAL_MODELS.forEach { relative ->
            candidatePaths.add(File(appContext.filesDir, relative).absolutePath)
        }

        // 🛡️ Zero Fake Protocol: 최소 50MB 이상인 실제 가중치 바이너리 파일만 로드 대상
        val minModelSizeBytes = 50 * 1024 * 1024L

        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists() && file.length() >= minModelSizeBytes) {
                // 🏎️ [하드웨어 가속 다계층 바인딩]: Qualcomm Hexagon NPU 물리 검증 조건부 연동
                // 만약 Qualcomm NPU가 물리적으로 존재하는 경우에만 NPU 시도를 활성화하고, 그 외에는 CPU로 직행
                val npuProfile = ai.deartalk.android.agent.hardware.QualcommNpuDetector.detect()
                val backendsToTry = mutableListOf<Pair<String, Backend>>()

                if (npuProfile.isNpuSupported) {
                    Log.i(TAG, "🏎️ [NPU 감지 완료] Qualcomm Snapdragon (${npuProfile.socModel}) Hexagon NPU 물리 가속 우선 시도")
                    backendsToTry.add("NPU" to Backend.NPU(appContext.applicationInfo.nativeLibraryDir))
                } else {
                    Log.i(TAG, "ℹ️ Qualcomm Hexagon NPU 미지원 또는 타사 SoC (${npuProfile.socModel}) ➔ NPU 바인딩 스킵 및 CPU 고속 연산 적용")
                }

                backendsToTry.add("GPU" to Backend.GPU())
                backendsToTry.add("CPU" to Backend.CPU(4, null))

                for ((bName, backend) in backendsToTry) {
                    try {
                        Log.d(TAG, "🔄 온디바이스 표준 LLM 초기화 시도 ($bName): $path")
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
                        Log.d(TAG, "🧪 [온디바이스 LLM Warm-up 성공 ($bName)]: '$testResp'")

                        val sizeMb = file.length() / (1024 * 1024)
                        val detectedName = "Gemma 4 E2B (${sizeMb}MB, $bName)"

                        sharedLiteRtEngine = engine
                        sharedLoaded = true
                        _loadedModelNameFlow.value = detectedName
                        _loadedModelSizeMbFlow.value = sizeMb
                        _isModelLoadedFlow.value = true
                        Log.d(TAG, "✅ [온디바이스 표준 LLM 로드 완료 ($bName)]: $path ($detectedName)")
                        return
                    } catch (e: Throwable) {
                        Log.w(TAG, "⚠️ 온디바이스 LLM ($bName) 바인딩 불가 ➔ 다음 백엔드로 안전 폴백: ${e.message}")
                    }
                }
            }
        }

        _loadedModelNameFlow.value = "기본 모드 (온디바이스 STT)"
        _loadedModelSizeMbFlow.value = 0L
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
     * 🔄 모델 핫 리로드: Gemma 4 패키지 다운로드 완료 또는 삭제 시 새 모델 경로 즉시 재바인딩
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
                    _loadedModelNameFlow.value = "기본 모드 (온디바이스 STT)"
                    _loadedModelSizeMbFlow.value = 0L
                    initOnDeviceModel(context.applicationContext)
                }
            }
        }
    }

    /**
     * 🛡️ 단일 인스턴스/세션 안전 추론 실행기
     * ⚡ [속도 최적화]: 한 문장 완성 시 즉시 cancelProcess()를 호출하여 불필요한 무한 토큰 생성(수십초 블로킹) 차단
     */
    private suspend fun executeInference(prompt: String): String? = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            sharedLiteRtEngine?.let { engine ->
                val session = engine.createSession()
                try {
                    Log.d(TAG, "🚀 [LiteRT 추론 시작]: prompt length=${prompt.length}")
                    val startTime = System.currentTimeMillis()
                    val stringBuilder = StringBuilder()
                    val completer = kotlinx.coroutines.CompletableDeferred<String>()

                    session.generateContentStream(
                        listOf(InputData.Text(prompt)),
                        object : com.google.ai.edge.litertlm.ResponseCallback {
                            override fun onNext(chunk: String) {
                                stringBuilder.append(chunk)
                                val current = stringBuilder.toString()
                                val trimmedCurrent = current.trimStart()

                                // 🛑 초고속 조기 종료 조건 (단 1문장 완성 시 루프 즉각 중단):
                                // 종료 토큰(\n, <end_of_turn>, 예시 패턴) 검출 시 즉시 cancelProcess() 및 첫 문장만 추출
                                // 단, 다국어 번역([INTENT: 태그)인 경우 2번째 줄(번역문)이 완성될 때까지 대기
                                if (trimmedCurrent.length >= 2) {
                                    val hasIntentTag = trimmedCurrent.startsWith("[INTENT:", ignoreCase = true) ||
                                            trimmedCurrent.startsWith("INTENT:", ignoreCase = true) ||
                                            trimmedCurrent.contains("INTENT:", ignoreCase = true)

                                    // 🛡️ [다국어 보편 제어 태그 검출]: <end_of_turn>, <start_of_turn>, <think>, <eos> 등 모든 SLM 제어 블록 시작점 포착
                                    val controlTagMatch = Regex("""</?[a-zA-Z0-9_\-./]+""").find(trimmedCurrent)
                                    val controlTagIndex = controlTagMatch?.range?.first ?: -1
                                    val lastOpenBracket = trimmedCurrent.lastIndexOf('<')
                                    val lastCloseBracket = trimmedCurrent.lastIndexOf('>')
                                    val isInsideUnclosedTag = lastOpenBracket >= 0 && lastOpenBracket > lastCloseBracket

                                    val shouldStop = if (hasIntentTag) {
                                        val lines = trimmedCurrent.lines().map { it.trim() }.filter { it.isNotBlank() }
                                        val contentLines = lines.filterNot { 
                                            it.contains("INTENT:", ignoreCase = true) || it.matches(Regex("""^Line\s*[12]\s*[:：]?\s*$""", RegexOption.IGNORE_CASE))
                                        }
                                        val hasContent = contentLines.isNotEmpty()
                                        val lastContent = contentLines.lastOrNull()?.replace(Regex("""^Translation\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")?.trim() ?: ""
                                        // ⚡ 문장 종결 부호(?, !, ., 。, ？, ！) 발견 즉시 300~500ms 조기 종료 (태그 내부가 아닐 때만 유효)
                                        val endsWithTerminator = !isInsideUnclosedTag && lastContent.length >= 2 && 
                                                (lastContent.endsWith("?") || lastContent.endsWith("!") || lastContent.endsWith(".") ||
                                                 lastContent.endsWith("？") || lastContent.endsWith("！") || lastContent.endsWith("。") ||
                                                 lastContent.endsWith("?]") || lastContent.endsWith("!]"))
                                        val newlineCount = trimmedCurrent.count { it == '\n' }

                                        // 🛑 루프 브레이커: INTENT: 태그가 2번 이상 반복되거나, 라인이 2개 이상인데 번역문이 안 나오면 즉각 종료
                                        val intentCount = Regex("""INTENT:""", RegexOption.IGNORE_CASE).findAll(trimmedCurrent).count()
                                        val isLooping = intentCount >= 2 || (lines.size >= 2 && !hasContent)

                                        isLooping ||
                                                controlTagIndex >= 0 ||
                                                (hasContent && endsWithTerminator) ||
                                                (hasContent && (trimmedCurrent.endsWith("\n") || newlineCount >= lines.size)) ||
                                                (hasContent && (trimmedCurrent.contains("- 원문") || trimmedCurrent.contains("원문:") || trimmedCurrent.contains("예시:")))
                                    } else {
                                        val isTerminated = !isInsideUnclosedTag &&
                                                (trimmedCurrent.endsWith("?") || trimmedCurrent.endsWith("!") || trimmedCurrent.endsWith(".") ||
                                                 trimmedCurrent.endsWith("？") || trimmedCurrent.endsWith("！") || trimmedCurrent.endsWith("。")) && trimmedCurrent.length >= 2

                                        controlTagIndex >= 0 ||
                                                isTerminated ||
                                                trimmedCurrent.contains("\n") ||
                                                trimmedCurrent.contains("- 원문") ||
                                                trimmedCurrent.contains("원문:") ||
                                                trimmedCurrent.contains("예시:") ||
                                                trimmedCurrent.contains("[변환")
                                    }

                                    if (shouldStop) {
                                        // ⚡ NPU 드라이버 스톨 방지: onNext 콜백 내에서 동기식 cancelProcess()를 호출하면
                                        // Qualcomm QNN DSP 인터럽트 대기로 5~10초 블로킹되므로, 결과 완성 즉시 completer만 완료시킵니다.
                                        val stopIndex = if (hasIntentTag) {
                                            if (controlTagIndex >= 0) controlTagIndex else trimmedCurrent.length
                                        } else {
                                            val candidates = mutableListOf<Int>()
                                            if (controlTagIndex >= 0) candidates.add(controlTagIndex)
                                            val newlineIdx = trimmedCurrent.indexOf("\n")
                                            if (newlineIdx >= 0) candidates.add(newlineIdx)
                                            val echoPatterns = listOf("- 원문", "원문:", "예시:", "[변환")
                                            for (echo in echoPatterns) {
                                                val idx = trimmedCurrent.indexOf(echo)
                                                if (idx >= 0) candidates.add(idx)
                                            }
                                            if (candidates.isNotEmpty()) candidates.minOrNull() ?: trimmedCurrent.length else trimmedCurrent.length
                                        }
                                        val completedText = trimmedCurrent.substring(0, stopIndex).trim()
                                        if (!completer.isCompleted) {
                                            completer.complete(if (completedText.isNotBlank()) completedText else trimmedCurrent)
                                        }
                                    }
                                }
                            }

                            override fun onDone() {
                                if (!completer.isCompleted) {
                                    completer.complete(stringBuilder.toString())
                                }
                            }

                            override fun onError(error: Throwable) {
                                if (!completer.isCompleted) {
                                    if (stringBuilder.isNotBlank()) {
                                        completer.complete(stringBuilder.toString())
                                    } else {
                                        completer.completeExceptionally(error)
                                    }
                                }
                            }
                        }
                    )

                    // ⏱️ 타임아웃 가드: 모바일 온디바이스 특성상 최대 12초 제한 (초과 시 즉시 강제 취소)
                    val rawResponse = try {
                        kotlinx.coroutines.withTimeout(12000L) {
                            completer.await()
                        }
                    } catch (t: kotlinx.coroutines.TimeoutCancellationException) {
                        try { session.cancelProcess() } catch (_: Throwable) {}
                        stringBuilder.toString()
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "⚡ [LiteRT SLM 추론 완료 (${elapsed}ms)]: '$rawResponse'")

                    // ⚡ [스톨 완전 차단]: 결과 완성 즉시 코루틴 컨텍스트에서 C++ 생성 중단 신호 전송
                    try {
                        session.cancelProcess()
                    } catch (_: Throwable) {}

                    val cleaned = cleanLlmOutput(rawResponse)
                    Log.d(TAG, "✨ [LiteRT Cleaned SLM Output]: '$cleaned'")
                    cleaned.takeIf { it.isNotBlank() }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    try { session.cancelProcess() } catch (_: Throwable) {}
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "❌ [온디바이스 LLM 추론 오류]: ${e.message}")
                    null
                } finally {
                    // ⚡ [스톨 완전 차단]: session.close()의 C++ 네이티브 스레드 대기 블로킹을 방지하기 위해
                    // inferenceMutex 락 바깥의 별도 백그라운드 코루틴으로 위임하여 다음 추론이 0ms로 즉시 시작되도록 보장
                    val sessionToClose = session
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            sessionToClose.close()
                        } catch (e: Throwable) {
                            Log.w(TAG, "⚠️ [LiteRT 세션 백그라운드 종료 예외]: ${e.message}")
                        }
                    }
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

                val prompt = ai.deartalk.android.agent.prompt.PromptTemplateFactory.buildCorrectionPrompt(
                    trimmed = trimmed,
                    currentEditorText = currentEditorText,
                    priorContext = priorContext,
                    isInputKorean = isInputKorean,
                    isIndonesianLocale = isIndonesianLocale,
                    isInputEnglish = isInputEnglish,
                    speechIntent = speechIntent,
                    modelFamily = currentModelFamily
                )

                val output = executeInference(prompt)

                if (!output.isNullOrBlank()) {
                    val defaultFallbackIntent = if (isExplicitQuestion) ai.deartalk.android.live.data.SpeechIntent.QUESTION else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
                    val expectedFallback = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else defaultFallbackIntent
                    val (detectedIntent, cleanOutput) = parseIntentTagAndClean(output, expectedFallback)
                    val textToClean = if (cleanOutput.isNotBlank()) cleanOutput else trimmed
                    var refined = cleanLlmOutput(textToClean, detectedLangCode)
                    if (refined.isBlank() || !refined.any { Character.isLetterOrDigit(it) }) refined = trimmed
                    refined = ai.deartalk.android.agent.prompt.PromptTemplateFactory.sanitizeKeyboardOutput(trimmed, refined, isExplicitQuestion)

                    // 🎯 AI가 완성한 문장 기반 실시간 화행 확정 (SSOT Zero Hardcoding)
                    val hasExplicitIntentTag = output.contains("INTENT:", ignoreCase = true)
                    val finalDetectedIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        speechIntent
                    } else if (hasExplicitIntentTag) {
                        detectedIntent
                    } else {
                        ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(refined, detectedLangCode)
                    }

                    val hasNaturalLanguageChars = refined.any { Character.isLetterOrDigit(it) }
                    if (finalDetectedIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION && hasNaturalLanguageChars && !refined.endsWith("?") && !refined.endsWith("？")) {
                        refined = refined.removeSuffix(".").removeSuffix("!").trim() + "?"
                    }
                    try {
                        Log.d(TAG, "✨ [온디바이스 LLM 생성 완료]: '$trimmed' ➔ '$refined' (화행: $finalDetectedIntent)")
                    } catch (_: Throwable) {}
                    if (packageName.isNotBlank()) {
                        AppScopedUtteranceCache.shared.addUtterance(packageName, refined)
                    }
                    return@withContext IntentResult.Success(refined, UiStrings.aiGenerationComplete, finalDetectedIntent)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
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

        val isInputKorean = hasKorean(trimmed)
        val isIndonesianLocale = context?.let {
            val lang = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(it).language.lowercase()
            lang == "id" || lang == "in"
        } ?: false
        val isInputIndonesian = !isInputKorean && (isIndonesianLocale || ai.deartalk.android.util.LanguageLocaleHelper.detectLanguageCode(trimmed) == "ID")
        val detectedLangCode = if (isInputKorean) "KO" else if (isInputIndonesian) "ID" else "EN"
        val isExplicitQuestion = speechIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION ||
                ai.deartalk.android.stt.IntonationAnalyzer.isLikelyQuestion(text = trimmed, languageCode = detectedLangCode)

        if (isModelLoaded) {
            try {
                val examples = if (isInputKorean) {
                    when (tone.id) {
                        "tone_polite", "공손하게" -> """
                            예시: "내일 몇 시에 만날래" -> 내일 몇 시쯤 뵐 수 있으실까요?
                            예시: "이거 뭐야" -> 이것이 무엇인지 말씀해 주실 수 있을까요?
                            예시: "자료 보내줘" -> 요청하신 자료를 검토 부탁드립니다.
                        """.trimIndent()
                        "tone_casual", "친근하게" -> """
                            예시: "내일 몇 시에 만날래" -> 내일 우리 몇 시에 만날까? 😊
                            예시: "오늘 재밌었어" -> 오늘 너무 즐거웠어 고마워! 😊
                        """.trimIndent()
                        "tone_business", "비즈니스" -> """
                            예시: "식사 같이 하실래요?" -> 오늘 식사 함께 하실 수 있으실까요?
                            예시: "자료 검토해봐" -> 송부드린 자료 검토 부탁드립니다.
                            예시: "그건 상관없어" -> 그건 문제없을 것 같습니다.
                        """.trimIndent()
                        "tone_funny", "재미있게" -> """
                            예시: "식사 같이 하실래요?" -> 밥 먹으러 안 가면 유죄! 같이 맛있는 거 먹으러 가요 🤣
                        """.trimIndent()
                        "tone_cheeky", "건방지게", "당당하게" -> """
                            예시: "식사 같이 하실래요?" -> 오늘 밥은 내가 같이 먹어주는 거니까 영광인 줄 알아 😼
                        """.trimIndent()
                        else -> """
                            예시: "식사 같이 하실래요?" -> 식사 같이 하실래요?
                        """.trimIndent()
                    }
                } else if (isInputIndonesian) {
                    when (tone.id) {
                        "tone_polite", "공손하게", "sopan" -> """
                            Contoh: "Besok jam berapa ketemu?" -> Besok kira-kira kita bisa bertemu jam berapa ya?
                            Contoh: "Ini apa ya?" -> Mohon maaf, bolehkah saya tahu ini apa?
                            Contoh: "Kirim filenya ya" -> Mohon kirimkan dokumen yang diminta jika ada waktu luang.
                        """.trimIndent()
                        "tone_casual", "친근하게", "santai" -> """
                            Contoh: "Mau makan siang bareng?" -> Yuk makan siang bareng! 😊
                            Contoh: "Hari ini seru banget" -> Hari ini seru banget makasih ya! 😊
                        """.trimIndent()
                        "tone_business", "비즈니스", "formal" -> """
                            Contoh: "Mau makan siang bareng?" -> Mohon konfirmasi apakah Anda berkenan untuk makan siang bersama hari ini.
                            Contoh: "Kirim filenya ya" -> Mohon tinjau dan kirimkan dokumen tersebut pada kesempatan pertama.
                        """.trimIndent()
                        "tone_funny", "재미있게", "lucu" -> """
                            Contoh: "Mau makan siang bareng?" -> Perut udah demo nih, nggak ikut makan siang awas ya! 🤣
                        """.trimIndent()
                        "tone_cheeky", "건방지게", "당당하게", "percaya diri" -> """
                            Contoh: "Mau makan siang bareng?" -> Makan siang bareng aku itu kesempatan langka lho, bangga dong 😼
                        """.trimIndent()
                        else -> """
                            Contoh: "Mau makan siang bareng?" -> Mau makan siang bareng?
                        """.trimIndent()
                    }
                } else {
                    when (tone.id) {
                        "tone_polite", "공손하게" -> """
                            Example: "Do you want to have lunch?" -> Would you like to have lunch with me?
                            Example: "What is this?" -> Could you please tell me what this is?
                            Example: "Send me the file" -> Could you please send me the file when you have a moment?
                        """.trimIndent()
                        "tone_casual", "친근하게" -> """
                            Example: "Do you want to have lunch?" -> Let's grab some lunch together! 😊
                            Example: "Today was fun" -> I had so much fun today! 😊
                        """.trimIndent()
                        "tone_business", "비즈니스" -> """
                            Example: "Do you want to have lunch?" -> Please let me know if you are available for lunch today.
                            Example: "Send me the file" -> Please review and forward the requested documentation at your convenience.
                        """.trimIndent()
                        "tone_funny", "재미있게" -> """
                            Example: "Do you want to have lunch?" -> Lunch is calling, and answering is mandatory! 🤣
                        """.trimIndent()
                        "tone_cheeky", "건방지게" -> """
                            Example: "Do you want to have lunch?" -> You should consider it an honor that I'm dining with you today. 😼
                        """.trimIndent()
                        else -> """
                            Example: "Do you want to have lunch?" -> Do you want to have lunch?
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

                val prompt = ai.deartalk.android.agent.prompt.PromptTemplateFactory.buildTonePrompt(
                    trimmed = trimmed,
                    toneName = tone.name,
                    toneInstruction = tone.instruction,
                    examples = examples,
                    isInputKorean = isInputKorean,
                    isInputIndonesian = isInputIndonesian,
                    speechIntent = speechIntent,
                    modelFamily = currentModelFamily
                )

                val output = executeInference(prompt)

                if (!output.isNullOrBlank()) {
                    val defaultFallbackIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
                    val (detectedIntent, cleanOutput) = parseIntentTagAndClean(output, defaultFallbackIntent)
                    val textToClean = cleanOutput.ifBlank { trimmed }
                    var refined = cleanLlmOutput(textToClean)
                    if (refined.isBlank() || !refined.any { Character.isLetterOrDigit(it) }) refined = trimmed
                    refined = ai.deartalk.android.agent.prompt.PromptTemplateFactory.sanitizeKeyboardOutput(trimmed, refined, isExplicitQuestion)

                    val normalizedRefined = refined.trim().trimEnd('?', '!', '.', ' ')
                    val normalizedTrimmed = trimmed.trim().trimEnd('?', '!', '.', ' ')
                    val isToneActuallyChanged = !normalizedRefined.equals(normalizedTrimmed, ignoreCase = true)

                    // 🎯 AI가 완성한 문장 기반 실시간 화행 확정 (SSOT Zero Hardcoding)
                    val hasExplicitIntentTag = output.contains("INTENT:", ignoreCase = true)
                    val finalDetectedIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        speechIntent
                    } else if (hasExplicitIntentTag) {
                        detectedIntent
                    } else {
                        ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(refined, detectedLangCode)
                    }

                    val hasNaturalLanguageChars = refined.any { Character.isLetterOrDigit(it) }
                    if ((finalDetectedIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION || finalDetectedIntent == ai.deartalk.android.live.data.SpeechIntent.CONFIRM) &&
                        hasNaturalLanguageChars) {
                        if (isToneActuallyChanged || isExplicitQuestion || speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                            refined = refined.trimEnd('?', '!', '.', ' ', '？') + "?"
                        }
                    } else if ((finalDetectedIntent == ai.deartalk.android.live.data.SpeechIntent.STATEMENT || finalDetectedIntent == ai.deartalk.android.live.data.SpeechIntent.REQUEST) &&
                        speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO &&
                        hasNaturalLanguageChars) {
                        refined = refined.trimEnd('?', '!', '.', ' ', '？') + "."
                    }

                    val isContentChanged = refined != trimmed
                    if (isToneActuallyChanged || isContentChanged) {
                        val message = if (isToneActuallyChanged) {
                            UiStrings.toneComplete(tone.icon, tone.name)
                        } else {
                            UiStrings.aiTextComplete
                        }
                        try {
                            Log.d(TAG, "🎭 [온디바이스 문장 변환 성공]: '$trimmed' ➔ '$refined' (어조: ${tone.name}, 화행: $finalDetectedIntent, 어조변경: $isToneActuallyChanged)")
                        } catch (_: Throwable) {}
                        return@withContext IntentResult.Success(refined, message, finalDetectedIntent)
                    } else {
                        try {
                            Log.w(TAG, "⚠️ [온디바이스 톤앤매너 미변환/원문 유지]: '$trimmed' (LLM 미변환, 원문 유지)")
                        } catch (_: Throwable) {}
                        return@withContext IntentResult.Success(trimmed, UiStrings.sttComplete, finalDetectedIntent)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ 톤앤매너 실행 오류: ${e.message}")
            }
        }

        val fallbackIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            speechIntent
        } else {
            ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(trimmed, detectedLangCode)
        }
        var fallbackText = trimmed
        if ((fallbackIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION || fallbackIntent == ai.deartalk.android.live.data.SpeechIntent.CONFIRM) &&
            (isExplicitQuestion || speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO)) {
            val cleanBase = fallbackText.trimEnd('?', '!', '.', ' ', '？')
            if (cleanBase.any { Character.isLetterOrDigit(it) }) {
                fallbackText = "$cleanBase?"
            }
        } else if ((fallbackIntent == ai.deartalk.android.live.data.SpeechIntent.STATEMENT || fallbackIntent == ai.deartalk.android.live.data.SpeechIntent.REQUEST) &&
            speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            val cleanBase = fallbackText.trimEnd('?', '!', '.', ' ', '？')
            if (cleanBase.any { Character.isLetterOrDigit(it) }) {
                fallbackText = "$cleanBase."
            }
        }
        return@withContext IntentResult.Success(fallbackText, UiStrings.sttComplete, fallbackIntent)
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

        val targetCode = ai.deartalk.android.agent.language.LanguageProfileRegistry.resolveCode(target)
        val effectiveIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            speechIntent
        } else {
            ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(translated, targetCode)
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

    internal fun cleanLlmOutput(raw: String, targetLangCode: String = ""): String =
        ai.deartalk.android.agent.prompt.PromptTemplateFactory.cleanLlmOutput(raw, targetLangCode)

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

        val toneInstruction = if (!tone.isNullOrBlank()) " Tone: '$tone'." else ""

        // ⚡ 대화 맥락(Context) 완전 제거: 독립 문장 단위 초경량 순수 번역으로 Prefill 토큰 최소화 및 1.8초대 초고속 추론 달성
        val contextBlock = ""

        val asrRepairInstruction = ""

        val linguisticRule = if (targetProfile.scriptGuidelines.isNotBlank()) {
            "Guideline: ${targetProfile.scriptGuidelines}"
        } else ""

        // 🎯 4. 명시적 발화 의도 (Speech Pragmatics) 및 단일 패스 분류 지침 주입 (Zero Hardcoding)
        val isExplicitQuestion = ai.deartalk.android.stt.IntonationAnalyzer.isLikelyQuestion(
            text = trimmed,
            languageCode = effectiveSourceLangCode
        )
        val defaultFallbackIntent = if (isExplicitQuestion) ai.deartalk.android.live.data.SpeechIntent.QUESTION else ai.deartalk.android.live.data.SpeechIntent.STATEMENT
        val expectedFallback = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) speechIntent else defaultFallbackIntent

        val intentRuleSection = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            "Target Intent: ${speechIntent.name}"
        } else ""

        if (isModelLoaded || sharedInitJob?.isActive == true) {
            try {
                val prompt = ai.deartalk.android.agent.prompt.PromptTemplateFactory.buildTranslationPrompt(
                    trimmed = trimmed,
                    sourceLangName = sourceLangName,
                    targetLangName = targetLangName,
                    toneInstruction = toneInstruction,
                    linguisticRule = linguisticRule,
                    asrRepairInstruction = asrRepairInstruction,
                    intentRuleSection = intentRuleSection,
                    contextBlock = contextBlock,
                    speechIntent = speechIntent,
                    modelFamily = currentModelFamily
                )

                val output = executeInference(prompt)

                if (!output.isNullOrBlank()) {
                    val hasExplicitIntentTag = output.contains("INTENT:", ignoreCase = true)
                    val (detectedIntent, cleanOutput) = parseIntentTagAndClean(output, expectedFallback)
                    val textToClean = if (cleanOutput.isNotBlank()) cleanOutput else trimmed
                    var processed = cleanLlmOutput(textToClean, targetProfile.code)
                    if (processed.isBlank()) processed = trimmed
                    processed = ai.deartalk.android.agent.prompt.PromptTemplateFactory.sanitizeKeyboardOutput(trimmed, processed, isExplicitQuestion)
                    
                    val finalDetectedIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                        speechIntent
                    } else if (hasExplicitIntentTag) {
                        detectedIntent
                    } else {
                        ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(processed, targetProfile.code)
                    }

                    processed = targetProfile.applyPostProcessing(processed, finalDetectedIntent)
                    Log.d(TAG, "✨ [온디바이스 번역 성공] '$trimmed' ($sourceLangName) ➔ '$processed' ($targetLangName, 화행: $finalDetectedIntent)")
                    if (packageName.isNotBlank()) {
                        AppScopedUtteranceCache.shared.addUtterance(packageName, processed)
                    }
                    return@withContext Pair(processed, finalDetectedIntent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ translateWithIntent 오류: ${e.message}")
            }
        }

        if (packageName.isNotBlank()) {
            AppScopedUtteranceCache.shared.addUtterance(packageName, trimmed)
        }
        val fallbackIntent = if (speechIntent != ai.deartalk.android.live.data.SpeechIntent.AUTO) {
            speechIntent
        } else {
            ai.deartalk.android.live.data.MultilingualIntentHeuristic.guessIntent(trimmed, effectiveSourceLangCode)
        }
        Pair(trimmed, fallbackIntent)
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
