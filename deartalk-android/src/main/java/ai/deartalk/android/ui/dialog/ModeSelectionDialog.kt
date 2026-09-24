package ai.deartalk.android.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.deartalk.android.data.pref.KeyboardMode
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*

/**
 * 🎛️ 최초 사용자 맞춤 2-Tier 모드 선택 온보딩 다이얼로그 (ModeSelectionDialog)
 * - 3세 이상 전연령이 설명서 없이 1초 만에 이해하고 선택할 수 있는 비주얼 카드 UI
 */
@Composable
fun ModeSelectionDialog(
    currentMode: KeyboardMode,
    onDismiss: () -> Unit,
    onConfirmMode: (KeyboardMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    val isKorean = UiStrings.isKo
    val isIndonesian = UiStrings.isId

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DearTalkSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6366F1).copy(alpha = 0.5f))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isKorean) "어떤 용도로 주로 쓰시나요?"
                    else if (isIndonesian) "Pilih Mode Penggunaan"
                    else "Choose Your Usage Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DearTalkText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isKorean) "나에게 꼭 맞는 키보드 환경을 선택하세요"
                    else if (isIndonesian) "Pilih tampilan keyboard yang paling nyaman"
                    else "Select the keyboard style that fits you best",
                    fontSize = 12.sp,
                    color = DearTalkTextDim
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 🟢 카드 1: 기본 모드 (Basic / 일상)
                val isBasicSelected = selectedMode == KeyboardMode.BASIC
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isBasicSelected) Color(0xFF4338CA).copy(alpha = 0.25f) else Color(0xFF1E293B))
                        .border(
                            width = if (isBasicSelected) 2.dp else 1.dp,
                            color = if (isBasicSelected) Color(0xFF818CF8) else Color(0xFF334155),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedMode = KeyboardMode.BASIC }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isKorean) "기본 모드 (일상·대화)"
                                    else if (isIndonesian) "Mode Dasar (Harian)"
                                    else "Basic Mode (Everyday)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBasicSelected) Color.White else DearTalkText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isKorean) "추천" else if (isIndonesian) "Rekomendasi" else "Recommended",
                                        fontSize = 10.sp,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isKorean) "내 말을 공손하고 자연스러운 어조로 다듬어줘요 (일상 대화에 집중)"
                                else if (isIndonesian) "Rapikan teks percakapan harian secara sopan dan alami"
                                else "Refines everyday speech into polite and natural conversations",
                                fontSize = 11.5.sp,
                                color = DearTalkTextDim,
                                lineHeight = 15.sp
                            )
                        }
                        if (isBasicSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 🔵 카드 2: 프로 모드 (Pro / 글로벌)
                val isProSelected = selectedMode == KeyboardMode.PRO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isProSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B))
                        .border(
                            width = if (isProSelected) 2.dp else 1.dp,
                            color = if (isProSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedMode = KeyboardMode.PRO }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKorean) "프로 모드 (글로벌·비즈니스)"
                                else if (isIndonesian) "Mode Pro (Global & Bisnis)"
                                else "Pro Mode (Global & Business)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProSelected) Color.White else DearTalkText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isKorean) "기본 모드 기능 포함 · 글로벌 비즈니스 및 실시간 번역 (DearTalk Live 대면 통역)"
                                else if (isIndonesian) "Termasuk mode dasar · Bisnis global, terjemahan langsung & DearTalk Live"
                                else "Includes Basic mode · Global business, real-time translation & DearTalk Live",
                                fontSize = 11.5.sp,
                                color = DearTalkTextDim,
                                lineHeight = 15.sp
                            )
                        }
                        if (isProSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isKorean) "※ 모드는 언제든 앱 설정에서 자유롭게 바꾸실 수 있습니다."
                    else if (isIndonesian) "※ Mode dapat diubah kapan saja di Pengaturan."
                    else "※ You can switch modes anytime in Settings.",
                    fontSize = 11.sp,
                    color = DearTalkTextDim
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onConfirmMode(selectedMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == KeyboardMode.BASIC) Color(0xFF4F46E5) else Color(0xFF0284C7)
                    )
                ) {
                    Text(
                        text = if (isKorean) "선택 완료하고 시작하기"
                        else if (isIndonesian) "Simpan & Mulai"
                        else "Save & Get Started",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
