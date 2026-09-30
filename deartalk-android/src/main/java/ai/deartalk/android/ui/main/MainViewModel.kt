package ai.deartalk.android.ui.main

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.ui.state.MainUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🧠 MainActivity MVI ViewModel
 * - Compose UI와 안드로이드 시스템/AI 엔진 사이의 상태 전이 및 비즈니스 로직 총괄
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    val sttManager: SpeechRecognitionManager = SpeechRecognitionManager(context)
    val intentEngine: DearTalkIntentEngine = DearTalkIntentEngine(context)

    private val _uiState = MutableStateFlow(
        MainUiState(
            isImeEnabled = checkIsImeEnabled(context),
            isImeSelected = checkIsImeSelected(context),
            isModelLoaded = intentEngine.isModelLoaded,
            loadedModelName = "온디바이스 음성 AI 키보드",
            languageDisplayTitle = DearTalkSettings.getLanguageDisplayTitle(context),
            isAutoLanguage = DearTalkSettings.isAutoLanguage(context),
            selectedLanguageCode = DearTalkSettings.getSelectedLanguageCode(context),
            selectedKoreanKeyboardType = DearTalkSettings.getKoreanKeyboardType(context)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var presetJob: Job? = null

    init {
        observeModelLoaded()
        observeStt()
    }

    private fun observeModelLoaded() {
        viewModelScope.launch {
            intentEngine.isModelLoadedFlow.collect { loaded ->
                _uiState.update { it.copy(isModelLoaded = loaded) }
            }
        }
        viewModelScope.launch {
            intentEngine.loadedModelNameFlow.collect { modelName ->
                _uiState.update { it.copy(loadedModelName = modelName) }
            }
        }
    }

    private fun observeStt() {
        viewModelScope.launch {
            sttManager.voiceState.collect { state ->
                when (state) {
                    is VoiceState.Listening -> {
                        _uiState.update {
                            it.copy(
                                isListening = true,
                                aiProcessingMessage = UiStrings.settingsListening
                            )
                        }
                    }
                    is VoiceState.PartialResult -> {
                        _uiState.update { it.copy(recognizedLiveText = state.text) }
                    }
                    is VoiceState.FinalResult -> {
                        _uiState.update {
                            it.copy(
                                isListening = false,
                                recognizedLiveText = state.text,
                                rawUtteranceText = state.text,
                                aiProcessingMessage = UiStrings.settingsAiAnalyzing
                            )
                        }
                        processVoiceResult(state.text)
                    }
                    is VoiceState.Error -> {
                        _uiState.update {
                            it.copy(
                                isListening = false,
                                aiProcessingMessage = UiStrings.settingsSttError(state.errorCode)
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isListening = false) }
                    }
                }
            }
        }
    }

    private fun processVoiceResult(text: String) {
        viewModelScope.launch {
            val result = intentEngine.process(
                voiceInput = text,
                currentEditorText = _uiState.value.testInputText,
                packageName = "ai.deartalk.android.test"
            )
            when (result) {
                is IntentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            aiTransformedText = result.text,
                            testInputText = result.text,
                            aiProcessingMessage = result.message.ifBlank { UiStrings.settingsAiComplete }
                        )
                    }
                }
                is IntentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            aiTransformedText = result.fallbackText,
                            testInputText = result.fallbackText,
                            aiProcessingMessage = "⚠️ ${result.error}"
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.RefreshImeStatus -> {
                _uiState.update {
                    it.copy(
                        isImeEnabled = checkIsImeEnabled(context),
                        isImeSelected = checkIsImeSelected(context)
                    )
                }
            }
            is MainUiEvent.ToggleMic -> {
                if (_uiState.value.isListening) {
                    sttManager.stopListening()
                } else {
                    _uiState.update {
                        it.copy(
                            recognizedLiveText = "",
                            rawUtteranceText = "",
                            aiTransformedText = "",
                            testInputText = ""
                        )
                    }
                    sttManager.startListening()
                }
            }
            is MainUiEvent.TestPreset -> {
                val preset = event.preset
                _uiState.update {
                    it.copy(
                        activePresetText = preset,
                        rawUtteranceText = preset,
                        testInputText = preset,
                        aiTransformedText = "✨ AI가 문맥과 맞춤법을 다듬는 중...",
                        aiProcessingMessage = UiStrings.settingsAiRefining
                    )
                }
                presetJob?.cancel()
                presetJob = viewModelScope.launch(Dispatchers.IO) {
                    val res = intentEngine.process(preset, "", "ai.deartalk.android.test")
                    withContext(Dispatchers.Main) {
                        when (res) {
                            is IntentResult.Success -> {
                                _uiState.update {
                                    it.copy(
                                        aiTransformedText = res.text,
                                        testInputText = res.text,
                                        aiProcessingMessage = res.message.ifBlank { UiStrings.settingsAiRefined }
                                    )
                                }
                            }
                            is IntentResult.Error -> {
                                _uiState.update {
                                    it.copy(
                                        aiTransformedText = res.fallbackText,
                                        testInputText = res.fallbackText,
                                        aiProcessingMessage = UiStrings.settingsAiRefined
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is MainUiEvent.UpdateTestInputText -> {
                _uiState.update { it.copy(testInputText = event.text) }
            }
            is MainUiEvent.ClearTestInputText -> {
                _uiState.update { it.copy(testInputText = "") }
            }
            is MainUiEvent.DetectAndInitModel -> {
                intentEngine.detectAndInitOnDeviceModel()
            }
            is MainUiEvent.SetAutoLanguage -> {
                DearTalkSettings.setAutoLanguage(context, event.auto)
                _uiState.update {
                    it.copy(
                        isAutoLanguage = event.auto,
                        languageDisplayTitle = DearTalkSettings.getLanguageDisplayTitle(context)
                    )
                }
            }
            is MainUiEvent.SelectLanguageCode -> {
                DearTalkSettings.setSelectedLanguageCode(context, event.code)
                _uiState.update {
                    it.copy(
                        selectedLanguageCode = event.code,
                        languageDisplayTitle = DearTalkSettings.getLanguageDisplayTitle(context)
                    )
                }
            }
            is MainUiEvent.ChangeKoreanKeyboardType -> {
                _uiState.update { it.copy(selectedKoreanKeyboardType = event.type) }
                DearTalkSettings.setKoreanKeyboardType(context, event.type)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sttManager.destroy()
    }

    companion object {
        fun checkIsImeEnabled(context: Context): Boolean {
            return try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val enabledList = imm?.enabledInputMethodList ?: emptyList()
                enabledList.any { it.packageName == context.packageName }
            } catch (_: Exception) {
                false
            }
        }

        fun checkIsImeSelected(context: Context): Boolean {
            return try {
                val defaultMethod = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
                defaultMethod.contains(context.packageName)
            } catch (_: Exception) {
                checkIsImeEnabled(context)
            }
        }
    }
}
