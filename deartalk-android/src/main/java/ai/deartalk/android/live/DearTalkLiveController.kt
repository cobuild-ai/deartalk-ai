package ai.deartalk.android.live

import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import ai.deartalk.android.live.data.LiveSession
import ai.deartalk.android.live.data.LiveSessionRepository
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.live.data.formatSourcePunctuation
import ai.deartalk.android.live.data.getLabel
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.tts.TextToSpeechManager
import ai.deartalk.android.tts.VoiceGender
import ai.deartalk.android.util.LanguageLocaleHelper
import ai.deartalk.android.data.pref.UiStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * 🎙️ 현재 활성 발화자 상태
 */
enum class ActiveSpeaker {
    NONE,
    ME,       // 내가 말하는 중
    PARTNER   // 상대방이 말하는 중
}

/**
 * 🧠 DearTalk Live 총괄 제어기 (Controller)
 * - 1:1 양방향 대화 STT ➔ 온디바이스 AI 번역/정제 ➔ SQLite 영속화 ➔ 자동 TTS 음성 재생
 * - Clean Code & MVVM: 비즈니스 로직과 Compose UI를 완벽히 분리
 */
class DearTalkLiveController(
    private val context: Context,
    private val repository: LiveSessionRepository,
    private val intentEngine: DearTalkIntentEngine,
    private val ttsManager: TextToSpeechManager,
    private val sttManager: SpeechRecognitionManager
) {
    companion object {
        private const val TAG = "DearTalkLiveController"
        const val DEFAULT_TTS_PITCH = 1.0f
        private const val PREFS_NAME = "deartalk_live_prefs"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val controllerJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + controllerJob)
    private var processJob: Job? = null
    private var lastProcessedText: String = ""
    private var lastProcessedTime: Long = 0L

    // UI 관찰 상태
    private val _currentSession = MutableStateFlow<LiveSession?>(null)
    val currentSession: StateFlow<LiveSession?> = _currentSession.asStateFlow()

    private val _messages = MutableStateFlow<List<LiveMessage>>(emptyList())
    val messages: StateFlow<List<LiveMessage>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<LiveSession>>(emptyList())
    val sessions: StateFlow<List<LiveSession>> = _sessions.asStateFlow()

    private val _activeSpeaker = MutableStateFlow(ActiveSpeaker.NONE)
    val activeSpeaker: StateFlow<ActiveSpeaker> = _activeSpeaker.asStateFlow()

    private val _processingSpeaker = MutableStateFlow(ActiveSpeaker.NONE)
    val processingSpeaker: StateFlow<ActiveSpeaker> = _processingSpeaker.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // 🔄 사용자가 의도/톤 변경 시 AI가 문장 구조를 재점검 중인 메시지 ID (UI 흐림 및 이탤릭 피드백용)
    private val _rephrasingMessageId = MutableStateFlow<String?>(null)
    val rephrasingMessageId: StateFlow<String?> = _rephrasingMessageId.asStateFlow()


    val rmsDb: StateFlow<Float> = sttManager.rmsDb

    // 🌐 언어별 모델 가용성 및 온디바이스 다운로드 진행 상태
    val modelDownloadStatus: StateFlow<Map<String, ai.deartalk.android.stt.LanguageModelStatus>> = sttManager.modelDownloadStatus

    // 사용자 설정 (내 언어 / 상대방 언어 / 톤) - Compose 반응형 상태
    var myLang: String by mutableStateOf("KO")
        private set
    var partnerLang: String by mutableStateOf("EN")
        private set
    var selectedTone: String by mutableStateOf("✨ " + UiStrings.toneRefine)

    fun setTone(newTone: String) {
        selectedTone = newTone
        val lastMsg = _messages.value.lastOrNull()
        if (lastMsg != null && lastMsg.sender == LiveSender.ME && !_isProcessing.value) {
            scope.launch(Dispatchers.IO) {
                _rephrasingMessageId.value = lastMsg.id
                try {
                    val recentContext = _messages.value.takeLast(5).map { msg ->
                        val senderTag = if (msg.sender == LiveSender.ME) "User (${msg.sourceLang})" else "Partner (${msg.sourceLang})"
                        "$senderTag: \"${msg.rawText}\" -> \"${msg.refinedText}\""
                    }
                    val cleanTone = selectedTone.replace(Regex("""[^\p{L}\p{N}\s]"""), "").trim()
                    val baseSourceText = if (lastMsg.originalRawText.isNotBlank()) lastMsg.originalRawText else lastMsg.rawText
                    val (newRaw, newRefined) = intentEngine.rephraseMessageWithIntent(
                        rawSourceText = baseSourceText,
                        sourceLangCode = lastMsg.sourceLang,
                        targetLangCode = lastMsg.targetLang,
                        targetIntent = myIntent,
                        tone = cleanTone,
                        packageName = "ai.deartalk.android.live",
                        conversationContext = recentContext
                    )
                    val updatedMsg = lastMsg.copy(
                        rawText = baseSourceText,
                        refinedText = newRefined,
                        tone = selectedTone,
                        originalRawText = baseSourceText
                    )
                    repository.updateMessage(updatedMsg)
                    withContext(Dispatchers.Main) {
                        val updatedList = _messages.value.toMutableList()
                        val lastIdx = updatedList.indexOfLast { it.id == updatedMsg.id }
                        if (lastIdx != -1) {
                            updatedList[lastIdx] = updatedMsg
                            _messages.value = updatedList
                        }
                    }
                    Log.d(TAG, "✨ [Live] 톤 변경 AI 재작성 완료: '${lastMsg.refinedText}' ➔ '$newRefined' (톤: $selectedTone)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [Live] 톤 변경 재작성 실패: ${e.message}", e)
                } finally {
                    _rephrasingMessageId.value = null
                }
            }
        }
    }

    // 🎯 실시간 대화 발화 의도 (Speech Pragmatics / 화행)
    // 1회성(One-shot)으로 적용되며, 발화 완료 후 각각 자동으로 AUTO(스마트)로 복귀
    var myIntent: SpeechIntent by mutableStateOf(SpeechIntent.AUTO)
    var partnerIntent: SpeechIntent by mutableStateOf(SpeechIntent.AUTO)

    // 🤖 AI 온디바이스 SLM이 감지한 최근 화행 의도 (UI 칩 실시간 하이라이팅용)
    var myDetectedIntent: SpeechIntent? by mutableStateOf(null)
    var partnerDetectedIntent: SpeechIntent? by mutableStateOf(null)

    // 하위 호환성 프로퍼티 (기존 단일 myIntent 바인딩 유지)
    var currentIntent: SpeechIntent
        get() = myIntent
        set(value) {
            myIntent = value
        }

    // 🔊 자동 TTS 읽기 토글 (기본값: false - 화면 텍스트 우선 표시 & 스피커 버튼 온디맨드 재생)
    var isAutoSpeakEnabled: Boolean by mutableStateOf(false)
        private set

    fun toggleAutoSpeak() {
        isAutoSpeakEnabled = !isAutoSpeakEnabled
    }

    // 🛡️ 더블 탭 및 STT 자동 종료 직후 토글 바운스(Stop->Start 오작동) 방지 가드
    private var lastActionTime = 0L
    private var lastSttFinishTime = 0L
    private val ACTION_COOLDOWN_MS = 600L

    init {
        observeStt()
        initSingleSession()
        prewarmAndDownloadLanguages(myLang, partnerLang)
    }

    private fun initSingleSession() {
        scope.launch(Dispatchers.IO) {
            val session = repository.getOrCreateDefaultSession(myLang, partnerLang)
            val msgs = repository.getRecentMessages(LiveSessionRepository.MAX_RING_BUFFER_SIZE)
            withContext(Dispatchers.Main) {
                _currentSession.value = session
                _messages.value = msgs
            }
        }
    }

    /**
     * 🧹 타임라인 링 버퍼 전체 비우기 (메모리 및 DB 메시지 0개로 즉시 리셋)
     */
    fun clearTimeline() {
        scope.launch(Dispatchers.IO) {
            repository.clearTimeline()
            withContext(Dispatchers.Main) {
                _messages.value = emptyList()
                _streamingText.value = ""
                myIntent = SpeechIntent.AUTO
                partnerIntent = SpeechIntent.AUTO
                myDetectedIntent = null
                partnerDetectedIntent = null
                Log.d(TAG, "🧹 [타임라인 비우기 완료]: 메시지 0건 리셋")
            }
        }
    }

    /**
     * 🎙️ STT 상태 감지 및 실시간 스트리밍 바인딩
     */
    private fun observeStt() {
        scope.launch {
            sttManager.voiceState.collect { state ->
                when (state) {
                    is VoiceState.PartialResult -> {
                        _streamingText.value = state.text
                    }
                    is VoiceState.FinalResult -> {
                        _streamingText.value = state.text
                        handleSttResult(state.text)
                    }
                    is VoiceState.Error -> {
                        _streamingText.value = ""
                        _activeSpeaker.value = ActiveSpeaker.NONE
                        _isProcessing.value = false
                        lastSttFinishTime = System.currentTimeMillis()
                    }
                    is VoiceState.Idle -> {
                        if (!_isProcessing.value) {
                            _activeSpeaker.value = ActiveSpeaker.NONE
                        }
                        lastSttFinishTime = System.currentTimeMillis()
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 🗣️ '내가 말하기' 토글 (내 언어로 청취)
     */
    fun toggleSpeakMe() {
        val now = System.currentTimeMillis()
        if (_activeSpeaker.value == ActiveSpeaker.ME) {
            // 이미 말하기 중이면 중지
            lastActionTime = now
            lastSttFinishTime = now
            sttManager.stopListening()
        } else {
            // 🛡️ 쿨다운 가드: AI 처리 중이거나, 종료/이전 액션 직후 600ms 이내 재진입 원천 차단 (Stop->Start 바운스 방지)
            if (_isProcessing.value) {
                Log.w(TAG, "🛡️ [Live 토글 가드]: 이전 발화 처리 중(_isProcessing=true)이므로 '내가 말하기' 시작 요청 무시")
                return
            }
            if ((now - lastSttFinishTime) < ACTION_COOLDOWN_MS || (now - lastActionTime) < ACTION_COOLDOWN_MS) {
                Log.w(TAG, "🛡️ [Live 토글 가드]: 직전 STT 종료/액션 후 쿨다운(${ACTION_COOLDOWN_MS}ms) 미달로 '내가 말하기' 시작 요청 무시 (경과: ${now - lastSttFinishTime}ms)")
                return
            }
            lastActionTime = now
            sttManager.cancelListening()
            ttsManager.stop()
            _activeSpeaker.value = ActiveSpeaker.ME
            _streamingText.value = ""
            ensureActiveSession()
            val locale = LanguageLocaleHelper.getLocaleForCode(myLang)
            // 🌟 내가 말하기 시에도 문장 중간의 호흡/생각으로 인한 조기 끊김 방지를 위해 장문 모드(isLongSpeech = true) 적용
            sttManager.startListening(locale, isLongSpeech = true)
        }
    }

    /**
     * 👂 '상대방 듣기' 토글 (상대방 언어로 청취)
     */
    fun toggleListenPartner() {
        val now = System.currentTimeMillis()
        if (_activeSpeaker.value == ActiveSpeaker.PARTNER) {
            // 이미 듣기 중이면 중지
            lastActionTime = now
            lastSttFinishTime = now
            sttManager.stopListening()
        } else {
            // 🛡️ 쿨다운 가드: AI 처리 중이거나, 종료/이전 액션 직후 600ms 이내 재진입 원천 차단 (Stop->Start 바운스 방지)
            if (_isProcessing.value) {
                Log.w(TAG, "🛡️ [Live 토글 가드]: 이전 발화 처리 중(_isProcessing=true)이므로 '상대방 듣기' 시작 요청 무시")
                return
            }
            if ((now - lastSttFinishTime) < ACTION_COOLDOWN_MS || (now - lastActionTime) < ACTION_COOLDOWN_MS) {
                Log.w(TAG, "🛡️ [Live 토글 가드]: 직전 STT 종료/액션 후 쿨다운(${ACTION_COOLDOWN_MS}ms) 미달로 '상대방 듣기' 시작 요청 무시 (경과: ${now - lastSttFinishTime}ms)")
                return
            }
            lastActionTime = now
            sttManager.cancelListening()
            ttsManager.stop()
            _activeSpeaker.value = ActiveSpeaker.PARTNER
            _streamingText.value = ""
            ensureActiveSession()
            val locale = LanguageLocaleHelper.getLocaleForCode(partnerLang)
            // 🌟 상대방 듣기 시 10초(10,000ms) 최대 무음 대기 시간 적용하여 긴 문장 조기 끊김 방지
            sttManager.startListening(locale, isLongSpeech = true)
        }
    }

    /**
     * 사용자가 명시적으로 발화 의도를 선택/변경할 때 호출됩니다.
     * 번역 중(isProcessing)에 덮어씌우기가 발생하면 즉시 번역을 재시작하고,
     * 이미 완료된 메시지일 경우 온디바이스 SLM을 통해 원문과 번역문 모두 완전한 문장 구조로 재작성합니다.
     */
    fun setIntentByUser(speakerType: ActiveSpeaker, intent: SpeechIntent) {
        val targetSender = if (speakerType == ActiveSpeaker.ME) LiveSender.ME else LiveSender.PARTNER
        if (speakerType == ActiveSpeaker.ME) {
            myIntent = intent
            if (intent != SpeechIntent.AUTO) {
                myDetectedIntent = intent
            }
        } else if (speakerType == ActiveSpeaker.PARTNER) {
            partnerIntent = intent
            if (intent != SpeechIntent.AUTO) {
                partnerDetectedIntent = intent
            }
        }
        
        if (_isProcessing.value) {
            Log.d(TAG, "🔄 [Live] 사용자 사후 교정 발생! 기존 번역 중단 후 재개: $intent")
            processJob?.cancel()
            val textToReprocess = lastProcessedText
            if (textToReprocess.isNotBlank()) {
                handleSttResult(textToReprocess, isReprocessing = true)
            }
        } else {
            // 🔄 번역이 이미 완료된 후 버튼을 눌렀을 경우:
            // 단순 부호 땜질이 아닌 온디바이스 SLM을 통한 원문 및 번역문 온전한 문장 재작성 (Whole-Sentence Rewrite)
            val lastMsg = _messages.value.lastOrNull()
            if (lastMsg != null && lastMsg.sender == targetSender && intent != SpeechIntent.AUTO) {
                scope.launch(Dispatchers.IO) {
                    _rephrasingMessageId.value = lastMsg.id
                    try {
                        val recentContext = _messages.value.takeLast(5).map { msg ->
                            val senderTag = if (msg.sender == LiveSender.ME) "User (${msg.sourceLang})" else "Partner (${msg.sourceLang})"
                            "$senderTag: \"${msg.rawText}\" -> \"${msg.refinedText}\""
                        }

                        val cleanTone = if (targetSender == LiveSender.ME) selectedTone.replace(Regex("""[^\p{L}\p{N}\s]"""), "").trim() else null
                        
                        // 🌟 최초 발화 원음(originalRawText)을 기준 삼아 의도를 재작성 (연쇄 변형 왜곡 원천 차단)
                        val baseSourceText = if (lastMsg.originalRawText.isNotBlank()) lastMsg.originalRawText else lastMsg.rawText
                        val (newRaw, newRefined) = intentEngine.rephraseMessageWithIntent(
                            rawSourceText = baseSourceText,
                            sourceLangCode = lastMsg.sourceLang,
                            targetLangCode = lastMsg.targetLang,
                            targetIntent = intent,
                            tone = cleanTone,
                            packageName = "ai.deartalk.android.live",
                            conversationContext = recentContext
                        )

                        val updatedMsg = lastMsg.copy(
                            rawText = baseSourceText,
                            refinedText = newRefined,
                            tone = intent.getLabel(),
                            originalRawText = baseSourceText
                        )

                        repository.updateMessage(updatedMsg)
                        withContext(Dispatchers.Main) {
                            val updatedList = _messages.value.toMutableList()
                            val lastIdx = updatedList.indexOfLast { it.id == updatedMsg.id }
                            if (lastIdx != -1) {
                                updatedList[lastIdx] = updatedMsg
                                _messages.value = updatedList
                            }
                        }
                        Log.d(TAG, "✨ [Live] AI 문장 전체 재작성 완료 (원음 기준: '$baseSourceText'): '${lastMsg.rawText}' ➔ '$newRaw' / '${lastMsg.refinedText}' ➔ '$newRefined'")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [Live] 사후 의도 재작성 실패: ${e.message}", e)
                    } finally {
                        _rephrasingMessageId.value = null
                    }
                }
            }
        }
    }

    /**
     * 🧠 STT 완료 후 온디바이스 SLM 추론 및 DB 저장, TTS 분기
     */
    private fun handleSttResult(rawText: String, isReprocessing: Boolean = false) {
        lastSttFinishTime = System.currentTimeMillis()
        val speaker = if (isReprocessing) _processingSpeaker.value else _activeSpeaker.value
        val cleanText = rawText.trim()

        // 🌟 1. 상태 즉시 선점 (Atomic State Lock) - 후속 중복 콜백의 재진입 원천 차단
        _activeSpeaker.value = ActiveSpeaker.NONE

        if (speaker == ActiveSpeaker.NONE || cleanText.isBlank()) {
            _streamingText.value = ""
            _processingSpeaker.value = ActiveSpeaker.NONE
            return
        }

        // 🌟 2. 디바운스 및 중복 방지 가드 (1.5초 이내 동일 텍스트 무시)
        val now = System.currentTimeMillis()
        if (!isReprocessing && cleanText == lastProcessedText && (now - lastProcessedTime) < 1500L) {
            Log.w(TAG, "🛡️ [Live 중복 억제]: 동일한 발화가 1.5초 내 중복 인입되어 무시됨: '$cleanText'")
            _streamingText.value = ""
            _processingSpeaker.value = ActiveSpeaker.NONE
            return
        }
        lastProcessedText = cleanText
        lastProcessedTime = now

        // 🌟 3. 부드러운 핸드오프: 번역 중에도 인식된 텍스트가 사라지지 않고 임시 버블에 유지됨
        _streamingText.value = cleanText
        _processingSpeaker.value = speaker

        // 🎯 1회성 발화 의도(Speech Pragmatics) 선점 (화자별 개별 인텐트 적용)
        val appliedIntent = if (speaker == ActiveSpeaker.PARTNER) partnerIntent else myIntent

        val previousJob = processJob
        processJob = scope.launch(Dispatchers.IO) {
            previousJob?.join()
            _isProcessing.value = true
            try {
                val session = ensureActiveSession()

                // 🏎️ [2-Track 하이브리드 통역: Track 1 초경량 초안 (0.01초 체감 지연)]
                val (srcLang, tgtLang, sender) = when (speaker) {
                    ActiveSpeaker.ME -> Triple(myLang, partnerLang, LiveSender.ME)
                    ActiveSpeaker.PARTNER -> Triple(partnerLang, myLang, LiveSender.PARTNER)
                    ActiveSpeaker.NONE -> return@launch
                }

                // 🏎️ [2-Track 하이브리드 통역: Track 1 Google ML Kit 온디바이스 NMT (50~100ms 체감 지연)]
                val mlkitDraft = ai.deartalk.android.live.translation.MlKitDraftTranslator.translate(
                    text = cleanText,
                    sourceLangCode = srcLang,
                    targetLangCode = tgtLang
                )

                val isDraftAvailable = !mlkitDraft.isNullOrBlank()
                val draftTranslation = mlkitDraft ?: ""

                val draftMessage = if (isDraftAvailable) {
                    val isDraftQuestion = ai.deartalk.android.stt.IntonationAnalyzer.isLikelyQuestion(cleanText, srcLang) || draftTranslation.endsWith("?")
                    val draftIntent = if (isDraftQuestion) SpeechIntent.QUESTION else SpeechIntent.STATEMENT
                    val draftFormattedSource = formatSourcePunctuation(cleanText, draftIntent, draftTranslation.endsWith("?"))

                    LiveMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        sessionId = session.id,
                        sender = sender,
                        rawText = draftFormattedSource,
                        refinedText = draftTranslation,
                        sourceLang = srcLang,
                        targetLang = tgtLang,
                        tone = if (appliedIntent != SpeechIntent.AUTO) appliedIntent.getLabel() else null,
                        originalRawText = cleanText,
                        isDraft = true,
                        draftText = draftTranslation
                    )
                } else null

                // ⚡ [Track 1 UI 즉시 렌더링]: ML Kit 초안이 준비된 경우 0.05초만에 등록 (대기 지연 0.1초 체감)
                if (draftMessage != null) {
                    repository.insertMessage(draftMessage)
                    _messages.value = (_messages.value + draftMessage).takeLast(LiveSessionRepository.MAX_RING_BUFFER_SIZE)
                    _streamingText.value = ""
                    _processingSpeaker.value = ActiveSpeaker.NONE
                }

                // 🧠 [Track 2 백그라운드 SLM 정밀 보정]: 문맥, 화행, 톤앤매너 정밀 반영 (1.8~2.5초)
                val cleanTone = if (partnerLang.equals(myLang, ignoreCase = true) && sender == LiveSender.ME) {
                    selectedTone.replace(Regex("""[^\p{L}\p{N}\s]"""), "").trim()
                } else null

                val (translation, detectedIntent) = intentEngine.translateWithIntent(
                    voiceInput = cleanText,
                    targetLangCode = tgtLang,
                    sourceLangCode = srcLang,
                    tone = cleanTone,
                    packageName = "ai.deartalk.android.live",
                    conversationContext = emptyList(),
                    speechIntent = appliedIntent
                )

                if (speaker == ActiveSpeaker.ME && appliedIntent == SpeechIntent.AUTO) {
                    myDetectedIntent = detectedIntent
                } else if (speaker == ActiveSpeaker.PARTNER && appliedIntent == SpeechIntent.AUTO) {
                    partnerDetectedIntent = detectedIntent
                }

                val detected = if (speaker == ActiveSpeaker.PARTNER) partnerDetectedIntent else myDetectedIntent
                val finalIntent = if (speaker == ActiveSpeaker.PARTNER) {
                    if (partnerIntent != SpeechIntent.AUTO) partnerIntent else detected ?: SpeechIntent.STATEMENT
                } else {
                    if (myIntent != SpeechIntent.AUTO) myIntent else detected ?: SpeechIntent.STATEMENT
                }

                val targetProfile = ai.deartalk.android.agent.language.LanguageProfileRegistry.get(tgtLang)
                val slmOutput = if (translation.isNotBlank()) translation else if (isDraftAvailable) draftTranslation else cleanText
                val finalRefined = targetProfile.applyPostProcessing(slmOutput, finalIntent)
                val isTranslationInterrogative = finalRefined.trim().trim('"', '\'', '`').trim().endsWith("?")
                val formattedSourceText = formatSourcePunctuation(
                    text = cleanText,
                    intent = finalIntent,
                    translationHasQuestion = isTranslationInterrogative
                )

                if (draftMessage != null) {
                    val finalizedMessage = draftMessage.copy(
                        rawText = formattedSourceText,
                        refinedText = finalRefined,
                        tone = if (finalIntent != SpeechIntent.AUTO) finalIntent.getLabel() else if (sender == LiveSender.ME) selectedTone else null,
                        isDraft = false,
                        draftText = draftTranslation
                    )
                    // 🌟 [Track 2 UI 갱신]: SLM 보정 완료 메시지로 전환 (변환전/후 비교 표시)
                    repository.updateMessage(finalizedMessage)
                    _messages.value = _messages.value.map { if (it.id == finalizedMessage.id) finalizedMessage else it }
                } else {
                    val finalizedMessage = LiveMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        sessionId = session.id,
                        sender = sender,
                        rawText = formattedSourceText,
                        refinedText = finalRefined,
                        sourceLang = srcLang,
                        targetLang = tgtLang,
                        tone = if (finalIntent != SpeechIntent.AUTO) finalIntent.getLabel() else if (sender == LiveSender.ME) selectedTone else null,
                        originalRawText = cleanText,
                        isDraft = false,
                        draftText = null
                    )
                    repository.insertMessage(finalizedMessage)
                    _messages.value = (_messages.value + finalizedMessage).takeLast(LiveSessionRepository.MAX_RING_BUFFER_SIZE)
                    _streamingText.value = ""
                    _processingSpeaker.value = ActiveSpeaker.NONE
                }

                // 자동 TTS 읽기 옵션 활성화 시 상대방 언어로 자동 발화
                if (isAutoSpeakEnabled && sender == LiveSender.ME && finalRefined.isNotBlank()) {
                    val ttsLang = LanguageLocaleHelper.detectLanguageCode(finalRefined, fallback = partnerLang)
                    ttsManager.speak(finalRefined, ttsLang, VoiceGender.FEMALE, DEFAULT_TTS_PITCH)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ [Live 처리 오류]: 세션(${_currentSession.value?.id}) 발화자($speaker) 처리 중 오류: ${e.message}", e)
            } finally {
                _isProcessing.value = false
                _streamingText.value = ""
                _processingSpeaker.value = ActiveSpeaker.NONE
                _activeSpeaker.value = ActiveSpeaker.NONE
                lastSttFinishTime = System.currentTimeMillis()

                // 🎯 [1회성 인텐트 자동 복귀]: 발화 및 번역 완료 후 다음 턴을 위해 '✨ 스마트' 모드로 즉각 복귀
                if (speaker == ActiveSpeaker.PARTNER) {
                    if (partnerIntent != SpeechIntent.AUTO) {
                        Log.d(TAG, "✨ [Live 상대방 인텐트 리셋]: '$partnerIntent' 발화 완료 ➔ '스마트' 모드로 자동 복귀")
                        partnerIntent = SpeechIntent.AUTO
                    }
                } else {
                    if (myIntent != SpeechIntent.AUTO) {
                        Log.d(TAG, "✨ [Live 내 인텐트 리셋]: '$myIntent' 발화 완료 ➔ '스마트' 모드로 자동 복귀")
                        myIntent = SpeechIntent.AUTO
                    }
                }
            }
        }
    }

    /**
     * - 현재 세션의 전체 대화 타임라인을 깔끔한 마크다운 리포트로 직렬화합니다.
     */
    fun exportCurrentSessionAsMarkdown(): String {
        val session = _currentSession.value ?: return "# DearTalk Live 대화 기록\n\n기록된 대화가 없습니다."
        val msgList = _messages.value
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val sb = StringBuilder()
        sb.appendLine("# 🎙️ DearTalk Live 대화 세션 기록")
        sb.appendLine()
        sb.appendLine("- **세션 ID**: `${session.id}`")
        sb.appendLine("- **세션 일시**: ${sdf.format(java.util.Date(session.createdAt))}")
        sb.appendLine("- **언어 설정**: ${session.myLang} ⇄ ${session.partnerLang}")
        sb.appendLine("- **총 메시지 수**: ${msgList.size}건")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        if (msgList.isEmpty()) {
            sb.appendLine("*기록된 대화 내용이 없습니다.*")
        } else {
            msgList.forEachIndexed { index, msg ->
                val speakerLabel = if (msg.sender == LiveSender.ME) "🙋 나" else "👤 상대방"
                val timeLabel = sdf.format(java.util.Date(msg.createdAt))
                sb.appendLine("### ${index + 1}. $speakerLabel ($timeLabel)")
                sb.appendLine("- **원문 (${msg.sourceLang})**: ${msg.rawText}")
                sb.appendLine("- **AI 번역/정제 (${msg.targetLang})**: ${msg.refinedText}")
                if (msg.tone != null && msg.sender == LiveSender.ME) {
                    sb.appendLine("- **적용 톤**: ${msg.tone}")
                }
                sb.appendLine()
            }
        }

        sb.appendLine("---")
        sb.appendLine("*Exported by DearTalk Live (100% On-Device AI)*")
        return sb.toString()
    }

    /**
     * 🧪 [테스트/검증 전용 인젝션 API]
     * 마이크 STT 없이 실제 대화 시나리오 텍스트를 주입하여
     * 온디바이스 SLM 추론, 2-Way 번역/정제, SQLite 적재, TTS 출력을 E2E로 검증합니다.
     */
    fun injectVoiceResult(speaker: ActiveSpeaker, rawText: String) {
        if (speaker == ActiveSpeaker.NONE || rawText.isBlank()) return
        _activeSpeaker.value = speaker
        _streamingText.value = rawText
        handleSttResult(rawText)
    }

    /**
     * 📸 [스크린샷 및 E2E 테스트 전용 완성형 대화 메시지 즉시 주입 API]
     * 대기 지연 없이 완성된 번역 메시지를 즉시 DB 적재 및 타임라인에 렌더링합니다.
     */
    fun insertCompletedTestMessage(
        speaker: ActiveSpeaker,
        rawText: String,
        refinedText: String,
        isDraft: Boolean = false
    ) {
        if (speaker == ActiveSpeaker.NONE || rawText.isBlank()) return
        val session = _currentSession.value ?: ensureActiveSession()
        val (srcLang, tgtLang, sender) = when (speaker) {
            ActiveSpeaker.ME -> Triple(myLang, partnerLang, LiveSender.ME)
            ActiveSpeaker.PARTNER -> Triple(partnerLang, myLang, LiveSender.PARTNER)
            ActiveSpeaker.NONE -> return
        }
        val msg = LiveMessage(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = session.id,
            sender = sender,
            rawText = rawText,
            refinedText = refinedText,
            sourceLang = srcLang,
            targetLang = tgtLang,
            tone = null,
            originalRawText = rawText,
            isDraft = isDraft,
            draftText = if (isDraft) refinedText else null
        )
        scope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            val updated = (_messages.value + msg).takeLast(LiveSessionRepository.MAX_RING_BUFFER_SIZE)
            withContext(Dispatchers.Main) {
                _messages.value = updated
                _streamingText.value = ""
                _processingSpeaker.value = ActiveSpeaker.NONE
                _activeSpeaker.value = ActiveSpeaker.NONE
                _isProcessing.value = false
            }
        }
    }

    /**
     * 🔄 언어 스왑 (내 언어 <-> 상대방 언어)
     */
    fun swapLanguages() {
        val temp = myLang
        myLang = partnerLang
        partnerLang = temp
        _currentSession.value?.let { session ->
            val updated = session.copy(myLang = myLang, partnerLang = partnerLang)
            _currentSession.value = updated
            scope.launch(Dispatchers.IO) {
                repository.createSession(updated) // 덮어쓰기/갱신
            }
        }
        prewarmAndDownloadLanguages(myLang, partnerLang)
    }

    /**
     * 🌐 언어 변경 시 즉각 저장 및 온디바이스 모델 자동 다운로드/프리웜 시작
     */
    fun setLanguages(my: String, partner: String) {
        myLang = my
        partnerLang = partner
        _currentSession.value?.let { session ->
            val updated = session.copy(myLang = myLang, partnerLang = partnerLang)
            _currentSession.value = updated
            scope.launch(Dispatchers.IO) {
                repository.createSession(updated)
            }
        }
        prewarmAndDownloadLanguages(my, partner)
    }

    /**
     * 🚀 선택된 언어의 온디바이스 STT 언어팩 자동 다운로드 및 TTS 보이스 사전 워밍업 (Zero-Wait Seamless)
     */
    fun prewarmAndDownloadLanguages(my: String = myLang, partner: String = partnerLang) {
        val myLocale = LanguageLocaleHelper.getLocaleForCode(my)
        val partnerLocale = LanguageLocaleHelper.getLocaleForCode(partner)

        Log.d(TAG, "🚀 [심리스 프리웜] 내 언어($my) & 상대 언어($partner) 온디바이스 언어팩 확인/다운로드 시작")

        // 1. STT 온디바이스 언어팩 자동 확인 및 백그라운드 선제 다운로드
        sttManager.ensureLanguageDownloaded(myLocale)
        sttManager.ensureLanguageDownloaded(partnerLocale)

        // 2. TTS 음성 합성기 사전 워밍업
        ttsManager.prewarmLanguage(myLocale)
        ttsManager.prewarmLanguage(partnerLocale)

        // 3. Google ML Kit 온디바이스 번역 모델 백그라운드 사전 다운로드/워밍업
        scope.launch(Dispatchers.IO) {
            ai.deartalk.android.live.translation.MlKitDraftTranslator.prewarm(my, partner)
        }
    }

    /**
     * 🔊 특정 메시지 다시 듣기
     */
    fun replayMessage(message: LiveMessage) {
        if (message.refinedText.isNotBlank()) {
            val lang = LanguageLocaleHelper.detectLanguageCode(message.refinedText, fallback = message.targetLang)
            ttsManager.speak(message.refinedText, lang, VoiceGender.FEMALE, DEFAULT_TTS_PITCH)
        }
    }

    /**
     * ➕ 신규 세션 / 비우기 (하위 호환)
     */
    fun createNewSession(my: String = myLang, partner: String = partnerLang): LiveSession {
        myLang = my
        partnerLang = partner
        clearTimeline()
        prewarmAndDownloadLanguages(my, partner)
        val session = _currentSession.value ?: LiveSession(
            id = LiveSessionRepository.DEFAULT_SESSION_ID,
            title = "DearTalk Live",
            myLang = my,
            partnerLang = partner
        )
        return session
    }

    /**
     * 📂 세션 로드 (하위 호환)
     */
    fun loadSession(session: LiveSession) {
        _currentSession.value = session
        myLang = session.myLang
        partnerLang = session.partnerLang
        prewarmAndDownloadLanguages(myLang, partnerLang)
        scope.launch(Dispatchers.IO) {
            val msgs = repository.getRecentMessages(LiveSessionRepository.MAX_RING_BUFFER_SIZE)
            withContext(Dispatchers.Main) {
                _messages.value = msgs
            }
        }
    }

    /**
     * 🗑️ 세션 삭제 -> 타임라인 비우기
     */
    fun deleteSession(sessionId: String) {
        clearTimeline()
    }

    fun purgeExpiredSessions() {
        // 링 버퍼 아키텍처 도입으로 자동 만료 워커 불필요
    }

    /**
     * ⚠️ 모든 대화 기록 즉시 비우기
     */
    fun deleteAllSessions() {
        clearTimeline()
    }

    /**
     * 💬 대화록 전문 텍스트 포맷팅 (카카오톡 및 시스템 공유용)
     */
    fun getFormattedTranscript(): String {
        val session = _currentSession.value ?: return ""
        val msgs = _messages.value
        val sb = StringBuilder()
        sb.appendLine("🎙️ [DearTalk Live] ${session.title}")
        sb.appendLine("언어: ${session.myLang} ↔ ${session.partnerLang}")
        sb.appendLine("일시: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(session.createdAt))}")
        sb.appendLine("----------------------------------------")
        if (msgs.isEmpty()) {
            sb.appendLine("(대화 내용 없음)")
        } else {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            msgs.forEach { msg ->
                val speakerLabel = if (msg.sender == LiveSender.ME) "👤 나 (${session.myLang})" else "👥 상대방 (${session.partnerLang})"
                val timeStr = timeFormat.format(Date(msg.createdAt))
                sb.appendLine("[$timeStr] $speakerLabel")
                sb.appendLine("🗣️ ${msg.rawText}")
                if (msg.refinedText.isNotBlank()) {
                    sb.appendLine("✨ ${msg.refinedText}")
                }
                sb.appendLine()
            }
        }
        sb.appendLine("----------------------------------------")
        sb.appendLine("DearTalk AI - On-Device Live Translation")
        return sb.toString()
    }

    /**
     * 📤 대화록 공유 (카카오톡, 문자, 이메일 등 시스템 공유 시트)
     */
    fun shareTranscript(activityContext: Context = context) {
        val text = getFormattedTranscript()
        if (text.isBlank()) return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, _currentSession.value?.title ?: "DearTalk Live 대화록")
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "DearTalk Live 대화록 공유").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activityContext.startActivity(chooser)
    }

    fun refreshSessions() {
        scope.launch(Dispatchers.IO) {
            val list = repository.getAllSessions()
            withContext(Dispatchers.Main) {
                _sessions.value = list
                if (_currentSession.value == null) {
                    if (list.isNotEmpty()) {
                        loadSession(list.first())
                    } else {
                        createNewSession()
                    }
                }
            }
        }
    }

    private fun ensureActiveSession(): LiveSession {
        return _currentSession.value ?: createNewSession()
    }

    fun stop() {
        sttManager.cancelListening()
        ttsManager.stop()
        _activeSpeaker.value = ActiveSpeaker.NONE
        _streamingText.value = ""
    }

    fun release() {
        destroy()
    }

    fun destroy() {
        stop()
        processJob?.cancel()
        controllerJob.cancel()
        _isProcessing.value = false
        ai.deartalk.android.live.translation.MlKitDraftTranslator.close()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
