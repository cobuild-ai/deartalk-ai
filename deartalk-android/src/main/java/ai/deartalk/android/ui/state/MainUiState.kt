package ai.deartalk.android.ui.state

import ai.deartalk.android.data.pref.KoreanKeyboardType

/**
 * 📱 MainActivity 단일 불변 UI 상태 모델 (UDF / MVI)
 */
data class MainUiState(
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
    val isModelLoaded: Boolean = false,
    val isAutoLanguage: Boolean = true,
    val selectedLanguageCode: String = "ko",
    val languageDisplayTitle: String = "한국어",
    val isListening: Boolean = false,
    val recognizedLiveText: String = "",
    val rawUtteranceText: String = "",
    val aiTransformedText: String = "",
    val aiProcessingMessage: String = "",
    val testInputText: String = "",
    val activePresetText: String = "",
    val silenceTimeoutMs: Float = 1500f,
    val selectedKoreanKeyboardType: KoreanKeyboardType = KoreanKeyboardType.DUBEOLSIK
)
