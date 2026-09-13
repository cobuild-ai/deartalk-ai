package ai.deartalk.android.ime.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import ai.deartalk.android.data.pref.CustomToneManager
import ai.deartalk.android.data.pref.TranslationTarget
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.live.data.getLabel

enum class MicUiState {
    IDLE,
    PREPARING,
    LISTENING,
    PROCESSING_AI
}

@Composable
fun DearTalkScreen(
    micUiState: MicUiState = MicUiState.IDLE,
    activeTier: ai.deartalk.android.data.ActiveAiTier = ai.deartalk.android.data.ActiveAiTier.STT_ONLY,
    recognizedText: String,
    statusMessage: String,
    aiText: String,
    tones: List<CustomTone> = emptyList(),
    aiModes: List<ai.deartalk.android.data.pref.AiModeItem> = emptyList(),
    // 🌐 신규 모드 & 톤 & 화행 파라미터
    isTranslationMode: Boolean = false,
    selectedTargetLanguage: TranslationTarget = CustomToneManager.DEFAULT_TRANSLATIONS.first(),
    availableLanguages: List<TranslationTarget> = CustomToneManager.DEFAULT_TRANSLATIONS,
    onToggleTranslationMode: () -> Unit = {},
    onSelectTargetLanguage: (TranslationTarget) -> Unit = {},
    selectedTone: CustomTone = CustomToneManager.DEFAULT_TONES.first(),
    availableTones: List<CustomTone> = CustomToneManager.DEFAULT_TONES,
    onSelectTone: (CustomTone) -> Unit = {},
    selectedSpeechIntent: SpeechIntent = SpeechIntent.AUTO,
    detectedSpeechIntent: SpeechIntent? = null,
    onSelectSpeechIntent: (SpeechIntent) -> Unit = {},
    isRetransforming: Boolean = false,
    // 기본 액션 콜백
    onApplyTone: (CustomTone) -> Unit = {},
    onApplyAiMode: (ai.deartalk.android.data.pref.AiModeItem) -> Unit = {},
    onMainMicClick: () -> Unit,
    onApplyAiText: (String) -> Unit,
    onClearAiTextClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onDeleteSentenceClick: () -> Unit = {},
    onSpaceClick: () -> Unit = {},
    onEnterClick: () -> Unit,
    onSwitchToKeyboardClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onLiveClick: () -> Unit = {},
    onDownloadPackClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    val isListening = micUiState == MicUiState.LISTENING
    val isPreparing = micUiState == MicUiState.PREPARING
    val isProcessingAi = micUiState == MicUiState.PROCESSING_AI

    val rawStt = recognizedText.trim()
    val refinedAi = aiText.trim()
    val displayText = refinedAi.ifBlank { rawStt }
    val hasContent = displayText.isNotBlank()

    var isToneMenuExpanded by remember { mutableStateOf(false) }
    var isLangMenuExpanded by remember { mutableStateOf(false) }

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
            .wrapContentHeight()
            .navigationBarsPadding(),
        color = DearTalkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─────────────────────────────────────────────────────────────
            // [1열 최상단] 🎙️ AI 음성 입력 메인 버튼 + [⌨️ 자판] + [✨ Live] + [⚙️ 설정]
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🎙️ 대형 AI 음성 입력 마이크 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMainMicClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
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
                        .height(44.dp)
                        .width(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkKeyActive),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Keyboard, contentDescription = UiStrings.keyboardContentDesc, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(UiStrings.keyboard, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // 🎙️ 실시간 대면 통역 바로가기 아이콘 (Live)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLiveClick()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
                            )
                        )
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = UiStrings.liveTitle,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // ⚙️ 설정 아이콘
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSettingsClick()
                    },
                    modifier = Modifier
                        .size(44.dp)
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

            // STT_ONLY 상태 시 모델 다운로드 배너 노출
            if (activeTier == ai.deartalk.android.data.ActiveAiTier.STT_ONLY) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF78350F).copy(alpha = 0.5f))
                        .clickable { onDownloadPackClick() }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = UiStrings.tierBadgeSttOnly,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "📥 ${UiStrings.tierDownloadAction} ›",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }

            // ─────────────────────────────────────────────────────────────
            // [2열] ✨ 스마트 DIFF 작업 캔버스 (헤더 스트립 + 마이크/로봇 아이콘화)
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasContent) Color(0xFF1E293B) else DearTalkSurface
                ),
                border = if (hasContent) androidx.compose.foundation.BorderStroke(1.dp, DearTalkSecondary.copy(alpha = 0.5f)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // [캔버스 상단 툴바 스트립] 모드(다듬기/번역) + 톤(비즈니스 등) + 비우기
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 좌측: [✨ 다듬기] ↔ [🌐 ➔ 영어 ▾] 세그먼트
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // [✨ 다듬기] 토글 칩
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isTranslationMode) Color(0xFF4338CA) else Color(0xFF334155).copy(alpha = 0.6f))
                                    .border(
                                        width = 1.dp,
                                        color = if (!isTranslationMode) Color(0xFFA5B4FC) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isTranslationMode) onToggleTranslationMode()
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = UiStrings.modeRefine,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (!isTranslationMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isTranslationMode) Color.White else DearTalkTextDim
                                )
                            }

                            // [🌐 번역 ▾] 인라인 토글 칩
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTranslationMode) Color(0xFF0284C7) else Color(0xFF334155).copy(alpha = 0.6f))
                                    .border(
                                        width = 1.dp,
                                        color = if (isTranslationMode) Color(0xFF7DD3FC) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isLangMenuExpanded = !isLangMenuExpanded
                                        isToneMenuExpanded = false
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${selectedTargetLanguage.flag} ${selectedTargetLanguage.name}",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isTranslationMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTranslationMode) Color.White else DearTalkTextDim
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isTranslationMode) Color.White else DearTalkTextDim,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            // 중앙/우측: [💼 비즈니스 ▾] 톤 토글 뱃지
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isToneMenuExpanded = !isToneMenuExpanded
                                        isLangMenuExpanded = false
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedTone.icon, fontSize = 10.5.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = selectedTone.name,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = DearTalkTextDim,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // 우측: [✕] 내용 비우기 미니 버튼 (내용이 있을 때만 노출)
                        if (hasContent) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155).copy(alpha = 0.7f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onClearAiTextClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = UiStrings.clearContentDesc,
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // [인라인 언어 선택 바 - PopupWindow 제거로 화면 깜빡임 0%]
                    AnimatedVisibility(
                        visible = isLangMenuExpanded,
                        enter = expandVertically(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)),
                        exit = shrinkVertically(animationSpec = tween(120)) + fadeOut(animationSpec = tween(120))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableLanguages.forEach { lang ->
                                val isSelected = selectedTargetLanguage.id == lang.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF7DD3FC) else Color(0xFF334155),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isLangMenuExpanded = false
                                            onSelectTargetLanguage(lang)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(lang.flag, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            lang.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // [인라인 톤앤매너 선택 바 - PopupWindow 제거로 화면 깜빡임 0%]
                    AnimatedVisibility(
                        visible = isToneMenuExpanded,
                        enter = expandVertically(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)),
                        exit = shrinkVertically(animationSpec = tween(120)) + fadeOut(animationSpec = tween(120))
                    ) {
                        val tonesList = if (tones.isNotEmpty()) tones else availableTones
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tonesList.forEach { tone ->
                                val isSelected = selectedTone.id == tone.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF7DD3FC) else Color(0xFF334155),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isToneMenuExpanded = false
                                            onSelectTone(tone)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tone.icon, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            tone.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFF334155).copy(alpha = 0.5f)
                    )

                    // [캔버스 본문] 100% 가로폭 활용 스마트 DIFF 텍스트 (마이크/로봇 아이콘 캡슐 적용)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp, max = 110.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isPreparing) {
                                Text(
                                    text = UiStrings.micConnecting,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            } else if (isListening) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDC2626).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (rawStt.isNotBlank()) rawStt else UiStrings.speakNowHint,
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            } else if (isProcessingAi) {
                                // STT 원문
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF334155)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = UiStrings.sttRaw, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rawStt,
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 2
                                    )
                                }
                                // AI 변환 중
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0C4A6E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = UiStrings.aiRefine, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = UiStrings.aiRefiningContext,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            } else if (hasContent) {
                                // 🌟 100% 가로폭 스마트 DIFF 뷰
                                if (rawStt.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF334155)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = UiStrings.sttRaw, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                        }
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
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0C4A6E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = UiStrings.aiRefine, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRetransforming) "✨ AI 문맥 재점검 중..." else refinedAi.ifBlank { rawStt },
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRetransforming) Color(0xFF38BDF8) else Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = UiStrings.canvasPlaceholder,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = DearTalkTextDim.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─────────────────────────────────────────────────────────────
            // [3열] 🎯 4대 발화 의도(화행) 칩 바 (맨 좌측 AI 로봇 아이콘 + 질문, 설명, 부탁, 확인)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🤖 맨 좌측: AI 지능형 의도 감지 캡슐 (AUTO 상태 표시 & 탭 시 스마트 모드로 리셋)
                val isAutoSelected = selectedSpeechIntent == SpeechIntent.AUTO
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isAutoSelected) Color(0xFF0C4A6E).copy(alpha = 0.7f) else DearTalkKey
                        )
                        .border(
                            width = if (isAutoSelected) 1.5.dp else 1.dp,
                            color = if (isAutoSelected) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color(0xFF6366F1).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectSpeechIntent(SpeechIntent.AUTO)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI 자동 감지 (기본)",
                        tint = if (isAutoSelected) Color(0xFF38BDF8) else Color.Gray,
                        modifier = Modifier.size(17.dp)
                    )
                }

                val intentItems = listOf(
                    SpeechIntent.QUESTION,
                    SpeechIntent.STATEMENT,
                    SpeechIntent.REQUEST,
                    SpeechIntent.CONFIRM
                )

                val effectiveActiveIntent = if (selectedSpeechIntent != SpeechIntent.AUTO) {
                    selectedSpeechIntent
                } else {
                    detectedSpeechIntent ?: SpeechIntent.STATEMENT
                }

                intentItems.forEach { intent ->
                    val isActive = effectiveActiveIntent == intent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) Color(0xFF4338CA) else DearTalkKey
                            )
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) Color(0xFFA5B4FC) else Color(0xFF6366F1).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSpeechIntent(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = intent.getLabel(),
                            color = if (isActive) Color.White else DearTalkText,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // ─────────────────────────────────────────────────────────────
            // [4열 최하단] ⚙️ 통합 유틸리티 바 ([⌫ 문장삭제] + [✓ 메시지 입력 (대형)] + [↵ 줄바꿈])
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⌫ 문장 단위 삭제 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteSentenceClick()
                    },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = UiStrings.deleteSentenceContentDesc,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = UiStrings.deleteSentence,
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // 📥 [✓ 메시지 입력] — 대형 메인 CTA 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (displayText.isNotBlank()) {
                            onApplyAiText(displayText)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasContent) Color(0xFF059669) else DearTalkKeyActive
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = UiStrings.applyContentDesc,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = UiStrings.applyMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // ↵ 전송 / 줄바꿈(Enter)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnterClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DearTalkSecondary)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = UiStrings.enterContentDesc,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
