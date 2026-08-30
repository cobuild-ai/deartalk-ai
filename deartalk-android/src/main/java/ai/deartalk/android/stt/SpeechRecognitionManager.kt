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
 * - 단일 인스턴스 유지 및 메인 루퍼 동기화로 오디오 락 및 NO_MATCH(7) 레이스 컨디션 방지
 */
class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognition"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

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
                _voiceState.value = VoiceState.RmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "🤫 음성 감지 종료 ➔ 인식 처리 중...")
            }

            override fun onError(error: Int) {
                Log.e(TAG, "⚠️ STT 에러 발생 (코드: $error)")
                _voiceState.value = VoiceState.Error(error)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                Log.d(TAG, "✅ STT 최종 결과 수신: '$recognizedText'")
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
                    Log.d(TAG, "💬 STT 중간 결과: '$text'")
                    _voiceState.value = VoiceState.PartialResult(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(locale: Locale = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(context)) {
        mainHandler.post {
            if (!isRecognitionAvailable) {
                Log.e(TAG, "❌ SpeechRecognizer 사용 불가")
                _voiceState.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT)
                return@post
            }

            _voiceState.value = VoiceState.Preparing

            ensureRecognizerInitialized()

            try {
                // 이전 세션 취소하여 깔끔한 상태로 시작
                speechRecognizer?.cancel()

                val langTag = when (locale.language) {
                    "ko" -> "ko-KR"
                    "en" -> "en-US"
                    "ja" -> "ja-JP"
                    "zh" -> if (locale.country == "TW") "zh-TW" else "zh-CN"
                    "id" -> "id-ID"
                    "es" -> "es-ES"
                    "fr" -> "fr-FR"
                    "de" -> "de-DE"
                    "vi" -> "vi-VN"
                    else -> locale.toLanguageTag().ifBlank { "ko-KR" }
                }

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
                }

                Log.d(TAG, "🚀 startListening 호출 (언어: $langTag)")
                speechRecognizer?.startListening(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "❌ startListening 실행 실패: ${e.message}")
                _voiceState.value = VoiceState.Error(SpeechRecognizer.ERROR_CLIENT)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Throwable) {}
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}
            _voiceState.value = VoiceState.Idle
        }
    }

    fun destroy() {
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
        _voiceState.value = VoiceState.Idle
    }
}
