package ai.deartalk.android.ui.state

import ai.deartalk.android.live.ActiveSpeaker
import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSession
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.stt.LanguageModelStatus

/**
 * 🎙️ DearTalkLiveScreen 단일 불변 UI 상태 모델 (State Hoisting / UDF)
 */
data class LiveUiState(
    val currentSession: LiveSession? = null,
    val messages: List<LiveMessage> = emptyList(),
    val sessions: List<LiveSession> = emptyList(),
    val activeSpeaker: ActiveSpeaker = ActiveSpeaker.NONE,
    val processingSpeaker: ActiveSpeaker = ActiveSpeaker.NONE,
    val streamingText: String = "",
    val isProcessing: Boolean = false,
    val rephrasingMessageId: String? = null,
    val rmsDb: Float = 0f,
    val myLang: String = "KO",
    val partnerLang: String = "EN",
    val myIntent: SpeechIntent = SpeechIntent.AUTO,
    val partnerIntent: SpeechIntent = SpeechIntent.AUTO,
    val myDetectedIntent: SpeechIntent? = null,
    val partnerDetectedIntent: SpeechIntent? = null,
    val selectedTone: String = "✨ 기본다듬기",
    val isFlipViewEnabled: Boolean = false,
    val isAutoSpeakEnabled: Boolean = false,
    val myModelStatus: LanguageModelStatus? = null,
    val partnerModelStatus: LanguageModelStatus? = null
)
