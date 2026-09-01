package ai.deartalk.android.voicestudio.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkKey
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSecondary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.ime.ui.theme.DearTalkTextDim
import ai.deartalk.android.tts.VoiceGender

@Composable
fun VoiceCustomizerRow(
    selectedGender: VoiceGender,
    onGenderSelected: (VoiceGender) -> Unit,
    selectedPitch: Float,
    onPitchSelected: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // 성별 선택 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(UiStrings.voiceToneCustomizerTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkText)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkKey)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedGender == VoiceGender.FEMALE) DearTalkPrimary else Color.Transparent)
                            .clickable { onGenderSelected(VoiceGender.FEMALE) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            UiStrings.voiceFemale,
                            fontSize = 11.sp,
                            fontWeight = if (selectedGender == VoiceGender.FEMALE) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedGender == VoiceGender.FEMALE) Color.White else DearTalkTextDim
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedGender == VoiceGender.MALE) DearTalkPrimary else Color.Transparent)
                            .clickable { onGenderSelected(VoiceGender.MALE) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            UiStrings.voiceMale,
                            fontSize = 11.sp,
                            fontWeight = if (selectedGender == VoiceGender.MALE) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedGender == VoiceGender.MALE) Color.White else DearTalkTextDim
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 피치(음높이) 매칭 칩
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(UiStrings.pitchMatchingLabel, fontSize = 11.sp, color = DearTalkTextDim)
                listOf(
                    UiStrings.pitchNormal to 1.0f,
                    UiStrings.pitchDeepLow to 0.85f,
                    UiStrings.pitchWarmMid to 0.95f,
                    UiStrings.pitchBrightHigh to 1.15f
                ).forEach { (label, pitch) ->
                    val isSelected = kotlin.math.abs(selectedPitch - pitch) < 0.04f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) DearTalkSecondary else DearTalkKey)
                            .clickable { onPitchSelected(pitch) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else DearTalkText
                        )
                    }
                }
            }
        }
    }
}
