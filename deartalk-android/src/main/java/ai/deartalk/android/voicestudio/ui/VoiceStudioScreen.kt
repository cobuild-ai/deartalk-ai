package ai.deartalk.android.voicestudio.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ai.deartalk.android.agent.SequentialVoicePipeline
import ai.deartalk.android.agent.VoicePipelineStage
import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.data.SystemDiagnosticManager
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.tts.VoiceGender
import ai.deartalk.android.util.LanguageLocaleHelper
import ai.deartalk.android.voicestudio.ui.components.DualCardDisplay
import ai.deartalk.android.voicestudio.ui.components.HardwareDiagnosticCard
import ai.deartalk.android.voicestudio.ui.components.MainRecordButton
import ai.deartalk.android.voicestudio.ui.components.ModeTabButton
import ai.deartalk.android.voicestudio.ui.components.QuickSampleChips
import ai.deartalk.android.voicestudio.ui.components.ToneSelectorRow
import ai.deartalk.android.voicestudio.ui.components.TargetLanguageSelectorRow
import ai.deartalk.android.voicestudio.ui.components.VoiceCustomizerRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(
    diagnosticManager: SystemDiagnosticManager,
    modelLifecycleManager: ModelLifecycleManager,
    intentEngine: ai.deartalk.android.agent.DearTalkIntentEngine,
    sttManager: SpeechRecognitionManager,
    voicePipeline: SequentialVoicePipeline,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val packState by modelLifecycleManager.packState.collectAsState()
    val pipelineStage by voicePipeline.stage.collectAsState()
    val sttVoiceState by sttManager.voiceState.collectAsState()
    val rmsDb by sttManager.rmsDb.collectAsState()

    var systemMetrics by remember { mutableStateOf(diagnosticManager.diagnose()) }
    var selectedMode by remember { mutableStateOf(0) } // 0: 톤 변환, 1: 실시간 통역
    var selectedTone by remember { mutableStateOf(UiStrings.tonePolite) }
    val myAppLangCode = if (UiStrings.isKo) "KO" else if (UiStrings.isId) "ID" else "EN"
    var targetLanguage by remember { mutableStateOf(if (UiStrings.isKo) "EN" else "KO") }
    var selectedGender by remember { mutableStateOf(VoiceGender.FEMALE) }
    var selectedPitch by remember { mutableStateOf(1.0f) }
    var micUiState by remember { mutableStateOf(MicUiState.IDLE) }

    var rawTextDisplay by remember { mutableStateOf("") }
    var aiTextDisplay by remember { mutableStateOf("") }
    var hasValidResult by remember { mutableStateOf(false) }

    val defaultAppLangCode = myAppLangCode
    val isListening = micUiState == MicUiState.LISTENING

    // 💡 클린코드: 중복 호출 제거용 헬퍼 함수
    fun executePipeline(text: String, tgtLang: String = targetLanguage, tone: String? = selectedTone) {
        if (text.isBlank()) return
        voicePipeline.processVoiceInput(
            voiceText = text,
            targetLang = if (selectedMode == 1) tgtLang else defaultAppLangCode,
            sourceLang = defaultAppLangCode,
            tone = if (selectedMode == 0) tone else null,
            gender = selectedGender,
            pitch = selectedPitch,
            appPackageName = "ai.deartalk.android.voicestudio"
        )
    }

    fun executeDirectSpeech(text: String, tgtLang: String = targetLanguage, gender: VoiceGender = selectedGender, pitch: Float = selectedPitch) {
        if (text.isBlank()) return
        voicePipeline.speakDirectly(
            text = text,
            targetLang = if (selectedMode == 1) tgtLang else defaultAppLangCode,
            gender = gender,
            pitch = pitch
        )
    }

    // 마이크 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sttManager.startListening(LanguageLocaleHelper.getLocaleForCode(defaultAppLangCode))
        } else {
            Toast.makeText(context, UiStrings.micPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        systemMetrics = diagnosticManager.diagnose()
    }

    // 🎙️ 실시간 STT 상태 감지
    LaunchedEffect(sttVoiceState) {
        when (val state = sttVoiceState) {
            is VoiceState.Preparing -> {
                micUiState = MicUiState.PREPARING
            }
            is VoiceState.Listening -> {
                micUiState = MicUiState.LISTENING
            }
            is VoiceState.PartialResult -> {
                micUiState = MicUiState.LISTENING
                rawTextDisplay = state.text
                hasValidResult = true
            }
            is VoiceState.FinalResult -> {
                micUiState = MicUiState.PROCESSING_AI
                rawTextDisplay = state.text
                hasValidResult = true
                executePipeline(state.text)
            }
            is VoiceState.Error -> {
                micUiState = MicUiState.IDLE
                if (!hasValidResult) {
                    rawTextDisplay = UiStrings.noSpeechDetected
                }
            }
            is VoiceState.Idle -> {
                if (micUiState != MicUiState.PROCESSING_AI) {
                    micUiState = MicUiState.IDLE
                }
            }
            else -> {}
        }
    }

    // 🧠 파이프라인 진행 상태 수신
    LaunchedEffect(pipelineStage) {
        when (val stage = pipelineStage) {
            is VoicePipelineStage.RefiningLLM -> {
                micUiState = MicUiState.PROCESSING_AI
                rawTextDisplay = stage.rawText
                hasValidResult = true
            }
            is VoicePipelineStage.SynthesizingTTS -> {
                micUiState = MicUiState.PROCESSING_AI
                aiTextDisplay = stage.aiText
            }
            is VoicePipelineStage.Completed -> {
                micUiState = MicUiState.IDLE
                rawTextDisplay = stage.rawText
                aiTextDisplay = stage.aiText
                hasValidResult = true
            }
            is VoicePipelineStage.Error -> {
                micUiState = MicUiState.IDLE
                aiTextDisplay = "⚠️ ${stage.message}"
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = UiStrings.voiceStudioTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = UiStrings.backButtonContentDesc,
                            tint = DearTalkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DearTalkBackground)
            )
        },
        containerColor = DearTalkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🛡️ 1. 하드웨어 진단 및 Qwen 상태 카드
            HardwareDiagnosticCard(
                metrics = systemMetrics,
                packState = packState,
                onDownloadClick = {
                    modelLifecycleManager.startDownload(
                        onSuccess = { intentEngine.reloadModel() }
                    )
                },
                onPurgeClick = {
                    modelLifecycleManager.purgeModels()
                    intentEngine.reloadModel()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 🔀 2. 모드 탭 (고운말 톤 변환 vs 실시간 다국어 통역)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DearTalkSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeTabButton(
                    title = UiStrings.modeToneTransform,
                    isSelected = selectedMode == 0,
                    onClick = { selectedMode = 0 }
                )
                ModeTabButton(
                    title = UiStrings.modeLiveTranslation,
                    isSelected = selectedMode == 1,
                    onClick = { selectedMode = 1 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎛️ 3. 세부 옵션 칩 (고운말 톤 선택 or 실시간 통역 상대방 언어 선택)
            if (selectedMode == 0) {
                ToneSelectorRow(
                    selectedTone = selectedTone,
                    onToneSelected = { newTone ->
                        selectedTone = newTone
                        if (hasValidResult) executePipeline(rawTextDisplay, tone = newTone)
                    }
                )
            } else {
                TargetLanguageSelectorRow(
                    myLangCode = defaultAppLangCode,
                    targetLang = targetLanguage,
                    onTargetLangSelected = { newTarget ->
                        targetLanguage = newTarget
                        if (hasValidResult) executePipeline(rawTextDisplay, tgtLang = newTarget)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎙️ 4. 음성 성별(여성/남성) & 내 목소리 톤(피치) 조절 바
            VoiceCustomizerRow(
                selectedGender = selectedGender,
                onGenderSelected = { newGender ->
                    selectedGender = newGender
                    executeDirectSpeech(aiTextDisplay, gender = newGender)
                },
                selectedPitch = selectedPitch,
                onPitchSelected = { newPitch ->
                    selectedPitch = newPitch
                    executeDirectSpeech(aiTextDisplay, pitch = newPitch)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 📋 5. Dual-Card 화면 (원문 STT + AI 조율 텍스트)
            val srcLabel = if (selectedMode == 1) UiStrings.getLangDisplayName(defaultAppLangCode) else null
            val tgtLabel = if (selectedMode == 1) UiStrings.getLangDisplayName(targetLanguage) else null

            DualCardDisplay(
                rawText = if (rawTextDisplay.isBlank()) UiStrings.initialRawPrompt else rawTextDisplay,
                aiText = if (aiTextDisplay.isBlank()) UiStrings.initialAiPrompt else aiTextDisplay,
                isListening = isListening,
                pipelineStage = pipelineStage,
                sourceLangLabel = srcLabel,
                targetLangLabel = tgtLabel,
                onReplayClick = { executeDirectSpeech(aiTextDisplay) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 💡 6. 빠른 테스트 문장 칩
            QuickSampleChips(
                onSampleSelected = { sample ->
                    rawTextDisplay = sample
                    hasValidResult = true
                    executePipeline(sample)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔴 7. 메인 마이크 발화 & 녹음 제어 버튼
            MainRecordButton(
                micUiState = micUiState,
                rmsDb = rmsDb,
                onClick = {
                    if (isListening) {
                        sttManager.stopListening()
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            sttManager.startListening(LanguageLocaleHelper.getLocaleForCode(defaultAppLangCode))
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
