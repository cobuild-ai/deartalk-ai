package ai.deartalk.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.SequentialVoicePipeline
import ai.deartalk.android.agent.VoicePipelineStage
import ai.deartalk.android.data.DeviceTierRating
import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.data.ModelPackState
import ai.deartalk.android.data.SystemDiagnosticManager
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkKey
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSecondary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.ime.ui.theme.DearTalkTextDim
import ai.deartalk.android.ime.ui.theme.DearTalkTheme
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import ai.deartalk.android.tts.TextToSpeechManager
import java.util.Locale

class VoiceStudioActivity : ComponentActivity() {

    private lateinit var diagnosticManager: SystemDiagnosticManager
    private lateinit var modelLifecycleManager: ModelLifecycleManager
    private lateinit var intentEngine: DearTalkIntentEngine
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var voicePipeline: SequentialVoicePipeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 사용자 설정 언어로 다국어 리소스 동기화
        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))

        diagnosticManager = SystemDiagnosticManager(this)
        modelLifecycleManager = ModelLifecycleManager(this)
        intentEngine = DearTalkIntentEngine(this)
        ttsManager = TextToSpeechManager(this)
        sttManager = SpeechRecognitionManager(this)
        voicePipeline = SequentialVoicePipeline(
            context = this,
            intentEngine = intentEngine,
            ttsManager = ttsManager,
            modelLifecycleManager = modelLifecycleManager
        )

        setContent {
            DearTalkTheme {
                VoiceStudioScreen(
                    diagnosticManager = diagnosticManager,
                    modelLifecycleManager = modelLifecycleManager,
                    sttManager = sttManager,
                    voicePipeline = voicePipeline,
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePipeline.releaseMemory()
        sttManager.destroy()
        ttsManager.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(
    diagnosticManager: SystemDiagnosticManager,
    modelLifecycleManager: ModelLifecycleManager,
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
    var selectedMode by remember { mutableStateOf(0) } // 0: 고운말 톤 변환, 1: 실시간 다국어 통역
    var selectedTone by remember { mutableStateOf(UiStrings.tonePolite) }
    var sourceLanguage by remember { mutableStateOf(if (UiStrings.isKo) "KO" else if (UiStrings.isId) "ID" else "EN") }
    var targetLanguage by remember { mutableStateOf(if (UiStrings.isKo) "JA" else "KO") }
    var selectedGender by remember { mutableStateOf(ai.deartalk.android.tts.VoiceGender.FEMALE) }
    var selectedPitch by remember { mutableStateOf(1.0f) }
    var micUiState by remember { mutableStateOf(MicUiState.IDLE) }
    
    // 명확한 상태 관리: 원문 및 AI 결과 텍스트 (문자열 startsWith 파싱 제거)
    var rawTextDisplay by remember { mutableStateOf("") }
    var aiTextDisplay by remember { mutableStateOf("") }
    var hasValidResult by remember { mutableStateOf(false) }

    val isListening = micUiState == MicUiState.LISTENING

    fun getLocaleForCode(code: String): Locale = when (code.uppercase()) {
        "EN" -> Locale.US
        "ES" -> Locale("es", "ES")
        "FR" -> Locale.FRANCE
        "DE" -> Locale.GERMANY
        "JA" -> Locale.JAPAN
        "ZH" -> Locale.CHINESE
        "ID" -> Locale("id", "ID")
        "VI" -> Locale("vi", "VN")
        "TL", "FIL" -> Locale("fil", "PH")
        "TH" -> Locale("th", "TH")
        "MS" -> Locale("ms", "MY")
        else -> Locale.KOREAN
    }

    // 마이크 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val targetLocale = getLocaleForCode(if (selectedMode == 1) sourceLanguage else if (UiStrings.isKo) "KO" else "EN")
            sttManager.startListening(targetLocale)
        } else {
            Toast.makeText(context, UiStrings.micPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        systemMetrics = diagnosticManager.diagnose()
    }

    // 🎙️ 실시간 STT 상태 감지 및 지속적인 녹음 상태(Listening) 유지
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
                if (state.text.isNotBlank()) {
                    voicePipeline.processVoiceInput(
                        simulatedVoiceText = state.text,
                        targetLang = if (selectedMode == 1) targetLanguage else if (UiStrings.isKo) "KO" else "EN",
                        sourceLang = if (selectedMode == 1) sourceLanguage else if (UiStrings.isKo) "KO" else "EN",
                        tone = if (selectedMode == 0) selectedTone else null,
                        gender = selectedGender,
                        pitch = selectedPitch
                    )
                }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = UiStrings.voiceStudioTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = UiStrings.backButtonContentDesc, tint = DearTalkText)
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
                onDownloadClick = { modelLifecycleManager.startDownload() },
                onPurgeClick = { modelLifecycleManager.purgeModels() }
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

            // 🎛️ 3. 세부 옵션 칩 (고운말 톤 선택 or 양방향 다국어 통역 언어 선택)
            if (selectedMode == 0) {
                ToneSelectorRow(
                    selectedTone = selectedTone,
                    onToneSelected = { newTone ->
                        selectedTone = newTone
                        if (hasValidResult && rawTextDisplay.isNotBlank()) {
                            voicePipeline.processVoiceInput(
                                simulatedVoiceText = rawTextDisplay,
                                targetLang = if (UiStrings.isKo) "KO" else "EN",
                                sourceLang = if (UiStrings.isKo) "KO" else "EN",
                                tone = newTone,
                                gender = selectedGender,
                                pitch = selectedPitch
                            )
                        }
                    }
                )
            } else {
                TwoWayLanguageSelectorRow(
                    sourceLang = sourceLanguage,
                    onSourceLangSelected = { newSource ->
                        sourceLanguage = newSource
                        if (hasValidResult && rawTextDisplay.isNotBlank()) {
                            voicePipeline.processVoiceInput(
                                simulatedVoiceText = rawTextDisplay,
                                targetLang = targetLanguage,
                                sourceLang = newSource,
                                tone = null,
                                gender = selectedGender,
                                pitch = selectedPitch
                            )
                        }
                    },
                    targetLang = targetLanguage,
                    onTargetLangSelected = { newTarget ->
                        targetLanguage = newTarget
                        if (hasValidResult && rawTextDisplay.isNotBlank()) {
                            voicePipeline.processVoiceInput(
                                simulatedVoiceText = rawTextDisplay,
                                targetLang = newTarget,
                                sourceLang = sourceLanguage,
                                tone = null,
                                gender = selectedGender,
                                pitch = selectedPitch
                            )
                        }
                    },
                    onSwap = {
                        val temp = sourceLanguage
                        sourceLanguage = targetLanguage
                        targetLanguage = temp
                        if (hasValidResult && rawTextDisplay.isNotBlank()) {
                            voicePipeline.processVoiceInput(
                                simulatedVoiceText = rawTextDisplay,
                                targetLang = temp,
                                sourceLang = targetLanguage,
                                tone = null,
                                gender = selectedGender,
                                pitch = selectedPitch
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎙️ 4. 음성 성별(여성/남성) & 내 목소리 톤(피치) 조절 바
            VoiceCustomizerRow(
                selectedGender = selectedGender,
                onGenderSelected = { newGender ->
                    selectedGender = newGender
                    if (aiTextDisplay.isNotBlank()) {
                        voicePipeline.speakDirectly(
                            text = aiTextDisplay,
                            targetLang = if (selectedMode == 1) targetLanguage else if (UiStrings.isKo) "KO" else "EN",
                            gender = newGender,
                            pitch = selectedPitch
                        )
                    }
                },
                selectedPitch = selectedPitch,
                onPitchSelected = { newPitch ->
                    selectedPitch = newPitch
                    if (aiTextDisplay.isNotBlank()) {
                        voicePipeline.speakDirectly(
                            text = aiTextDisplay,
                            targetLang = if (selectedMode == 1) targetLanguage else if (UiStrings.isKo) "KO" else "EN",
                            gender = selectedGender,
                            pitch = newPitch
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 📋 5. Dual-Card 화면 (원문 STT + AI 조율 텍스트)
            DualCardDisplay(
                rawText = if (rawTextDisplay.isBlank()) UiStrings.initialRawPrompt else rawTextDisplay,
                aiText = if (aiTextDisplay.isBlank()) UiStrings.initialAiPrompt else aiTextDisplay,
                isListening = isListening,
                pipelineStage = pipelineStage,
                onReplayClick = {
                    if (aiTextDisplay.isNotBlank()) {
                        voicePipeline.speakDirectly(
                            text = aiTextDisplay,
                            targetLang = if (selectedMode == 1) targetLanguage else if (UiStrings.isKo) "KO" else "EN",
                            gender = selectedGender,
                            pitch = selectedPitch
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 💡 6. 빠른 테스트 문장 칩
            QuickSampleChips(
                onSampleSelected = { sample ->
                    rawTextDisplay = sample
                    hasValidResult = true
                    voicePipeline.processVoiceInput(
                        simulatedVoiceText = sample,
                        targetLang = if (selectedMode == 1) targetLanguage else if (UiStrings.isKo) "KO" else "EN",
                        sourceLang = if (selectedMode == 1) sourceLanguage else if (UiStrings.isKo) "KO" else "EN",
                        tone = if (selectedMode == 0) selectedTone else null,
                        gender = selectedGender,
                        pitch = selectedPitch
                    )
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
                            val targetLocale = getLocaleForCode(if (selectedMode == 1) sourceLanguage else if (UiStrings.isKo) "KO" else "EN")
                            sttManager.startListening(targetLocale)
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

@Composable
fun HardwareDiagnosticCard(
    metrics: ai.deartalk.android.data.SystemMetrics,
    packState: ModelPackState,
    onDownloadClick: () -> Unit,
    onPurgeClick: () -> Unit
) {
    val tierColor = when (metrics.tierRating) {
        DeviceTierRating.OPTIMAL -> Color(0xFF10B981)
        DeviceTierRating.CAUTION -> Color(0xFFF59E0B)
        DeviceTierRating.RESTRICTED -> Color(0xFFEF4444)
    }

    val ramFormatted = String.format(Locale.US, "%.1f", metrics.totalRamGb)
    val storageFormatted = String.format(Locale.US, "%.1f", metrics.availableStorageGb)

    val tierText = when (metrics.tierRating) {
        DeviceTierRating.OPTIMAL -> UiStrings.diagOptimal(ramFormatted, storageFormatted)
        DeviceTierRating.CAUTION -> UiStrings.diagCaution(ramFormatted)
        DeviceTierRating.RESTRICTED -> UiStrings.diagRestricted(ramFormatted)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tierColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tierText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tierColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (packState) {
                is ModelPackState.NotInstalled -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(UiStrings.diagModelTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                            Text(UiStrings.diagModelSubtitle, fontSize = 11.sp, color = DearTalkTextDim)
                        }
                        Button(
                            onClick = onDownloadClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(UiStrings.diagDownloadBtn, fontSize = 12.sp)
                        }
                    }
                }
                is ModelPackState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(UiStrings.diagDownloadingLabel, fontSize = 13.sp, color = DearTalkSecondary)
                            Text("${packState.progressPercent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DearTalkSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { packState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = DearTalkSecondary,
                            trackColor = DearTalkKey
                        )
                    }
                }
                is ModelPackState.Installed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(UiStrings.diagActiveLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                        }
                        IconButton(onClick = onPurgeClick) {
                            Icon(Icons.Default.Delete, contentDescription = UiStrings.diagPurgeContentDesc, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                is ModelPackState.Error -> {
                    Text(UiStrings.diagErrorLabel(packState.message), fontSize = 12.sp, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun ModeTabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DearTalkPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else DearTalkTextDim
        )
    }
}

@Composable
fun ToneSelectorRow(selectedTone: String, onToneSelected: (String) -> Unit) {
    val tones = listOf(
        UiStrings.toneRefine,
        UiStrings.tonePolite,
        UiStrings.toneCasual,
        UiStrings.toneBusiness,
        UiStrings.toneCheeky,
        UiStrings.toneFunny
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tones.forEach { tone ->
            val isSelected = selectedTone == tone
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) DearTalkSecondary else DearTalkKey)
                    .clickable { onToneSelected(tone) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tone,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else DearTalkText
                )
            }
        }
    }
}

@Composable
fun TwoWayLanguageSelectorRow(
    sourceLang: String,
    onSourceLangSelected: (String) -> Unit,
    targetLang: String,
    onTargetLangSelected: (String) -> Unit,
    onSwap: () -> Unit
) {
    val langCodes = listOf("KO", "EN", "JA", "ZH", "ES", "FR", "DE", "ID", "VI", "TL", "TH", "MS")

    val sourceLabel = UiStrings.getLangDisplayName(sourceLang)
    val targetLabel = UiStrings.getLangDisplayName(targetLang)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 헤더: [ 🗣️ 말할 언어 ]  ⇄ (스왑)  [ 🌐 번역 언어 ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(UiStrings.inputHeaderLabel, fontSize = 11.sp, color = DearTalkTextDim)
                    Text(sourceLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkSecondary)
                }

                // ⇄ 언어 맞바꾸기(Swap) 버튼
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DearTalkKey)
                        .clickable(onClick = onSwap)
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = UiStrings.swapLangContentDesc,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(UiStrings.outputHeaderLabel, fontSize = 11.sp, color = DearTalkTextDim)
                    Text(targetLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. 내가 말할 언어 (마이크 STT 입력)
            Text(UiStrings.step1SpokenLang, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DearTalkTextDim)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                langCodes.forEach { code ->
                    val isSelected = sourceLang.equals(code, ignoreCase = true)
                    val label = UiStrings.getLangDisplayName(code)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) DearTalkSecondary else DearTalkKey)
                            .clickable { onSourceLangSelected(code) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else DearTalkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. AI가 번역할 언어 (스피커 TTS 출력)
            Text(UiStrings.step2TargetLang, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DearTalkTextDim)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                langCodes.forEach { code ->
                    val isSelected = targetLang.equals(code, ignoreCase = true)
                    val label = UiStrings.getLangDisplayName(code)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) DearTalkPrimary else DearTalkKey)
                            .clickable { onTargetLangSelected(code) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else DearTalkText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DualCardDisplay(
    rawText: String,
    aiText: String,
    isListening: Boolean,
    pipelineStage: VoicePipelineStage,
    onReplayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 상단: 내가 말한 원문
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isListening) Color(0xFFEF4444) else DearTalkSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) UiStrings.rawSttListening else UiStrings.rawSttTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color(0xFFEF4444) else DearTalkSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rawText,
                    fontSize = 15.sp,
                    color = DearTalkText,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 하단: AI 조율 / 번역 결과
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.horizontalGradient(listOf(DearTalkPrimary, DearTalkSecondary)), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(UiStrings.aiResultTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                    }
                    IconButton(onClick = onReplayClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = UiStrings.replayButtonDesc, tint = DearTalkSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = aiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 24.sp
                )

                // 파형 및 상태 표시 애니메이션
                if (pipelineStage is VoicePipelineStage.SynthesizingTTS || pipelineStage is VoicePipelineStage.RefiningLLM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    WaveformVisualizer()
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale1 by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 1.0f, animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val scale2 by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 0.2f, animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val scale3 by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(scale1, scale2, scale3, scale1, scale2).forEach { scale ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height((20 * scale).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DearTalkSecondary)
            )
        }
    }
}

@Composable
fun QuickSampleChips(onSampleSelected: (String) -> Unit) {
    val samples = UiStrings.quickSamples
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(UiStrings.quickTestTitle, fontSize = 12.sp, color = DearTalkTextDim)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            samples.forEach { sample ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DearTalkKey)
                        .clickable { onSampleSelected(sample) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(sample, fontSize = 12.sp, color = DearTalkText)
                }
            }
        }
    }
}

@Composable
fun MainRecordButton(
    micUiState: MicUiState,
    rmsDb: Float = 0f,
    onClick: () -> Unit
) {
    val isListening = micUiState == MicUiState.LISTENING
    val isPreparing = micUiState == MicUiState.PREPARING
    val isProcessing = micUiState == MicUiState.PROCESSING_AI

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // 실제 마이크 데시벨(rmsDb)에 따른 다이내믹 스케일 (1.0 ~ 1.25)
    val dynamicScale = if (isListening) {
        (pulseScale + (rmsDb.coerceIn(0f, 10f) / 35f)).coerceIn(1.0f, 1.3f)
    } else {
        1.0f
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .scale(dynamicScale)
                .clip(CircleShape)
                .background(
                    when (micUiState) {
                        MicUiState.PREPARING -> Brush.radialGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                        MicUiState.LISTENING -> Brush.radialGradient(listOf(Color(0xFFFF2E2E), Color(0xFF991B1B)))
                        MicUiState.PROCESSING_AI -> Brush.radialGradient(listOf(DearTalkPrimary, Color(0xFF4338CA)))
                        MicUiState.IDLE -> Brush.radialGradient(listOf(DearTalkPrimary, Color(0xFF4338CA)))
                    }
                )
                .clickable(enabled = !isPreparing && !isProcessing, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when (micUiState) {
                MicUiState.PROCESSING_AI -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                }
                MicUiState.PREPARING -> {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = UiStrings.contentDescPreparing,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                MicUiState.LISTENING -> {
                    Icon(
                        Icons.Default.StopCircle,
                        contentDescription = UiStrings.contentDescStop,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                MicUiState.IDLE -> {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = UiStrings.contentDescSpeak,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (micUiState) {
                MicUiState.PREPARING -> UiStrings.micBtnPreparing
                MicUiState.LISTENING -> UiStrings.micBtnListening
                MicUiState.PROCESSING_AI -> UiStrings.micBtnProcessing
                MicUiState.IDLE -> UiStrings.micBtnIdle
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when (micUiState) {
                MicUiState.PREPARING -> Color(0xFFD97706)
                MicUiState.LISTENING -> Color(0xFFFF2E2E)
                MicUiState.PROCESSING_AI -> DearTalkSecondary
                MicUiState.IDLE -> DearTalkTextDim
            }
        )
    }
}

@Composable
fun VoiceCustomizerRow(
    selectedGender: ai.deartalk.android.tts.VoiceGender,
    onGenderSelected: (ai.deartalk.android.tts.VoiceGender) -> Unit,
    selectedPitch: Float,
    onPitchSelected: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // 성별 선택 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(UiStrings.voiceToneCustomizerTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkKey)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedGender == ai.deartalk.android.tts.VoiceGender.FEMALE) DearTalkPrimary else Color.Transparent)
                            .clickable { onGenderSelected(ai.deartalk.android.tts.VoiceGender.FEMALE) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            UiStrings.voiceFemale,
                            fontSize = 11.sp,
                            fontWeight = if (selectedGender == ai.deartalk.android.tts.VoiceGender.FEMALE) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedGender == ai.deartalk.android.tts.VoiceGender.FEMALE) Color.White else DearTalkTextDim
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedGender == ai.deartalk.android.tts.VoiceGender.MALE) DearTalkPrimary else Color.Transparent)
                            .clickable { onGenderSelected(ai.deartalk.android.tts.VoiceGender.MALE) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            UiStrings.voiceMale,
                            fontSize = 11.sp,
                            fontWeight = if (selectedGender == ai.deartalk.android.tts.VoiceGender.MALE) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedGender == ai.deartalk.android.tts.VoiceGender.MALE) Color.White else DearTalkTextDim
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 피치(음높이) 매칭 칩
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(UiStrings.pitchMatchingLabel, fontSize = 11.sp, color = DearTalkTextDim)
                listOf(
                    UiStrings.pitchNormal to 1.0f,
                    UiStrings.pitchDeepLow to 0.85f,
                    UiStrings.pitchWarmMid to 0.95f,
                    UiStrings.pitchBrightHigh to 1.15f
                ).forEach { (label, pitch) ->
                    val isSelected = kotlin.math.abs(selectedPitch - pitch) < 0.04f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) DearTalkSecondary else DearTalkKey)
                            .clickable { onPitchSelected(pitch) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else DearTalkText
                        )
                    }
                }
            }
        }
    }
}
