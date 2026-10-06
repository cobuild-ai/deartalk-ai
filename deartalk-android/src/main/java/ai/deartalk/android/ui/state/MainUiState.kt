package ai.deartalk.android.ui.state

import ai.deartalk.android.data.pref.KoreanKeyboardType

/**
 * 📱 MainActivity 단일 불변 UI 상태 모델 (UDF / MVI)
 */
data class MainUiState(
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
    val isModelLoaded: Boolean = false,
    val loadedModelName: String = "온디바이스 음성 AI 키보드",
    val languageDisplayTitle: String = "한국어 (시스템 언어 자동 연동)",
    val isListening: Boolean = false,
    val recognizedLiveText: String = "",
    val rawUtteranceText: String = "",
    val aiTransformedText: String = "",
    val aiProcessingMessage: String = "",
    val testInputText: String = "",
    val activePresetText: String = "",
    val silenceTimeoutMs: Float = 1500f,
    val isAutoLanguage: Boolean = true,
    val selectedLanguageCode: String = "ko",
    val selectedKoreanKeyboardType: KoreanKeyboardType = KoreanKeyboardType.DUBEOLSIK,
    val hasMicPermission: Boolean = false,
    val isOnboardingDismissed: Boolean = false
)
