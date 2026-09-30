package ai.deartalk.android.ui.main

import ai.deartalk.android.data.pref.KoreanKeyboardType

/**
 * 🎯 MainActivity MVI 사용자 액션 / 인텐트 계약 (Sealed Interface)
 * - UI 컴포넌트와 비즈니스 로직 사이의 단일 진입점 역할
 */
sealed interface MainUiEvent {
    object RefreshImeStatus : MainUiEvent
    object ToggleMic : MainUiEvent
    data class TestPreset(val preset: String) : MainUiEvent
    data class UpdateTestInputText(val text: String) : MainUiEvent
    object ClearTestInputText : MainUiEvent
    object DetectAndInitModel : MainUiEvent
    data class SetAutoLanguage(val auto: Boolean) : MainUiEvent
    data class SelectLanguageCode(val code: String) : MainUiEvent
    data class ChangeKoreanKeyboardType(val type: KoreanKeyboardType) : MainUiEvent
}
