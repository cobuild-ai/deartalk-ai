package ai.deartalk.android.live.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.live.ActiveSpeaker
import ai.deartalk.android.live.DearTalkLiveController
import ai.deartalk.android.live.ui.components.DualActionMicBar
import ai.deartalk.android.live.ui.components.FullScreenSymmetricStage
import ai.deartalk.android.live.ui.components.LiveMessengerTimeline
import ai.deartalk.android.live.ui.components.LiveSessionListSheet
import ai.deartalk.android.live.ui.components.LiveSettingsBottomSheet
import ai.deartalk.android.live.ui.components.SymmetricActionMicBar
import ai.deartalk.android.stt.LanguageModelStatus
import ai.deartalk.android.util.LanguageLocaleHelper
import ai.deartalk.android.ui.state.LiveUiState
import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.SpeechIntent

import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.data.ModelPackState
import ai.deartalk.android.data.SystemDiagnosticManager

/**
 * 🎙️ DearTalk Live: 1:1 실시간 대면 대화 & 지능형 보이스 레코더 메인 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DearTalkLiveScreen(
    controller: DearTalkLiveController,
    modelLifecycleManager: ModelLifecycleManager,
    diagnosticManager: SystemDiagnosticManager,
    intentEngine: DearTalkIntentEngine,
    autoStartDownload: Boolean = false,
    openSettings: Boolean = false,
    initialFlipView: Boolean = false,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentSession by controller.currentSession.collectAsState()
    val messages by controller.messages.collectAsState()
    val sessions by controller.sessions.collectAsState()
    val activeSpeaker by controller.activeSpeaker.collectAsState()
    val processingSpeaker by controller.processingSpeaker.collectAsState()
    val rawStreamingText by controller.streamingText.collectAsState()

    val packState by modelLifecycleManager.packState.collectAsState()

    LaunchedEffect(autoStartDownload) {
        if (autoStartDownload && packState !is ModelPackState.Installed && packState !is ModelPackState.Downloading) {
            Toast.makeText(context, "📥 AI 팩 다운로드를 즉시 시작합니다...", Toast.LENGTH_SHORT).show()
            modelLifecycleManager.startDownload(
                onSuccess = { intentEngine.reloadModel() }
            )
        }
    }
    
    val currentActiveIntent = remember(activeSpeaker, processingSpeaker, controller.myIntent, controller.partnerIntent) {
        if (activeSpeaker == ActiveSpeaker.ME || processingSpeaker == ActiveSpeaker.ME) {
            controller.myIntent
        } else if (activeSpeaker == ActiveSpeaker.PARTNER || processingSpeaker == ActiveSpeaker.PARTNER) {
            controller.partnerIntent
        } else {
            ai.deartalk.android.live.data.SpeechIntent.AUTO
        }
    }

    val streamingText = remember(rawStreamingText, currentActiveIntent) {
        if (rawStreamingText.isNotBlank()) {
            if (currentActiveIntent == ai.deartalk.android.live.data.SpeechIntent.QUESTION && !rawStreamingText.trim().endsWith("?")) {
                "${rawStreamingText.trim()}?"
            } else if (currentActiveIntent == ai.deartalk.android.live.data.SpeechIntent.STATEMENT && !rawStreamingText.trim().endsWith(".")) {
                "${rawStreamingText.trim()}."
            } else {
                rawStreamingText
            }
        } else {
            rawStreamingText
        }
    }
    val isProcessing by controller.isProcessing.collectAsState()
    val rephrasingMessageId by controller.rephrasingMessageId.collectAsState()
    val rmsDb by controller.rmsDb.collectAsState()
    val modelDownloadStatus by controller.modelDownloadStatus.collectAsState()

    val myLangTag = remember(controller.myLang) {
        LanguageLocaleHelper.getLanguageTag(LanguageLocaleHelper.getLocaleForCode(controller.myLang))
    }
    val partnerLangTag = remember(controller.partnerLang) {
        LanguageLocaleHelper.getLanguageTag(LanguageLocaleHelper.getLocaleForCode(controller.partnerLang))
    }
    val myStatus = modelDownloadStatus[myLangTag]
    val partnerStatus = modelDownloadStatus[partnerLangTag]

    var showSessionSheet by remember { mutableStateOf(false) }
    var showLiveSettingsSheet by remember { mutableStateOf(openSettings) }
    var isFlipViewEnabled by remember { mutableStateOf(initialFlipView) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // 마이크 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, UiStrings.micPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndExecute(action: () -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            action()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showLiveSettingsSheet) {
        LiveSettingsBottomSheet(
            controller = controller,
            modelLifecycleManager = modelLifecycleManager,
            diagnosticManager = diagnosticManager,
            intentEngine = intentEngine,
            onOpenHistory = { showSessionSheet = true },
            onDismiss = { showLiveSettingsSheet = false }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = if (UiStrings.isKo) "대화 내용 비우기" else if (UiStrings.isId) "Kosongkan Percakapan" else "Clear Conversation",
                    fontWeight = FontWeight.Bold,
                    color = DearTalkText
                )
            },
            text = {
                Text(
                    text = if (UiStrings.isKo) "현재 화면의 모든 대화 기록을 지우시겠습니까?" else if (UiStrings.isId) "Hapus semua percakapan di layar saat ini?" else "Clear all conversation history on this screen?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        controller.clearTimeline()
                    }
                ) {
                    Text(
                        text = if (UiStrings.isKo) "비우기" else if (UiStrings.isId) "Kosongkan" else "Clear",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(
                        text = UiStrings.btnCancel,
                        color = Color.Gray
                    )
                }
            },
            containerColor = DearTalkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSessionSheet) {
        LiveSessionListSheet(
            sessions = sessions,
            currentSessionId = currentSession?.id,
            onSessionSelected = { controller.loadSession(it) },
            onSessionDelete = { controller.deleteSession(it) },
            onDismiss = { showSessionSheet = false }
        )
    }

    val liveUiState = LiveUiState(
        currentSession = currentSession,
        messages = messages,
        sessions = sessions,
        activeSpeaker = activeSpeaker,
        processingSpeaker = processingSpeaker,
        streamingText = streamingText,
        isProcessing = isProcessing,
        rephrasingMessageId = rephrasingMessageId,
        rmsDb = rmsDb,
        myLang = controller.myLang,
        partnerLang = controller.partnerLang,
        myIntent = controller.myIntent,
        partnerIntent = controller.partnerIntent,
        myDetectedIntent = controller.myDetectedIntent,
        partnerDetectedIntent = controller.partnerDetectedIntent,
        selectedTone = controller.selectedTone,
        isFlipViewEnabled = isFlipViewEnabled,
        isAutoSpeakEnabled = controller.isAutoSpeakEnabled,
        myModelStatus = myStatus,
        partnerModelStatus = partnerStatus
    )

    DearTalkLiveContent(
        state = liveUiState,
        onBackClick = onBackClick,
        onToggleAutoSpeak = {
            controller.toggleAutoSpeak()
            val toastMsg = if (controller.isAutoSpeakEnabled) {
                if (UiStrings.isKo) "🔊 자동 음성 읽기 켜짐" else if (UiStrings.isId) "🔊 Baca Suara Otomatis Aktif" else "🔊 Auto-Speak Enabled"
            } else {
                if (UiStrings.isKo) "🔇 자동 음성 읽기 꺼짐" else if (UiStrings.isId) "🔇 Baca Suara Otomatis Nonaktif" else "🔇 Auto-Speak Disabled"
            }
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
        },
        onToggleFlipView = {
            isFlipViewEnabled = !isFlipViewEnabled
            val toastMsg = if (isFlipViewEnabled) UiStrings.liveMenuFlipOn else UiStrings.liveMenuFlipOff
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
        },
        onClearClick = { showClearConfirmDialog = true },
        onOpenSettings = { showLiveSettingsSheet = true },
        onSpeakMeClick = { checkAndExecute { controller.toggleSpeakMe() } },
        onListenPartnerClick = { checkAndExecute { controller.toggleListenPartner() } },
        onMyIntentSelected = { controller.setIntentByUser(ActiveSpeaker.ME, it) },
        onPartnerIntentSelected = { controller.setIntentByUser(ActiveSpeaker.PARTNER, it) },
        onMyLangSelected = { code -> controller.setLanguages(my = code, partner = controller.partnerLang) },
        onPartnerLangSelected = { code -> controller.setLanguages(my = controller.myLang, partner = code) },
        onSwapLanguages = { controller.swapLanguages() },
        onToneSelected = { controller.setTone(it) },
        onReplayMessage = { controller.replayMessage(it) }
    )
}

/**
 * 🎨 DearTalk Live 순수 상태 기반 화면 (Stateless Composable / Clean Architecture)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DearTalkLiveContent(
    state: LiveUiState,
    onBackClick: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onToggleFlipView: () -> Unit,
    onClearClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSpeakMeClick: () -> Unit,
    onListenPartnerClick: () -> Unit,
    onMyIntentSelected: (SpeechIntent) -> Unit,
    onPartnerIntentSelected: (SpeechIntent) -> Unit,
    onMyLangSelected: (String) -> Unit,
    onPartnerLangSelected: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    onToneSelected: (String) -> Unit,
    onReplayMessage: (LiveMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMyLangMenu by remember { mutableStateOf(false) }
    var showPartnerLangMenu by remember { mutableStateOf(false) }
    var showToneMenu by remember { mutableStateOf(false) }

    val tones = remember(UiStrings.currentLocale) {
        listOf(
            "✨ " + UiStrings.toneRefine,
            "🙇 " + UiStrings.tonePolite,
            "😊 " + UiStrings.toneCasual,
            "💼 " + UiStrings.toneBusiness,
            "😆 " + UiStrings.toneFunny,
            "😼 " + UiStrings.toneCheeky
        )
    }

    val officialLangs = remember(UiStrings.currentLocale) {
        listOf(
            "KO" to (if (UiStrings.isKo) "🇰🇷 한국어" else if (UiStrings.isId) "🇰🇷 Korea" else "🇰🇷 Korean"),
            "EN" to (if (UiStrings.isKo) "🇺🇸 English" else if (UiStrings.isId) "🇺🇸 Inggris" else "🇺🇸 English"),
            "ID" to (if (UiStrings.isKo) "🇮🇩 인도네시아어" else if (UiStrings.isId) "🇮🇩 Indonesia" else "🇮🇩 Indonesian")
        )
    }

    val crossLangs = remember(UiStrings.currentLocale) {
        listOf(
            "JA" to (if (UiStrings.isKo) "🇯🇵 일본어" else if (UiStrings.isId) "🇯🇵 Jepang" else "🇯🇵 Japanese"),
            "ZH" to (if (UiStrings.isKo) "🇨🇳 중국어" else if (UiStrings.isId) "🇨🇳 Mandarin" else "🇨🇳 Chinese"),
            "ES" to (if (UiStrings.isKo) "🇪🇸 스페인어" else if (UiStrings.isId) "🇪🇸 Spanyol" else "🇪🇸 Spanish"),
            "FR" to (if (UiStrings.isKo) "🇫🇷 프랑스어" else if (UiStrings.isId) "🇫🇷 Prancis" else "🇫🇷 French"),
            "DE" to (if (UiStrings.isKo) "🇩🇪 독일어" else if (UiStrings.isId) "🇩🇪 Jerman" else "🇩🇪 German"),
            "VI" to (if (UiStrings.isKo) "🇻🇳 베트남어" else if (UiStrings.isId) "🇻🇳 Vietnam" else "🇻🇳 Vietnamese"),
            "TH" to (if (UiStrings.isKo) "🇹🇭 태국어" else if (UiStrings.isId) "🇹🇭 Thailand" else "🇹🇭 Thai")
        )
    }

    val allSupportedLangs = remember(officialLangs, crossLangs) { officialLangs + crossLangs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🎙️ DearTalk Live",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (UiStrings.isKo) "뒤로가기" else if (UiStrings.isId) "Kembali" else "Back",
                            tint = DearTalkText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleAutoSpeak) {
                        Icon(
                            imageVector = if (state.isAutoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = UiStrings.liveAutoSpeakTitle,
                            tint = if (state.isAutoSpeakEnabled) DearTalkPrimary else Color.Gray
                        )
                    }
                    IconButton(onClick = onToggleFlipView) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = if (state.isFlipViewEnabled) UiStrings.liveMenuFlipOff else UiStrings.liveMenuFlipOn,
                            tint = if (state.isFlipViewEnabled) DearTalkPrimary else DearTalkText
                        )
                    }
                    IconButton(onClick = onClearClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = if (UiStrings.isKo) "대화 비우기" else if (UiStrings.isId) "Kosongkan" else "Clear Timeline",
                            tint = DearTalkText
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = UiStrings.liveSettingsTitle,
                            tint = DearTalkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DearTalkBackground)
            )
        },
        bottomBar = {
            if (!state.isFlipViewEnabled) {
                DualActionMicBar(
                    activeSpeaker = state.activeSpeaker,
                    processingSpeaker = state.processingSpeaker,
                    isProcessing = state.isProcessing,
                    rmsDb = state.rmsDb,
                    currentIntent = state.myIntent,
                    detectedIntent = state.myDetectedIntent,
                    langCode = state.myLang,
                    onIntentSelected = onMyIntentSelected,
                    onSpeakMeClick = onSpeakMeClick,
                    onListenPartnerClick = onListenPartnerClick,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        },
        containerColor = DearTalkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isFlipViewEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationZ = 180f }
                ) {
                    SymmetricActionMicBar(
                        speakerType = ActiveSpeaker.PARTNER,
                        langCode = state.partnerLang,
                        languageLabel = allSupportedLangs.firstOrNull { it.first == state.partnerLang }?.second ?: state.partnerLang,
                        onLanguageSelect = onPartnerLangSelected,
                        supportedOfficialLangs = officialLangs,
                        supportedCrossLangs = crossLangs,
                        isActive = state.activeSpeaker == ActiveSpeaker.PARTNER,
                        isProcessing = state.isProcessing && (state.activeSpeaker == ActiveSpeaker.PARTNER || state.processingSpeaker == ActiveSpeaker.PARTNER),
                        rmsDb = if (state.activeSpeaker == ActiveSpeaker.PARTNER) state.rmsDb else 0f,
                        currentIntent = state.partnerIntent,
                        detectedIntent = state.partnerDetectedIntent,
                        onIntentSelected = onPartnerIntentSelected,
                        onActionClick = onListenPartnerClick,
                        accentColor = Color(0xFF6366F1),
                        icon = if (state.activeSpeaker == ActiveSpeaker.PARTNER) Icons.Default.Stop else Icons.Default.Hearing
                    )
                }
            }

            if (!state.isFlipViewEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DearTalkSurface.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 내 언어 캡슐 칩
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DearTalkBackground)
                            .border(1.dp, DearTalkPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { showMyLangMenu = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = UiStrings.liveMePrefix,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                LanguageStatusBadge(state.myModelStatus)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = allSupportedLangs.firstOrNull { it.first == state.myLang }?.second ?: state.myLang,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DearTalkPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 11.sp,
                                    color = DearTalkPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMyLangMenu,
                            onDismissRequest = { showMyLangMenu = false }
                        ) {
                            Text(
                                text = "✨ ${UiStrings.officialLangSection}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            officialLangs.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    },
                                    onClick = {
                                        onMyLangSelected(code)
                                        showMyLangMenu = false
                                    }
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "🌐 ${UiStrings.crossLangSection}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            crossLangs.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = label, fontSize = 13.sp)
                                    },
                                    onClick = {
                                        onMyLangSelected(code)
                                        showMyLangMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // ⇄ 미니멀 스왑 아이콘
                    IconButton(
                        onClick = onSwapLanguages,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = UiStrings.swapLangContentDesc,
                            tint = DearTalkPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 상대방 언어 캡슐 칩
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DearTalkBackground)
                            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { showPartnerLangMenu = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = UiStrings.livePartnerPrefix,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                LanguageStatusBadge(state.partnerModelStatus)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = allSupportedLangs.firstOrNull { it.first == state.partnerLang }?.second ?: state.partnerLang,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF818CF8),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 11.sp,
                                    color = Color(0xFF818CF8).copy(alpha = 0.7f)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showPartnerLangMenu,
                            onDismissRequest = { showPartnerLangMenu = false }
                        ) {
                            Text(
                                text = "✨ ${UiStrings.officialLangSection}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            officialLangs.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    },
                                    onClick = {
                                        onPartnerLangSelected(code)
                                        showPartnerLangMenu = false
                                    }
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "🌐 ${UiStrings.crossLangSection}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            crossLangs.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = label, fontSize = 13.sp)
                                    },
                                    onClick = {
                                        onPartnerLangSelected(code)
                                        showPartnerLangMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 💼 내 발화 스타일(톤앤매너) 스트립
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DearTalkSurface)
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable { showToneMenu = true }
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.selectedTone,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "▾",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = showToneMenu,
                        onDismissRequest = { showToneMenu = false }
                    ) {
                        tones.forEach { toneItem ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = toneItem,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (state.selectedTone == toneItem) FontWeight.Bold else FontWeight.Normal,
                                        color = if (state.selectedTone == toneItem) DearTalkPrimary else DearTalkText
                                    )
                                },
                                onClick = {
                                    onToneSelected(toneItem)
                                    showToneMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (state.isFlipViewEnabled) {
                // 🪞 180° 대면 모드
                FullScreenSymmetricStage(
                    messages = state.messages,
                    streamingText = state.streamingText,
                    activeSpeaker = state.activeSpeaker,
                    processingSpeaker = state.processingSpeaker,
                    isProcessing = state.isProcessing,
                    rephrasingMessageId = state.rephrasingMessageId,
                    onReplayClick = onReplayMessage,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 💬 핸드헬드 일반 모드
                LiveMessengerTimeline(
                    messages = state.messages,
                    streamingText = state.streamingText,
                    activeSpeaker = state.activeSpeaker,
                    processingSpeaker = state.processingSpeaker,
                    isProcessing = state.isProcessing,
                    isFlipViewEnabled = false,
                    rephrasingMessageId = state.rephrasingMessageId,
                    onReplayClick = onReplayMessage,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.isFlipViewEnabled) {
                // 🙋 [내 0° 대면 대칭 마이크 바]
                SymmetricActionMicBar(
                    speakerType = ActiveSpeaker.ME,
                    langCode = state.myLang,
                    languageLabel = allSupportedLangs.firstOrNull { it.first == state.myLang }?.second ?: state.myLang,
                    onLanguageSelect = onMyLangSelected,
                    supportedOfficialLangs = officialLangs,
                    supportedCrossLangs = crossLangs,
                    isActive = state.activeSpeaker == ActiveSpeaker.ME,
                    isProcessing = state.isProcessing && (state.activeSpeaker == ActiveSpeaker.ME || state.processingSpeaker == ActiveSpeaker.ME),
                    rmsDb = if (state.activeSpeaker == ActiveSpeaker.ME) state.rmsDb else 0f,
                    currentIntent = state.myIntent,
                    detectedIntent = state.myDetectedIntent,
                    onIntentSelected = onMyIntentSelected,
                    onActionClick = onSpeakMeClick,
                    accentColor = Color(0xFF10B981),
                    icon = if (state.activeSpeaker == ActiveSpeaker.ME) Icons.Default.Stop else Icons.Default.Mic,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}

/**
 * 🌐 온디바이스 언어팩 가용성 뱃지 (심플 & 신뢰 중심 UX: 사용 가능 / 사용 불가)
 */
@Composable
private fun LanguageStatusBadge(status: LanguageModelStatus?) {
    when (status) {
        is LanguageModelStatus.Downloading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(7.dp),
                    strokeWidth = 1.2.dp,
                    color = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${status.progress}%",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            }
        }
        is LanguageModelStatus.Error -> {
            Text(
                text = UiStrings.liveBadgeUnavailable,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
            )
        }
        else -> {
            // 🌟 Installed, SupportedOnline, Scheduled, Checking 등 모든 정상 동작 상태는 🟢 사용 가능으로 단일화!
            Text(
                text = UiStrings.liveBadgeReady,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10B981),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
            )
        }
    }
}
