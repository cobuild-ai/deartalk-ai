package ai.deartalk.android.ui.state

import ai.deartalk.android.data.ActiveAiTier
import ai.deartalk.android.data.pref.AiModeItem
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.CustomToneManager
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.data.pref.TranslationTarget
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.live.data.SpeechIntent

/**
 * ⌨️ DearTalkIME 단일 불변 UI 상태 모델 (UDF / MVI)
 */
data class ImeUiState(
    val currentPackageName: String = "",
    val micUiState: MicUiState = MicUiState.IDLE,
    val activeTier: ActiveAiTier = ActiveAiTier.STT_ONLY,
    val recognizedText: String = "",
    val statusMessage: String = "",
    val aiText: String = "",
    val tones: List<CustomTone> = emptyList(),
    val aiModes: List<AiModeItem> = emptyList(),
    val isTranslationMode: Boolean = false,
    val selectedTargetLanguage: TranslationTarget = CustomToneManager.DEFAULT_TRANSLATIONS.first(),
    val selectedTone: CustomTone = CustomToneManager.DEFAULT_TONES.first(),
    val selectedSpeechIntent: SpeechIntent = SpeechIntent.AUTO,
    val detectedSpeechIntent: SpeechIntent? = null,
    val isRetransforming: Boolean = false,
    val isStandardKeyboardMode: Boolean = false,
    val koreanKeyboardType: KoreanKeyboardType = KoreanKeyboardType.DUBEOLSIK,
    val keyboardMode: ai.deartalk.android.data.pref.KeyboardMode = ai.deartalk.android.data.pref.KeyboardMode.BASIC,
    val clipboardText: String? = null
) {
    /**
     * 🌟 [UX 원칙]: 메시지 입력 완료 / 삭제 / 신규 음성 입력 시작 시
     * 톤앤매너(기본다듬기) 및 화행(AUTO) 선택을 기본 상태로 일괄 초기화하여 인지 부하를 최소화합니다.
     */
    fun resetSelections(defaultTone: CustomTone = tones.firstOrNull() ?: CustomToneManager.DEFAULT_TONES.first()): ImeUiState {
        return this.copy(
            selectedTone = defaultTone,
            selectedSpeechIntent = SpeechIntent.AUTO,
            detectedSpeechIntent = SpeechIntent.AUTO
        )
    }
}

