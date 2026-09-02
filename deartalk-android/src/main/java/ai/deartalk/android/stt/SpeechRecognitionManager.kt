package ai.deartalk.android.stt

import android.content.Context
import android.content.Intent
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
import java.util.Locale

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
 * 안드로이드 표준 SpeechRecognizer 음성 인식 관리자
 * - 단일 인스턴스 유지 및 무음 타임아웃 자동 재연결(Keep-Alive)로 녹음 끊김 방지
 */
class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognition"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private var isUserIntentionallyListening = false
    private var currentListeningLocale: Locale = Locale.KOREAN
    private var lastRecognizedText: String = ""

    private val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        mainHandler.post {
            ensureRecognizerInitialized()
        }
    }

    private fun ensureRecognizerInitialized() {
        if (speechRecognizer == null && isRecognitionAvailable) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                Log.d(TAG, "🎙️ SpeechRecognizer 인스턴스 초기화 완료")
            } catch (e: Throwable) {
                Log.e(TAG, "❌ SpeechRecognizer 초기화 실패: ${e.message}")
            }
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
                Log.w(TAG, "⚠️ STT 에러/타임아웃 감지 (코드: $error, 사용자 청취 의도: $isUserIntentionallyListening)")
                
                isUserIntentionallyListening = false

                // 침묵 또는 미인식 시 무한 재시작 루프를 돌지 않고, 인식된 텍스트가 있으면 전달하고 없으면 세션을 정상 종료
                if (lastRecognizedText.isNotBlank()) {
                    _voiceState.value = VoiceState.FinalResult(lastRecognizedText)
                } else if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
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

    fun startListening(locale: Locale = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(context)) {
        currentListeningLocale = locale
        isUserIntentionallyListening = true
        lastRecognizedText = ""
        mainHandler.post {
            startListeningInternal(locale)
        }
    }

    private fun startListeningInternal(locale: Locale) {
        if (!isRecognitionAvailable) {
            Log.e(TAG, "❌ SpeechRecognizer 사용 불가")
            _voiceState.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT)
            isUserIntentionallyListening = false
            return
        }

        _voiceState.value = VoiceState.Preparing

        ensureRecognizerInitialized()

        try {
            speechRecognizer?.cancel()

            val langTag = ai.deartalk.android.util.LanguageLocaleHelper.getLanguageTag(locale)

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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            Log.d(TAG, "🚀 startListening 실행 (언어: $langTag)")
            speechRecognizer?.startListening(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "❌ startListening 실행 실패: ${e.message}")
            _voiceState.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT)
            isUserIntentionallyListening = false
        }
    }

    fun stopListening() {
        isUserIntentionallyListening = false
        mainHandler.post {
            try {
                if (lastRecognizedText.isNotBlank()) {
                    _voiceState.value = VoiceState.FinalResult(lastRecognizedText)
                }
                speechRecognizer?.stopListening()
            } catch (_: Throwable) {}
        }
    }

    fun cancelListening() {
        isUserIntentionallyListening = false
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}
            _voiceState.value = VoiceState.Idle
        }
    }

    fun destroy() {
        isUserIntentionallyListening = false
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null
            _voiceState.value = VoiceState.Idle
        }
    }

    fun resetState() {
        isUserIntentionallyListening = false
        _voiceState.value = VoiceState.Idle
    }
}
