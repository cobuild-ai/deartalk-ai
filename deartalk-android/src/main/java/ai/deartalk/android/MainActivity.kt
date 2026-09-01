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
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
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

        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))

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

    var silenceTimeoutMs by remember { mutableFloatStateOf(DearTalkSettings.getSilenceTimeoutMillis(context).toFloat()) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isKorean) "DearTalk AI 설정 및 가이드"
                        else if (isIndonesian) "Pengaturan & Panduan DearTalk AI"
                        else "DearTalk AI Settings & Guide",
                        fontWeight = FontWeight.Bold
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
                                text = if (isKorean) "🚀 1분 키보드 빠른 시작" else if (isIndonesian) "🚀 Mulai Cepat Papan Ketik 1 Menit" else "🚀 Quick Keyboard Setup",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkText
                            )
                            Text(
                                text = if (isImeSelected) {
                                    if (isKorean) "✅ 기본 키보드로 설정되어 바로 사용할 수 있습니다." else if (isIndonesian) "✅ Sudah diatur sebagai papan ketik utama dan siap digunakan." else "✅ Ready to use as your default keyboard."
                                } else {
                                    if (isKorean) "키보드를 사용하려면 아래 2단계를 완료해 주세요." else if (isIndonesian) "Selesaikan 2 langkah di bawah ini untuk mulai menggunakan." else "Complete the 2 steps below to use the keyboard."
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
                                text = if (isKorean) "1단계: 키보드 켜기 (활성화)" else if (isIndonesian) "Langkah 1: Aktifkan Papan Ketik" else "Step 1: Enable Keyboard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isImeEnabled) Color(0xFF4ADE80) else DearTalkText
                            )
                            Text(
                                text = if (isImeEnabled) {
                                    if (isKorean) "✅ DearTalk AI 키보드가 켜져 있습니다" else if (isIndonesian) "✅ Papan ketik DearTalk AI telah aktif" else "✅ DearTalk AI keyboard is turned on"
                                } else {
                                    if (isKorean) "설정 화면에서 DearTalk AI 스위치를 켜주세요" else if (isIndonesian) "Nyalakan tombol DearTalk AI di pengaturan sistem" else "Turn on the DearTalk AI switch in settings"
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
                                text = if (isImeEnabled) (if (isKorean) "✅ 완료" else if (isIndonesian) "✅ Selesai" else "✅ Done") else (if (isKorean) "설정 열기 ➔" else if (isIndonesian) "Buka Pengaturan ➔" else "Open Settings ➔"),
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
                                text = if (isKorean) "2단계: 기본 키보드로 선택" else if (isIndonesian) "Langkah 2: Pilih Papan Ketik Utama" else "Step 2: Set as Default Keyboard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isImeSelected) Color(0xFF4ADE80) else DearTalkText
                            )
                            Text(
                                text = if (isImeSelected) {
                                    if (isKorean) "✅ 기본 키보드로 지정되어 어디서나 즉시 사용 가능" else if (isIndonesian) "✅ Dipilih sebagai papan ketik utama di semua aplikasi" else "✅ Selected as default keyboard across all apps"
                                } else {
                                    if (isKorean) "팝업 창에서 DearTalk AI를 선택해 주세요" else if (isIndonesian) "Pilih DearTalk AI pada daftar sembul (popup)" else "Select DearTalk AI from the popup list"
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
                                    if (isKorean) "✅ 선택됨" else if (isIndonesian) "✅ Terpilih" else "✅ Selected"
                                } else {
                                    if (isKorean) "키보드 선택 ➔" else if (isIndonesian) "Pilih Papan Ketik ➔" else "Select Keyboard ➔"
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
            // 🌟 🎙️ 온디바이스 AI 대면 통역 & 보이스 스튜디오
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
                            Column {
                                Text(
                                    text = if (isKorean) "🎙️ Voice Studio (대면 통역 & 고운말)" else if (isIndonesian) "🎙️ Voice Studio (Penerjemah & Suara)" else "🎙️ Voice Studio (Live Interpreter)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DearTalkText
                                )
                                Text(
                                    text = if (isKorean) "Qwen 기반 다국어 실시간 통역 & 고운말 톤 스피킹" else if (isIndonesian) "Terjemahan langsung multibahasa & penyesuaian nada berbasis Qwen" else "Qwen-powered live multilingual translation & tone speaking",
                                    fontSize = 11.sp,
                                    color = DearTalkSecondary
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val intent = android.content.Intent(context, VoiceStudioActivity::class.java)
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
                                text = if (isKorean) "🔒 100% 온디바이스 AI 안심 보호" else if (isIndonesian) "🔒 Privasi AI 100% On-Device" else "🔒 100% On-Device AI Privacy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkSecondary
                            )
                            Text(
                                text = if (isModelLoaded) {
                                    if (isKorean) "✨ 내 폰 안에서만 작동 중 (외부 유출 0%)" else if (isIndonesian) "✨ Berjalan lokal di HP Anda (Nol Kebocoran Cloud)" else "✨ Running locally inside your phone (Zero Cloud Leak)"
                                } else {
                                    if (isKorean) "ℹ️ 오프라인 음성 인식 모드 가동 중" else if (isIndonesian) "ℹ️ Mode Pengenalan Suara Offline Aktif" else "ℹ️ Offline Speech Recognition Mode Active"
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
                                text = if (isKorean) "🌐 AI 인식 언어: $languageDisplayTitle" else if (isIndonesian) "🌐 Bahasa AI: $languageDisplayTitle" else "🌐 AI Language: $languageDisplayTitle",
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isKorean) "📦 Gemma 온디바이스 AI" else if (isIndonesian) "📦 AI On-Device Gemma" else "📦 Gemma On-Device AI",
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
                                        "외부 서버로 대화 내용이 전혀 전송되지 않고, 내 스마트폰 안에서만 100% 오프라인으로 안전하게 생각하고 다듬어 주는 구글 Gemma 온디바이스 AI입니다. 비행기 모드나 인터넷이 안 되는 곳에서도 100% 작동합니다."
                                    else if (isIndonesian)
                                        "Model AI Google Gemma yang berjalan 100% secara lokal di ponsel pintar Anda tanpa mengirim data apa pun ke server luar. Berfungsi sepenuhnya offline bahkan dalam mode pesawat."
                                    else
                                        "Google Gemma on-device AI executing 100% locally on your smartphone without sending any data to the cloud. Works completely offline even in airplane mode.",
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
                                            text = if (isKorean) "🔄 AI 엔진 다시 감지 및 연결" else if (isIndonesian) "🔄 Deteksi Ulang & Hubungkan Mesin AI" else "🔄 Rescan & Connect AI Engine",
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
                        text = if (isKorean) "🔍 AI 엔진 작동 상태" else if (isIndonesian) "🔍 Status Mesin AI" else "🔍 AI Engine Status",
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
                        label = if (isKorean) "AI 엔진 상태" else if (isIndonesian) "Status Mesin AI" else "Engine Status",
                        value = if (isModelLoaded) (if (isKorean) "✅ 온디바이스 AI 정상 작동 중" else if (isIndonesian) "✅ AI On-Device Aktif Normal" else "✅ On-Device AI Active") else (if (isKorean) "⏳ 오프라인 음성인식 모드 가동" else if (isIndonesian) "⏳ Mode Suara Offline Aktif" else "⏳ Offline Voice Mode"),
                        valueColor = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "설치된 모델" else if (isIndonesian) "Model Terpasang" else "Installed Model",
                        value = if (modelFiles.isNotEmpty()) modelFiles.joinToString(", ") { "${it.name} (${it.length() / 1024 / 1024}MB)" } else (if (isKorean) "기본 내장 엔진" else if (isIndonesian) "Mesin Bawaan Standar" else "Default Engine"),
                        valueColor = if (modelFiles.isNotEmpty()) DearTalkText else Color(0xFFFCA5A5)
                    )
                    DiagnosticRow(
                        label = if (isKorean) "개인정보 보호" else if (isIndonesian) "Perlindungan Privasi" else "Privacy",
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
                        text = if (isKorean) "🎙️ 실시간 음성 & AI 체험하기" else if (isIndonesian) "🎙️ Uji Coba Suara & AI Langsung" else "🎙️ Real-time Voice & AI Sandbox",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKorean) "마이크를 누르고 말씀하시거나 아래 예시를 터치하여 AI가 문장을 어떻게 다듬는지 바로 확인해 보세요." else if (isIndonesian) "Ketuk mikrofon dan bicaralah, atau ketuk contoh kalimat untuk melihat bagaimana AI merapikannya secara langsung." else "Tap the mic and speak, or tap sample sentences to see how AI polishes them in real time.",
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
                                if (isKorean) "🛑 말씀 끝내기 (터치하여 AI 다듬기)" else if (isIndonesian) "🛑 Selesai Bicara (Rapikan dengan AI)" else "🛑 Finish Speaking (Polish with AI)"
                            } else {
                                if (isKorean) "🎙️ 마이크 켜고 말씀하기" else if (isIndonesian) "🎙️ Ketuk untuk Bicara" else "🎙️ Tap to Speak"
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
                                Text(if (isKorean) "🎤 1. 내가 말한 내용:" else if (isIndonesian) "🎤 1. Yang Anda Katakan:" else "🎤 1. What You Said:", fontSize = 11.sp, color = DearTalkTextDim, fontWeight = FontWeight.SemiBold)
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
                                Text(if (isKorean) "✨ 2. AI가 다듬은 문장:" else if (isIndonesian) "✨ 2. Hasil Dirapikan AI:" else "✨ 2. AI Polished Result:", fontSize = 11.sp, color = DearTalkSecondary, fontWeight = FontWeight.Bold)
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
                                "Saya sedang di jalan tapi macet sekali mungkin terlambat 15 menit maaf ya",
                                "Proposal yang sudah diperbarui sudah saya kirim tolong dicek ya",
                                "Bisa tolong beri tahu kapan waktu luang untuk makan siang besok?",
                                "Di mana letak ruang rapat lantai dua tolong beri tahu",
                                "Terima kasih banyak atas bantuan dan kerja samanya hari ini"
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
                                    // ⚡ 0ms 즉각 반응: 사용자 터치 즉시 프리셋 내용을 UI와 입력창에 즉시 노출
                                    testInputText = preset
                                    aiTransformedText = preset
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        label = { Text(if (isKorean) "키보드로 직접 써보기 (여기를 터치)" else if (isIndonesian) "Coba ketik langsung di sini (Ketuk di sini)" else "Type directly to test keyboard (Tap here)") },
                        placeholder = { Text(if (isKorean) "키보드 자판에서 음성과 말투 변환을 직접 써보세요" else if (isIndonesian) "Uji suara dan perubahan gaya nada langsung di papan ketik" else "Test voice and tone directly on keyboard") },
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

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DearTalkKey)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isKorean) "🎙️ 말 끝남 자동 감지 시간" else if (isIndonesian) "🎙️ Waktu Jeda Deteksi Selesai Bicara" else "🎙️ Speech Pause Wait Time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isKorean) "말씀이 끝난 후 자동으로 입력을 완료할 때까지의 대기 시간 (${String.format("%.1f", silenceTimeoutMs / 1000f)}초)" else if (isIndonesian) "Waktu tunggu setelah Anda berhenti bicara sebelum selesai otomatis (${String.format("%.1f", silenceTimeoutMs / 1000f)} dtk)" else "Wait time after you stop speaking (${String.format("%.1f", silenceTimeoutMs / 1000f)}s)",
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
                        Text("1.5${if (isKorean) "초 (빠름)" else if (isIndonesian) "dtk (Cepat)" else "s (Fast)"}", fontSize = 10.sp, color = DearTalkTextDim)
                        Text("8.0${if (isKorean) "초 (여유)" else if (isIndonesian) "dtk (Santai)" else "s (Relaxed)"}", fontSize = 10.sp, color = DearTalkTextDim)
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // 5. 🛡️ 100% Zero-Persistence 프라이버시
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
                        text = if (isKorean) "DearTalk-AI는 사용자의 음성 및 변환 문장을 단 1바이트도 저장하지 않는 Zero-Data Retention 키보드입니다. 변환 즉시 화면에 입력된 후 흔적이 100% 소멸됩니다." else if (isIndonesian) "DearTalk-AI adalah papan ketik dengan Zero-Data Retention 100%. Suara dan teks langsung dimasukkan ke layar tanpa menyimpan data sedikit pun ke memori." else "DearTalk-AI is a 100% Zero-Data Retention keyboard. Spoken text and AI transformations disappear instantly without saving a single byte anywhere.",
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
                            "DearTalk-AI 사용이 어색하거나 기존 키보드(삼성, Gboard 등)로 돌아가고 싶으시다면 아래 버튼을 눌러 언제든 자유롭게 전환하실 수 있습니다."
                        else if (isIndonesian)
                            "Jika Anda merasa canggung atau ingin kembali ke papan ketik sebelumnya (Samsung, Gboard, dll.), ketuk tombol di bawah untuk beralih kapan saja."
                        else
                            "If you want to switch back to your previous keyboard (Samsung, Gboard, etc.), tap the button below to change anytime.",
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
                        value = "v${packageInfo?.versionName ?: "1.0.0"} (빌드 ${packageInfo?.longVersionCode ?: 1})",
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = UiStrings.buildTimestampLabel,
                        value = buildTimeStr,
                        valueColor = DearTalkText
                    )
                    DiagnosticRow(
                        label = if (isKorean) "보안 등급" else if (isIndonesian) "Tingkat Keamanan" else "Security",
                        value = if (isKorean) "🔒 100% 온디바이스 (외부 유출 0%)" else if (isIndonesian) "🔒 100% On-Device (Nol Kebocoran)" else "🔒 100% On-Device (Zero Cloud Leak)",
                        valueColor = DearTalkSecondary
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 8. 📖 사용 방법 안내
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = DearTalkTextDim,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(92.dp)
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
