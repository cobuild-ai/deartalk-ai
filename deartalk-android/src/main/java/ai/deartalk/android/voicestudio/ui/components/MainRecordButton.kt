package ai.deartalk.android.voicestudio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import ai.deartalk.android.ime.ui.MicUiState
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSecondary
import ai.deartalk.android.ime.ui.theme.DearTalkTextDim

@Composable
fun MainRecordButton(
    micUiState: MicUiState,
    rmsDb: Float = 0f,
    onClick: () -> Unit
) {
    val isListening = micUiState == MicUiState.LISTENING
    val isPreparing = micUiState == MicUiState.PREPARING
    val isProcessing = micUiState == MicUiState.PROCESSING_AI

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // 실제 마이크 데시벨(rmsDb)에 따른 다이내믹 스케일 (1.0 ~ 1.25)
    val dynamicScale = if (isListening) {
        (pulseScale + (rmsDb.coerceIn(0f, 10f) / 35f)).coerceIn(1.0f, 1.3f)
    } else {
        1.0f
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .scale(dynamicScale)
                .clip(CircleShape)
                .background(
                    when (micUiState) {
                        MicUiState.PREPARING -> Brush.radialGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                        MicUiState.LISTENING -> Brush.radialGradient(listOf(Color(0xFFFF2E2E), Color(0xFF991B1B)))
                        MicUiState.PROCESSING_AI -> Brush.radialGradient(listOf(DearTalkPrimary, Color(0xFF4338CA)))
                        MicUiState.IDLE -> Brush.radialGradient(listOf(DearTalkPrimary, Color(0xFF4338CA)))
                    }
                )
                .clickable(enabled = !isPreparing && !isProcessing, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when (micUiState) {
                MicUiState.PROCESSING_AI -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                }
                MicUiState.PREPARING -> {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = UiStrings.contentDescPreparing,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                MicUiState.LISTENING -> {
                    Icon(
                        Icons.Default.StopCircle,
                        contentDescription = UiStrings.contentDescStop,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                MicUiState.IDLE -> {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = UiStrings.contentDescSpeak,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (micUiState) {
                MicUiState.PREPARING -> UiStrings.micBtnPreparing
                MicUiState.LISTENING -> UiStrings.micBtnListening
                MicUiState.PROCESSING_AI -> UiStrings.micBtnProcessing
                MicUiState.IDLE -> UiStrings.micBtnIdle
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when (micUiState) {
                MicUiState.PREPARING -> Color(0xFFD97706)
                MicUiState.LISTENING -> Color(0xFFFF2E2E)
                MicUiState.PROCESSING_AI -> DearTalkSecondary
                MicUiState.IDLE -> DearTalkTextDim
            }
        )
    }
}
