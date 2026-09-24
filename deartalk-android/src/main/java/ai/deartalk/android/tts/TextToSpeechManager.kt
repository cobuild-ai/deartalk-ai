package ai.deartalk.android.tts

import ai.deartalk.android.crash.CrashLogger
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

    private val appContext: Context = context.applicationContext
    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    private var activeLocale: Locale = Locale.getDefault()
    private var currentGender = VoiceGender.FEMALE
    private var currentPitch = 1.0f
    private var currentRate = 1.0f

    init {
        initTts()
    }

    @Synchronized
    private fun initTts(onReady: (() -> Unit)? = null) {
        try {
            tts?.shutdown()
        } catch (t: Throwable) {
            CrashLogger.logHandledException("TextToSpeechManager.initTts", "Previous TTS instance shutdown error", t)
        }

        isInitialized = false
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val defaultLocale = Locale.getDefault()
                val res = tts?.setLanguage(defaultLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.KOREAN)
                    activeLocale = Locale.KOREAN
                } else {
                    activeLocale = defaultLocale
                }
                isInitialized = true
                applyVoiceConfig()
                Log.d(TAG, "🔊 TTS 초기화 완료 (음성 개수: ${tts?.voices?.size ?: 0})")
                onReady?.invoke()
            } else {
                Log.e(TAG, "❌ TTS 초기화 실패 (status=$status)")
                isInitialized = false
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

        val loc = targetLocale ?: activeLocale
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

    /**
     * 🔊 언어 선택 시 해당 언어의 TTS 보이스 및 엔진을 사전 워밍업 (첫 발화 지연 0ms 목표)
     */
    fun prewarmLanguage(targetLocale: Locale) {
        if (!isInitialized || tts == null) {
            initTts {
                prewarmLanguage(targetLocale)
            }
            return
        }
        try {
            val availability = tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            Log.d(TAG, "🔊 [${targetLocale.toLanguageTag()}] TTS 언어 가용성: $availability")
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                applyVoiceConfig(targetLocale)
                Log.d(TAG, "⚡ [${targetLocale.toLanguageTag()}] TTS 보이스 프리웜 성공")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "⚠️ [${targetLocale.toLanguageTag()}] TTS 프리웜 예외: ${e.message}")
        }
    }

    fun speak(
        text: String,
        targetLangCode: String = "KO",
        gender: VoiceGender = currentGender,
        pitch: Float = currentPitch
    ) {
        if (text.isBlank()) return

        currentGender = gender
        currentPitch = pitch
        val targetLocale = ai.deartalk.android.util.LanguageLocaleHelper.getLocaleForCode(targetLangCode)

        if (!isInitialized || tts == null) {
            Log.w(TAG, "⚠️ TTS 미초기화 상태 감지 ➔ 자율 재초기화(Self-Healing) 후 발화")
            initTts {
                performSpeak(text, targetLocale)
            }
            return
        }

        val result = performSpeak(text, targetLocale)
        if (result == TextToSpeech.ERROR) {
            Log.w(TAG, "⚠️ TTS 발화 실패 (Dead Binder 또는 서비스 단절) ➔ 세션 재연결 및 자동 재시도")
            initTts {
                performSpeak(text, targetLocale)
            }
        }
    }

    private fun performSpeak(text: String, targetLocale: Locale): Int {
        return try {
            tts?.setLanguage(targetLocale)
            applyVoiceConfig(targetLocale)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DearTalk_TTS") ?: TextToSpeech.ERROR
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak exception: ${e.message}")
            TextToSpeech.ERROR
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (t: Throwable) {
            CrashLogger.logHandledException("TextToSpeechManager.stop", "TTS stop failure", t)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "TTS shutdown exception: ${e.message}")
        } finally {
            tts = null
            isInitialized = false
        }
    }
}
