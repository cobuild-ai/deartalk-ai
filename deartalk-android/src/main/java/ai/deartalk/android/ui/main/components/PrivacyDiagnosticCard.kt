package ai.deartalk.android.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.ime.ui.theme.*

/**
 * ⚡ AI 엔진 동작 상태 및 진단 카드 (간결하고 직관적인 상태 표기)
 * - 중복된 온디바이스/오프라인 문구를 제거하고 핵심 진단 지표 3가지를 명확히 제시
 */
@Composable
fun PrivacyDiagnosticCard(
    isModelLoaded: Boolean,
    isKorean: Boolean,
    isIndonesian: Boolean,
    onDetectAndInitModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DearTalkSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = DearTalkSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isKorean) "⚡ AI 엔진 동작 상태" else if (isIndonesian) "⚡ Status Mesin AI" else "⚡ AI Engine Status",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DearTalkText
                    )
                    Text(
                        text = if (isModelLoaded) {
                            if (isKorean) "온디바이스 AI 활성화 (0.2초 초고속 변환)"
                            else if (isIndonesian) "AI On-Device Aktif (Respons 0,2 detik)"
                            else "On-Device AI Active (~0.2s ultra-fast)"
                        } else {
                            if (isKorean) "오프라인 음성 인식 준비 완료"
                            else if (isIndonesian) "Pengenalan Suara Offline Siap"
                            else "Offline Voice Recognition Ready"
                        },
                        fontSize = 11.5.sp,
                        color = if (isModelLoaded) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 진단 지표
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticRow(
                    label = if (isKorean) "⚡ 동작 방식" else if (isIndonesian) "⚡ Pemrosesan" else "⚡ Mode",
                    value = if (isKorean) "100% 로컬 오프라인 (외부 전송 0%)" else if (isIndonesian) "100% Offline Lokal" else "100% Local Offline",
                    valueColor = Color(0xFF4ADE80)
                )
                DiagnosticRow(
                    label = if (isKorean) "🔒 데이터 보호" else if (isIndonesian) "🔒 Perlindungan" else "🔒 Privacy",
                    value = if (isKorean) "변환 즉시 메모리 소멸" else if (isIndonesian) "Langsung dihapus dari memori" else "Zero-Persistence (Cleared instantly)",
                    valueColor = Color(0xFF4ADE80)
                )
            }

            if (!isModelLoaded) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDetectAndInitModel,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DearTalkSecondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKorean) "AI 모델 상태 다시 확인" else if (isIndonesian) "Periksa Status AI" else "Refresh AI Status",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
