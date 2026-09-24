package ai.deartalk.android.stt

import ai.deartalk.android.crash.CrashLogger
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

sealed interface VoiceState {
    object Idle : VoiceState
    object Preparing : VoiceState
    object Listening : VoiceState
    data class PartialResult(val text: String) : VoiceState
    data class FinalResult(val text: String) : VoiceState
    data class RmsChanged(val rmsDb: Float) : VoiceState
    data class Error(val errorCode: Int) : VoiceState
}

/**
 * 🌐 온디바이스 언어팩 다운로드 및 가용성 상태
 */
sealed interface LanguageModelStatus {
    object Idle : LanguageModelStatus
    object Checking : LanguageModelStatus
    object Installed : LanguageModelStatus
    data class Downloading(val progress: Int) : LanguageModelStatus
    object Scheduled : LanguageModelStatus
    object SupportedOnline : LanguageModelStatus
    data class Error(val code: Int) : LanguageModelStatus
}

/**
 * 안드로이드 표준 SpeechRecognizer 음성 인식 관리자
 * - 세션 단위 생명주기 관리 및 원격 서비스 단절(Error 11) 자동 복구 탑재
 * - Android 14+ 온디바이스 언어팩 자동 확인 및 백그라운드 선제 다운로드 지원
 */
class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognition"
        private const val ERROR_SERVER_DISCONNECTED = 11
        private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        private const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    // 🌐 언어별 모델 가용성 / 다운로드 상태 맵
    private val activeDownloadRecognizers = ConcurrentHashMap<String, SpeechRecognizer>()
    private val _modelDownloadStatus = MutableStateFlow<Map<String, LanguageModelStatus>>(emptyMap())
    val modelDownloadStatus: StateFlow<Map<String, LanguageModelStatus>> = _modelDownloadStatus.asStateFlow()

    private var isUserIntentionallyListening = false
    private var currentListeningLocale: Locale = Locale.KOREAN
    private var currentIsLongSpeech: Boolean = false
    private var lastRecognizedText: String = ""
    private var retryCount = 0

    private val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private fun createSpeechRecognizerInstance(): SpeechRecognizer {
        Log.d(TAG, "🎙️ 시스템 기본 SpeechRecognizer 생성 (context: ${context.packageName})")
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Throwable) {
        } finally {
            speechRecognizer = null
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "🎙️ 마이크 준비 완료 (Ready for Speech) ➔ 지금 말씀하세요!")
                _voiceState.value = VoiceState.Listening
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🗣️ 사용자 음성 감지 시작")
                _voiceState.value = VoiceState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsDb.value = rmsdB
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "🤫 음성 감지 일시 정지 ➔ 처리 중")
            }

            override fun onError(error: Int) {
                Log.w(TAG, "⚠️ STT 에러 감지 (코드: $error, 사용자 청취 의도: $isUserIntentionallyListening, 재시도: $retryCount)")
                
                // 에러 발생 시 즉시 죽은 바인더 리소스 완전 정리
                destroyRecognizerInternal()

                if (lastRecognizedText.isNotBlank()) {
                    isUserIntentionallyListening = false
                    retryCount = 0
                    val textToEmit = lastRecognizedText
                    lastRecognizedText = ""
                    _voiceState.value = VoiceState.FinalResult(textToEmit)
                    return
                }

                // 🌟 원격 서비스 단절(11) 또는 클라이언트 바인더 에러(5) 시 자동 1회 복구 재시도
                if (isUserIntentionallyListening && (error == ERROR_SERVER_DISCONNECTED || error == SpeechRecognizer.ERROR_CLIENT) && retryCount < 1) {
                    retryCount++
                    Log.i(TAG, "🔄 원격 서비스 단절(코드: $error) 감지 ➔ 클린 세션으로 즉시 자동 복구 재시도 (장문모드: $currentIsLongSpeech)")
                    mainHandler.postDelayed({
                        if (isUserIntentionallyListening) {
                            startListeningInternal(currentListeningLocale, currentIsLongSpeech)
                        }
                    }, 100)
                    return
                }

                isUserIntentionallyListening = false
                retryCount = 0
                lastRecognizedText = ""

                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    _voiceState.value = VoiceState.Idle
                } else {
                    _voiceState.value = VoiceState.Error(error)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: lastRecognizedText
                Log.d(TAG, "✅ STT 최종 결과 수신: '$recognizedText'")

                isUserIntentionallyListening = false
                retryCount = 0
                lastRecognizedText = "" // 🌟 잔여 텍스트 즉시 비움 (이중 방출 및 onError 중복 방지)

                // 결과 수신 완료 즉시 recognizer 리소스 해제하여 백그라운드 원격 서비스 타임아웃 단절 방지
                destroyRecognizerInternal()

                if (recognizedText.isNotBlank()) {
                    _voiceState.value = VoiceState.FinalResult(recognizedText)
                } else {
                    _voiceState.value = VoiceState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    lastRecognizedText = text
                    Log.d(TAG, "💬 STT 중간 결과: '$text'")
                    _voiceState.value = VoiceState.PartialResult(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(
        locale: Locale = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(context),
        isLongSpeech: Boolean = false
    ) {
        currentListeningLocale = locale
        currentIsLongSpeech = isLongSpeech
        isUserIntentionallyListening = true
        retryCount = 0
        lastRecognizedText = ""
        mainHandler.post {
            startListeningInternal(locale, isLongSpeech)
        }
    }

    private fun startListeningInternal(locale: Locale, isLongSpeech: Boolean = false) {
        if (!isRecognitionAvailable) {
            Log.e(TAG, "❌ SpeechRecognizer 사용 불가")
            _voiceState.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT)
            isUserIntentionallyListening = false
            return
        }

        _voiceState.value = VoiceState.Preparing

        // 기존에 남아있을 수 있는 이전 recognizer 인스턴스 완전 파괴 및 정리
        destroyRecognizerInternal()

        try {
            val recognizer = createSpeechRecognizerInstance()
            recognizer.setRecognitionListener(createListener())
            speechRecognizer = recognizer

            val langTag = ai.deartalk.android.util.LanguageLocaleHelper.getLanguageTag(locale)

            // 🌟 발화 중 호흡이나 단어 생각으로 인한 끊김을 원천 차단하기 위해 단일 10초(10,000ms) 안전 대기 적용
            // (말씀이 끝났을 때는 사용자가 [완료] 또는 [입력] 버튼을 터치하여 0ms 만에 즉시 전송)
            val completeSilence = 10000L
            val possibleSilence = 8000L
            val minLength = 2000L

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                if (langTag != "en-US") {
                    putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", arrayOf("en-US"))
                }
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, completeSilence)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, possibleSilence)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minLength)
            }

            Log.d(TAG, "🚀 startListening 실행 (언어: $langTag, 장문대화모드: $isLongSpeech, 무음한계: ${completeSilence}ms)")
            recognizer.startListening(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "❌ startListening 실행 실패: ${e.message}")
            destroyRecognizerInternal()
            _voiceState.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT)
            isUserIntentionallyListening = false
        }
    }

    fun stopListening() {
        isUserIntentionallyListening = false
        retryCount = 0
        mainHandler.post {
            try {
                if (speechRecognizer != null) {
                    // 🌟 인스턴스가 활성 상태이면 안드로이드 시스템 음성 엔진에 정상 정지 요청 -> onResults 콜백으로 정식 수신 유도
                    speechRecognizer?.stopListening()
                } else if (lastRecognizedText.isNotBlank()) {
                    // 🌟 recognizer가 이미 정리된 경우에만 안전 폴백으로 1회 방출
                    val fallbackText = lastRecognizedText
                    lastRecognizedText = ""
                    _voiceState.value = VoiceState.FinalResult(fallbackText)
                } else {
                    _voiceState.value = VoiceState.Idle
                }
            } catch (t: Throwable) {
                CrashLogger.logHandledException("SpeechRecognitionManager.stopListening", "SpeechRecognizer stopListening failure", t)
                if (lastRecognizedText.isNotBlank()) {
                    val fallbackText = lastRecognizedText
                    lastRecognizedText = ""
                    _voiceState.value = VoiceState.FinalResult(fallbackText)
                }
                destroyRecognizerInternal()
            }
        }
    }

    fun cancelListening() {
        isUserIntentionallyListening = false
        retryCount = 0
        lastRecognizedText = ""
        mainHandler.post {
            destroyRecognizerInternal()
            _voiceState.value = VoiceState.Idle
        }
    }

    /**
     * 🌐 언어 선택 시 온디바이스 언어팩 자동 점검 및 백그라운드 선제 다운로드 (Seamless Auto-Download)
     */
    fun ensureLanguageDownloaded(locale: Locale) {
        val langTag = ai.deartalk.android.util.LanguageLocaleHelper.getLanguageTag(locale)
        val currentStatus = _modelDownloadStatus.value[langTag]
        if (currentStatus is LanguageModelStatus.Installed || currentStatus is LanguageModelStatus.Downloading) {
            Log.d(TAG, "⚡ [$langTag] 이미 언어팩이 준비되었거나 다운로드 진행 중입니다 ($currentStatus)")
            return
        }

        _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Checking) }
        Log.d(TAG, "🌐 [$langTag] 언어팩 가용성 확인 및 자동 다운로드 프로세스 시작")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ (API 34+)
            mainHandler.post {
                try {
                    val recognizer = createSpeechRecognizerInstance()
                    activeDownloadRecognizers[langTag] = recognizer

                    recognizer.checkRecognitionSupport(
                        intent,
                        androidx.core.content.ContextCompat.getMainExecutor(context),
                        object : android.speech.RecognitionSupportCallback {
                            override fun onSupportResult(recognitionSupport: android.speech.RecognitionSupport) {
                                val installed = recognitionSupport.installedOnDeviceLanguages
                                val supported = recognitionSupport.supportedOnDeviceLanguages
                                val pending = recognitionSupport.pendingOnDeviceLanguages

                                Log.d(TAG, "🔍 [$langTag] 지원 현황: 설치됨=${installed.contains(langTag)}, 지원됨=${supported.contains(langTag)}, 대기중=${pending.contains(langTag)}")

                                if (installed.contains(langTag)) {
                                    _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Installed) }
                                    activeDownloadRecognizers.remove(langTag)?.destroy()
                                } else if (pending.contains(langTag)) {
                                    _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Scheduled) }
                                    activeDownloadRecognizers.remove(langTag)?.destroy()
                                } else if (supported.contains(langTag)) {
                                    Log.i(TAG, "📥 [$langTag] 온디바이스 언어팩 백그라운드 자동 다운로드 트리거!")
                                    _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Scheduled) }

                                    recognizer.triggerModelDownload(
                                        intent,
                                        androidx.core.content.ContextCompat.getMainExecutor(context),
                                        object : android.speech.ModelDownloadListener {
                                            override fun onProgress(progress: Int) {
                                                Log.d(TAG, "📦 [$langTag] 다운로드 진행 중: $progress%")
                                                _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Downloading(progress)) }
                                            }

                                            override fun onSuccess() {
                                                Log.i(TAG, "🎉 [$langTag] 온디바이스 언어팩 다운로드 성공!")
                                                _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Installed) }
                                                activeDownloadRecognizers.remove(langTag)?.destroy()
                                            }

                                            override fun onScheduled() {
                                                Log.d(TAG, "⏳ [$langTag] 다운로드 작업 예약됨")
                                                _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.Scheduled) }
                                            }

                                            override fun onError(error: Int) {
                                                Log.w(TAG, "⚠️ [$langTag] 언어팩 다운로드 오류($error) ➔ 하이브리드 스트리밍 보장")
                                                _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.SupportedOnline) }
                                                activeDownloadRecognizers.remove(langTag)?.destroy()
                                            }
                                        }
                                    )
                                } else {
                                    Log.d(TAG, "🌐 [$langTag] 온디바이스 미지원 ➔ 하이브리드 스트리밍 모드")
                                    _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.SupportedOnline) }
                                    activeDownloadRecognizers.remove(langTag)?.destroy()
                                }
                            }

                            override fun onError(error: Int) {
                                Log.w(TAG, "⚠️ [$langTag] checkRecognitionSupport 오류($error) ➔ 하이브리드 스트리밍")
                                _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.SupportedOnline) }
                                activeDownloadRecognizers.remove(langTag)?.destroy()
                            }
                        }
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "⚠️ [$langTag] ensureLanguageDownloaded 예외: ${e.message}")
                    _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.SupportedOnline) }
                    activeDownloadRecognizers.remove(langTag)?.destroy()
                }
            }
        } else {
            _modelDownloadStatus.update { it + (langTag to LanguageModelStatus.SupportedOnline) }
        }
    }

    fun destroy() {
        isUserIntentionallyListening = false
        retryCount = 0
        mainHandler.post {
            destroyRecognizerInternal()
            activeDownloadRecognizers.values.forEach { 
                try { it.destroy() } catch (_: Throwable) {}
            }
            activeDownloadRecognizers.clear()
            _voiceState.value = VoiceState.Idle
        }
    }

    fun resetState() {
        isUserIntentionallyListening = false
        retryCount = 0
        _voiceState.value = VoiceState.Idle
    }
}
