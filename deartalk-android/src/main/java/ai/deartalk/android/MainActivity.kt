package ai.deartalk.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.data.repository.ContextRepository
import ai.deartalk.android.ime.ui.theme.*
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var intentEngine: DearTalkIntentEngine
    private lateinit var contextRepository: ContextRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))

        sttManager = SpeechRecognitionManager(this)
        contextRepository = ContextRepository(this)
        intentEngine = DearTalkIntentEngine(this, contextRepository)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        handleTestIntent(intent)

        setContent {
            DearTalkTheme {
                MainOnDeviceScreen(
                    sttManager = sttManager,
                    intentEngine = intentEngine,
                    contextRepository = contextRepository,
                    onEnableIme = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onSelectIme = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleTestIntent(intent)
    }

    private fun handleTestIntent(intent: Intent?) {
        val testPrompt = intent?.getStringExtra("test_prompt")
        if (!testPrompt.isNullOrBlank()) {
            android.util.Log.d("DearTalkAI", "🧪 [ADB 테스트 프롬프트 수신]: '$testPrompt'")
            lifecycleScope.launch {
                val res = intentEngine.process(testPrompt, "", "ai.deartalk.android.adb_test")
                when (res) {
                    is IntentResult.Success -> {
                        android.util.Log.d("DearTalkAI", "🎯 [ADB 테스트 Gemma LLM 성공]: '${res.text}' (메시지: ${res.message})")
                    }
                    is IntentResult.Error -> {
                        android.util.Log.e("DearTalkAI", "⚠️ [ADB 테스트 에러]: ${res.error}")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sttManager.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOnDeviceScreen(
    sttManager: SpeechRecognitionManager,
    intentEngine: DearTalkIntentEngine,
    contextRepository: ContextRepository,
    onEnableIme: () -> Unit,
    onSelectIme: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isModelLoaded by intentEngine.isModelLoadedFlow.collectAsState(initial = intentEngine.isModelLoaded)
    val isDownloading by ai.deartalk.android.agent.ModelDownloader.shared.isDownloading.collectAsState(initial = false)
    val downloadProgress by ai.deartalk.android.agent.ModelDownloader.shared.progress.collectAsState(initial = 0.0f)
    val statusMessage by ai.deartalk.android.agent.ModelDownloader.shared.statusMessage.collectAsState(initial = "")
    val downloadErrorMessage by ai.deartalk.android.agent.ModelDownloader.shared.errorMessage.collectAsState(initial = null)


    // 설정 상태
    var isAutoLanguage by remember { mutableStateOf(DearTalkSettings.isAutoLanguage(context)) }
    var selectedLanguageCode by remember { mutableStateOf(DearTalkSettings.getSelectedLanguageCode(context)) }
    var languageDisplayTitle by remember { mutableStateOf(DearTalkSettings.getLanguageDisplayTitle(context)) }

    // 음성 인식 & AI 테스트 상태
    var isListening by remember { mutableStateOf(false) }
    var recognizedLiveText by remember { mutableStateOf("") }
    var rawUtteranceText by remember { mutableStateOf("") }
    var aiTransformedText by remember { mutableStateOf("") }
    var aiProcessingMessage by remember { mutableStateOf("") }
    var testInputText by remember { mutableStateOf("") }

    // 설정 UI 상태
    var silenceTimeoutMs by remember { mutableFloatStateOf(DearTalkSettings.getSilenceTimeoutMillis(context).toFloat()) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var historyClearedMessage by remember { mutableStateOf("") }

    // STT 상태 관찰
    LaunchedEffect(Unit) {
        sttManager.voiceState.collect { state ->
            when (state) {
                is VoiceState.Listening -> {
                    isListening = true
                    aiProcessingMessage = UiStrings.settingsListening
                }
                is VoiceState.PartialResult -> {
                    recognizedLiveText = state.text
                }
                is VoiceState.FinalResult -> {
                    isListening = false
                    recognizedLiveText = state.text
                    rawUtteranceText = state.text
                    aiProcessingMessage = UiStrings.settingsAiAnalyzing

                    // 100% 온디바이스 로컬 AI 실행
                    coroutineScope.launch {
                        val result = intentEngine.process(
                            voiceInput = state.text,
                            currentEditorText = testInputText,
                            packageName = "ai.deartalk.android.test"
                        )
                        when (result) {
                            is IntentResult.Success -> {
                                aiTransformedText = result.text
                                testInputText = result.text
                                aiProcessingMessage = result.message.ifBlank { UiStrings.settingsAiComplete }
                            }
                            is IntentResult.Error -> {
                                aiTransformedText = result.fallbackText
                                testInputText = result.fallbackText
                                aiProcessingMessage = "⚠️ ${result.error}"
                            }
                        }
                    }
                }
                is VoiceState.Error -> {
                    isListening = false
                    aiProcessingMessage = UiStrings.settingsSttError(state.errorCode)
                }
                else -> {
                    isListening = false
                }
            }
        }
    }

    val isKorean = DearTalkSettings.getEffectiveLocale(context).language == "ko"

    // 히스토리 초기화 확인 다이얼로그
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        contextRepository.clearAllHistory()
                        historyClearedMessage = if (isKorean) "✅ 히스토리가 초기화되었습니다" else "✅ History cleared"
                    }
                    showClearHistoryDialog = false
                }) {
                    Text(if (isKorean) "삭제" else "Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(if (isKorean) "취소" else "Cancel", color = DearTalkTextDim)
                }
            },
            title = { Text(if (isKorean) "🗑️ 히스토리 초기화" else "🗑️ Clear History", fontWeight = FontWeight.Bold) },
            text = { Text(if (isKorean) "모든 AI 변환 히스토리 데이터가 영구 삭제됩니다.\n이 작업은 되돌릴 수 없습니다." else "All AI transformation history will be permanently deleted.\nThis action cannot be undone.") },
            containerColor = DearTalkSurface,
            titleContentColor = DearTalkText,
            textContentColor = DearTalkTextDim
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isKorean) "DearTalkAI 설정 및 안내" else "DearTalkAI Settings & Guide", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DearTalkBackground,
                    titleContentColor = DearTalkText
                )
            )
        },
        containerColor = DearTalkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // 1. 🔒 100% 온디바이스 보안 배너 & AI 엔진 진단
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DearTalkSecondary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = DearTalkSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isKorean) "🔒 100% 온디바이스 AI 키보드" else "🔒 100% On-Device AI Keyboard",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkSecondary
                            )
                            Text(
                                text = if (isModelLoaded) {
                                    if (isKorean) "✨ 온디바이스 Gemma LLM 가동 중 (외부 통신 0%)" else "✨ On-Device Gemma LLM Running (Zero Network)"
                                } else {
                                    if (isKorean) "ℹ️ 온디바이스 Gemma LLM 모델 준비 중 (STT 원문 모드)" else "ℹ️ Initializing On-Device LLM (STT Raw Mode)"
                                },
                                fontSize = 12.sp,
                                color = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 현재 AI 번역/작동 언어 표시 뱃지
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DearTalkKey.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = DearTalkSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isKorean) "🌐 AI 작동 언어: $languageDisplayTitle" else "🌐 AI Target Language: $languageDisplayTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DearTalkSecondary
                            )
                        }
                    }

                    // 모델 미배치 시 원클릭 인앱 자동 다운로더 카드 노출
                    if (!isModelLoaded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DearTalkKey.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isKorean) "📦 Google Gemma 온디바이스 AI 모델 설치" else "📦 Install Google Gemma On-Device AI Model",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DearTalkText
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DearTalkSecondary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isKorean) "오프라인 저장 (~1.3GB)" else "Offline Store (~1.3GB)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DearTalkSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = if (isKorean) 
                                        "외부 도구 설치 없이 앱 내부에서 100% 온디바이스 Gemma 신경망을 원클릭으로 다운로드하여 즉시 실시간 글쓰기 교정을 활성화합니다."
                                    else 
                                        "Download the 100% on-device Gemma neural network directly inside the app with a single click to instantly activate real-time writing refinement.",
                                    fontSize = 11.sp,
                                    color = DearTalkTextDim
                                )

                                if (isDownloading) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = DearTalkSecondary,
                                            trackColor = DearTalkKey
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = statusMessage,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DearTalkSecondary
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            TextButton(
                                                onClick = { ai.deartalk.android.agent.ModelDownloader.shared.cancelDownload() },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text(
                                                    text = if (isKorean) "취소" else "Cancel",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { 
                                                ai.deartalk.android.agent.ModelDownloader.shared.startDownload(context, intentEngine) 
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkSecondary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowCircleDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color.Black
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isKorean) "🚀 온디바이스 AI 모델 다운로드" else "🚀 Download On-Device AI Model",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { 
                                                intentEngine.detectAndInitOnDeviceModel() 
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkKey),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = DearTalkText
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isKorean) "로컬 감지 새로고침" else "Refresh Local Detection",
                                                    fontSize = 11.sp,
                                                    color = DearTalkText
                                                )
                                            }
                                        }
                                    }
                                }

                                downloadErrorMessage?.let { err ->
                                    Text(
                                        text = err,
                                        fontSize = 10.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DearTalkKey)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 🔍 AI 엔진 상태 진단 정보
                    Text(
                        text = if (isKorean) "🔍 온디바이스 AI 엔진 진단" else "🔍 On-Device AI Engine Diagnostics",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val modelDir1 = File("/data/local/tmp/llm/")
                    val modelDir2 = File(context.filesDir, "models/")
                    val modelFiles1 = try {
                        modelDir1.listFiles()?.filter {
                            it.isFile &&
                            (it.name.endsWith(".litertlm", ignoreCase = true) || it.name.endsWith(".bin", ignoreCase = true)) &&
                            !it.name.contains("cache") &&
                            !it.name.contains("encoder") &&
                            !it.name.contains("adapter")
                        } ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                    val modelFiles2 = try {
                        modelDir2.listFiles()?.filter {
                            it.isFile &&
                            (it.name.endsWith(".litertlm", ignoreCase = true) || it.name.endsWith(".bin", ignoreCase = true))
                        } ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                    val modelFiles = modelFiles1 + modelFiles2

                    DiagnosticRow(
                        label = if (isKorean) "엔진 상태" else "Engine Status",
                        value = if (isModelLoaded) (if (isKorean) "✅ 정상 가동 중 (온디바이스 LLM)" else "✅ Running (On-Device LLM)") else (if (isKorean) "⏳ 초기화 중 / 미로드" else "⏳ Initializing"),
                        valueColor = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "로드된 모델" else "Loaded Model",
                        value = if (modelFiles.isNotEmpty()) modelFiles.joinToString(", ") { "${it.name} (${it.length() / 1024 / 1024}MB)" } else (if (isKorean) "온디바이스 기본 엔진" else "On-Device Default"),
                        valueColor = if (modelFiles.isNotEmpty()) DearTalkText else Color(0xFFFCA5A5)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "모델 디렉토리" else "Model Directory",
                        value = if (modelFiles2.isNotEmpty()) context.filesDir.absolutePath + "/models/" else "/data/local/tmp/llm/",
                        valueColor = DearTalkTextDim
                    )
                    DiagnosticRow(
                        label = if (isKorean) "프라이버시" else "Privacy",
                        value = if (isKorean) "🔒 외부 통신 0% (완전 오프라인)" else "🔒 Zero Network (Fully Offline)",
                        valueColor = Color(0xFF4ADE80)
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 2. 🎙️ 음성 테스트 샌드박스
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "🎙️ 실시간 음성 & AI 변환 테스트" else "🎙️ Real-time Voice & AI Sandbox",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "마이크를 누르고 말씀하신 뒤 다시 누르면 실시간 온디바이스 AI 조율 결과를 확인할 수 있습니다." else "Tap mic and speak, tap again to see real-time on-device AI results.",
                        fontSize = 11.sp,
                        color = DearTalkTextDim
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val transition = rememberInfiniteTransition(label = "sandbox_mic")
                    val pulseScale by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isListening) 1.12f else 1f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label = "pulse"
                    )

                    // 원버튼 마이크 토글: 시작 <-> 종료
                    Button(
                        onClick = {
                            if (isListening) {
                                sttManager.stopListening()
                            } else {
                                recognizedLiveText = ""
                                rawUtteranceText = ""
                                aiTransformedText = ""
                                testInputText = ""
                                sttManager.startListening()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(pulseScale),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) Color(0xFFDC2626) else DearTalkPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) {
                                if (isKorean) "🛑 마이크 종료 (터치하여 AI 변환)" else "🛑 Stop Mic (Refine with AI)"
                            } else {
                                if (isKorean) "🎙️ AI 음성 입력" else "🎙️ AI Voice Input"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (aiProcessingMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiProcessingMessage,
                            fontSize = 12.sp,
                            color = if (isListening) DearTalkSecondary else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 변환된 내역 (STT ➔ AI 기준 변경 내역)
                    if (rawUtteranceText.isNotBlank() || recognizedLiveText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DearTalkKey.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(if (isKorean) "🎤 1. 음성 인식 원본 (STT):" else "🎤 1. Recognized Speech (STT):", fontSize = 11.sp, color = DearTalkTextDim, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (rawUtteranceText.isNotBlank()) rawUtteranceText else recognizedLiveText,
                                    fontSize = 13.sp,
                                    color = DearTalkText
                                )
                            }
                        }
                    }

                    if (aiTransformedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DearTalkSecondary.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(if (isKorean) "✨ 2. AI 조율 및 다듬기 결과 (온디바이스 Gemma):" else "✨ 2. AI Refined Result (Gemma On-Device):", fontSize = 11.sp, color = DearTalkSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(aiTransformedText, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 시나리오 원클릭 프리셋 칩
                    Text(
                        text = if (isKorean) "💡 주요 대화 시나리오 원클릭 테스트:" else "💡 One-Click Scenario Tests:",
                        fontSize = 11.sp,
                        color = DearTalkTextDim,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (isKorean) {
                            listOf(
                                "내일 아침 9시 만나 이것을 좀 공손하게 바꿔 줘",
                                "지금 출발했어 조금 늦을 것 같아 정중하게 다듬어줘",
                                "다음 주 화요일 오후 2시 어떠냐고 물어봐줘",
                                "정리한 파일 보냈으니 확인해봐 공손하게 바꿔줘",
                                "지금 어디야 정중하게",
                                "Hello how are you doing today make it polite",
                                "Where is the meeting room please tell me"
                            )
                        } else {
                            listOf(
                                "Let's meet tomorrow at 9 AM make it polite",
                                "I just left, I might be a bit late, please polish it politely",
                                "Ask if next Tuesday 2 PM works",
                                "Sent the updated file please review make it polite",
                                "Where are you now politely"
                            )
                        }
                        presets.forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    rawUtteranceText = preset
                                    testInputText = ""
                                    aiProcessingMessage = UiStrings.settingsAiRefining
                                    coroutineScope.launch {
                                        val res = intentEngine.process(preset, "", "ai.deartalk.android.test")
                                        if (res is IntentResult.Success) {
                                            aiTransformedText = res.text
                                            testInputText = res.text
                                            aiProcessingMessage = res.message.ifBlank {
                                                UiStrings.settingsAiRefined
                                            }
                                        }
                                    }
                                },
                                label = { Text(preset.take(20) + "...", fontSize = 10.sp, color = DearTalkText) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Color(0xFF334155)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 실제 키보드로 타이핑해볼 수 있는 입력창
                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        label = { Text(if (isKorean) "키보드로 직접 테스트하기 (터치)" else "Type to test directly (Tap)") },
                        placeholder = { Text(if (isKorean) "키보드 자판을 띄워 테스트할 수 있습니다" else "Open keyboard to test typing") },
                        trailingIcon = {
                            if (testInputText.isNotBlank()) {
                                IconButton(onClick = { testInputText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = if (isKorean) "지우기" else "Clear", tint = DearTalkTextDim)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DearTalkSecondary,
                            unfocusedBorderColor = DearTalkKey,
                            focusedTextColor = DearTalkText,
                            unfocusedTextColor = DearTalkText,
                            focusedLabelColor = DearTalkSecondary,
                            unfocusedLabelColor = DearTalkTextDim
                        )
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 3. ⚙️ 키보드 설정 (언어, 침묵 타임아웃, 키보드 활성화)
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 언어 자동 감지 토글
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKorean) "⚙️ 키보드 AI 언어 설정" else "⚙️ Keyboard AI Language Settings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkText
                            )
                            Text(
                                text = if (isKorean) "안드로이드 설정 기본 언어 자동 사용" else "Use Android System Default Language",
                                fontSize = 12.sp,
                                color = DearTalkTextDim
                            )
                        }
                        Switch(
                            checked = isAutoLanguage,
                            onCheckedChange = { checked ->
                                isAutoLanguage = checked
                                DearTalkSettings.setAutoLanguage(context, checked)
                                UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(context))
                                languageDisplayTitle = DearTalkSettings.getLanguageDisplayTitle(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DearTalkSecondary,
                                checkedTrackColor = DearTalkPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (!isAutoLanguage) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isKorean) "👉 AI가 우선 인식하고 처리할 언어 직접 선택:" else "👉 Select target language for AI:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DearTalkSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DearTalkSettings.SUPPORTED_LANGUAGES.forEach { lang ->
                                val isSelected = selectedLanguageCode == lang.code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedLanguageCode = lang.code
                                        DearTalkSettings.setSelectedLanguageCode(context, lang.code)
                                        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(context))
                                        languageDisplayTitle = DearTalkSettings.getLanguageDisplayTitle(context)
                                    },
                                    label = { Text("${lang.flag} ${lang.nativeName}", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DearTalkPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = DearTalkKey,
                                        labelColor = DearTalkTextDim
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DearTalkKey)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 🎙️ 음성 인식 침묵 타임아웃 슬라이더
                    Text(
                        text = if (isKorean) "🎙️ 음성 인식 침묵 대기 시간" else "🎙️ Speech Silence Timeout",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isKorean) "말이 끊긴 후 자동 인식 완료까지의 대기 시간 (${String.format("%.1f", silenceTimeoutMs / 1000f)}초)" else "Wait time after speech pause (${String.format("%.1f", silenceTimeoutMs / 1000f)}s)",
                        fontSize = 11.sp,
                        color = DearTalkTextDim
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = silenceTimeoutMs,
                        onValueChange = { silenceTimeoutMs = it },
                        onValueChangeFinished = {
                            DearTalkSettings.setSilenceTimeoutMillis(context, silenceTimeoutMs.toLong())
                        },
                        valueRange = 1500f..8000f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = DearTalkSecondary,
                            activeTrackColor = DearTalkPrimary,
                            inactiveTrackColor = DearTalkKey
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1.5${if (isKorean) "초" else "s"}", fontSize = 10.sp, color = DearTalkTextDim)
                        Text("8${if (isKorean) "초" else "s"}", fontSize = 10.sp, color = DearTalkTextDim)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DearTalkKey)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 키보드 활성화 & 시스템 등록
                    Text(
                        text = if (isKorean) "키보드 활성화 및 시스템 등록" else "Keyboard Activation & IME Setup",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onEnableIme,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkKeyActive)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DearTalkSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isKorean) "키보드 켜짐" else "Keyboard Enabled", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onSelectIme,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkKeyActive)
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, tint = DearTalkSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isKorean) "기본 키보드 설정됨" else "Default Selected", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 4. 🗄️ 데이터 관리
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "🗄️ 데이터 관리" else "🗄️ Data Management",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "AI 변환 히스토리는 앱 내부에만 저장되며 외부 전송되지 않습니다." else "AI transformation history is stored locally only and never transmitted.",
                        fontSize = 11.sp,
                        color = DearTalkTextDim
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKorean) "🗑️ 모든 AI 변환 히스토리 초기화" else "🗑️ Clear All AI History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    }

                    if (historyClearedMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = historyClearedMessage,
                            fontSize = 12.sp,
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 5. ℹ️ 앱 정보
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "ℹ️ 앱 정보" else "ℹ️ App Info",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val packageInfo = try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) { null }

                    DiagnosticRow(
                        label = if (isKorean) "앱 이름" else "App Name",
                        value = "DearTalkAI",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = if (isKorean) "패키지" else "Package",
                        value = context.packageName,
                        valueColor = DearTalkTextDim
                    )
                    DiagnosticRow(
                        label = if (isKorean) "버전" else "Version",
                        value = "${packageInfo?.versionName ?: "1.0"} (${packageInfo?.longVersionCode ?: 1})",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = if (isKorean) "AI 철학" else "AI Philosophy",
                        value = if (isKorean) "🔒 100% 온디바이스 · 외부통신 0% · 가짜규칙 0%" else "🔒 100% On-Device · Zero Network · Zero Fake",
                        valueColor = DearTalkSecondary
                    )
                }
            }

            // 하단 여백
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 진단 정보를 한 줄로 표시하는 레이아웃 컴포넌트
 */
@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color = DearTalkText
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = DearTalkTextDim,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
