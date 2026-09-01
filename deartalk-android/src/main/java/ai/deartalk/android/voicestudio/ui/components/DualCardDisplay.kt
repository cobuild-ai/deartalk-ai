package ai.deartalk.android.voicestudio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.agent.VoicePipelineStage
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSecondary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText

@Composable
fun DualCardDisplay(
    rawText: String,
    aiText: String,
    isListening: Boolean,
    pipelineStage: VoicePipelineStage,
    sourceLangLabel: String? = null,
    targetLangLabel: String? = null,
    onReplayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 상단: 내가 말한 원문
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isListening) Color(0xFFEF4444) else DearTalkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListening) UiStrings.rawSttListening else UiStrings.rawSttTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) Color(0xFFEF4444) else DearTalkSecondary
                        )
                    }

                    if (!sourceLangLabel.isNullOrBlank()) {
                        Text(
                            text = sourceLangLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rawText,
                    fontSize = 15.sp,
                    color = DearTalkText,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 하단: AI 조율 / 번역 결과
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.horizontalGradient(listOf(DearTalkPrimary, DearTalkSecondary)), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(UiStrings.aiResultTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                        if (!targetLangLabel.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("($targetLangLabel)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                        }
                    }
                    IconButton(onClick = onReplayClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = UiStrings.replayButtonDesc, tint = DearTalkSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = aiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 24.sp
                )

                // 파형 및 상태 표시 애니메이션
                if (pipelineStage is VoicePipelineStage.SynthesizingTTS || pipelineStage is VoicePipelineStage.RefiningLLM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    WaveformVisualizer()
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(scale1, scale2, scale3, scale1, scale2).forEach { scale ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height((20 * scale).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DearTalkSecondary)
            )
        }
    }
}
