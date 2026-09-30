package ai.deartalk.android.ime.ui

import ai.deartalk.android.data.pref.AiModeItem
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.SpeechIntent

/**
 * 🎯 DearTalk 키보드 화면(DearTalkScreen) MVI 이벤트 계약
 * - 32개의 콜백 파라미터를 단일 이벤트 채널로 통합
 */
sealed interface ImeUiEvent {
    object MainMicClick : ImeUiEvent
    data class SelectTone(val tone: CustomTone) : ImeUiEvent
    data class SelectSpeechIntent(val intent: SpeechIntent) : ImeUiEvent
    data class ApplyTone(val tone: CustomTone) : ImeUiEvent
    data class ApplyAiMode(val mode: AiModeItem) : ImeUiEvent
    data class ApplyAiText(val text: String) : ImeUiEvent
    object ClearAiTextClick : ImeUiEvent
    object DeleteClick : ImeUiEvent
    object DeleteSentenceClick : ImeUiEvent
    object SpaceClick : ImeUiEvent
    object EnterClick : ImeUiEvent
    object SwitchToKeyboardClick : ImeUiEvent
    object SettingsClick : ImeUiEvent
    object LiveClick : ImeUiEvent
    object DownloadPackClick : ImeUiEvent
}
