package ai.deartalk.android.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ai.deartalk.android.MainActivity
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.data.pref.AiModeItem
import ai.deartalk.android.data.pref.AiModeType
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.CustomToneManager
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.data.repository.ContextRepository
import ai.deartalk.android.ime.ui.DearTalkScreen
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.ime.ui.StandardKeyboardView
import ai.deartalk.android.ime.ui.theme.DearTalkTheme
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DearTalk AI 키보드 IME 서비스 (Android InputMethodService)
 * - Clean Code 원칙: UI 상태 캡슐화, 명확한 오토마타 이벤트 전파, 단일 진입점 관리.
 */
class DearTalkIME : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var contextRepository: ContextRepository
    private lateinit var intentEngine: DearTalkIntentEngine
    private lateinit var ttsManager: TextToSpeechManager

    private var currentPackageName by mutableStateOf("")
    private var micUiState by mutableStateOf(MicUiState.IDLE)
    private var recognizedTextState by mutableStateOf("")
    private var statusMessageState by mutableStateOf("")
    private var aiTextState by mutableStateOf("")
    private var tonesState by mutableStateOf<List<CustomTone>>(emptyList())
    private var aiModesState by mutableStateOf<List<AiModeItem>>(emptyList())
    private var isStandardKeyboardModeState by mutableStateOf(false)
    private val hangulComposer = HangulComposer()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))

        contextRepository = ContextRepository(this)
        intentEngine = DearTalkIntentEngine(this, contextRepository)
        sttManager = SpeechRecognitionManager(this)
        ttsManager = TextToSpeechManager(this)

        refreshToneAndModeState()
        observeStt()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentPackageName = info?.packageName ?: ""
        hangulComposer.reset()

        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))
        refreshToneAndModeState()
    }

    private fun refreshToneAndModeState() {
        tonesState = CustomToneManager.getTones(this)
        aiModesState = CustomToneManager.getAllAiModes(this)
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DearTalkIME)
            setViewTreeViewModelStoreOwner(this@DearTalkIME)
            setViewTreeSavedStateRegistryOwner(this@DearTalkIME)

            setContent {
                DearTalkTheme {
                    if (isStandardKeyboardModeState) {
                        StandardKeyboardView(
                            onCharClick = { char ->
                                hangulComposer.inputJamo(currentInputConnection, char)
                            },
                            onDeleteClick = {
                                if (!hangulComposer.delete(currentInputConnection)) {
                                    currentInputConnection?.deleteSurroundingText(1, 0)
                                }
                            },
                            onSpaceClick = {
                                hangulComposer.commit(currentInputConnection)
                                currentInputConnection?.commitText(" ", 1)
                            },
                            onEnterClick = {
                                hangulComposer.commit(currentInputConnection)
                                handleEnter()
                            },
                            onSwitchToAiModeClick = {
                                hangulComposer.commit(currentInputConnection)
                                isStandardKeyboardModeState = false
                            }
                        )
                    } else {
                        DearTalkScreen(
                            micUiState = micUiState,
                            recognizedText = recognizedTextState,
                            statusMessage = statusMessageState,
                            aiText = aiTextState,
                            tones = tonesState,
                            aiModes = aiModesState,
                            onApplyTone = { tone -> handleApplyTone(tone) },
                            onApplyAiMode = { mode -> handleApplyAiMode(mode) },
                            onMainMicClick = { toggleMainMic() },
                            onApplyAiText = { text -> handleApplyAiText(text) },
                            onClearAiTextClick = { handleClearAiText() },
                            onDeleteClick = { handleDelete() },
                            onDeleteSentenceClick = { handleDeleteSentence() },
                            onSpaceClick = { handleSpace() },
                            onEnterClick = { handleEnter() },
                            onSwitchToKeyboardClick = { isStandardKeyboardModeState = true },
                            onSettingsClick = { openSettingsActivity() }
                        )
                    }
                }
            }
        }
    }

    private fun openSettingsActivity() {
        val intent = Intent(this@DearTalkIME, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun observeStt() {
        serviceScope.launch {
            sttManager.voiceState.collect { state ->
                when (state) {
                    is VoiceState.Idle -> micUiState = MicUiState.IDLE
                    is VoiceState.Preparing -> micUiState = MicUiState.PREPARING
                    is VoiceState.Listening -> micUiState = MicUiState.LISTENING
                    is VoiceState.PartialResult -> recognizedTextState = state.text
                    is VoiceState.FinalResult -> {
                        micUiState = MicUiState.PROCESSING_AI
                        recognizedTextState = state.text
                        processVoiceCommand(state.text)
                    }
                    is VoiceState.Error -> {
                        micUiState = MicUiState.IDLE
                        if (aiTextState.isBlank() && recognizedTextState.isNotBlank()) {
                            aiTextState = recognizedTextState
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun toggleMainMic() {
        when (micUiState) {
            MicUiState.LISTENING -> {
                sttManager.stopListening()
                micUiState = MicUiState.IDLE
            }
            MicUiState.PREPARING, MicUiState.PROCESSING_AI -> Unit
            MicUiState.IDLE -> {
                recognizedTextState = ""
                aiTextState = ""
                val targetLocale = DearTalkSettings.getEffectiveLocale(this)
                sttManager.startListening(targetLocale)
            }
        }
    }

    private fun processVoiceCommand(voicePrompt: String) {
        val ic = currentInputConnection
        val currentText = ic?.getTextBeforeCursor(200, 0)?.toString() ?: ""

        serviceScope.launch {
            statusMessageState = UiStrings.aiProcessing

            val result = try {
                intentEngine.process(
                    voiceInput = voicePrompt,
                    currentEditorText = currentText,
                    packageName = currentPackageName
                )
            } catch (e: Throwable) {
                IntentResult.Error(voicePrompt, UiStrings.errorOccurred)
            }

            withContext(Dispatchers.Main) {
                micUiState = MicUiState.IDLE
                when (result) {
                    is IntentResult.Success -> {
                        aiTextState = result.text.ifBlank { voicePrompt }
                        statusMessageState = result.message.ifBlank { UiStrings.aiTextComplete }
                    }
                    is IntentResult.Error -> {
                        aiTextState = result.fallbackText.ifBlank { voicePrompt }
                        statusMessageState = UiStrings.sttComplete
                    }
                }
            }
        }
    }

    private fun handleApplyAiMode(mode: AiModeItem) {
        val currentEditorText = currentInputConnection?.getTextBeforeCursor(2000, 0)?.toString() ?: ""
        val textToTransform = if (recognizedTextState.isNotBlank()) recognizedTextState else currentEditorText.trim()

        if (textToTransform.isBlank()) {
            statusMessageState = UiStrings.noTextToTransform
            return
        }

        micUiState = MicUiState.PROCESSING_AI
        statusMessageState = UiStrings.modeProcessing(mode.icon, mode.name)
        serviceScope.launch {
            val result = when (mode.type) {
                AiModeType.TRANSLATION -> {
                    val target = mode.translationTarget ?: return@launch
                    intentEngine.processWithTranslation(
                        voiceInput = textToTransform,
                        target = target,
                        currentEditorText = currentEditorText,
                        packageName = currentPackageName
                    )
                }
                AiModeType.TONE -> {
                    val tone = mode.customTone ?: return@launch
                    intentEngine.processWithTone(
                        voiceInput = textToTransform,
                        tone = tone,
                        currentEditorText = currentEditorText,
                        packageName = currentPackageName
                    )
                }
                AiModeType.DEFAULT -> {
                    intentEngine.process(
                        voiceInput = textToTransform,
                        currentEditorText = currentEditorText,
                        packageName = currentPackageName
                    )
                }
            }
            withContext(Dispatchers.Main) {
                micUiState = MicUiState.IDLE
                if (result is IntentResult.Success && result.text.isNotBlank()) {
                    aiTextState = result.text
                    statusMessageState = UiStrings.modeComplete(mode.name)
                } else {
                    statusMessageState = UiStrings.transformFailed
                }
            }
        }
    }

    private fun handleApplyTone(tone: CustomTone) {
        val currentEditorText = currentInputConnection?.getTextBeforeCursor(2000, 0)?.toString() ?: ""
        val textToTransform = if (recognizedTextState.isNotBlank()) recognizedTextState else currentEditorText.trim()

        if (textToTransform.isBlank()) {
            statusMessageState = UiStrings.noTextToTransform
            return
        }

        micUiState = MicUiState.PROCESSING_AI
        statusMessageState = UiStrings.toneConverting(tone.icon, tone.name)
        serviceScope.launch {
            val result = intentEngine.processWithTone(
                voiceInput = textToTransform,
                tone = tone,
                currentEditorText = currentEditorText,
                packageName = currentPackageName
            )
            withContext(Dispatchers.Main) {
                micUiState = MicUiState.IDLE
                if (result is IntentResult.Success && result.text.isNotBlank()) {
                    aiTextState = result.text
                    statusMessageState = UiStrings.toneApplied(tone.name)
                } else {
                    statusMessageState = UiStrings.transformFailed
                }
            }
        }
    }

    private fun handleApplyAiText(text: String) {
        val textToCommit = text.ifBlank { aiTextState.ifBlank { recognizedTextState } }
        if (textToCommit.isNotBlank()) {
            currentInputConnection?.commitText(textToCommit, 1)
            aiTextState = ""
            recognizedTextState = ""
            micUiState = MicUiState.IDLE
            statusMessageState = UiStrings.textApplied
        }
    }

    private fun handleClearAiText() {
        aiTextState = ""
        recognizedTextState = ""
        micUiState = MicUiState.IDLE
        statusMessageState = UiStrings.aiTextCleared
    }

    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    /**
     * 문장 단위 삭제 (Sentence Backspace)
     */
    private fun handleDeleteSentence() {
        val targetCanvasText = if (aiTextState.isNotBlank()) aiTextState else recognizedTextState

        if (targetCanvasText.isNotBlank()) {
            val cutIndex = findLastSentenceCutIndex(targetCanvasText)
            if (cutIndex > 0) {
                val remainingText = targetCanvasText.substring(0, cutIndex).trimEnd()
                if (aiTextState.isNotBlank()) aiTextState = remainingText else recognizedTextState = remainingText
                statusMessageState = UiStrings.lastSentenceDeleted
            } else {
                handleClearAiText()
            }
            return
        }

        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(2000, 0)?.toString() ?: ""
        if (textBefore.isBlank()) return

        val cutIndex = findLastSentenceCutIndex(textBefore)
        val deleteLength = textBefore.length - cutIndex

        if (deleteLength > 0) {
            ic.deleteSurroundingText(deleteLength, 0)
            statusMessageState = UiStrings.editorSentenceDeleted
        }
    }

    private fun handleSpace() {
        currentInputConnection?.commitText(" ", 1)
    }

    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
        )
        currentInputConnection?.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        sttManager.destroy()
        ttsManager.shutdown()
        store.clear()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        /**
         * 문장의 마지막 경계 인덱스(잘라낼 시작 위치)를 탐색합니다.
         */
        internal fun findLastSentenceCutIndex(text: String): Int {
            val delimiters = setOf('.', '?', '!', '\n')
            val trimmed = text.trimEnd()
            if (trimmed.isEmpty()) return 0

            var searchEnd = trimmed.length - 1
            while (searchEnd >= 0 && delimiters.contains(trimmed[searchEnd])) {
                searchEnd--
            }

            if (searchEnd < 0) return 0

            for (i in searchEnd downTo 0) {
                if (delimiters.contains(trimmed[i])) {
                    return i + 1
                }
            }
            return 0
        }
    }
}
