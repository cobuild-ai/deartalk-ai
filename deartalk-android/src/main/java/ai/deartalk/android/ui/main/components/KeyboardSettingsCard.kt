package ai.deartalk.android.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.ime.ui.theme.*

/**
 * ⌨️ 키보드 및 기본 언어 환경 설정 카드 (KeyboardSettingsCard)
 * - 기본 언어 자동 맞춤(기기 시스템 기본 언어) 토글 스위치
 * - 비한국어 및 다국어 테스트를 위한 수동 언어 선택 지원 (9개 주요 언어)
 * - 한국어 환경 시 한글 자판 형태(두벌식/천지인) 선택 제공
 */
@Composable
fun KeyboardSettingsCard(
    isAutoLanguage: Boolean,
    selectedLanguageCode: String,
    selectedKoreanKeyboardType: KoreanKeyboardType,
    isKorean: Boolean,
    isIndonesian: Boolean,
    onSetAutoLanguage: (Boolean) -> Unit,
    onSelectLanguageCode: (String) -> Unit,
    onChangeKoreanKeyboardType: (KoreanKeyboardType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 헤더
            Text(
                text = if (isKorean) "⚙️ 기본 언어 및 자판 설정" else if (isIndonesian) "⚙️ Pengaturan Bahasa & Keyboard" else "⚙️ Language & Keyboard Settings",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Text(
                text = if (isKorean) "음성 인식 및 AI 보정에 적용할 기본 언어를 설정합니다." else if (isIndonesian) "Atur bahasa utama untuk pengenalan suara dan AI." else "Set default language for speech recognition and AI.",
                fontSize = 11.5.sp,
                color = DearTalkTextDim
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. 기본 언어 자동 맞춤 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKorean) "🌐 기본 언어 자동 맞춤" else if (isIndonesian) "🌐 Deteksi Bahasa Otomatis" else "🌐 Auto Language Detection",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isKorean) "휴대폰 시스템 설정 언어에 100% 자동 연동" else if (isIndonesian) "Gunakan bahasa sistem ponsel secara otomatis" else "Automatically follow device system language",
                        fontSize = 11.sp,
                        color = DearTalkTextDim
                    )
                }
                Switch(
                    checked = isAutoLanguage,
                    onCheckedChange = onSetAutoLanguage,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DearTalkSecondary,
                        checkedTrackColor = DearTalkPrimary.copy(alpha = 0.5f)
                    )
                )
            }

            // 2. 수동 언어 선택 칩 (자동 맞춤 꺼짐 시 노출)
            if (!isAutoLanguage) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isKorean) "👉 사용할 언어 직접 선택:" else if (isIndonesian) "👉 Pilih bahasa yang digunakan:" else "👉 Select language manually:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DearTalkSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DearTalkSettings.SUPPORTED_LANGUAGES.forEach { lang ->
                        val isSelected = selectedLanguageCode == lang.code
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectLanguageCode(lang.code) },
                            label = { Text("${lang.flag} ${lang.nativeName}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DearTalkPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = DearTalkKey,
                                labelColor = DearTalkTextDim
                            )
                        )
                    }
                }
            }

            // 3. 한글 자판 형태 (한국어 환경이거나 한국어가 선택된 경우 노출)
            val showKoreanLayout = isKorean || (!isAutoLanguage && selectedLanguageCode == "ko")
            if (showKoreanLayout) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⌨️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isKorean) "한글 자판 형태" else "Hangul Layout",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DearTalkText
                                )
                                Text(
                                    text = if (isKorean) "두벌식 또는 천지인 선택" else "Dubeolsik or Cheonjiin",
                                    fontSize = 10.sp,
                                    color = DearTalkTextDim
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DearTalkKeyActive)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedKoreanKeyboardType == KoreanKeyboardType.DUBEOLSIK) DearTalkPrimary else Color.Transparent)
                                    .clickable { onChangeKoreanKeyboardType(KoreanKeyboardType.DUBEOLSIK) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "두벌식",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedKoreanKeyboardType == KoreanKeyboardType.DUBEOLSIK) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedKoreanKeyboardType == KoreanKeyboardType.DUBEOLSIK) Color.White else DearTalkTextDim
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedKoreanKeyboardType == KoreanKeyboardType.CHEONJIIN) DearTalkPrimary else Color.Transparent)
                                    .clickable { onChangeKoreanKeyboardType(KoreanKeyboardType.CHEONJIIN) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "천지인",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedKoreanKeyboardType == KoreanKeyboardType.CHEONJIIN) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedKoreanKeyboardType == KoreanKeyboardType.CHEONJIIN) Color.White else DearTalkTextDim
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
