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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import ai.deartalk.android.live.ui.components.LiveMessengerTimeline
import ai.deartalk.android.live.ui.components.LiveSessionListSheet
import ai.deartalk.android.stt.LanguageModelStatus
import ai.deartalk.android.util.LanguageLocaleHelper

/**
 * 🎙️ DearTalk Live: 1:1 실시간 대면 대화 & 지능형 보이스 레코더 메인 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DearTalkLiveScreen(
    controller: DearTalkLiveController,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentSession by controller.currentSession.collectAsState()
    val messages by controller.messages.collectAsState()
    val sessions by controller.sessions.collectAsState()
    val activeSpeaker by controller.activeSpeaker.collectAsState()
    val streamingText by controller.streamingText.collectAsState()
    val isProcessing by controller.isProcessing.collectAsState()
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
    var showMoreMenu by remember { mutableStateOf(false) }
    var isFlipViewEnabled by remember { mutableStateOf(false) }

    // 🌟 1. 공식 지원 3대 언어 (KO, EN, ID)
    val officialLangs = remember {
        listOf(
            "KO" to "🇰🇷 한국어",
            "EN" to "🇺🇸 English",
            "ID" to "🇮🇩 인도네시아어"
        )
    }

    // 🌐 2. 글로벌 교차 통역 지원 언어
    val crossLangs = remember {
        listOf(
            "JA" to "🇯🇵 일본어",
            "ZH" to "🇨🇳 중국어",
            "ES" to "🇪🇸 스페인어",
            "FR" to "🇫🇷 프랑스어",
            "DE" to "🇩🇪 독일어",
            "VI" to "🇻🇳 베트남어",
            "TH" to "🇹🇭 태국어"
        )
    }

    val allSupportedLangs = remember { officialLangs + crossLangs }

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
                    // 1. 새 대화 시작 (Add)
                    IconButton(onClick = {
                        controller.createNewSession()
                        Toast.makeText(context, "새로운 대화 세션이 시작되었습니다.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "새 대화",
                            tint = DearTalkPrimary
                        )
                    }

                    // 2. 세션 히스토리 목록 (Folder)
                    IconButton(onClick = { showSessionSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "대화 기록 목록",
                            tint = DearTalkText
                        )
                    }

                    // 3. 더보기 오버플로우 메뉴 (플립뷰, 연속청취, 공유)
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "추가 옵션",
                                tint = DearTalkText
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            // 대면 180도 플립 뷰
                            DropdownMenuItem(
                                text = {
                                    Text(if (isFlipViewEnabled) UiStrings.liveMenuFlipOff else UiStrings.liveMenuFlipOn)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = null,
                                        tint = if (isFlipViewEnabled) DearTalkPrimary else DearTalkText
                                    )
                                },
                                onClick = {
                                    isFlipViewEnabled = !isFlipViewEnabled
                                    showMoreMenu = false
                                    val toastMsg = if (isFlipViewEnabled) UiStrings.liveMenuFlipOn else UiStrings.liveMenuFlipOff
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 연속 청취 / 강의 레코더 모드
                            DropdownMenuItem(
                                text = {
                                    Text(if (isContinuousListening) UiStrings.liveMenuContinuousOff else UiStrings.liveMenuContinuousOn)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = if (isContinuousListening) Color(0xFFEF4444) else DearTalkText
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    checkAndExecute {
                                        controller.toggleContinuousListening()
                                        val toastMsg = if (!isContinuousListening) UiStrings.liveMenuContinuousOn else UiStrings.liveMenuContinuousOff
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // 대화록 공유
                            DropdownMenuItem(
                                text = { Text(UiStrings.liveMenuExport) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = DearTalkText
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val markdown = controller.exportCurrentSessionAsMarkdown()
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "DearTalk Live")
                                        putExtra(Intent.EXTRA_TEXT, markdown)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, UiStrings.liveMenuExport)
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DearTalkBackground)
            )
        },
        bottomBar = {
            DualActionMicBar(
                activeSpeaker = activeSpeaker,
                isProcessing = isProcessing,
                rmsDb = rmsDb,
                selectedTone = controller.selectedTone,
                onToneSelected = { controller.selectedTone = it },
                onSpeakMeClick = {
                    checkAndExecute { controller.toggleSpeakMe() }
                },
                onListenPartnerClick = {
                    checkAndExecute { controller.toggleListenPartner() }
                },
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = DearTalkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 🌐 슬림 컴팩트 언어 바 (높이 및 패딩을 대폭 축소하여 타임라인 영역 확보)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DearTalkSurface.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
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
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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

            // 💬 중앙 카카오톡/Continuum 스타일 2-Way 대화 타임라인
            LiveMessengerTimeline(
                messages = messages,
                streamingText = streamingText,
                activeSpeaker = activeSpeaker,
                isProcessing = isProcessing,
                isFlipViewEnabled = isFlipViewEnabled,
                onReplayClick = { controller.replayMessage(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 🌐 온디바이스 언어팩 다운로드 및 가용성 뱃지 (심리스 컴팩트 태그)
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
        is LanguageModelStatus.Scheduled -> {
            Text(
                text = UiStrings.liveBadgeDownloading,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF59E0B),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
        is LanguageModelStatus.Installed -> {
            Text(
                text = UiStrings.liveBadgeOffline,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
        is LanguageModelStatus.SupportedOnline -> {
            Text(
                text = UiStrings.liveBadgeStreaming,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6366F1),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
        is LanguageModelStatus.Checking -> {
            Text(
                text = if (UiStrings.isKo) "확인중..." else if (UiStrings.isId) "Memeriksa..." else "Checking...",
                fontSize = 7.5.sp,
                color = Color.Gray,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.15f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
        else -> {}
    }
}
