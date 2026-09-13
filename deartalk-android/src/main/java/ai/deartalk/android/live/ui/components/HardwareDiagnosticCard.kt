package ai.deartalk.android.live.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.DeviceTierRating
import ai.deartalk.android.data.ModelPackState
import ai.deartalk.android.data.SystemMetrics
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkKey
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSecondary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.ime.ui.theme.DearTalkTextDim
import java.util.Locale

/**
 * 🛡️ 하드웨어 진단 및 Qwen 온디바이스 AI 팩 상태 카드
 */
@Composable
fun HardwareDiagnosticCard(
    metrics: SystemMetrics,
    packState: ModelPackState,
    onDownloadClick: () -> Unit,
    onPurgeClick: () -> Unit
) {
    val tierColor = when (metrics.tierRating) {
        DeviceTierRating.OPTIMAL -> Color(0xFF10B981)
        DeviceTierRating.CAUTION -> Color(0xFFF59E0B)
        DeviceTierRating.RESTRICTED -> Color(0xFFEF4444)
    }

    val ramFormatted = String.format(Locale.US, "%.1f", metrics.totalRamGb)
    val storageFormatted = String.format(Locale.US, "%.1f", metrics.availableStorageGb)

    val tierText = when (metrics.tierRating) {
        DeviceTierRating.OPTIMAL -> UiStrings.diagOptimal(ramFormatted, storageFormatted)
        DeviceTierRating.CAUTION -> UiStrings.diagCaution(ramFormatted)
        DeviceTierRating.RESTRICTED -> UiStrings.diagRestricted(ramFormatted)
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
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(UiStrings.diagModelTitle, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                            Text(UiStrings.diagModelSubtitle, fontSize = 11.sp, color = DearTalkTextDim)
                        }
                        Button(
                            onClick = onDownloadClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(UiStrings.diagDownloadBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is ModelPackState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(UiStrings.diagDownloadingLabel, fontSize = 13.sp, color = DearTalkSecondary)
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
                            Text(UiStrings.diagActiveLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                        }
                        IconButton(onClick = onPurgeClick) {
                            Icon(Icons.Default.Delete, contentDescription = UiStrings.diagPurgeContentDesc, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                is ModelPackState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            UiStrings.diagErrorLabel(packState.message),
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        OutlinedButton(
                            onClick = onDownloadClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = DearTalkSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(UiStrings.diagRetryBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkSecondary)
                        }
                    }
                }
            }
        }
    }
}
