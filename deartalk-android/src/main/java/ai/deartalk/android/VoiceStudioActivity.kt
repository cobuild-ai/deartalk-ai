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
        sttManager.destroy()
        voicePipeline.releaseMemory()
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
    var selectedTone by remember { mutableStateOf("공손하게") }
    var sourceLanguage by remember { mutableStateOf("KO") } // 🗣️ 내가 말할 언어 (STT 입력)
    var targetLanguage by remember { mutableStateOf("JA") } // 🌐 번역할 목표 언어 (TTS 출력)
    var selectedGender by remember { mutableStateOf(ai.deartalk.android.tts.VoiceGender.FEMALE) }
    var selectedPitch by remember { mutableStateOf(1.0f) }
    var micUiState by remember { mutableStateOf(MicUiState.IDLE) }
    var rawTextDisplay by remember { mutableStateOf("하단의 마이크 버튼을 누르고 말씀해 보세요.") }
    var aiTextDisplay by remember { mutableStateOf("AI가 정제한 결과가 이곳에 표시됩니다.") }

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
            val targetLocale = getLocaleForCode(if (selectedMode == 1) sourceLanguage else "KO")
            sttManager.startListening(targetLocale)
        } else {
            Toast.makeText(context, "음성 인식을 위해 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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
                rawTextDisplay = "마이크 준비 중..."
            }
            is VoiceState.Listening -> {
                micUiState = MicUiState.LISTENING
                rawTextDisplay = "🎤 듣고 있습니다... 말씀해 주세요."
            }
            is VoiceState.PartialResult -> {
                micUiState = MicUiState.LISTENING
                rawTextDisplay = state.text
            }
            is VoiceState.FinalResult -> {
                micUiState = MicUiState.PROCESSING_AI
                rawTextDisplay = state.text
                if (state.text.isNotBlank()) {
                    voicePipeline.processVoiceInput(
                        simulatedVoiceText = state.text,
                        targetLang = if (selectedMode == 1) targetLanguage else "KO",
                        sourceLang = if (selectedMode == 1) sourceLanguage else "KO",
                        tone = if (selectedMode == 0) selectedTone else null,
                        gender = selectedGender,
                        pitch = selectedPitch
                    )
                }
            }
            is VoiceState.Error -> {
                micUiState = MicUiState.IDLE
                if (rawTextDisplay.startsWith("🎤") || rawTextDisplay.startsWith("마이크")) {
                    rawTextDisplay = "음성이 감지되지 않았습니다. 다시 탭하고 말씀해 보세요."
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
                aiTextDisplay = "✨ AI가 문맥과 언어를 정제하고 있습니다..."
            }
            is VoicePipelineStage.SynthesizingTTS -> {
                micUiState = MicUiState.PROCESSING_AI
                aiTextDisplay = stage.aiText
            }
            is VoicePipelineStage.Completed -> {
                micUiState = MicUiState.IDLE
                rawTextDisplay = stage.rawText
                aiTextDisplay = stage.aiText
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
                            text = "🎙️ DearTalk Voice Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = DearTalkText)
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
                    title = "✨ 고운말 톤 변환",
                    isSelected = selectedMode == 0,
                    onClick = { selectedMode = 0 }
                )
                ModeTabButton(
                    title = "🌐 실시간 다국어 통역",
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
                        if (rawTextDisplay.isNotBlank() && !rawTextDisplay.startsWith("하단의") && !rawTextDisplay.startsWith("🎤") && !rawTextDisplay.startsWith("음성")) {
                            voicePipeline.processVoiceInput(
                                simulatedVoiceText = rawTextDisplay,
                                targetLang = "KO",
                                sourceLang = "KO",
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
                        if (rawTextDisplay.isNotBlank() && !rawTextDisplay.startsWith("하단의") && !rawTextDisplay.startsWith("🎤") && !rawTextDisplay.startsWith("음성")) {
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
                        if (rawTextDisplay.isNotBlank() && !rawTextDisplay.startsWith("하단의") && !rawTextDisplay.startsWith("🎤") && !rawTextDisplay.startsWith("음성")) {
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
                        if (rawTextDisplay.isNotBlank() && !rawTextDisplay.startsWith("하단의") && !rawTextDisplay.startsWith("🎤") && !rawTextDisplay.startsWith("음성")) {
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

            // 🎙️ 4. 음성 성별(여성/남성) & 내 목소리 톤(피치) 조절 바 (변경 즉시 현재 텍스트 프리뷰 발화)
            VoiceCustomizerRow(
                selectedGender = selectedGender,
                onGenderSelected = { newGender ->
                    selectedGender = newGender
                    if (aiTextDisplay.isNotBlank() && !aiTextDisplay.startsWith("✨") && !aiTextDisplay.startsWith("AI가")) {
                        voicePipeline.speakDirectly(
                            text = aiTextDisplay,
                            targetLang = if (selectedMode == 1) targetLanguage else "KO",
                            gender = newGender,
                            pitch = selectedPitch
                        )
                    }
                },
                selectedPitch = selectedPitch,
                onPitchSelected = { newPitch ->
                    selectedPitch = newPitch
                    if (aiTextDisplay.isNotBlank() && !aiTextDisplay.startsWith("✨") && !aiTextDisplay.startsWith("AI가")) {
                        voicePipeline.speakDirectly(
                            text = aiTextDisplay,
                            targetLang = if (selectedMode == 1) targetLanguage else "KO",
                            gender = selectedGender,
                            pitch = newPitch
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 📋 5. Dual-Card 화면 (원문 STT + AI 조율 텍스트)
            DualCardDisplay(
                rawText = rawTextDisplay,
                aiText = aiTextDisplay,
                isListening = isListening,
                pipelineStage = pipelineStage,
                onReplayClick = {
                    if (aiTextDisplay.isNotBlank() && !aiTextDisplay.startsWith("✨") && !aiTextDisplay.startsWith("AI가")) {
                        voicePipeline.speakDirectly(
                            text = aiTextDisplay,
                            targetLang = if (selectedMode == 1) targetLanguage else "KO",
                            gender = selectedGender,
                            pitch = selectedPitch
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 💡 6. 빠른 테스트 샘플 칩
            QuickSampleChips(
                onSampleSelected = { sample ->
                    rawTextDisplay = sample
                    voicePipeline.processVoiceInput(
                        simulatedVoiceText = sample,
                        targetLang = if (selectedMode == 1) targetLanguage else "KO",
                        sourceLang = if (selectedMode == 1) sourceLanguage else "KO",
                        tone = if (selectedMode == 0) selectedTone else null,
                        gender = selectedGender,
                        pitch = selectedPitch
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🎤 7. 대형 마이크 실시간 녹음 버튼 (STT 완벽 연동 & 키보드와 동일한 토글 UX)
            MainRecordButton(
                micUiState = micUiState,
                rmsDb = rmsDb,
                onClick = {
                    when (micUiState) {
                        MicUiState.LISTENING -> {
                            sttManager.stopListening()
                        }
                        MicUiState.IDLE -> {
                            val hasAudioPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasAudioPerm) {
                                val targetLocale = getLocaleForCode(if (selectedMode == 1) sourceLanguage else "KO")
                                sttManager.startListening(targetLocale)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        MicUiState.PREPARING, MicUiState.PROCESSING_AI -> {}
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

    val tierText = when (metrics.tierRating) {
        DeviceTierRating.OPTIMAL -> "🟢 최적 사양 (RAM ${String.format("%.1f", metrics.totalRamGb)}GB / 여유 ${String.format("%.1f", metrics.availableStorageGb)}GB)"
        DeviceTierRating.CAUTION -> "🟡 주의 사양 (RAM ${String.format("%.1f", metrics.totalRamGb)}GB - 순차 파이프라인 안전 구동)"
        DeviceTierRating.RESTRICTED -> "🔴 사양 제한 (RAM ${String.format("%.1f", metrics.totalRamGb)}GB - 기본 모델 권장)"
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
                            Text("Qwen 고품질 보이스 모델", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                            Text("STT(0.6B) + LLM(1.7B) + TTS(0.6B) · 1.8GB", fontSize = 11.sp, color = DearTalkTextDim)
                        }
                        Button(
                            onClick = onDownloadClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("다운로드", fontSize = 12.sp)
                        }
                    }
                }
                is ModelPackState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("고품질 패키지 다운로드 중...", fontSize = 13.sp, color = DearTalkSecondary)
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
                            Text("Qwen 고성능 엔진 활성화됨", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                        }
                        IconButton(onClick = onPurgeClick) {
                            Icon(Icons.Default.Delete, contentDescription = "모델 삭제", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                is ModelPackState.Error -> {
                    Text("⚠️ 다운로드 오류: ${packState.message}", fontSize = 12.sp, color = Color(0xFFEF4444))
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
    val tones = listOf("공손하게", "친근하게", "다정하게", "비즈니스", "당당하게", "재미있게")
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
    val languages = listOf(
        "KO" to "🇰🇷 한국어",
        "EN" to "🇺🇸 영어",
        "JA" to "🇯🇵 일본어",
        "ZH" to "🇨🇳 중국어",
        "ES" to "🇪🇸 스페인어",
        "FR" to "🇫🇷 프랑스어",
        "DE" to "🇩🇪 독일어",
        "ID" to "🇮🇩 인도네시아어",
        "VI" to "🇻🇳 베트남어",
        "TL" to "🇵🇭 필리핀어",
        "TH" to "🇹🇭 태국어",
        "MS" to "🇲🇾 말레이어"
    )

    val sourceLabel = languages.find { it.first == sourceLang }?.second ?: sourceLang
    val targetLabel = languages.find { it.first == targetLang }?.second ?: targetLang

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
                    Text("🗣️ 입력: ", fontSize = 11.sp, color = DearTalkTextDim)
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
                        contentDescription = "입력/출력 언어 맞바꾸기",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌐 번역: ", fontSize = 11.sp, color = DearTalkTextDim)
                    Text(targetLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. 내가 말할 언어 (마이크 STT 입력)
            Text("1️⃣ 내가 말할 언어 (마이크 입력):", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DearTalkTextDim)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.forEach { (code, label) ->
                    val isSelected = sourceLang == code
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
            Text("2️⃣ AI가 번역할 언어 (스피커 출력):", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DearTalkTextDim)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.forEach { (code, label) ->
                    val isSelected = targetLang == code
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
                        text = if (isListening) "음성 인식 중..." else "내가 말한 내용 (STT)",
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
                        Text("✨ AI 조율 및 번역 결과", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                    }
                    IconButton(onClick = onReplayClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "다시 듣기", tint = DearTalkSecondary, modifier = Modifier.size(18.dp))
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
    val samples = listOf(
        "오늘 밥 같이 먹을래?",
        "차 막혀서 늦을 것 같아 미안해",
        "자료 검토 후 회신 부탁드립니다",
        "이 제품 가격이 어떻게 되나요?"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("💡 빠른 테스트 문장", fontSize = 12.sp, color = DearTalkTextDim)
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
                        contentDescription = "준비 중",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                MicUiState.LISTENING -> {
                    Icon(
                        Icons.Default.StopCircle,
                        contentDescription = "녹음 중지",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                MicUiState.IDLE -> {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "말하기",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (micUiState) {
                MicUiState.PREPARING -> "⏳ 마이크 준비 중..."
                MicUiState.LISTENING -> "🔴 녹음 중... (탭하여 완료)"
                MicUiState.PROCESSING_AI -> "✨ AI 정제 및 번역 중..."
                MicUiState.IDLE -> "탭하여 마이크로 말하기"
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
                Text("🎙️ 발화 음색", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
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
                            "👩 여성 음성",
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
                            "👨 남성 음성",
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
                Text("🎚️ 톤 매칭:", fontSize = 11.sp, color = DearTalkTextDim)
                listOf(
                    "보통" to 1.0f,
                    "중후한 저음" to 0.85f,
                    "부드러운 중음" to 0.95f,
                    "밝은 고음" to 1.15f
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

