package ai.deartalk.android.agent

import android.content.Context
import android.util.Log
import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class VoicePipelineStage {
    object Idle : VoicePipelineStage()
    object Recording : VoicePipelineStage()
    data class ProcessingSTT(val progress: Float) : VoicePipelineStage()
    data class RefiningLLM(val rawText: String) : VoicePipelineStage()
    data class SynthesizingTTS(val aiText: String) : VoicePipelineStage()
    data class Completed(
        val rawText: String,
        val aiText: String,
        val targetLang: String,
        val isQwenEngine: Boolean
    ) : VoicePipelineStage()
    data class Error(val message: String) : VoicePipelineStage()
}

/**
 * 🛡️ 메모리 보존 순차 음성 처리 파이프라인 (Sequential Voice Pipeline)
 * [음성인식 STT ➔ 텍스트 생성 LLM ➔ 음성합성 TTS]를 직렬로 연결하여
 * 최대 동시 RAM 사용량을 1.5GB 미만으로 통제합니다.
 */
class SequentialVoicePipeline(
    private val context: Context,
    private val intentEngine: DearTalkIntentEngine,
    private val ttsManager: TextToSpeechManager,
    private val modelLifecycleManager: ModelLifecycleManager
) {
    companion object {
        private const val TAG = "SequentialVoicePipeline"
    }

    private val pipelineScope = CoroutineScope(Dispatchers.IO)
    private var currentJob: Job? = null

    private val _stage = MutableStateFlow<VoicePipelineStage>(VoicePipelineStage.Idle)
    val stage: StateFlow<VoicePipelineStage> = _stage.asStateFlow()

    /**
     * 🚀 음성 파이프라인 실행
     * @param simulatedVoiceText 직접 입력받은 음성 텍스트 (또는 STT 결과)
     * @param targetLang 목표 언어 코드 (KO, EN, JA, ZH, TH, ID)
     * @param sourceLang 입력 언어 코드
     * @param tone 톤앤매너 (기본 다듬기, 정중하게, 다정하게, 당당하게 등)
     * @param autoSpeak TTS 자동 발화 여부
     */
    fun processVoiceInput(
        simulatedVoiceText: String,
        targetLang: String = "KO",
        sourceLang: String = "KO",
        tone: String? = null,
        autoSpeak: Boolean = true,
        gender: ai.deartalk.android.tts.VoiceGender = ai.deartalk.android.tts.VoiceGender.FEMALE,
        pitch: Float = 1.0f
    ) {
        currentJob?.cancel()
        currentJob = pipelineScope.launch {
            try {
                if (simulatedVoiceText.isBlank()) {
                    _stage.value = VoicePipelineStage.Error(ai.deartalk.android.data.pref.UiStrings.noSpeechDetected)
                    return@launch
                }

                val isQwen = modelLifecycleManager.isInstalled()
                Log.d(TAG, "🎙️ [1단계: STT 음성인식 완료] 원문: '$simulatedVoiceText' (입력언어: $sourceLang, 출력언어: $targetLang, Qwen: $isQwen)")
                _stage.value = VoicePipelineStage.ProcessingSTT(1.0f)
                delay(80) // UI 반응용 마이크로 딜레이

                // 2단계: LLM 문맥 정제 또는 다국어 번역
                Log.d(TAG, "🧠 [2단계: LLM 문맥/번역 시작] $sourceLang ➔ $targetLang (톤: $tone)")
                _stage.value = VoicePipelineStage.RefiningLLM(simulatedVoiceText)

                val refinedText = if (targetLang.equals("KO", ignoreCase = true) && tone == null && sourceLang.equals("KO", ignoreCase = true)) {
                    when (val res = intentEngine.processIntent(simulatedVoiceText)) {
                        is IntentResult.Success -> res.text
                        is IntentResult.Error -> res.fallbackText
                    }
                } else if (tone != null && targetLang.equals("KO", ignoreCase = true) && sourceLang.equals("KO", ignoreCase = true)) {
                    when (val res = intentEngine.applyTone(simulatedVoiceText, tone)) {
                        is IntentResult.Success -> res.text
                        is IntentResult.Error -> res.fallbackText
                    }
                } else {
                    val translated = intentEngine.translate(simulatedVoiceText, targetLang, sourceLang, tone)
                    if (translated.isNotBlank()) translated else simulatedVoiceText
                }

                // 3단계: TTS 음성 합성
                Log.d(TAG, "🔊 [3단계: TTS 음성합성 시작] 결과: '$refinedText' (성별: $gender, 피치: $pitch)")
                _stage.value = VoicePipelineStage.SynthesizingTTS(refinedText)

                if (autoSpeak) {
                    ttsManager.speak(refinedText, targetLang, gender, pitch)
                }

                _stage.value = VoicePipelineStage.Completed(
                    rawText = simulatedVoiceText,
                    aiText = refinedText,
                    targetLang = targetLang,
                    isQwenEngine = isQwen
                )
                Log.d(TAG, "🎉 [파이프라인 완결] 성공")

            } catch (e: Exception) {
                Log.e(TAG, "❌ 파이프라인 오류", e)
                _stage.value = VoicePipelineStage.Error(e.message ?: ai.deartalk.android.data.pref.UiStrings.errorOccurred)
            }
        }
    }

    /**
     * 🔊 이미 정제된 텍스트를 LLM 재연산 없이 0초 지연으로 즉시 TTS 발화
     */
    fun speakDirectly(
        text: String,
        targetLang: String = "KO",
        gender: ai.deartalk.android.tts.VoiceGender = ai.deartalk.android.tts.VoiceGender.FEMALE,
        pitch: Float = 1.0f
    ) {
        if (text.isNotBlank()) {
            Log.d(TAG, "🔊 [스피커 즉시 재생] '$text' (0ms 지연, LLM 재연산 생략)")
            ttsManager.speak(text, targetLang, gender, pitch)
        }
    }



    fun stop() {
        currentJob?.cancel()
        ttsManager.stop()
        _stage.value = VoicePipelineStage.Idle
    }

    fun releaseMemory() {
        stop()
        ttsManager.shutdown()
        System.gc()
    }
}
