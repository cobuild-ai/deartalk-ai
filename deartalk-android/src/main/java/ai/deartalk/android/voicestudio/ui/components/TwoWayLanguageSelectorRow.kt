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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import ai.deartalk.android.util.LanguageLocaleHelper

@Composable
fun TwoWayLanguageSelectorRow(
    sourceLang: String,
    onSourceLangSelected: (String) -> Unit,
    targetLang: String,
    onTargetLangSelected: (String) -> Unit,
    onSwap: () -> Unit
) {
    val langCodes = LanguageLocaleHelper.SUPPORTED_LANG_CODES

    val sourceLabel = UiStrings.getLangDisplayName(sourceLang)
    val targetLabel = UiStrings.getLangDisplayName(targetLang)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 헤더: [ 🗣️ 말할 언어 ]  ⇄ (스왑)  [ 🌐 번역 언어 ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(UiStrings.inputHeaderLabel, fontSize = 11.sp, color = DearTalkTextDim)
                    Text(sourceLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkSecondary)
                }

                // ⇄ 언어 맞바꾸기(Swap) 버튼
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DearTalkKey)
                        .clickable(onClick = onSwap)
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = UiStrings.swapLangContentDesc,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(UiStrings.outputHeaderLabel, fontSize = 11.sp, color = DearTalkTextDim)
                    Text(targetLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DearTalkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. 내가 말할 언어 (마이크 STT 입력)
            Text(UiStrings.step1SpokenLang, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DearTalkTextDim)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                langCodes.forEach { code ->
                    val isSelected = sourceLang.equals(code, ignoreCase = true)
                    val label = UiStrings.getLangDisplayName(code)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) DearTalkSecondary else DearTalkKey)
                            .clickable { onSourceLangSelected(code) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else DearTalkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. AI가 번역할 언어 (스피커 TTS 출력)
            Text(UiStrings.step2TargetLang, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DearTalkTextDim)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                langCodes.forEach { code ->
                    val isSelected = targetLang.equals(code, ignoreCase = true)
                    val label = UiStrings.getLangDisplayName(code)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) DearTalkPrimary else DearTalkKey)
                            .clickable { onTargetLangSelected(code) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else DearTalkText
                        )
                    }
                }
            }
        }
    }
}
