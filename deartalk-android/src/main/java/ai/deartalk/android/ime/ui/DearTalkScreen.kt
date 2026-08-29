package ai.deartalk.android.ime.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*

enum class MicUiState {
    IDLE,
    PREPARING,
    LISTENING,
    PROCESSING_AI
}

@Composable
fun DearTalkScreen(
    micUiState: MicUiState = MicUiState.IDLE,
    recognizedText: String,
    statusMessage: String,
    aiText: String,
    tones: List<CustomTone> = emptyList(),
    aiModes: List<ai.deartalk.android.data.pref.AiModeItem> = emptyList(),
    onApplyTone: (CustomTone) -> Unit = {},
    onApplyAiMode: (ai.deartalk.android.data.pref.AiModeItem) -> Unit = {},
    onMainMicClick: () -> Unit,
    onApplyAiText: (String) -> Unit,
    onClearAiTextClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteSentenceClick: () -> Unit = {},
    onSpaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSwitchToKeyboardClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    val isListening = micUiState == MicUiState.LISTENING
    val isPreparing = micUiState == MicUiState.PREPARING
    val isProcessingAi = micUiState == MicUiState.PROCESSING_AI

    val rawStt = recognizedText.trim()
    val refinedAi = aiText.trim()
    val displayText = refinedAi.ifBlank { rawStt }
    val hasContent = displayText.isNotBlank()

    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = DearTalkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─────────────────────────────────────────────────────────────
            // [1열 최상단] 🎙️ AI 음성 입력 메인 버튼 + 우측 [⌨️ 자판 바로가기]
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🎙️ 대형 AI 음성 입력 마이크 버튼 (완전 준비 상태 피드백 포함)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMainMicClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .scale(if (isListening) pulseScale else 1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (micUiState) {
                            MicUiState.PREPARING -> Color(0xFFD97706)      // ⏳ 마이크 준비 중 (오렌지)
                            MicUiState.LISTENING -> Color(0xFFDC2626)      // 🔴 지금 말씀하세요 (레드 펄스)
                            MicUiState.PROCESSING_AI -> Color(0xFF6366F1)  // 🔒 AI 변환 중 (인디고)
                            MicUiState.IDLE -> DearTalkPrimary             // 🎙️ 평상시 대기 (블루)
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = when (micUiState) {
                            MicUiState.PREPARING -> Icons.Default.HourglassTop
                            MicUiState.LISTENING -> Icons.Default.StopCircle
                            MicUiState.PROCESSING_AI -> Icons.Default.AutoFixHigh
                            MicUiState.IDLE -> Icons.Default.Mic
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (micUiState) {
                            MicUiState.PREPARING -> UiStrings.micPreparing
                            MicUiState.LISTENING -> UiStrings.micListening
                            MicUiState.PROCESSING_AI -> UiStrings.micProcessingAi
                            MicUiState.IDLE -> UiStrings.micIdle
                        },
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // ⌨️ 키보드 바로가기 자판 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchToKeyboardClick()
                    },
                    modifier = Modifier
                        .height(46.dp)
                        .width(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkKeyActive),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Keyboard, contentDescription = UiStrings.keyboardContentDesc, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(UiStrings.keyboard, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // ⚙️ 설정 아이콘 (DearTalk 설정 화면으로 진입)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSettingsClick()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DearTalkKey)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = UiStrings.settingsContentDesc,
                        tint = DearTalkTextDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // ─────────────────────────────────────────────────────────────
            // [2열] ✨ 스마트 DIFF 작업 캔버스 (STT 원문 ➔ AI 다듬기 비교) + 우측 [📥 입력] / [✕ 취소]
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasContent) Color(0xFF1E293B) else DearTalkSurface
                ),
                border = if (hasContent) androidx.compose.foundation.BorderStroke(1.dp, DearTalkSecondary.copy(alpha = 0.5f)) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 왼쪽: 스크롤 가능한 대형 스마트 DIFF 캔버스
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp, max = 120.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isPreparing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = UiStrings.micConnecting,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24)
                                    )
                                }
                                Text(
                                    text = UiStrings.micConnectingDesc,
                                    fontSize = 13.sp,
                                    color = DearTalkTextDim
                                )
                            } else if (isListening) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = UiStrings.listeningLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DearTalkSecondary
                                    )
                                }
                                Text(
                                    text = if (rawStt.isNotBlank()) rawStt else UiStrings.speakNowHint,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            } else if (isProcessingAi) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = UiStrings.sttRaw,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF334155))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rawStt,
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 2
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = UiStrings.aiRefine,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF0C4A6E))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = UiStrings.aiRefiningContext,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            } else if (rawStt.isNotBlank() || refinedAi.isNotBlank()) {
                                // 🌟 스마트 DIFF 뷰: STT 원문과 AI 다듬기 항상 명확 대조
                                if (rawStt.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = UiStrings.sttRaw,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF334155))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = rawStt,
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 2
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = UiStrings.aiRefine,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF0C4A6E))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = refinedAi.ifBlank { rawStt },
                                        fontSize = 14.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = UiStrings.canvasPlaceholder,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = DearTalkTextDim.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 우측: [📥 입력] 과 [✕ 취소(비우기)] 대칭 배치
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // [📥 입력]
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (displayText.isNotBlank()) {
                                    onApplyAiText(displayText)
                                }
                            },
                            modifier = Modifier
                                .height(34.dp)
                                .width(74.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasContent) Color(0xFF059669) else DearTalkKeyActive
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = UiStrings.applyContentDesc, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(UiStrings.apply, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // [✕ 취소] (AI Text 비우기)
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClearAiTextClick()
                            },
                            modifier = Modifier
                                .height(30.dp)
                                .width(74.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155).copy(alpha = 0.85f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = UiStrings.cancelContentDesc, tint = Color(0xFFFCA5A5), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(UiStrings.cancel, color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // ─────────────────────────────────────────────────────────────
            // [3열] 👔 6대 톤앤매너 프리셋 칩 바
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 6대 톤앤매너 칩들 (기본다듬기, 공손하게, 친근하게, 비즈니스, 재미있게, 당당하게)
                var selectedToneId by remember { mutableStateOf<String?>(null) }
                val displayTones = if (tones.isNotEmpty()) tones else ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES
                displayTones.forEach { tone ->
                    val isSelected = selectedToneId == tone.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF4338CA) else DearTalkKey)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFF6366F1).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedToneId = tone.id
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onApplyTone(tone)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tone.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tone.name,
                                color = if (isSelected) Color.White else DearTalkText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ─────────────────────────────────────────────────────────────
            // [4열] ⚙️ 최하단 유틸리티 바 (1글자 지우기, 1문장 지우기, 스페이스, 줄바꿈)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⌫ 1글자 지우기(Backspace)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDeleteClick()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkKey)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = UiStrings.deleteCharContentDesc, tint = DearTalkText, modifier = Modifier.size(16.dp))
                }

                // ⌫ 문장 단위 삭제 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteSentenceClick()
                    },
                    modifier = Modifier
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = UiStrings.deleteSentenceContentDesc, tint = Color(0xFFFCA5A5), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(UiStrings.deleteSentence, color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Space 띄어쓰기
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpaceClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkKey),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(UiStrings.space, color = DearTalkText, fontSize = 12.sp)
                }

                // ↵ 전송 / 줄바꿈(Enter)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnterClick()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkSecondary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = UiStrings.enterContentDesc, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
