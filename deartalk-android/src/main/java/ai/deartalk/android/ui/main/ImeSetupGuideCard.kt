package ai.deartalk.android.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.ime.ui.theme.*

/**
 * 🚀 1분 키보드 빠른 시작 (설정 및 기본 키보드 지정 가이드 카드)
 * - 클린코드 컴포넌트 분리: 상태 비저장(Stateless) 단일 책임 컴포저블
 */
@Composable
fun ImeSetupGuideCard(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    isKorean: Boolean,
    isIndonesian: Boolean,
    onEnableIme: () -> Unit,
    onSelectIme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isImeSelected) Color(0xFF22C55E).copy(alpha = 0.5f) else DearTalkSecondary.copy(alpha = 0.5f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isImeSelected) Color(0xFF166534) else DearTalkKey,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isImeSelected) "✅" else "⌨️", fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKorean) "🚀 1분 키보드 빠른 시작" else if (isIndonesian) "🚀 Mulai Cepat 1 Menit" else "🚀 Quick Keyboard Setup",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isImeSelected) {
                            if (isKorean) "✅ 기본 키보드로 설정되어 바로 사용 가능" else if (isIndonesian) "✅ Papan ketik utama siap digunakan" else "✅ Ready to use as default keyboard"
                        } else {
                            if (isKorean) "아래 2단계를 완료해 주세요" else if (isIndonesian) "Selesaikan 2 langkah di bawah" else "Complete 2 steps below"
                        },
                        fontSize = 12.sp,
                        color = if (isImeSelected) Color(0xFF4ADE80) else DearTalkSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1단계: 키보드 켜기
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKorean) "1단계: 키보드 켜기 (활성화)" else if (isIndonesian) "1. Aktifkan Papan Ketik" else "Step 1: Enable Keyboard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isImeEnabled) Color(0xFF4ADE80) else DearTalkText
                    )
                    Text(
                        text = if (isImeEnabled) {
                            if (isKorean) "✅ DearTalk AI 키보드가 켜져 있습니다" else if (isIndonesian) "✅ Papan ketik DearTalk AI telah aktif" else "✅ DearTalk AI keyboard is on"
                        } else {
                            if (isKorean) "설정에서 DearTalk AI 스위치를 켜주세요" else if (isIndonesian) "Aktifkan tombol DearTalk AI" else "Turn on DearTalk AI in settings"
                        },
                        fontSize = 11.sp,
                        color = DearTalkTextDim,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onEnableIme,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isImeEnabled) Color(0xFF166534) else DearTalkPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isImeEnabled) (if (isKorean) "✅ 완료" else if (isIndonesian) "✅ Selesai" else "✅ Done") else (if (isKorean) "설정 열기 ➔" else if (isIndonesian) "Buka Pengaturan ➔" else "Open Settings ➔"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DearTalkKey.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // 2단계: 기본 키보드로 선택
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKorean) "2단계: 기본 키보드로 선택" else if (isIndonesian) "2. Pilih Papan Ketik" else "Step 2: Set as Default Keyboard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isImeSelected) Color(0xFF4ADE80) else DearTalkText
                    )
                    Text(
                        text = if (isImeSelected) {
                            if (isKorean) "✅ 기본 키보드로 설정됨" else if (isIndonesian) "✅ Papan ketik utama aktif" else "✅ Set as default keyboard"
                        } else {
                            if (isKorean) "팝업에서 DearTalk AI를 선택하세요" else if (isIndonesian) "Pilih DearTalk AI di popup" else "Select DearTalk AI from popup"
                        },
                        fontSize = 11.sp,
                        color = DearTalkTextDim,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSelectIme,
                    enabled = isImeEnabled,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isImeSelected) Color(0xFF166534) else DearTalkSecondary,
                        disabledContainerColor = DearTalkKey
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isImeSelected) {
                            if (isKorean) "✅ 선택됨" else if (isIndonesian) "✅ Terpilih" else "✅ Selected"
                        } else {
                            if (isKorean) "키보드 선택 ➔" else if (isIndonesian) "Pilih ➔" else "Select ➔"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isImeSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}
