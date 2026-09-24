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
    onOpenHistory: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPurgeModelDialog by remember { mutableStateOf(false) }

    val packState by modelLifecycleManager.packState.collectAsState()
    val loadedModelName by intentEngine.loadedModelNameFlow.collectAsState()
    var systemMetrics by remember { mutableStateOf(diagnosticManager.diagnose()) }

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
                loadedModelName = loadedModelName,
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

            // ⚙️ 2. 통역 편의 기능
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
