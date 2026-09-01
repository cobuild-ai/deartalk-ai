package ai.deartalk.android.voicestudio.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
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

/**
 * 🌐 1-Tap 실시간 통역 상대방 언어 선택 바 (군더더기 없는 미니멀 Clean UX)
 * 내 언어는 시스템 설정으로 자동 바인딩되며, 사용자는 상대방 언어 딱 1개만 탭하여 양방향 통역 페어를 완성합니다.
 */
@Composable
fun TargetLanguageSelectorRow(
    myLangCode: String,
    targetLang: String,
    onTargetLangSelected: (String) -> Unit
) {
    // 내 언어를 제외한 상대방 대상 언어 목록
    val candidateLangs = LanguageLocaleHelper.SUPPORTED_LANG_CODES.filterNot {
        it.equals(myLangCode, ignoreCase = true)
    }

    val myLabel = UiStrings.getLangDisplayName(myLangCode)
    val targetLabel = UiStrings.getLangDisplayName(targetLang)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 헤더: [ 🔗 🇰🇷 한국어 ⇄ 🇺🇸 English (양방향 자동 통역) ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = DearTalkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = UiStrings.conversationPairBadge(myLabel, targetLabel),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1-Tap 상대방 언어 선택 칩 리스트
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                candidateLangs.forEach { code ->
                    val isSelected = targetLang.equals(code, ignoreCase = true)
                    val label = UiStrings.getLangDisplayName(code)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DearTalkPrimary else DearTalkKey)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onTargetLangSelected(code) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else DearTalkText
                        )
                    }
                }
            }
        }
    }
}
