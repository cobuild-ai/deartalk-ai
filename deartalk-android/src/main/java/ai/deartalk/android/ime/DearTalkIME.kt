package ai.deartalk.android.ime

import android.inputmethodservice.InputMethodService
import android.os.Bundle
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
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.crash.CrashLogger
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.CustomToneManager
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.ime.ui.DearTalkScreen
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.ime.ui.StandardKeyboardView
import ai.deartalk.android.ime.ui.theme.DearTalkTheme
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DearTalkIME : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var intentEngine: DearTalkIntentEngine
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var modelLifecycleManager: ai.deartalk.android.data.ModelLifecycleManager

    private var currentPackageName by mutableStateOf("")
    private var micUiState by mutableStateOf(MicUiState.IDLE)
    private var activeTierState by mutableStateOf(ai.deartalk.android.data.ActiveAiTier.STT_ONLY)
    private var recognizedTextState by mutableStateOf("")
    private var statusMessageState by mutableStateOf("")
    private var aiTextState by mutableStateOf("")
    private var tonesState by mutableStateOf<List<CustomTone>>(emptyList())
    private var aiModesState by mutableStateOf<List<ai.deartalk.android.data.pref.AiModeItem>>(emptyList())
    private var isTranslationModeState by mutableStateOf(false)
    private var selectedTargetLanguageState by mutableStateOf(ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TRANSLATIONS.first())
    private var selectedToneState by mutableStateOf(ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES.first())
    private var selectedSpeechIntentState by mutableStateOf(ai.deartalk.android.live.data.SpeechIntent.AUTO)
    private var detectedSpeechIntentState by mutableStateOf<ai.deartalk.android.live.data.SpeechIntent?>(null)
    private var isRetransformingState by mutableStateOf(false)
    private var isStandardKeyboardModeState by mutableStateOf(false)
    private var koreanKeyboardTypeState by mutableStateOf(KoreanKeyboardType.DUBEOLSIK)
    private var clipboardTextState by mutableStateOf<String?>(null)
    private var lastPastedClipText: String? = null
    private var lastDismissedClipText: String? = null
    private var lastObservedClipText: String? = null
    private var firstObservedClipTime: Long = 0L
    private var clipboardDismissJob: Job? = null
    private var retransformJob: Job? = null

    private val hangulComposer = HangulComposer()
    private val cheonjiinComposer = CheonjiinComposer()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))
        koreanKeyboardTypeState = DearTalkSettings.getKoreanKeyboardType(this)
        refreshClipboard()

        modelLifecycleManager = ai.deartalk.android.data.ModelLifecycleManager(this)
        activeTierState = modelLifecycleManager.activeTier.value
        intentEngine = DearTalkIntentEngine(this)
        sttManager = SpeechRecognitionManager(this)
        ttsManager = TextToSpeechManager(this)

        tonesState = CustomToneManager.getTones(this)
        aiModesState = CustomToneManager.getAllAiModes(this)

        observeStt()
        observeTier()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val pkg = info?.packageName ?: ""
        currentPackageName = pkg
        hangulComposer.reset()
        cheonjiinComposer.reset()

        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))
        koreanKeyboardTypeState = DearTalkSettings.getKoreanKeyboardType(this)
        refreshClipboard()

        modelLifecycleManager.refreshState()
        activeTierState = modelLifecycleManager.activeTier.value
        tonesState = CustomToneManager.getTones(this)
        aiModesState = CustomToneManager.getAllAiModes(this)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissClipboard()
        hangulComposer.reset()
        cheonjiinComposer.reset()
        if (micUiState == MicUiState.LISTENING || micUiState == MicUiState.PREPARING) {
            sttManager.cancelListening()
            micUiState = MicUiState.IDLE
        }
    }

    /**
     * 사용자가 텍스트 중간을 터치하거나 커서/선택영역을 이동했을 때 호출되는 핵심 안드로이드 IME 콜백.
     * 활성 조합 영역(Composing Span) 밖으로 커서가 이동하면 즉시 조합을 완료(finishComposingText)하고
     * 오토마타(CheonjiinComposer / HangulComposer) 상태를 리셋하여 이전 글자가 중간에 합쳐지거나
     * 끝으로 커서가 튀는 버그를 원천 차단합니다.
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        val isComposing = cheonjiinComposer.isComposing || hangulComposer.isComposing
        if (!isComposing) return

        val hasComposingSpan = candidatesStart >= 0 && candidatesEnd >= 0
        val isCursorOutside = if (hasComposingSpan) {
            newSelStart < candidatesStart || newSelEnd > candidatesEnd
        } else {
            oldSelStart != newSelStart || oldSelEnd != newSelEnd
        }

        if (isCursorOutside) {
            runCatching {
                currentInputConnection?.finishComposingText()
            }
            cheonjiinComposer.reset()
            hangulComposer.reset()
        }
    }

    /**
     * 클립보드 제안 스트립 새로고침 및 라이프사이클 관리:
     * 1) 3분 TTL 만료 검증 (오래된 클립 무한 노출 방지)
     * 2) 1회 붙여넣기(소비) 또는 수동 닫기 텍스트 재노출 방지
     * 3) 15초 미사용 시 자동 숨김 타이머 연동
     */
    private fun refreshClipboard() {
        try {
            val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (cm != null && cm.hasPrimaryClip()) {
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val rawText = clip.getItemAt(0).coerceToText(this)?.toString()?.trim()
                    if (rawText.isNullOrBlank()) {
                        dismissClipboard()
                        return
                    }

                    // 1. 이미 붙여넣었거나 수동으로 닫은 텍스트는 재노출 방지
                    if (rawText == lastPastedClipText || rawText == lastDismissedClipText) {
                        dismissClipboard()
                        return
                    }

                    // 2. TTL (3분) 검증
                    val now = System.currentTimeMillis()
                    val timestamp = clip.description?.timestamp ?: 0L
                    val isExpired = if (timestamp > 0L) {
                        (now - timestamp) > CLIPBOARD_TTL_MS
                    } else {
                        if (rawText != lastObservedClipText) {
                            lastObservedClipText = rawText
                            firstObservedClipTime = now
                            false
                        } else {
                            (now - firstObservedClipTime) > CLIPBOARD_TTL_MS
                        }
                    }

                    if (isExpired) {
                        dismissClipboard()
                        return
                    }

                    if (rawText != lastObservedClipText) {
                        lastObservedClipText = rawText
                        firstObservedClipTime = now
                    }

                    clipboardTextState = rawText
                    scheduleClipboardAutoDismiss()
                } else {
                    dismissClipboard()
                }
            } else {
                dismissClipboard()
            }
        } catch (e: Exception) {
            dismissClipboard()
        }
    }

    private fun dismissClipboard() {
        clipboardDismissJob?.cancel()
        clipboardDismissJob = null
        clipboardTextState = null
    }

    private fun scheduleClipboardAutoDismiss() {
        clipboardDismissJob?.cancel()
        clipboardDismissJob = serviceScope.launch {
            delay(CLIPBOARD_AUTO_DISMISS_MS)
            clipboardTextState = null
        }
    }

    private fun onUserKeyTyped() {
        if (clipboardTextState != null) {
            dismissClipboard()
        }
    }

    private fun observeTier() {
        serviceScope.launch {
            modelLifecycleManager.activeTier.collect { tier ->
                activeTierState = tier
            }
        }
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DearTalkIME)
            setViewTreeViewModelStoreOwner(this@DearTalkIME)
            setViewTreeSavedStateRegistryOwner(this@DearTalkIME)

            setContent {
                DearTalkTheme {
                    if (isStandardKeyboardModeState) {
                        StandardKeyboardView(
                            koreanKeyboardType = koreanKeyboardTypeState,
                            onKoreanKeyboardTypeChange = { type ->
                                hangulComposer.commit(currentInputConnection)
                                cheonjiinComposer.commit(currentInputConnection)
                                koreanKeyboardTypeState = type
                                DearTalkSettings.setKoreanKeyboardType(this@DearTalkIME, type)
                            },
                            clipboardText = clipboardTextState,
                            onPasteClick = { text ->
                                currentInputConnection?.commitText(text, 1)
                                lastPastedClipText = text
                                dismissClipboard()
                            },
                            onDismissClipboardClick = {
                                lastDismissedClipText = clipboardTextState
                                dismissClipboard()
                            },
                            onCharClick = { char ->
                                onUserKeyTyped()
                                runCatching {
                                    if (koreanKeyboardTypeState == KoreanKeyboardType.CHEONJIIN) {
                                        cheonjiinComposer.commit(currentInputConnection)
                                        currentInputConnection?.commitText(char.toString(), 1)
                                    } else {
                                        hangulComposer.inputJamo(currentInputConnection, char)
                                    }
                                }.onFailure { e ->
                                    CrashLogger.logHandledException("DearTalkIME.onCharClick($char)", "IME character input exception", e)
                                }
                            },
                            onCheonjiinConsonantClick = { key ->
                                onUserKeyTyped()
                                runCatching {
                                    cheonjiinComposer.inputConsonantKey(currentInputConnection, key)
                                }.onFailure { e ->
                                    CrashLogger.logHandledException("DearTalkIME.onCheonjiinConsonantClick($key)", "Cheonjiin consonant input exception", e)
                                    cheonjiinComposer.reset()
                                }
                            },
                            onCheonjiinVowelClick = { key ->
                                onUserKeyTyped()
                                runCatching {
                                    cheonjiinComposer.inputVowelKey(currentInputConnection, key)
                                }.onFailure { e ->
                                    CrashLogger.logHandledException("DearTalkIME.onCheonjiinVowelClick($key)", "Cheonjiin vowel input exception", e)
                                    cheonjiinComposer.reset()
                                }
                            },
                            onDeleteClick = {
                                onUserKeyTyped()
                                runCatching {
                                    if (koreanKeyboardTypeState == KoreanKeyboardType.CHEONJIIN) {
                                        cheonjiinComposer.delete(currentInputConnection)
                                    } else {
                                        if (!hangulComposer.delete(currentInputConnection)) {
                                            currentInputConnection?.deleteSurroundingText(1, 0)
                                        }
                                    }
                                }.onFailure { e ->
                                    CrashLogger.logHandledException("DearTalkIME.onDeleteClick", "Delete action exception", e)
                                    currentInputConnection?.deleteSurroundingText(1, 0)
                                }
                            },
                            onSpaceClick = {
                                onUserKeyTyped()
                                runCatching {
                                    if (koreanKeyboardTypeState == KoreanKeyboardType.CHEONJIIN) {
                                        cheonjiinComposer.space(currentInputConnection)
                                    } else {
                                        hangulComposer.commit(currentInputConnection)
                                        currentInputConnection?.commitText(" ", 1)
                                    }
                                }.onFailure { e ->
                                    CrashLogger.logHandledException("DearTalkIME.onSpaceClick", "Space action exception", e)
                                    currentInputConnection?.commitText(" ", 1)
                                }
                            },
                            onEnterClick = {
                                onUserKeyTyped()
                                runCatching {
                                    if (koreanKeyboardTypeState == KoreanKeyboardType.CHEONJIIN) {
                                        cheonjiinComposer.commit(currentInputConnection)
                                    } else {
                                        hangulComposer.commit(currentInputConnection)
                                    }
                                    handleEnter()
                                }.onFailure { e ->
                                    CrashLogger.logHandledException("DearTalkIME.onEnterClick", "Enter action exception", e)
                                }
                            },
                            onSwitchToAiModeClick = {
                                if (koreanKeyboardTypeState == KoreanKeyboardType.CHEONJIIN) {
                                    cheonjiinComposer.commit(currentInputConnection)
                                } else {
                                    hangulComposer.commit(currentInputConnection)
                                }
                                isStandardKeyboardModeState = false
                            }
                        )
                    } else {
                        DearTalkScreen(
                            micUiState = micUiState,
                            activeTier = activeTierState,
                            recognizedText = recognizedTextState,
                            statusMessage = statusMessageState,
                            aiText = aiTextState,
                            tones = tonesState,
                            aiModes = aiModesState,
                            isTranslationMode = isTranslationModeState,
                            selectedTargetLanguage = selectedTargetLanguageState,
                            availableLanguages = ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TRANSLATIONS,
                            onToggleTranslationMode = {
                                isTranslationModeState = !isTranslationModeState
                                retransformCurrentText(newTranslationMode = isTranslationModeState)
                            },
                            onSelectTargetLanguage = { target ->
                                selectedTargetLanguageState = target
                                isTranslationModeState = true
                                retransformCurrentText(newTranslationMode = true, newTargetLang = target)
                            },
                            selectedTone = selectedToneState,
                            availableTones = if (tonesState.isNotEmpty()) tonesState else ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES,
                            onSelectTone = { tone ->
                                selectedToneState = tone
                                retransformCurrentText(newTone = tone)
                            },
                            selectedSpeechIntent = selectedSpeechIntentState,
                            detectedSpeechIntent = detectedSpeechIntentState,
                            onSelectSpeechIntent = { intent ->
                                val newIntent = if (selectedSpeechIntentState == intent) ai.deartalk.android.live.data.SpeechIntent.AUTO else intent
                                selectedSpeechIntentState = newIntent
                                retransformCurrentText(newIntent = newIntent)
                            },
                            isRetransforming = isRetransformingState,
                            onApplyTone = { tone -> handleApplyTone(tone) },
                            onApplyAiMode = { mode -> handleApplyAiMode(mode) },
                            onMainMicClick = {
                                toggleMainMic()
                            },
                            onApplyAiText = { text -> handleApplyAiText(text) },
                            onClearAiTextClick = { handleClearAiText() },
                            onDeleteClick = { handleDelete() },
                            onDeleteSentenceClick = { handleDeleteSentence() },
                            onSpaceClick = { handleSpace() },
                            onEnterClick = { handleEnter() },
                            onSwitchToKeyboardClick = {
                                isStandardKeyboardModeState = true
                            },
                            onSettingsClick = {
                                val intent = android.content.Intent(this@DearTalkIME, ai.deartalk.android.MainActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                            },
                            onLiveClick = {
                                val intent = android.content.Intent(this@DearTalkIME, ai.deartalk.android.live.DearTalkLiveActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                            },
                            onDownloadPackClick = {
                                val intent = android.content.Intent(this@DearTalkIME, ai.deartalk.android.live.DearTalkLiveActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    putExtra("auto_start_download", true)
                                    putExtra("open_settings", true)
                                }
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
        return composeView
    }

    private fun observeStt() {
        serviceScope.launch {
            sttManager.voiceState.collect { state ->
                when (state) {
                    is VoiceState.Idle -> {
                        micUiState = MicUiState.IDLE
                    }
                    is VoiceState.Preparing -> {
                        micUiState = MicUiState.PREPARING
                    }
                    is VoiceState.Listening -> {
                        micUiState = MicUiState.LISTENING
                    }
                    is VoiceState.PartialResult -> {
                        recognizedTextState = state.text
                    }
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
                        if (recognizedTextState.isBlank() && aiTextState.isBlank()) {
                            statusMessageState = UiStrings.noSpeechDetected
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
            MicUiState.PREPARING, MicUiState.PROCESSING_AI -> {
                // 준비 중 또는 변환 중에는 중복 터치 무시하여 하드웨어 안정성 보장
            }
            MicUiState.IDLE -> {
                recognizedTextState = ""
                aiTextState = ""
                val targetLocale = ai.deartalk.android.data.pref.DearTalkSettings.getEffectiveLocale(this)
                sttManager.startListening(targetLocale)
            }
        }
    }

    private fun processVoiceCommand(voicePrompt: String) {
        val ic = currentInputConnection
        val currentText = ic?.getTextBeforeCursor(200, 0)?.toString() ?: ""

        val effectiveIntent = selectedSpeechIntentState

        serviceScope.launch {
            statusMessageState = UiStrings.aiProcessing

            val result = try {
                if (isTranslationModeState) {
                    intentEngine.processWithTranslation(
                        voiceInput = voicePrompt,
                        target = selectedTargetLanguageState,
                        currentEditorText = currentText,
                        packageName = currentPackageName,
                        tone = selectedToneState.name,
                        speechIntent = effectiveIntent
                    )
                } else {
                    intentEngine.processWithTone(
                        voiceInput = voicePrompt,
                        tone = selectedToneState,
                        currentEditorText = currentText,
                        packageName = currentPackageName,
                        speechIntent = effectiveIntent
                    )
                }
            } catch (e: Throwable) {
                IntentResult.Error(voicePrompt, UiStrings.errorOccurred)
            }

            withContext(Dispatchers.Main) {
                micUiState = MicUiState.IDLE
                when (result) {
                    is IntentResult.Success -> {
                        val text = result.text.ifBlank { voicePrompt }
                        aiTextState = text
                        statusMessageState = result.message.ifBlank { UiStrings.aiTextComplete }
                        detectedSpeechIntentState = result.detectedIntent
                    }
                    is IntentResult.Error -> {
                        aiTextState = result.fallbackText.ifBlank { voicePrompt }
                        statusMessageState = UiStrings.sttComplete
                    }
                }
            }
        }
    }

    private fun retransformCurrentText(
        newTranslationMode: Boolean = isTranslationModeState,
        newTargetLang: ai.deartalk.android.data.pref.TranslationTarget = selectedTargetLanguageState,
        newTone: CustomTone = selectedToneState,
        newIntent: ai.deartalk.android.live.data.SpeechIntent = selectedSpeechIntentState
    ) {
        // 🛡️ [원문 보존 불변 원칙]: 최초 발화 원문(recognizedTextState)을 항상 최우선 기준으로 유지
        val textToTransform = recognizedTextState.ifBlank {
            // recognizedTextState가 비어있다면 에디터 텍스트나 aiTextState를 가져오되,
            // 캔버스 상단 마이크 행에 즉시 채워넣어 사용자가 말한 내용이 화면에서 절대 사라지지 않도록 영구 보존!
            val editorFallback = currentInputConnection?.getTextBeforeCursor(2000, 0)?.toString()?.trim() ?: ""
            val base = if (editorFallback.isNotBlank()) editorFallback else aiTextState.trim()
            if (base.isNotBlank() && recognizedTextState.isBlank()) {
                recognizedTextState = base
            }
            base
        }.trim()

        if (textToTransform.isBlank()) return

        // 🌟 [화면 깜빡임 원천 방지]: micUiState(전체 화면 모드)를 건드리지 않고 인라인 상태(isRetransformingState)만 활성화
        isRetransformingState = true
        statusMessageState = UiStrings.aiProcessing

        // 🛡️ [동시성 보호 & 즉시 반응]: 기존 진행 중인 변환 작업이 있다면 즉각 취소하여 지연 및 결과 덮어쓰기 원천 방지
        retransformJob?.cancel()
        retransformJob = serviceScope.launch {
            try {
                val result = if (newTranslationMode) {
                    intentEngine.processWithTranslation(
                        voiceInput = textToTransform,
                        target = newTargetLang,
                        packageName = currentPackageName,
                        tone = newTone.name,
                        speechIntent = newIntent
                    )
                } else {
                    intentEngine.processWithTone(
                        voiceInput = textToTransform,
                        tone = newTone,
                        packageName = currentPackageName,
                        speechIntent = newIntent
                    )
                }

                ensureActive()

                withContext(Dispatchers.Main) {
                    isRetransformingState = false
                    when (result) {
                        is IntentResult.Success -> {
                            aiTextState = result.text.ifBlank { textToTransform }
                            statusMessageState = result.message.ifBlank { UiStrings.aiTextComplete }
                            if (newIntent == ai.deartalk.android.live.data.SpeechIntent.AUTO) {
                                detectedSpeechIntentState = result.detectedIntent
                            }
                        }
                        is IntentResult.Error -> {
                            aiTextState = result.fallbackText.ifBlank { textToTransform }
                        }
                    }
                }
            } catch (_: CancellationException) {
                // 이전 작업이 취소된 경우 아무 작업도 하지 않음 (새로운 작업이 UI 갱신 담당)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isRetransformingState = false
                    aiTextState = textToTransform
                    statusMessageState = UiStrings.errorOccurred
                }
            }
        }
    }

    private fun handleApplyAiMode(mode: ai.deartalk.android.data.pref.AiModeItem) {
        val currentEditorText = currentInputConnection?.getTextBeforeCursor(2000, 0)?.toString() ?: ""
        // [원문 불변 보존 원칙] 톤 변환은 항상 최초 STT 원문(recognizedTextState) 또는 입력 에디터 원본을 기준으로 수행
        val textToTransform = if (recognizedTextState.isNotBlank()) recognizedTextState else currentEditorText.trim()

        if (textToTransform.isBlank()) {
            statusMessageState = UiStrings.noTextToTransform
            return
        }

        micUiState = MicUiState.PROCESSING_AI
        statusMessageState = UiStrings.modeProcessing(mode.icon, mode.name)
        serviceScope.launch {
            val result = when (mode.type) {
                ai.deartalk.android.data.pref.AiModeType.TRANSLATION -> {
                    val target = mode.translationTarget ?: return@launch
                    intentEngine.processWithTranslation(
                        voiceInput = textToTransform,
                        target = target,
                        currentEditorText = currentEditorText,
                        packageName = currentPackageName
                    )
                }
                ai.deartalk.android.data.pref.AiModeType.TONE -> {
                    val tone = mode.customTone ?: return@launch
                    intentEngine.processWithTone(
                        voiceInput = textToTransform,
                        tone = tone,
                        currentEditorText = currentEditorText,
                        packageName = currentPackageName
                    )
                }
                ai.deartalk.android.data.pref.AiModeType.DEFAULT -> {
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
        // [원문 불변 보존 원칙] 톤 변환은 맥락 오염 방지를 위해 무조건 100% 최초 STT 음성 원문(recognizedTextState)만을 기준으로 수행
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
            val ic = currentInputConnection
            if (ic != null && ic.commitText(textToCommit, 1)) {
                aiTextState = ""
                recognizedTextState = ""
                micUiState = MicUiState.IDLE
                statusMessageState = UiStrings.textApplied
            } else {
                statusMessageState = "⚠️ 입력 대상 앱 연결 단절"
            }
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
     * 1. 캔버스에 대기 중인 AI/STT 텍스트가 있으면 마지막 문장만 삭제
     * 2. 캔버스가 비어 있으면 실제 에디터의 커서 앞 마지막 1문장(마침표/물음표/느낌표/개행 기준) 삭제
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

        // 실제 에디터 입력창의 마지막 문장 삭제
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

    companion object {
        private const val CLIPBOARD_TTL_MS = 180_000L // 3분 (180초)
        private const val CLIPBOARD_AUTO_DISMISS_MS = 15_000L // 15초 자동 숨김

        /**
         * 문장의 마지막 경계(잘라낼 시작 인덱스)를 계산
         * 예: "안녕하세요. 반갑습니다." -> "안녕하세요." 뒤의 인덱스 반환
         * 예: "안녕하세요. 반갑습니다" -> "안녕하세요." 뒤의 인덱스 반환
         * 예: "안녕하세요." -> 0 반환 (1문장 전체 삭제)
         */
        internal fun findLastSentenceCutIndex(text: String): Int {
            val delimiters = setOf('.', '?', '!', '\n')
            val trimmed = text.trimEnd()
            if (trimmed.isEmpty()) return 0

            // 1. 끝에 연속된 문장부호 건너뛰기
            var searchEnd = trimmed.length - 1
            while (searchEnd >= 0 && delimiters.contains(trimmed[searchEnd])) {
                searchEnd--
            }

            if (searchEnd < 0) return 0

            // 2. searchEnd 이전의 직전 문장부호(. ? ! \n) 위치 탐색
            var prevDelimIndex = -1
            for (i in searchEnd downTo 0) {
                if (delimiters.contains(trimmed[i])) {
                    prevDelimIndex = i
                    break
                }
            }

            return if (prevDelimIndex != -1) {
                prevDelimIndex + 1
            } else {
                0
            }
        }
    }

    private fun handleSpace() {
        currentInputConnection?.commitText(" ", 1)
    }

    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
        )
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER)
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
}
