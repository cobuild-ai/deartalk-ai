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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
    val isContinuousListening by controller.isContinuousListening.collectAsState()
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
    var showMyLangMenu by remember { mutableStateOf(false) }
    var showPartnerLangMenu by remember { mutableStateOf(false) }
    var showLiveSettingsSheet by remember { mutableStateOf(openSettings) }
    var isFlipViewEnabled by remember { mutableStateOf(initialFlipView) }
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

    // 🌟 1. 공식 지원 3대 언어 (KO, EN, ID)
    val officialLangs = remember(UiStrings.currentLocale) {
        listOf(
            "KO" to (if (UiStrings.isKo) "🇰🇷 한국어" else if (UiStrings.isId) "🇰🇷 Korea" else "🇰🇷 Korean"),
            "EN" to (if (UiStrings.isKo) "🇺🇸 English" else if (UiStrings.isId) "🇺🇸 Inggris" else "🇺🇸 English"),
            "ID" to (if (UiStrings.isKo) "🇮🇩 인도네시아어" else if (UiStrings.isId) "🇮🇩 Indonesia" else "🇮🇩 Indonesian")
        )
    }

    // 🌐 2. 글로벌 교차 통역 지원 언어
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

    if (showSessionSheet) {
        LiveSessionListSheet(
            sessions = sessions,
            currentSessionId = currentSession?.id,
            onSessionSelected = { controller.loadSession(it) },
            onSessionDelete = { controller.deleteSession(it) },
            onDismiss = { showSessionSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🎙️ DearTalk Live",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkText,
                            maxLines = 1
                        )
                        currentSession?.let {
                            Text(
                                text = it.title,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = DearTalkText
                        )
                    }
                },
                actions = {
                    // 1. 대면 180° 대칭 모드 즉각 전환 (ScreenRotation)
                    IconButton(onClick = {
                        isFlipViewEnabled = !isFlipViewEnabled
                        val toastMsg = if (isFlipViewEnabled) UiStrings.liveMenuFlipOn else UiStrings.liveMenuFlipOff
                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = if (isFlipViewEnabled) UiStrings.liveMenuFlipOff else UiStrings.liveMenuFlipOn,
                            tint = if (isFlipViewEnabled) DearTalkPrimary else DearTalkText
                        )
                    }

                    // 2. 통합 설정 바텀 시트 (Settings)
                    IconButton(onClick = { showLiveSettingsSheet = true }) {
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
            if (!isFlipViewEnabled) {
                DualActionMicBar(
                    activeSpeaker = activeSpeaker,
                    processingSpeaker = processingSpeaker,
                    isProcessing = isProcessing,
                    rmsDb = rmsDb,
                    currentIntent = controller.myIntent,
                    detectedIntent = controller.myDetectedIntent,
                    langCode = controller.myLang,
                    onIntentSelected = { controller.setIntentByUser(ai.deartalk.android.live.ActiveSpeaker.ME, it) },
                    onSpeakMeClick = {
                        checkAndExecute { controller.toggleSpeakMe() }
                    },
                    onListenPartnerClick = {
                        checkAndExecute { controller.toggleListenPartner() }
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        },
        containerColor = DearTalkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isFlipViewEnabled) {
                // 🔄 [상대방 180° 대면 대칭 마이크 바]
                // 180도 회전 배치되어 맞은편 상대방 시야에서 완벽한 정방향 대칭 조작 제공
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationZ = 180f }
                ) {
                    SymmetricActionMicBar(
                        speakerType = ActiveSpeaker.PARTNER,
                        langCode = controller.partnerLang,
                        languageLabel = allSupportedLangs.firstOrNull { it.first == controller.partnerLang }?.second ?: controller.partnerLang,
                        onLanguageSelect = { code ->
                            controller.setLanguages(my = controller.myLang, partner = code)
                        },
                        supportedOfficialLangs = officialLangs,
                        supportedCrossLangs = crossLangs,
                        isActive = activeSpeaker == ActiveSpeaker.PARTNER,
                        isProcessing = isProcessing && (activeSpeaker == ActiveSpeaker.PARTNER || processingSpeaker == ActiveSpeaker.PARTNER),
                        rmsDb = if (activeSpeaker == ActiveSpeaker.PARTNER) rmsDb else 0f,
                        currentIntent = controller.partnerIntent,
                        detectedIntent = controller.partnerDetectedIntent,
                        onIntentSelected = { controller.setIntentByUser(ai.deartalk.android.live.ActiveSpeaker.PARTNER, it) },
                        onActionClick = {
                            checkAndExecute { controller.toggleListenPartner() }
                        },
                        accentColor = Color(0xFF6366F1),
                        icon = if (activeSpeaker == ActiveSpeaker.PARTNER) Icons.Default.Stop else Icons.Default.Hearing
                    )
                }
            }

            if (!isFlipViewEnabled) {
                // 🌐 슬림 컴팩트 언어 바 (핸드헬드 일반 모드 전용 - 180도 대면 모드에서는 상하 대칭 마이크 바로 흡수되어 타임라인 공간 극대화)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DearTalkSurface.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // 내 언어 캡슐 칩 (2단 모던 카드 구조: 화자/상태 + 언어명)
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
                            LanguageStatusBadge(myStatus)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = allSupportedLangs.firstOrNull { it.first == controller.myLang }?.second ?: controller.myLang,
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
                                    controller.setLanguages(my = code, partner = controller.partnerLang)
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
                                    controller.setLanguages(my = code, partner = controller.partnerLang)
                                    showMyLangMenu = false
                                }
                            )
                        }
                    }
                }

                // ⇄ 미니멀 스왑 아이콘 (터치 타겟 36dp 최적화)
                IconButton(
                    onClick = { controller.swapLanguages() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "언어 교환",
                        tint = DearTalkPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 상대방 언어 캡슐 칩 (2단 모던 카드 구조: 화자/상태 + 언어명)
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
                            LanguageStatusBadge(partnerStatus)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = allSupportedLangs.firstOrNull { it.first == controller.partnerLang }?.second ?: controller.partnerLang,
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
                                    controller.setLanguages(my = controller.myLang, partner = code)
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
                                    controller.setLanguages(my = controller.myLang, partner = code)
                                    showPartnerLangMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 💼 내 발화 스타일(톤앤매너) 스트립 (IME 키보드와 100% 동일한 비주얼 & 실시간 재점검 연동)
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
                            text = controller.selectedTone,
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
                                        fontWeight = if (controller.selectedTone == toneItem) FontWeight.Bold else FontWeight.Normal,
                                        color = if (controller.selectedTone == toneItem) DearTalkPrimary else DearTalkText
                                    )
                                },
                                onClick = {
                                    controller.setTone(toneItem)
                                    showToneMenu = false
                                }
                            )
                        }
                    }
                }

                // 우측: 신규 대화 빠른 생성 버튼
                IconButton(
                    onClick = { controller.createNewSession() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "새 대화",
                        tint = Color.Gray,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

            if (isFlipViewEnabled) {
                // 🪞 180° 대면 모드: 화면을 50:50으로 꽉 채우는 대형 대칭 스테이지 (풀스크린 고가독성 모드)
                FullScreenSymmetricStage(
                    messages = messages,
                    streamingText = streamingText,
                    activeSpeaker = activeSpeaker,
                    processingSpeaker = processingSpeaker,
                    isProcessing = isProcessing,
                    rephrasingMessageId = rephrasingMessageId,
                    onReplayClick = { controller.replayMessage(it) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 💬 핸드헬드 일반 모드: 카카오톡/메신저 스타일 2-Way 대화 타임라인
                LiveMessengerTimeline(
                    messages = messages,
                    streamingText = streamingText,
                    activeSpeaker = activeSpeaker,
                    processingSpeaker = processingSpeaker,
                    isProcessing = isProcessing,
                    isFlipViewEnabled = false,
                    rephrasingMessageId = rephrasingMessageId,
                    onReplayClick = { controller.replayMessage(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isFlipViewEnabled) {
                // 🙋 [내 0° 대면 대칭 마이크 바]
                // 내 시야에서 정방향으로 버튼이 손가락 쪽, 칩이 화면 중앙 쪽으로 배치
                SymmetricActionMicBar(
                    speakerType = ActiveSpeaker.ME,
                    langCode = controller.myLang,
                    languageLabel = allSupportedLangs.firstOrNull { it.first == controller.myLang }?.second ?: controller.myLang,
                    onLanguageSelect = { code ->
                        controller.setLanguages(my = code, partner = controller.partnerLang)
                    },
                    supportedOfficialLangs = officialLangs,
                    supportedCrossLangs = crossLangs,
                    isActive = activeSpeaker == ActiveSpeaker.ME,
                    isProcessing = isProcessing && (activeSpeaker == ActiveSpeaker.ME || processingSpeaker == ActiveSpeaker.ME),
                    rmsDb = if (activeSpeaker == ActiveSpeaker.ME) rmsDb else 0f,
                    currentIntent = controller.myIntent,
                    detectedIntent = controller.myDetectedIntent,
                    onIntentSelected = { controller.setIntentByUser(ai.deartalk.android.live.ActiveSpeaker.ME, it) },
                    onActionClick = {
                        checkAndExecute { controller.toggleSpeakMe() }
                    },
                    accentColor = Color(0xFF10B981),
                    icon = if (activeSpeaker == ActiveSpeaker.ME) Icons.Default.Stop else Icons.Default.Mic,
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
