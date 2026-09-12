package ai.deartalk.android.live.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.live.ActiveSpeaker

/**
 * 🎙️ 하단 듀얼 대형 마이크 & 톤앤매너 칩 바
 */
@Composable
fun DualActionMicBar(
    activeSpeaker: ActiveSpeaker,
    isProcessing: Boolean,
    rmsDb: Float,
    selectedTone: String,
    onToneSelected: (String) -> Unit,
    onSpeakMeClick: () -> Unit,
    onListenPartnerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tones = listOf(
        UiStrings.toneRefine,
        UiStrings.tonePolite,
        UiStrings.toneCasual,
        UiStrings.toneBusiness
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DearTalkBackground,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 톤앤매너 선택 칩 (DearTalk 키보드 스타일)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tones.forEach { tone ->
                    val isSelected = selectedTone == tone
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) DearTalkPrimary.copy(alpha = 0.2f) else DearTalkSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) DearTalkPrimary else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onToneSelected(tone) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tone,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DearTalkPrimary else DearTalkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 나란히 배치된 대형 듀얼 액션 버튼 (KO, EN, ID 다국어 대응 & 오버플로우 방지)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 🗣️ [내가 말하기] 버튼
                BigActionButton(
                    modifier = Modifier.weight(1f),
                    title = if (activeSpeaker == ActiveSpeaker.ME) UiStrings.liveFinishRecording else UiStrings.liveSpeakMe,
                    subtitle = if (activeSpeaker == ActiveSpeaker.ME) UiStrings.liveFinishRecordingSub else UiStrings.liveSpeakMeSub,
                    isActive = activeSpeaker == ActiveSpeaker.ME,
                    isProcessing = isProcessing && activeSpeaker == ActiveSpeaker.ME,
                    rmsDb = if (activeSpeaker == ActiveSpeaker.ME) rmsDb else 0f,
                    activeColor = Color(0xFF10B981), // Emerald Green
                    idleColor = DearTalkSurface,
                    icon = if (activeSpeaker == ActiveSpeaker.ME) Icons.Default.Stop else Icons.Default.Mic,
                    onClick = onSpeakMeClick
                )

                // 👂 [상대방 듣기] 버튼
                BigActionButton(
                    modifier = Modifier.weight(1f),
                    title = if (activeSpeaker == ActiveSpeaker.PARTNER) UiStrings.liveFinishRecording else UiStrings.liveListenPartner,
                    subtitle = if (activeSpeaker == ActiveSpeaker.PARTNER) UiStrings.liveFinishRecordingSub else UiStrings.liveListenPartnerSub,
                    isActive = activeSpeaker == ActiveSpeaker.PARTNER,
                    isProcessing = isProcessing && activeSpeaker == ActiveSpeaker.PARTNER,
                    rmsDb = if (activeSpeaker == ActiveSpeaker.PARTNER) rmsDb else 0f,
                    activeColor = Color(0xFF6366F1), // Indigo Blue
                    idleColor = DearTalkSurface,
                    icon = if (activeSpeaker == ActiveSpeaker.PARTNER) Icons.Default.Stop else Icons.Default.Hearing,
                    onClick = onListenPartnerClick
                )
            }
        }
    }
}

@Composable
private fun BigActionButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isActive: Boolean,
    isProcessing: Boolean,
    rmsDb: Float,
    activeColor: Color,
    idleColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dbScale = if (isActive) (1.0f + (rmsDb / 60f).coerceIn(0f, 0.25f)) else 1.0f
    val combinedScale = pulseScale * dbScale

    Box(
        modifier = modifier
            .height(76.dp)
            .scale(if (isActive) combinedScale else 1.0f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        colors = listOf(activeColor, activeColor.copy(alpha = 0.8f))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(idleColor, idleColor.copy(alpha = 0.9f))
                    )
                }
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White.copy(alpha = 0.25f) else DearTalkBackground),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) Color.White else DearTalkText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else DearTalkText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = if (isActive) Color.White.copy(alpha = 0.85f) else Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
