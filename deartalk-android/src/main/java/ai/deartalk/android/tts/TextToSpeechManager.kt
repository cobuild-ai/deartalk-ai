package ai.deartalk.android.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

enum class VoiceGender {
    FEMALE, // 👩 여성 보이스 (맑고 경쾌한 톤)
    MALE    // 👨 남성 보이스 (차분하고 묵직한 톤)
}

class TextToSpeechManager(context: Context) {
    companion object {
        private const val TAG = "TextToSpeechManager"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentGender = VoiceGender.FEMALE
    private var currentPitch = 1.0f
    private var currentRate = 1.0f

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val defaultLocale = Locale.getDefault()
                val res = tts?.setLanguage(defaultLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.KOREAN
                }
                isInitialized = true
                applyVoiceConfig()
                Log.d(TAG, "🔊 TTS 초기화 완료 (음성 개수: ${tts?.voices?.size ?: 0})")
            }
        }
    }

    fun setGender(gender: VoiceGender) {
        currentGender = gender
        applyVoiceConfig()
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(currentPitch)
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(currentRate)
    }

    private fun applyVoiceConfig(targetLocale: Locale? = null) {
        if (!isInitialized || tts == null) return

        val loc = targetLocale ?: tts?.language ?: Locale.KOREAN
        val availableVoices = tts?.voices ?: emptySet()

        // 1. 해당 언어의 여성/남성 음성 탐색
        val matchingVoices = availableVoices.filter {
            it.locale.language.equals(loc.language, ignoreCase = true)
        }

        if (matchingVoices.isNotEmpty()) {
            val isMaleTarget = currentGender == VoiceGender.MALE
            val selectedVoice = matchingVoices.find { voice ->
                val nameLower = voice.name.lowercase()
                if (isMaleTarget) {
                    nameLower.contains("male") || nameLower.contains("man") || nameLower.contains("-b-") || nameLower.contains("-c-")
                } else {
                    nameLower.contains("female") || nameLower.contains("woman") || nameLower.contains("-a-") || nameLower.contains("-d-")
                }
            } ?: matchingVoices.first()

            try {
                tts?.voice = selectedVoice
                Log.d(TAG, "🎙️ TTS 보이스 적용: ${selectedVoice.name} (성별: $currentGender)")
            } catch (_: Throwable) {}
        }

        // 2. 피치(음높이)로 음색 보정 (남성은 저음 0.85, 여성은 1.1)
        val calculatedPitch = if (currentGender == VoiceGender.MALE) {
            (currentPitch * 0.82f).coerceIn(0.7f, 1.2f)
        } else {
            (currentPitch * 1.08f).coerceIn(0.8f, 1.4f)
        }

        tts?.setPitch(calculatedPitch)
        tts?.setSpeechRate(currentRate)
    }

    fun speak(
        text: String,
        targetLangCode: String = "KO",
        gender: VoiceGender = currentGender,
        pitch: Float = currentPitch
    ) {
        if (isInitialized && text.isNotBlank()) {
            currentGender = gender
            currentPitch = pitch

            val targetLocale = when (targetLangCode.uppercase()) {
                "EN" -> Locale.US
                "ES" -> Locale("es", "ES")
                "FR" -> Locale.FRANCE
                "DE" -> Locale.GERMANY
                "JA" -> Locale.JAPAN
                "ZH" -> Locale.CHINESE
                "ID" -> Locale("id", "ID")
                "VI" -> Locale("vi", "VN")
                "TL", "FIL" -> Locale("fil", "PH")
                "TH" -> Locale("th", "TH")
                "MS" -> Locale("ms", "MY")
                else -> Locale.KOREAN
            }

            try {
                tts?.setLanguage(targetLocale)
                applyVoiceConfig(targetLocale)
            } catch (_: Throwable) {}

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DearTalk_TTS")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
