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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.agent.ModelDownloader
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
            android.util.Log.d("DearTalkAI", "🧪 [테스트 프롬프트 수신]: '$testPrompt'")
            lifecycleScope.launch {
                val res = intentEngine.process(testPrompt, "", "ai.deartalk.android.adb_test")
                when (res) {
                    is IntentResult.Success -> {
                        android.util.Log.d("DearTalkAI", "🎯 [AI 변환 성공]: '${res.text}' (${res.message})")
                    }
                    is IntentResult.Error -> {
                        android.util.Log.e("DearTalkAI", "⚠️ [AI 에러]: ${res.error}")
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

/**
 * Android 14+ (API 34+) SecurityException을 원천 방지하는 공식 InputMethodManager 기반 활성화 검사
 */
fun checkIsImeEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        enabledList.any { it.packageName == context.packageName }
    } catch (_: Exception) {
        false
    }
}

/**
 * 기본 키보드 선택 여부 검사 (SecurityException 안전 방어)
 */
fun checkIsImeSelected(context: Context): Boolean {
    return try {
        val defaultMethod = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        defaultMethod.contains(context.packageName)
    } catch (_: Exception) {
        // API 34+에서 SecurityException 발생 시 InputMethodManager 활성화 여부로 안전 폴백
        checkIsImeEnabled(context)
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isImeEnabled by remember { mutableStateOf(checkIsImeEnabled(context)) }
    var isImeSelected by remember { mutableStateOf(checkIsImeSelected(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isImeEnabled = checkIsImeEnabled(context)
                isImeSelected = checkIsImeSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isModelLoaded by intentEngine.isModelLoadedFlow.collectAsState(initial = intentEngine.isModelLoaded)
    val isDownloading by ModelDownloader.shared.isDownloading.collectAsState(initial = false)
    val downloadProgress by ModelDownloader.shared.progress.collectAsState(initial = 0.0f)
    val downloadedMB by ModelDownloader.shared.downloadedSizeMB.collectAsState(initial = 0.0)
    val totalMB by ModelDownloader.shared.totalSizeMB.collectAsState(initial = 0.0)
    val statusMessage by ModelDownloader.shared.statusMessage.collectAsState(initial = "")
    val downloadErrorMessage by ModelDownloader.shared.errorMessage.collectAsState(initial = null)

    var isAutoLanguage by remember { mutableStateOf(DearTalkSettings.isAutoLanguage(context)) }
    var selectedLanguageCode by remember { mutableStateOf(DearTalkSettings.getSelectedLanguageCode(context)) }
    var languageDisplayTitle by remember { mutableStateOf(DearTalkSettings.getLanguageDisplayTitle(context)) }

    var isListening by remember { mutableStateOf(false) }
    var recognizedLiveText by remember { mutableStateOf("") }
    var rawUtteranceText by remember { mutableStateOf("") }
    var aiTransformedText by remember { mutableStateOf("") }
    var aiProcessingMessage by remember { mutableStateOf("") }
    var testInputText by remember { mutableStateOf("") }

    var silenceTimeoutMs by remember { mutableFloatStateOf(DearTalkSettings.getSilenceTimeoutMillis(context).toFloat()) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var historyClearedMessage by remember { mutableStateOf("") }

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

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        contextRepository.clearAllHistory()
                        historyClearedMessage = if (isKorean) "✅ 대화 기록이 모두 초기화되었습니다." else "✅ All conversation history cleared."
                    }
                    showClearHistoryDialog = false
                }) {
                    Text(if (isKorean) "기록 삭제" else "Delete History", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(if (isKorean) "취소" else "Cancel", color = DearTalkTextDim)
                }
            },
            title = { Text(if (isKorean) "🗑️ AI 대화 기록 초기화" else "🗑️ Clear AI History", fontWeight = FontWeight.Bold) },
            text = { Text(if (isKorean) "내 휴대폰에 안전하게 저장된 AI 변환 기록을 모두 삭제합니다.\n삭제된 기록은 다시 복구할 수 없습니다." else "All AI conversation logs stored safely on this phone will be deleted.\nThis cannot be undone.") },
            containerColor = DearTalkSurface,
            titleContentColor = DearTalkText,
            textContentColor = DearTalkTextDim
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isKorean) "DearTalk AI 설정 및 가이드" else "DearTalk AI Settings & Guide", fontWeight = FontWeight.Bold) },
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
            // 🚀 1. 키보드 빠른 시작 안내 카드 (2단계 마법사)
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isImeSelected) Color(0xFF0F172A) else DearTalkSurface
                ),
                border = if (!isImeSelected) CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DearTalkSecondary)
                ) else null
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DearTalkSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = DearTalkSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isKorean) "🚀 1분 키보드 빠른 시작" else "🚀 Quick Keyboard Setup",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkText
                            )
                            Text(
                                text = if (isImeSelected) {
                                    if (isKorean) "✅ 기본 키보드로 설정되어 바로 사용할 수 있습니다." else "✅ Ready to use as your default keyboard."
                                } else {
                                    if (isKorean) "키보드를 사용하려면 아래 2단계를 완료해 주세요." else "Complete the 2 steps below to use the keyboard."
                                },
                                fontSize = 12.sp,
                                color = if (isImeSelected) Color(0xFF4ADE80) else DearTalkSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1단계: 키보드 켜기
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKorean) "1단계: 키보드 켜기 (활성화)" else "Step 1: Enable Keyboard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isImeEnabled) Color(0xFF4ADE80) else DearTalkText
                            )
                            Text(
                                text = if (isImeEnabled) {
                                    if (isKorean) "✅ DearTalk AI 키보드가 켜져 있습니다" else "✅ DearTalk AI keyboard is turned on"
                                } else {
                                    if (isKorean) "설정 화면에서 DearTalk AI 스위치를 켜주세요" else "Turn on the DearTalk AI switch in settings"
                                },
                                fontSize = 11.sp,
                                color = DearTalkTextDim
                            )
                        }

                        Button(
                            onClick = onEnableIme,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isImeEnabled) Color(0xFF166534) else DearTalkPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isImeEnabled) (if (isKorean) "✅ 완료" else "✅ Done") else (if (isKorean) "설정 열기 ➔" else "Open Settings ➔"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DearTalkKey.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2단계: 기본 키보드로 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKorean) "2단계: 기본 키보드로 선택" else "Step 2: Set as Default Keyboard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isImeSelected) Color(0xFF4ADE80) else DearTalkText
                            )
                            Text(
                                text = if (isImeSelected) {
                                    if (isKorean) "✅ 기본 키보드로 지정되어 어디서나 즉시 사용 가능" else "✅ Selected as default keyboard across all apps"
                                } else {
                                    if (isKorean) "팝업 창에서 DearTalk AI를 선택해 주세요" else "Select DearTalk AI from the popup list"
                                },
                                fontSize = 11.sp,
                                color = DearTalkTextDim
                            )
                        }

                        Button(
                            onClick = onSelectIme,
                            enabled = isImeEnabled,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isImeSelected) Color(0xFF166534) else DearTalkSecondary,
                                disabledContainerColor = DearTalkKey
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isImeSelected) {
                                    if (isKorean) "✅ 선택됨" else "✅ Selected"
                                } else {
                                    if (isKorean) "키보드 선택 ➔" else "Select Keyboard ➔"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isImeSelected) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 2. 🔒 100% 온디바이스 개인정보 보호 & AI 모델
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
                                text = if (isKorean) "🔒 100% 온디바이스 AI 안심 보호" else "🔒 100% On-Device AI Privacy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkSecondary
                            )
                            Text(
                                text = if (isModelLoaded) {
                                    if (isKorean) "✨ 내 폰 안에서만 작동 중 (외부 유출 0%)" else "✨ Running locally inside your phone (Zero Cloud Leak)"
                                } else {
                                    if (isKorean) "ℹ️ 오프라인 음성 인식 모드 가동 중" else "ℹ️ Offline Speech Recognition Mode Active"
                                },
                                fontSize = 12.sp,
                                color = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                text = if (isKorean) "🌐 AI 인식 언어: $languageDisplayTitle" else "🌐 AI Language: $languageDisplayTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DearTalkSecondary
                            )
                        }
                    }

                    if (!isModelLoaded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DearTalkKey.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isKorean) "📦 Google Gemma 온디바이스 AI 모델" else "📦 Google Gemma On-Device AI Model",
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
                                            text = if (isKorean) "휴대폰 저장 (~1.3GB)" else "Phone Storage (~1.3GB)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DearTalkSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isKorean)
                                        "외부 서버로 대화 내용이 전혀 전송되지 않고, 내 휴대폰 안에서만 안전하게 생각하고 답변하는 구글 Gemma AI 모델입니다. 다운로드 후 인터넷이 안 되는 곳에서도 100% 작동합니다."
                                    else
                                        "Google Gemma AI model executing 100% locally on your device without sending any chat data to the cloud. Works completely offline.",
                                    fontSize = 11.sp,
                                    color = DearTalkTextDim,
                                    lineHeight = 16.sp
                                )

                                if (isDownloading) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF020617))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isKorean) "⏳ AI 모델 다운로드 중..." else "⏳ Downloading AI Model...",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DearTalkSecondary
                                            )
                                            Text(
                                                text = "${String.format("%.1f", downloadProgress * 100f)}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DearTalkSecondary
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = DearTalkSecondary,
                                            trackColor = DearTalkKey
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = statusMessage.ifBlank {
                                                    if (isKorean) "다운로드 진행 중..." else "Downloading in progress..."
                                                },
                                                fontSize = 11.sp,
                                                color = DearTalkTextDim
                                            )
                                            TextButton(
                                                onClick = { ModelDownloader.shared.cancelDownload() },
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
                                                ModelDownloader.shared.startDownload(context, intentEngine)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkSecondary),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowCircleDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Black
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isKorean) "🚀 AI 모델 다운로드" else "🚀 Download AI Model",
                                                    fontSize = 12.sp,
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
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = DearTalkText
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isKorean) "내 폰 모델 다시 찾기" else "Rescan Phone",
                                                    fontSize = 12.sp,
                                                    color = DearTalkText
                                                )
                                            }
                                        }
                                    }
                                }

                                downloadErrorMessage?.let { err ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF7F1D1D).copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = err,
                                                fontSize = 11.sp,
                                                color = Color(0xFFFCA5A5)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DearTalkKey)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isKorean) "🔍 AI 엔진 작동 상태" else "🔍 AI Engine Status",
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
                        label = if (isKorean) "AI 엔진 상태" else "Engine Status",
                        value = if (isModelLoaded) (if (isKorean) "✅ 온디바이스 AI 정상 작동 중" else "✅ On-Device AI Active") else (if (isKorean) "⏳ 오프라인 음성인식 모드 가동" else "⏳ Offline Voice Mode"),
                        valueColor = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "설치된 모델" else "Installed Model",
                        value = if (modelFiles.isNotEmpty()) modelFiles.joinToString(", ") { "${it.name} (${it.length() / 1024 / 1024}MB)" } else (if (isKorean) "기본 내장 엔진" else "Default Engine"),
                        valueColor = if (modelFiles.isNotEmpty()) DearTalkText else Color(0xFFFCA5A5)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "개인정보 보호" else "Privacy",
                        value = if (isKorean) "🔒 100% 안전 (외부 서버 통신 0%)" else "🔒 100% Safe (Zero Cloud Traffic)",
                        valueColor = Color(0xFF4ADE80)
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 3. 🎙️ 실시간 음성 & AI 체험 샌드박스
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "🎙️ 실시간 음성 & AI 체험하기" else "🎙️ Real-time Voice & AI Sandbox",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "마이크를 누르고 말씀하시거나 아래 예시를 터치하여 AI가 문장을 어떻게 다듬는지 바로 확인해 보세요." else "Tap the mic and speak, or tap sample sentences to see how AI polishes them in real time.",
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
                                if (isKorean) "🛑 말씀 끝내기 (터치하여 AI 다듬기)" else "🛑 Finish Speaking (Polish with AI)"
                            } else {
                                if (isKorean) "🎙️ 마이크 켜고 말씀하기" else "🎙️ Tap to Speak"
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
                                Text(if (isKorean) "🎤 1. 내가 말한 내용:" else "🎤 1. What You Said:", fontSize = 11.sp, color = DearTalkTextDim, fontWeight = FontWeight.SemiBold)
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
                                Text(if (isKorean) "✨ 2. AI가 다듬은 문장:" else "✨ 2. AI Polished Result:", fontSize = 11.sp, color = DearTalkSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(aiTransformedText, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isKorean) "💡 예시 문장 눌러서 바로 테스트:" else "💡 Tap sample sentences to test:",
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
                                "금요일 제외한 매일 11시에서 11시30분까지는 Privacy 스크럼이니 절대로 잊지마",
                                "지금 출발했는데 도로가 너무 막혀서 15분 정도 늦을 것 같아",
                                "방금 수정한 기획안 메일로 보냈으니까 확인해보고 의견 줘",
                                "내일 점심 같이 먹을 수 있는지 시간 언제가 좋은지 알려줘",
                                "어제 부탁했던 회의록 정리 다 됐으면 나한테 넘겨줘",
                                "Hello how are you doing today please confirm",
                                "Where is the conference room please tell me"
                            )
                        } else {
                            listOf(
                                "Privacy scrum is from 11:00 to 11:30 every day except Friday so never forget",
                                "I just departed but traffic is heavy so I might be 15 minutes late",
                                "Sent the updated proposal via email please review and let me know your thoughts",
                                "Please let me know when works best for lunch tomorrow",
                                "Where is the conference room please tell me"
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
                                label = { Text(preset.take(24) + if (preset.length > 24) "..." else "", fontSize = 11.sp, color = DearTalkText) },
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

                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        label = { Text(if (isKorean) "키보드로 직접 써보기 (여기를 터치)" else "Type directly to test keyboard (Tap here)") },
                        placeholder = { Text(if (isKorean) "키보드 자판에서 음성과 말투 변환을 직접 써보세요" else "Test voice and tone directly on keyboard") },
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
            // 4. ⚙️ 키보드 환경 설정
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKorean) "⚙️ 기본 언어 자동 맞춤" else "⚙️ Auto Language Detection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkText
                            )
                            Text(
                                text = if (isKorean) "휴대폰 기본 설정 언어 자동 사용" else "Use phone's default system language",
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
                            text = if (isKorean) "👉 사용할 언어 직접 선택:" else "👉 Select language manually:",
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

                    Text(
                        text = if (isKorean) "🎙️ 말 끝남 자동 감지 시간" else "🎙️ Speech Pause Wait Time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isKorean) "말씀이 끝난 후 자동으로 입력을 완료할 때까지의 대기 시간 (${String.format("%.1f", silenceTimeoutMs / 1000f)}초)" else "Wait time after you stop speaking (${String.format("%.1f", silenceTimeoutMs / 1000f)}s)",
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
                        Text("1.5${if (isKorean) "초 (빠름)" else "s (Fast)"}", fontSize = 10.sp, color = DearTalkTextDim)
                        Text("8.0${if (isKorean) "초 (여유)" else "s (Relaxed)"}", fontSize = 10.sp, color = DearTalkTextDim)
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 5. 🗄️ 대화 기록 관리
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "🗄️ 대화 기록 관리" else "🗄️ History Management",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "AI 변환 기록은 오직 내 휴대폰에만 안전하게 보관되며, 외부 서버로 절대 전송되지 않습니다." else "All AI transformation logs are stored securely only on your phone and never sent to cloud servers.",
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
                            text = if (isKorean) "🗑️ AI 변환 기록 모두 지우기" else "🗑️ Clear All AI History",
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
            // 6. ℹ️ 앱 정보
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = UiStrings.settingsTabAbout,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val packageInfo = try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) { null }

                    val buildTimeStr = packageInfo?.let {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(it.lastUpdateTime))
                    } ?: "2026-08-23 21:45:00"

                    DiagnosticRow(
                        label = if (isKorean) "앱 이름" else "App Name",
                        value = "DearTalk AI",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = UiStrings.appVersionLabel,
                        value = "v${packageInfo?.versionName ?: "1.0.0"} (빌드 ${packageInfo?.longVersionCode ?: 1})",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = UiStrings.buildTimestampLabel,
                        value = buildTimeStr,
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = if (isKorean) "보안 등급" else "Security",
                        value = if (isKorean) "🔒 100% 온디바이스 (외부 유출 0%)" else "🔒 100% On-Device (Zero Cloud Leak)",
                        valueColor = DearTalkSecondary
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 7. 📖 사용 방법 안내
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = UiStrings.userGuideTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = UiStrings.userGuideHowToUseTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UiStrings.userGuideHowToUseContent,
                        fontSize = 12.sp,
                        color = DearTalkTextDim,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
            modifier = Modifier.width(100.dp)
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
