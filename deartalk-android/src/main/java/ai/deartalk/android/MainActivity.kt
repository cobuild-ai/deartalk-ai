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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.KeyboardMode
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ui.dialog.ModeSelectionDialog
import ai.deartalk.android.ime.ui.theme.*
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.stt.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var intentEngine: DearTalkIntentEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val testLocale = intent?.getStringExtra("test_locale")
        if (!testLocale.isNullOrBlank()) {
            DearTalkSettings.setAutoLanguage(this, false)
            DearTalkSettings.setSelectedLanguageCode(this, testLocale)
            UiStrings.setLocale(java.util.Locale.forLanguageTag(testLocale))
        } else {
            UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))
        }

        sttManager = SpeechRecognitionManager(this)
        intentEngine = DearTalkIntentEngine(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        handleTestIntent(intent)

        setContent {
            DearTalkTheme {
                MainOnDeviceScreen(
                    sttManager = sttManager,
                    intentEngine = intentEngine,
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
        setIntent(intent)
        val testLocale = intent.getStringExtra("test_locale")
        if (!testLocale.isNullOrBlank()) {
            DearTalkSettings.setAutoLanguage(this, false)
            DearTalkSettings.setSelectedLanguageCode(this, testLocale)
            UiStrings.setLocale(java.util.Locale.forLanguageTag(testLocale))
        }
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

    override fun onPause() {
        super.onPause()
        sttManager.cancelListening()
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
    val loadedModelName by intentEngine.loadedModelNameFlow.collectAsState(initial = "기본 모드 (온디바이스 STT)")

    var isAutoLanguage by remember { mutableStateOf(DearTalkSettings.isAutoLanguage(context)) }
    var selectedLanguageCode by remember { mutableStateOf(DearTalkSettings.getSelectedLanguageCode(context)) }
    var languageDisplayTitle by remember { mutableStateOf(DearTalkSettings.getLanguageDisplayTitle(context)) }

    var isListening by remember { mutableStateOf(false) }
    var recognizedLiveText by remember { mutableStateOf("") }
    var rawUtteranceText by remember { mutableStateOf("") }
    var aiTransformedText by remember { mutableStateOf("") }
    var aiProcessingMessage by remember { mutableStateOf("") }
    var testInputText by remember { mutableStateOf("") }
    var activePresetText by remember { mutableStateOf("") }
    var presetJob by remember { mutableStateOf<Job?>(null) }

    var selectedKoreanKeyboardType by remember { mutableStateOf(DearTalkSettings.getKoreanKeyboardType(context)) }
    var currentKeyboardMode by remember { mutableStateOf(DearTalkSettings.getKeyboardMode(context)) }
    var showOnboardingDialog by remember { mutableStateOf(!DearTalkSettings.isOnboardingModeShown(context)) }


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

    val isKorean = UiStrings.isKo
    val isIndonesian = UiStrings.isId

    if (showOnboardingDialog) {
        ModeSelectionDialog(
            currentMode = currentKeyboardMode,
            onDismiss = {
                showOnboardingDialog = false
                DearTalkSettings.setOnboardingModeShown(context, true)
            },
            onConfirmMode = { newMode ->
                currentKeyboardMode = newMode
                DearTalkSettings.setKeyboardMode(context, newMode)
                DearTalkSettings.setOnboardingModeShown(context, true)
                showOnboardingDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isKorean) "DearTalk AI 설정 및 가이드"
                        else if (isIndonesian) "Pengaturan DearTalk AI"
                        else "DearTalk AI Settings & Guide",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
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
            // 🚀 1. 키보드 빠른 시작 안내 카드 (ImeSetupGuideCard)
            // ═══════════════════════════════════════════════════
            ai.deartalk.android.ui.main.ImeSetupGuideCard(
                isImeEnabled = isImeEnabled,
                isImeSelected = isImeSelected,
                isKorean = isKorean,
                isIndonesian = isIndonesian,
                onEnableIme = onEnableIme,
                onSelectIme = onSelectIme
            )

            // ═══════════════════════════════════════════════════
            // 2. 🔒 100% 온디바이스 개인정보 보호 및 상태 진단
            // ═══════════════════════════════════════════════════
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
                                text = if (isKorean) "🔒 100% 온디바이스 안심 보호" else if (isIndonesian) "🔒 Privasi 100% On-Device" else "🔒 100% On-Device Privacy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkSecondary
                            )
                            Text(
                                text = if (isModelLoaded) {
                                    if (isKorean) "내 폰 안에서만 작동 중 (외부 유출 0%)" else if (isIndonesian) "Berjalan lokal di HP (100% Aman)" else "Running locally inside your phone (Zero Cloud Leak)"
                                } else {
                                    if (isKorean) "오프라인 음성 인식 모드 가동 중" else if (isIndonesian) "Mode Pengenalan Suara Offline Aktif" else "Offline Speech Recognition Mode Active"
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
                                text = if (isKorean) "인식 언어: $languageDisplayTitle" else if (isIndonesian) "Bahasa: $languageDisplayTitle" else "Language: $languageDisplayTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DearTalkSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DearTalkKey.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = DearTalkSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isModelLoaded) {
                                    if (isKorean) "온디바이스 실시간 처리"
                                    else if (isIndonesian) "Pemrosesan Cepat On-Device"
                                    else "On-Device Real-Time Processing"
                                } else {
                                    if (isKorean) "온디바이스 음성 인식 가동 중"
                                    else if (isIndonesian) "Pengenalan Suara On-Device"
                                    else "On-Device Voice Recognition"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DearTalkSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isKorean) "🔒 100% 온디바이스 보호" else if (isIndonesian) "🔒 100% On-Device" else "🔒 100% On-Device",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DearTalkText,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DearTalkSecondary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isKorean) "⚡ 100% 오프라인" else if (isIndonesian) "⚡ 100% Offline" else "⚡ 100% Offline",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DearTalkSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Text(
                                    text = if (isKorean)
                                        "외부 서버 전송 없이 기기 내에서 100% 오프라인으로 0.2초 만에 안전하게 다듬어 줍니다."
                                    else if (isIndonesian)
                                        "Data tidak dikirim ke server, diproses 100% offline langsung di perangkat dalam 0,2 detik."
                                    else
                                        "Processed 100% locally on-device in ~0.2s without sending data to external servers.",
                                    fontSize = 11.sp,
                                    color = DearTalkTextDim,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = {
                                        intentEngine.detectAndInitOnDeviceModel()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkSecondary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                    ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isKorean) "상태 다시 확인" else if (isIndonesian) "Periksa Ulang Status" else "Check Status",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DearTalkKey)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isKorean) "🔍 보안 및 동작 상태" else if (isIndonesian) "🔍 Status Keamanan & Operasi" else "🔍 Security & Operation Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow(
                        label = if (isKorean) "동작 상태" else if (isIndonesian) "Status Operasi" else "Status",
                        value = if (isModelLoaded) (if (isKorean) "✅ 온디바이스 정상 작동 중" else if (isIndonesian) "✅ On-Device Aktif Normal" else "✅ On-Device Active") else (if (isKorean) "⏳ 오프라인 음성인식 모드" else if (isIndonesian) "⏳ Mode Suara Offline" else "⏳ Offline Voice Mode"),
                        valueColor = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "동작 방식" else if (isIndonesian) "Metode Pemrosesan" else "Processing Mode",
                        value = if (isKorean) "⚡ 100% 로컬 오프라인 처리" else if (isIndonesian) "⚡ 100% Pemrosesan Offline Lokal" else "⚡ 100% Local Offline Processing",
                        valueColor = Color(0xFF4ADE80)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "개인정보 보호" else if (isIndonesian) "Privasi" else "Privacy",
                        value = if (isKorean) "🔒 100% 안전 (외부 서버 통신 0%)" else if (isIndonesian) "🔒 100% Aman (Nol Trafik Cloud)" else "🔒 100% Safe (Zero Cloud Traffic)",
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
                        text = if (isKorean) "🎙️ 실시간 음성 & AI 체험하기" else if (isIndonesian) "🎙️ Uji Coba Suara & AI" else "🎙️ Real-time Voice & AI Sandbox",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "마이크를 켜고 말씀하시거나 예시를 눌러 AI 다듬기를 체험해 보세요." else if (isIndonesian) "Bicara lewat mikrofon atau ketuk contoh kalimat untuk mencoba AI." else "Tap mic to speak or tap sample sentences to test AI refinement.",
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
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListening) {
                                if (isKorean) "녹음 중... 터치하여 완료" else if (isIndonesian) "Merekam... Ketuk Selesai" else "Recording... Tap to Stop"
                            } else {
                                if (isKorean) "마이크 켜고 말씀하기" else if (isIndonesian) "Ketuk untuk Bicara" else "Tap to Speak"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
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
                                Text(if (isKorean) "1. 내가 말한 내용:" else if (isIndonesian) "1. Yang Anda Katakan:" else "1. What You Said:", fontSize = 11.sp, color = DearTalkTextDim, fontWeight = FontWeight.SemiBold)
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
                                Text(if (isKorean) "2. 다듬어진 문장:" else if (isIndonesian) "2. Hasil Dirapikan:" else "2. Refined Result:", fontSize = 11.sp, color = DearTalkSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(aiTransformedText, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isKorean) "💡 예시 문장 눌러서 바로 테스트:" else if (isIndonesian) "💡 Ketuk contoh kalimat untuk uji coba:" else "💡 Tap sample sentences to test:",
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
                        } else if (isIndonesian) {
                            listOf(
                                "Macet sekali, mungkin telat 15 menit ya",
                                "Proposal sudah saya kirim, tolong dicek ya",
                                "Ada waktu makan siang bareng besok?",
                                "Ruang rapat di lantai berapa ya?",
                                "Terima kasih banyak atas bantuannya"
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
                            val isCurrentActive = activePresetText == preset
                            SuggestionChip(
                                onClick = {
                                    activePresetText = preset
                                    rawUtteranceText = preset
                                    // ⚡ 0ms 즉각 반응: 원문은 즉시 노출하고, AI 결과 영역에는 다듬는 중 상태 표시
                                    testInputText = preset
                                    aiTransformedText = "✨ AI가 문맥과 맞춤법을 다듬는 중..."
                                    aiProcessingMessage = UiStrings.settingsAiRefining

                                    // 이전 비동기 작업 즉시 취소 후 새 추론 가동 (연타 시 딜레이 방지)
                                    presetJob?.cancel()
                                    presetJob = coroutineScope.launch(Dispatchers.IO) {
                                        val res = intentEngine.process(preset, "", "ai.deartalk.android.test")
                                        withContext(Dispatchers.Main) {
                                            when (res) {
                                                is IntentResult.Success -> {
                                                    aiTransformedText = res.text
                                                    testInputText = res.text
                                                    aiProcessingMessage = res.message.ifBlank {
                                                        UiStrings.settingsAiRefined
                                                    }
                                                }
                                                is IntentResult.Error -> {
                                                    aiTransformedText = res.fallbackText
                                                    testInputText = res.fallbackText
                                                    aiProcessingMessage = UiStrings.settingsAiRefined
                                                }
                                            }
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = preset.take(24) + if (preset.length > 24) "..." else "",
                                        fontSize = 11.sp,
                                        color = if (isCurrentActive) DearTalkSecondary else DearTalkText,
                                        fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isCurrentActive) DearTalkSecondary.copy(alpha = 0.18f) else Color(0xFF1E293B)
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isCurrentActive) DearTalkSecondary else Color(0xFF334155)
                                )
                            )
                        }
                    }

                    // 💬 모의 메신저 대화 시뮬레이터 (프로 모드 비즈니스 체험 전용)
                    if (currentKeyboardMode == KeyboardMode.PRO) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(DearTalkPrimary.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👔", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isKorean) "팀장님" else if (isIndonesian) "Alex (Manajer)" else "Alex (Lead)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DearTalkText
                                        )
                                        Text(
                                            text = if (isKorean) "비즈니스 대화 체험" else if (isIndonesian) "Simulasi Obrolan" else "Mock Chat Simulator",
                                            fontSize = 10.sp,
                                            color = DearTalkSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "10:45 AM",
                                        fontSize = 10.sp,
                                        color = DearTalkTextDim
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isKorean) "오늘 오후 프로젝트 회의 15분 뒤에 시작합니다. 참석 가능하신가요?"
                                        else if (isIndonesian) "Rapat proyek hari ini dimulai 15 menit lagi. Apakah bisa hadir?"
                                        else "Today's project sync starts in 15 minutes. Can you join?",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        label = { Text(if (isKorean) "💬 답장 작성하기 (터치하여 키보드 실행)" else if (isIndonesian) "💬 Balas pesan (Ketuk untuk buka keyboard)" else "💬 Reply to message (Tap to open keyboard)") },
                        placeholder = { Text(if (isKorean) "마이크로 말씀하시거나 예시를 눌러보세요" else if (isIndonesian) "Bicara atau ketuk contoh kalimat" else "Speak naturally or tap samples to refine") },
                        trailingIcon = {
                            if (testInputText.isNotBlank()) {
                                IconButton(onClick = { testInputText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = if (isKorean) "지우기" else if (isIndonesian) "Hapus" else "Clear", tint = DearTalkTextDim)
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
            // 🌟 🎙️ 온디바이스 AI 대면 통역 & 보이스 스튜디오 (DearTalk Live)
            // ═══════════════════════════════════════════════════
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DearTalkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DearTalkPrimary)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(DearTalkPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = DearTalkSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isKorean) "🎙️ DearTalk Live" else if (isIndonesian) "🎙️ DearTalk Live" else "🎙️ DearTalk Live",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DearTalkText
                                    )
                                    Text(
                                        text = if (isKorean) "1:1 대면 실시간 통역 & 음성 스튜디오" else if (isIndonesian) "Percakapan 1:1 & studio suara AI" else "1:1 live conversation & voice studio",
                                        fontSize = 11.sp,
                                        color = DearTalkSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(context, ai.deartalk.android.live.DearTalkLiveActivity::class.java)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isKorean) "열기 ➔" else if (isIndonesian) "Buka ➔" else "Open ➔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                    // ═══════════════════════════════════════════════════
                    // 🎛️ 키보드 사용 모드 선택 (기본 모드 vs 프로 모드)
                    // ═══════════════════════════════════════════════════
                    Text(
                        text = if (isKorean) "🎛️ 키보드 사용 모드" else if (isIndonesian) "🎛️ Mode Penggunaan Keyboard" else "🎛️ Keyboard Usage Mode",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isKorean) "화면 구성 및 타이핑 환경 맞춤 설정" else if (isIndonesian) "Atur tata letak keyboard sesuai kebutuhan" else "Customize keyboard layout and typing options",
                        fontSize = 12.sp,
                        color = DearTalkTextDim
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 🟢 기본 모드 버튼
                        val isBasic = currentKeyboardMode == KeyboardMode.BASIC
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isBasic) Color(0xFF4338CA).copy(alpha = 0.3f) else Color(0xFF1E293B))
                                .border(
                                    width = if (isBasic) 1.5.dp else 1.dp,
                                    color = if (isBasic) Color(0xFF818CF8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    currentKeyboardMode = KeyboardMode.BASIC
                                    DearTalkSettings.setKeyboardMode(context, KeyboardMode.BASIC)
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💬", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isKorean) "기본 모드" else if (isIndonesian) "Mode Dasar" else "Basic Mode",
                                    fontSize = 13.sp,
                                    fontWeight = if (isBasic) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isBasic) Color.White else DearTalkText
                                )
                                Text(
                                    text = if (isKorean) "일상 대화·어조 다듬기" else if (isIndonesian) "Percakapan Harian" else "Everyday Conversation",
                                    fontSize = 10.sp,
                                    color = if (isBasic) Color(0xFFA5B4FC) else DearTalkTextDim
                                )
                            }
                        }

                        // 🔵 프로 모드 버튼
                        val isPro = currentKeyboardMode == KeyboardMode.PRO
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPro) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B))
                                .border(
                                    width = if (isPro) 1.5.dp else 1.dp,
                                    color = if (isPro) Color(0xFF38BDF8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            .clickable {
                                currentKeyboardMode = KeyboardMode.PRO
                                DearTalkSettings.setKeyboardMode(context, KeyboardMode.PRO)
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌐", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isKorean) "프로 모드" else if (isIndonesian) "Mode Pro" else "Pro Mode",
                                    fontSize = 13.sp,
                                    fontWeight = if (isPro) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPro) Color.White else DearTalkText
                                )
                                Text(
                                    text = if (isKorean) "기본 포함·글로벌 비즈니스" else if (isIndonesian) "Bisnis Global + Live" else "Global Business + Live",
                                    fontSize = 10.sp,
                                    color = if (isPro) Color(0xFF7DD3FC) else DearTalkTextDim
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⌨️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isKorean) "기본 한글 자판" else if (isIndonesian) "Tata Letak Hangul" else "Hangul Layout",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DearTalkText
                                    )
                                    Text(
                                        text = if (isKorean) "두벌식 또는 천지인 선택" else if (isIndonesian) "Pilih Dubeolsik atau Cheonjiin" else "Select Dubeolsik or Cheonjiin",
                                        fontSize = 10.sp,
                                        color = DearTalkTextDim
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DearTalkKeyActive)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedKoreanKeyboardType == KoreanKeyboardType.DUBEOLSIK) DearTalkPrimary else Color.Transparent)
                                        .clickable {
                                            selectedKoreanKeyboardType = KoreanKeyboardType.DUBEOLSIK
                                            DearTalkSettings.setKoreanKeyboardType(context, KoreanKeyboardType.DUBEOLSIK)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "두벌식",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedKoreanKeyboardType == KoreanKeyboardType.DUBEOLSIK) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedKoreanKeyboardType == KoreanKeyboardType.DUBEOLSIK) Color.White else DearTalkTextDim
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedKoreanKeyboardType == KoreanKeyboardType.CHEONJIIN) DearTalkPrimary else Color.Transparent)
                                        .clickable {
                                            selectedKoreanKeyboardType = KoreanKeyboardType.CHEONJIIN
                                            DearTalkSettings.setKoreanKeyboardType(context, KoreanKeyboardType.CHEONJIIN)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "천지인",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedKoreanKeyboardType == KoreanKeyboardType.CHEONJIIN) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedKoreanKeyboardType == KoreanKeyboardType.CHEONJIIN) Color.White else DearTalkTextDim
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKorean) "⚙️ 기본 언어 자동 맞춤" else if (isIndonesian) "⚙️ Deteksi Bahasa Otomatis" else "⚙️ Auto Language Detection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkText
                            )
                            Text(
                                text = if (isKorean) "휴대폰 기본 설정 언어 자동 사용" else if (isIndonesian) "Gunakan bahasa sistem utama ponsel" else "Use phone's default system language",
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
                            text = if (isKorean) "👉 사용할 언어 직접 선택:" else if (isIndonesian) "👉 Pilih bahasa secara manual:" else "👉 Select language manually:",
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
                }
            }

            // ═══════════════════════════════════════════════════
            // 5. 📖 사용 방법 안내
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

            // ═══════════════════════════════════════════════════
            // 6. 🛡️ 100% Zero-Persistence 프라이버시
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "🛡️ 100% Zero-Persistence 프라이버시" else if (isIndonesian) "🛡️ Privasi 100% Zero-Persistence" else "🛡️ 100% Zero-Persistence Privacy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "음성과 변환 내용은 단 1바이트도 저장되지 않으며, 변환 즉시 완전히 소멸됩니다." else if (isIndonesian) "Suara dan teks tidak disimpan sedikit pun, langsung dihapus dari memori." else "No voice or text data is stored. Disappears from memory instantly.",
                        fontSize = 11.sp,
                        color = DearTalkTextDim,
                        lineHeight = 16.sp
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 6. ⌨️ 키보드 언제든 변경 / 전환 (사용자 편의 기능)
            // ═══════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isKorean) "⌨️ 키보드 언제든 변경 / 전환" else if (isIndonesian) "⌨️ Beralih / Ganti Papan Ketik" else "⌨️ Switch / Change Keyboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean)
                            "기존 키보드(삼성, Gboard 등)로 언제든 자유롭게 되돌아가실 수 있습니다."
                        else if (isIndonesian)
                            "Anda dapat kembali ke keyboard sebelumnya (Samsung, Gboard, dll.) kapan saja."
                        else
                            "Switch back to your previous keyboard (Samsung, Gboard, etc.) anytime.",
                        fontSize = 11.sp,
                        color = DearTalkTextDim,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSelectIme,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DearTalkSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKorean) "🔄 다른 키보드로 바로 전환하기" else if (isIndonesian) "🔄 Beralih ke Papan Ketik Lain" else "🔄 Switch to Another Keyboard",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 7. ℹ️ 앱 정보
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
                        label = if (isKorean) "앱 이름" else if (isIndonesian) "Nama Aplikasi" else "App Name",
                        value = "DearTalk AI",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = UiStrings.appVersionLabel,
                        value = "v${packageInfo?.versionName ?: "1.0.0"} (${if (isKorean) "빌드" else "Build"} ${packageInfo?.longVersionCode ?: 1})",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = UiStrings.buildTimestampLabel,
                        value = buildTimeStr,
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = if (isKorean) "보안 등급" else if (isIndonesian) "Keamanan" else "Security",
                        value = if (isKorean) "🔒 100% 온디바이스 (외부 유출 0%)" else if (isIndonesian) "🔒 100% On-Device (Nol Kebocoran)" else "🔒 100% On-Device (Zero Cloud Leak)",
                        valueColor = DearTalkSecondary
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = DearTalkTextDim,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 80.dp, max = 110.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            lineHeight = 15.sp
        )
    }
}
