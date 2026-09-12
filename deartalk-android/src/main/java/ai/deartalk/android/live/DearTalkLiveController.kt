package ai.deartalk.android.live

import android.content.Context
import android.util.Log
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import ai.deartalk.android.live.data.LiveSession
import ai.deartalk.android.live.data.LiveSessionRepository
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.tts.TextToSpeechManager
import ai.deartalk.android.tts.VoiceGender
import ai.deartalk.android.util.LanguageLocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var processJob: Job? = null

    // UI 관찰 상태
    private val _currentSession = MutableStateFlow<LiveSession?>(null)
    val currentSession: StateFlow<LiveSession?> = _currentSession.asStateFlow()

    private val _messages = MutableStateFlow<List<LiveMessage>>(emptyList())
    val messages: StateFlow<List<LiveMessage>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<LiveSession>>(emptyList())
    val sessions: StateFlow<List<LiveSession>> = _sessions.asStateFlow()

    private val _activeSpeaker = MutableStateFlow(ActiveSpeaker.NONE)
    val activeSpeaker: StateFlow<ActiveSpeaker> = _activeSpeaker.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isContinuousListening = MutableStateFlow(false)
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    val rmsDb: StateFlow<Float> = sttManager.rmsDb

    // 🌐 언어별 모델 가용성 및 온디바이스 다운로드 진행 상태
    val modelDownloadStatus: StateFlow<Map<String, ai.deartalk.android.stt.LanguageModelStatus>> = sttManager.modelDownloadStatus

    // 사용자 설정 (내 언어 / 상대방 언어 / 톤) - Compose 반응형 상태
    var myLang: String by mutableStateOf("KO")
        private set
    var partnerLang: String by mutableStateOf("EN")
        private set
    var selectedTone: String by mutableStateOf("✨ 기본다듬기")

    init {
        observeStt()
        refreshSessions()
        prewarmAndDownloadLanguages(myLang, partnerLang)
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
                    }
                    is VoiceState.Idle -> {
                        if (!_isProcessing.value) {
                            _activeSpeaker.value = ActiveSpeaker.NONE
                        }
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
        if (_activeSpeaker.value == ActiveSpeaker.ME) {
            sttManager.stopListening()
        } else {
            sttManager.cancelListening()
            ttsManager.stop()
            _activeSpeaker.value = ActiveSpeaker.ME
            _streamingText.value = ""
            ensureActiveSession()
            val locale = LanguageLocaleHelper.getLocaleForCode(myLang)
            sttManager.startListening(locale)
        }
    }

    /**
     * 👂 '상대방 듣기' 토글 (상대방 언어로 청취)
     */
    fun toggleListenPartner() {
        if (_activeSpeaker.value == ActiveSpeaker.PARTNER) {
            sttManager.stopListening()
        } else {
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
     * 🧠 STT 완료 후 온디바이스 SLM 추론 및 DB 저장, TTS 분기
     */
    private fun handleSttResult(rawText: String) {
        val speaker = _activeSpeaker.value
        if (speaker == ActiveSpeaker.NONE || rawText.isBlank()) {
            _activeSpeaker.value = ActiveSpeaker.NONE
            _streamingText.value = ""
            return
        }

        val previousJob = processJob
        processJob = scope.launch(Dispatchers.IO) {
            previousJob?.join()
            _isProcessing.value = true
            try {
                val session = ensureActiveSession()

                // 🌟 [대화 맥락 파악 및 ASR 음성 오인식 보정]
                // 현재 세션의 최근 5개 메시지를 추출하여 대화 흐름 컨텍스트 구성
                val recentContext = _messages.value.takeLast(5).map { msg ->
                    val senderTag = if (msg.sender == LiveSender.ME) "User (${msg.sourceLang})" else "Partner (${msg.sourceLang})"
                    "$senderTag: \"${msg.rawText}\" -> \"${msg.refinedText}\""
                }

                val (refined, srcLang, tgtLang, sender) = when (speaker) {
                    ActiveSpeaker.ME -> {
                        // 내가 말함: 내 언어 -> 상대방 언어로 번역 (대화 맥락 기반 ASR 오류 자동 보정)
                        val translation = intentEngine.translate(
                            voiceInput = rawText,
                            targetLangCode = partnerLang,
                            sourceLangCode = myLang,
                            tone = if (partnerLang.equals(myLang, ignoreCase = true)) selectedTone else null,
                            packageName = "ai.deartalk.android.live",
                            conversationContext = recentContext
                        )
                        val resultText = if (translation.isNotBlank()) translation else rawText
                        Quadruple(resultText, myLang, partnerLang, LiveSender.ME)
                    }
                    ActiveSpeaker.PARTNER -> {
                        // 상대방이 말함: 상대방 언어 -> 내 언어로 번역 (대화 맥락 기반 ASR 오류 자동 보정)
                        val translation = intentEngine.translate(
                            voiceInput = rawText,
                            targetLangCode = myLang,
                            sourceLangCode = partnerLang,
                            tone = null,
                            packageName = "ai.deartalk.android.live",
                            conversationContext = recentContext
                        )
                        val resultText = if (translation.isNotBlank()) translation else rawText
                        Quadruple(resultText, partnerLang, myLang, LiveSender.PARTNER)
                    }
                    ActiveSpeaker.NONE -> return@launch
                }

                val message = LiveMessage(
                    sessionId = session.id,
                    sender = sender,
                    rawText = rawText,
                    refinedText = refined,
                    sourceLang = srcLang,
                    targetLang = tgtLang,
                    tone = if (sender == LiveSender.ME) selectedTone else null
                )

                // 1. SQLite 저장
                repository.insertMessage(message)

                // 2. UI 타임라인 갱신
                val updatedList = _messages.value + message
                _messages.value = updatedList

                // 3. 내가 말한 경우 상대방 언어로 자동 TTS 발화
                if (sender == LiveSender.ME && refined.isNotBlank()) {
                    val ttsLang = LanguageLocaleHelper.detectLanguageCode(refined, fallback = partnerLang)
                    ttsManager.speak(refined, ttsLang, VoiceGender.FEMALE, DEFAULT_TTS_PITCH)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ [Live 처리 오류]: 세션(${_currentSession.value?.id}) 발화자($speaker) 처리 중 오류: ${e.message}", e)
            } finally {
                _isProcessing.value = false
                _activeSpeaker.value = ActiveSpeaker.NONE
                _streamingText.value = ""

                // 🔁 [연속 청취/강의 모드] 처리 완료 후 상대방 발화 연속 수신 자동 재개
                if (_isContinuousListening.value) {
                    scope.launch {
                        kotlinx.coroutines.delay(600)
                        if (_isContinuousListening.value && _activeSpeaker.value == ActiveSpeaker.NONE) {
                            toggleListenPartner()
                        }
                    }
                }
            }
        }
    }

    /**
     * 🔁 연속 청취 / 강의 레코더 모드 토글
     * - 활성화 시 상대방(강의자/외국인)의 발화를 연속으로 청취하고 기록/번역합니다.
     */
    fun toggleContinuousListening() {
        val nextState = !_isContinuousListening.value
        _isContinuousListening.value = nextState
        if (nextState) {
            if (_activeSpeaker.value != ActiveSpeaker.PARTNER) {
                toggleListenPartner()
            }
        } else {
            if (_activeSpeaker.value == ActiveSpeaker.PARTNER) {
                sttManager.stopListening()
            }
        }
    }

    /**
     * 📄 대화록 마크다운(Markdown) 내보내기
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
     * ➕ 신규 세션 생성
     */
    fun createNewSession(my: String = myLang, partner: String = partnerLang): LiveSession {
        val session = LiveSession(myLang = my, partnerLang = partner)
        _currentSession.value = session
        _messages.value = emptyList()
        scope.launch(Dispatchers.IO) {
            repository.createSession(session)
            refreshSessions()
        }
        prewarmAndDownloadLanguages(my, partner)
        return session
    }

    /**
     * 📂 이전 세션 로드
     */
    fun loadSession(session: LiveSession) {
        _currentSession.value = session
        myLang = session.myLang
        partnerLang = session.partnerLang
        prewarmAndDownloadLanguages(myLang, partnerLang)
        scope.launch(Dispatchers.IO) {
            val msgs = repository.getMessagesForSession(session.id)
            _messages.value = msgs
        }
    }

    /**
     * 🗑️ 세션 삭제
     */
    fun deleteSession(sessionId: String) {
        scope.launch(Dispatchers.IO) {
            repository.deleteSession(sessionId)
            if (_currentSession.value?.id == sessionId) {
                createNewSession()
            }
            refreshSessions()
        }
    }

    fun refreshSessions() {
        scope.launch(Dispatchers.IO) {
            val list = repository.getAllSessions()
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

    private fun ensureActiveSession(): LiveSession {
        return _currentSession.value ?: createNewSession()
    }

    fun stop() {
        sttManager.cancelListening()
        ttsManager.stop()
        processJob?.cancel()
        _activeSpeaker.value = ActiveSpeaker.NONE
        _isProcessing.value = false
        _streamingText.value = ""
    }

    fun destroy() {
        stop()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
