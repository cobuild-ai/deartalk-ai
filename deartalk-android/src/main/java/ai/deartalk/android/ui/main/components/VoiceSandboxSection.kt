package ai.deartalk.android.ui.main.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.ime.ui.theme.*
import ai.deartalk.android.ui.main.MainUiEvent
import ai.deartalk.android.ui.state.MainUiState

@Composable
fun VoiceSandboxSection(
    uiState: MainUiState,
    isKorean: Boolean,
    isIndonesian: Boolean,
    onEvent: (MainUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = uiState.isListening
    val aiProcessingMessage = uiState.aiProcessingMessage
    val rawUtteranceText = uiState.rawUtteranceText
    val recognizedLiveText = uiState.recognizedLiveText
    val aiTransformedText = uiState.aiTransformedText
    val testInputText = uiState.testInputText
    val activePresetText = uiState.activePresetText

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DearTalkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isKorean) "🎙️ 실시간 음성 & AI 체험하기" else if (isIndonesian) "🎙️ Uji Coba Suara & AI" else "🎙️ Real-time Voice & AI Sandbox",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isKorean) "마이크를 켜고 말씀하시거나 예시를 눌러 AI 다듬기를 체험해 보세요." else if (isIndonesian) "Bicara lewat mikrofon atau ketuk contoh kalimat untuk mencoba AI." else "Tap mic to speak or tap sample sentences to test AI refinement.",
                fontSize = 11.sp,
                color = DearTalkTextDim
            )

            Spacer(modifier = Modifier.height(14.dp))

            val transition = rememberInfiniteTransition(label = "sandbox_mic")
            val pulseScale by transition.animateFloat(
                initialValue = 1f,
                targetValue = if (isListening) 1.12f else 1f,
                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                label = "pulse"
            )

            Button(
                onClick = { onEvent(MainUiEvent.ToggleMic) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .scale(pulseScale),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFDC2626) else DearTalkPrimary
                )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.StopCircle else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isListening) {
                        if (isKorean) "녹음 중... 터치하여 완료" else if (isIndonesian) "Merekam... Ketuk Selesai" else "Recording... Tap to Stop"
                    } else {
                        if (isKorean) "마이크 켜고 말씀하기" else if (isIndonesian) "Ketuk untuk Bicara" else "Tap to Speak"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (aiProcessingMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = aiProcessingMessage,
                    fontSize = 12.sp,
                    color = if (isListening) DearTalkSecondary else Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
            }

            if (rawUtteranceText.isNotBlank() || recognizedLiveText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkKey.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(if (isKorean) "1. 내가 말한 내용:" else if (isIndonesian) "1. Yang Anda Katakan:" else "1. What You Said:", fontSize = 11.sp, color = DearTalkTextDim, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (rawUtteranceText.isNotBlank()) rawUtteranceText else recognizedLiveText,
                            fontSize = 13.sp,
                            color = DearTalkText
                        )
                    }
                }
            }

            if (aiTransformedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkSecondary.copy(alpha = 0.15f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(if (isKorean) "2. 다듬어진 문장:" else if (isIndonesian) "2. Hasil Dirapikan:" else "2. Refined Result:", fontSize = 11.sp, color = DearTalkSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(aiTransformedText, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isKorean) "💡 예시 문장 눌러서 바로 테스트:" else if (isIndonesian) "💡 Ketuk contoh kalimat untuk uji coba:" else "💡 Tap sample sentences to test:",
                fontSize = 11.sp,
                color = DearTalkTextDim,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = if (isKorean) {
                    listOf(
                        "금요일 제외한 매일 11시에서 11시30분까지는 Privacy 스크럼이니 절대로 잊지마",
                        "지금 출발했는데 도로가 너무 막혀서 15분 정도 늦을 것 같아",
                        "방금 수정한 기획안 메일로 보냈으니까 확인해보고 의견 줘",
                        "내일 점심 같이 먹을 수 있는지 시간 언제가 좋은지 알려줘",
                        "어제 부탁했던 회의록 정리 다 됐으면 나한테 넘겨줘",
                        "Hello how are you doing today please confirm",
                        "Where is the conference room please tell me"
                    )
                } else if (isIndonesian) {
                    listOf(
                        "Macet sekali, mungkin telat 15 menit ya",
                        "Proposal sudah saya kirim, tolong dicek ya",
                        "Ada waktu makan siang bareng besok?",
                        "Ruang rapat di lantai berapa ya?",
                        "Terima kasih banyak atas bantuannya"
                    )
                } else {
                    listOf(
                        "Privacy scrum is from 11:00 to 11:30 every day except Friday so never forget",
                        "I just departed but traffic is heavy so I might be 15 minutes late",
                        "Sent the updated proposal via email please review and let me know your thoughts",
                        "Please let me know when works best for lunch tomorrow",
                        "Where is the conference room please tell me"
                    )
                }
                presets.forEach { preset ->
                    val isCurrentActive = activePresetText == preset
                    SuggestionChip(
                        onClick = { onEvent(MainUiEvent.TestPreset(preset)) },
                        label = {
                            Text(
                                text = preset.take(24) + if (preset.length > 24) "..." else "",
                                fontSize = 11.sp,
                                color = if (isCurrentActive) DearTalkSecondary else DearTalkText,
                                fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isCurrentActive) DearTalkSecondary.copy(alpha = 0.18f) else Color(0xFF1E293B)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isCurrentActive) DearTalkSecondary else Color(0xFF334155)
                        )
                    )
                }
            }

            // 💬 모의 메신저 대화 시뮬레이터 (AI 비즈니스 & 톤앤매너 체험)
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DearTalkPrimary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👔", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isKorean) "팀장님" else if (isIndonesian) "Alex (Manajer)" else "Alex (Lead)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DearTalkText
                                )
                                Text(
                                    text = if (isKorean) "비즈니스 대화 체험" else if (isIndonesian) "Simulasi Obrolan" else "Mock Chat Simulator",
                                    fontSize = 10.sp,
                                    color = DearTalkSecondary
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "10:45 AM",
                                fontSize = 10.sp,
                                color = DearTalkTextDim
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isKorean) "오늘 오후 프로젝트 회의 15분 뒤에 시작합니다. 참석 가능하신가요?"
                                else if (isIndonesian) "Rapat proyek hari ini dimulai 15 menit lagi. Apakah bisa hadir?"
                                else "Today's project sync starts in 15 minutes. Can you join?",
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = testInputText,
                onValueChange = { onEvent(MainUiEvent.UpdateTestInputText(it)) },
                label = { Text(if (isKorean) "💬 답장 작성하기 (터치하여 키보드 실행)" else if (isIndonesian) "💬 Balas pesan (Ketuk untuk buka keyboard)" else "💬 Reply to message (Tap to open keyboard)") },
                placeholder = { Text(if (isKorean) "마이크로 말씀하시거나 예시를 눌러보세요" else if (isIndonesian) "Bicara atau ketuk contoh kalimat" else "Speak naturally or tap samples to refine") },
                trailingIcon = {
                    if (testInputText.isNotBlank()) {
                        IconButton(onClick = { onEvent(MainUiEvent.ClearTestInputText) }) {
                            Icon(Icons.Default.Clear, contentDescription = if (isKorean) "지우기" else if (isIndonesian) "Hapus" else "Clear", tint = DearTalkTextDim)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DearTalkSecondary,
                    unfocusedBorderColor = DearTalkKey,
                    focusedTextColor = DearTalkText,
                    unfocusedTextColor = DearTalkText,
                    focusedLabelColor = DearTalkSecondary,
                    unfocusedLabelColor = DearTalkTextDim
                )
            )
        }
    }
}
