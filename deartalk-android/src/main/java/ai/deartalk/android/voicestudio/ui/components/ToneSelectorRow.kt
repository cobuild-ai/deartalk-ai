package ai.deartalk.android.voicestudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkKey
import ai.deartalk.android.ime.ui.theme.DearTalkSecondary
import ai.deartalk.android.ime.ui.theme.DearTalkText

@Composable
fun ToneSelectorRow(selectedTone: String, onToneSelected: (String) -> Unit) {
    val tones = listOf(
        UiStrings.toneRefine,
        UiStrings.tonePolite,
        UiStrings.toneCasual,
        UiStrings.toneBusiness,
        UiStrings.toneCheeky,
        UiStrings.toneFunny
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tones.forEach { tone ->
            val isSelected = selectedTone == tone
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) DearTalkSecondary else DearTalkKey)
                    .clickable { onToneSelected(tone) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tone,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else DearTalkText
                )
            }
        }
    }
}
