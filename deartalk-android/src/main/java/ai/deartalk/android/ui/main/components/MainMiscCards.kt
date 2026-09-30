package ai.deartalk.android.ui.main.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FamilyAppsSectionCard(
    context: Context,
    isKorean: Boolean,
    isIndonesian: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DearTalkPrimary.copy(alpha = 0.5f))
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (isKorean) "🤝 Cobuild AI 패밀리 앱" else if (isIndonesian) "🤝 Aplikasi Keluarga Cobuild AI" else "🤝 Cobuild AI Family Suite",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Text(
                text = if (isKorean) "단 1개의 AI 모델 공유로 기기 저장공간을 효율적으로 절약합니다." 
                    else if (isIndonesian) "Berbagi 1 model AI lokal untuk menghemat ruang penyimpanan HP."
                    else "Shares a single on-device AI model to save device storage.",
                fontSize = 11.5.sp,
                color = DearTalkSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // 1. DearTalk Voice Translator (출시 예정)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DearTalkPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = DearTalkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKorean) "🗣️ DearTalk 음성 통역기" else if (isIndonesian) "🗣️ DearTalk Penerjemah Suara" else "🗣️ DearTalk Voice Translator",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkText
                        )
                        Text(
                            text = if (isKorean) "1:1 실시간 대면 통역기" else if (isIndonesian) "Penerjemah tatap muka 1:1" else "1:1 live face-to-face translator",
                            fontSize = 11.sp,
                            color = DearTalkTextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    onClick = {
                        val msg = if (isKorean) "🗣️ DearTalk 음성 통역기는 곧 출시될 예정입니다!"
                                  else if (isIndonesian) "🗣️ DearTalk Penerjemah Suara akan segera hadir!"
                                  else "🗣️ DearTalk Voice Translator is coming soon!"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = DearTalkSecondary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DearTalkSecondary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isKorean) "출시 예정" else if (isIndonesian) "Segera Hadir" else "Coming Soon",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserGuideCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = UiStrings.userGuideTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = UiStrings.userGuideHowToUseTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = UiStrings.userGuideHowToUseContent,
                fontSize = 12.sp,
                color = DearTalkTextDim,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ZeroPersistencePrivacyCard(
    isKorean: Boolean,
    isIndonesian: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isKorean) "🛡️ 100% Zero-Persistence 프라이버시" else if (isIndonesian) "🛡️ Privasi 100% Zero-Persistence" else "🛡️ 100% Zero-Persistence Privacy",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isKorean) "음성과 변환 내용은 단 1바이트도 저장되지 않으며, 변환 즉시 완전히 소멸됩니다." else if (isIndonesian) "Suara dan teks tidak disimpan sedikit pun, langsung dihapus dari memori." else "No voice or text data is stored. Disappears from memory instantly.",
                fontSize = 11.sp,
                color = DearTalkTextDim,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SwitchKeyboardCard(
    isKorean: Boolean,
    isIndonesian: Boolean,
    onSelectIme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isKorean) "⌨️ 키보드 언제든 변경 / 전환" else if (isIndonesian) "⌨️ Beralih / Ganti Papan Ketik" else "⌨️ Switch / Change Keyboard",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isKorean)
                    "기존 키보드(삼성, Gboard 등)로 언제든 자유롭게 되돌아가실 수 있습니다."
                else if (isIndonesian)
                    "Anda dapat kembali ke keyboard sebelumnya (Samsung, Gboard, dll.) kapan saja."
                else
                    "Switch back to your previous keyboard (Samsung, Gboard, etc.) anytime.",
                fontSize = 11.sp,
                color = DearTalkTextDim,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSelectIme,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DearTalkSecondary)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isKorean) "🔄 다른 키보드로 바로 전환하기" else if (isIndonesian) "🔄 Beralih ke Papan Ketik Lain" else "🔄 Switch to Another Keyboard",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun AppAboutCard(
    context: Context,
    isKorean: Boolean,
    isIndonesian: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = UiStrings.settingsTabAbout,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Spacer(modifier = Modifier.height(8.dp))

            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) { null }

            val buildTimeStr = packageInfo?.let {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.format(Date(it.lastUpdateTime))
            } ?: "2026-08-23 21:45:00"

            DiagnosticRow(
                label = if (isKorean) "앱 이름" else if (isIndonesian) "Nama Aplikasi" else "App Name",
                value = "DearTalk AI",
                valueColor = DearTalkText
            )
            DiagnosticRow(
                label = UiStrings.appVersionLabel,
                value = "v${packageInfo?.versionName ?: "1.0.0"} (${if (isKorean) "빌드" else "Build"} ${packageInfo?.longVersionCode ?: 1})",
                valueColor = DearTalkText
            )
            DiagnosticRow(
                label = UiStrings.buildTimestampLabel,
                value = buildTimeStr,
                valueColor = DearTalkText
            )
            DiagnosticRow(
                label = if (isKorean) "보안 등급" else if (isIndonesian) "Keamanan" else "Security",
                value = if (isKorean) "🔒 100% 온디바이스 (외부 유출 0%)" else if (isIndonesian) "🔒 100% On-Device (Nol Kebocoran)" else "🔒 100% On-Device (Zero Cloud Leak)",
                valueColor = DearTalkSecondary
            )
        }
    }
}
