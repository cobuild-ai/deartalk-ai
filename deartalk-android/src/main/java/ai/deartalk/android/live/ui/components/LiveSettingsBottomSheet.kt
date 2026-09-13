package ai.deartalk.android.live.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.live.DearTalkLiveController

import androidx.compose.runtime.collectAsState
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.data.SystemDiagnosticManager
import ai.deartalk.android.live.ui.components.HardwareDiagnosticCard

/**
 * ⚙️ DearTalk Live 전용 통합 설정 바텀 시트
 * - 온디바이스 AI 팩 & 하드웨어 진단 (다운로드, 진행률, 퍼지)
 * - 대화 내역 보관 주기 (3일, 7일, 10일, 30일, 수동 보관)
 * - 카카오톡 / 외부 공유
 * - 지난 기록 보기 / 신규 대화
 * - 자동 TTS / 연속 청취 토글
 * - 전체 대화 내역 즉시 삭제 (확인 팝업)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSettingsBottomSheet(
    controller: DearTalkLiveController,
    modelLifecycleManager: ModelLifecycleManager,
    diagnosticManager: SystemDiagnosticManager,
    intentEngine: DearTalkIntentEngine,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPurgeModelDialog by remember { mutableStateOf(false) }

    val packState by modelLifecycleManager.packState.collectAsState()
    var systemMetrics by remember { mutableStateOf(diagnosticManager.diagnose()) }

    val retentionOptions = remember {
        listOf(
            3 to "3${UiStrings.liveDays}",
            7 to "7${UiStrings.liveDays}",
            10 to "10${UiStrings.liveDays}",
            30 to "30${UiStrings.liveDays}",
            0 to UiStrings.liveManualKeep
        )
    }

    if (showPurgeModelDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeModelDialog = false },
            title = {
                Text(
                    text = UiStrings.diagPurgeConfirmTitle,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = UiStrings.diagPurgeConfirmMessage,
                    color = DearTalkText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeModelDialog = false
                        modelLifecycleManager.purgeModels()
                        Toast.makeText(context, "${UiStrings.diagPurgeContentDesc} 완료", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(UiStrings.btnDelete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeModelDialog = false }) {
                    Text(UiStrings.btnCancel, color = Color.Gray)
                }
            },
            containerColor = DearTalkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = UiStrings.liveDeleteAllConfirmTitle,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = UiStrings.liveDeleteAllConfirmMsg,
                    color = DearTalkText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        controller.deleteAllSessions()
                        Toast.makeText(context, UiStrings.liveDeleteAllSuccess, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(UiStrings.btnDelete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(UiStrings.btnCancel, color = Color.Gray)
                }
            },
            containerColor = DearTalkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DearTalkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 🏷️ 1. 헤더 (타이틀 & 닫기)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚙️ ${UiStrings.liveSettingsTitle}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🧠 1. 온디바이스 AI 팩 및 하드웨어 진단
            HardwareDiagnosticCard(
                metrics = systemMetrics,
                packState = packState,
                onDownloadClick = {
                    modelLifecycleManager.startDownload(
                        onSuccess = { intentEngine.reloadModel() }
                    )
                },
                onPurgeClick = {
                    showPurgeModelDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🗓️ 2. 대화 기록 자동 삭제 주기 (Auto-Delete Retention)
            SettingsCard(title = "🗓️ ${UiStrings.liveAutoDeleteTitle}") {
                Text(
                    text = UiStrings.liveAutoDeleteDesc,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    retentionOptions.forEach { (days, label) ->
                        val isSelected = controller.retentionDays == days
                        val isDefault = (days == 10)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) DearTalkPrimary else DearTalkBackground
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) DearTalkPrimary else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    controller.updateRetentionDays(days)
                                    val msg = if (days == 0) UiStrings.liveManualKeep else "$days${UiStrings.liveDays}"
                                    Toast.makeText(context, "$msg 설정 완료", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DearTalkText
                                )
                                if (isDefault) {
                                    Text(
                                        text = UiStrings.liveDefaultLabel,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else DearTalkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 💬 3. 대화록 공유 및 세션 관리
            SettingsCard(title = "💬 ${UiStrings.liveSectionSharing}") {
                // 카카오톡 / 시스템 공유
                SettingsActionButton(
                    icon = Icons.Default.Share,
                    title = UiStrings.liveShareTranscript,
                    subtitle = UiStrings.liveShareTranscriptSub,
                    iconTint = Color(0xFFFBBF24),
                    onClick = {
                        controller.shareTranscript(context)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.15f)
                )

                // 지난 대화 기록 보기
                SettingsActionButton(
                    icon = Icons.Default.Folder,
                    title = UiStrings.livePastSessions,
                    subtitle = UiStrings.livePastSessionsSub,
                    iconTint = DearTalkPrimary,
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.15f)
                )

                // 새 대화 시작하기
                SettingsActionButton(
                    icon = Icons.Default.Add,
                    title = UiStrings.liveStartNewSession,
                    subtitle = UiStrings.liveStartNewSessionSub,
                    iconTint = Color(0xFF10B981),
                    onClick = {
                        controller.createNewSession()
                        Toast.makeText(context, UiStrings.liveNewSessionCreated, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ⚙️ 4. 통역 편의 기능
            SettingsCard(title = "⚙️ ${UiStrings.liveSectionConvenience}") {
                // 🔊 자동 음성 읽기 토글
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DearTalkPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = DearTalkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = UiStrings.liveAutoSpeakTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DearTalkText
                            )
                            Text(
                                text = UiStrings.liveAutoSpeakDesc,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = controller.isAutoSpeakEnabled,
                        onCheckedChange = { controller.toggleAutoSpeak() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DearTalkPrimary)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.15f)
                )

                // 🔁 연속 청취 토글
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = UiStrings.liveContinuousTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DearTalkText
                            )
                            Text(
                                text = UiStrings.liveContinuousDesc,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = controller.isContinuousListening.value,
                        onCheckedChange = { controller.toggleContinuousListening() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ⚠️ 5. 위험 구역 (모든 대화 기록 삭제)
            SettingsCard(title = "⚠️ 저장 공간 관리") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = UiStrings.liveDeleteAll,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "모든 세션과 메시지를 DB에서 영구 삭제",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DearTalkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DearTalkText
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingsActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DearTalkText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
