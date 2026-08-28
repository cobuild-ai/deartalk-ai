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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.AiModeItem
import ai.deartalk.android.data.pref.AiModeType
import ai.deartalk.android.data.pref.CustomTone
import ai.deartalk.android.data.pref.CustomToneManager
import ai.deartalk.android.data.pref.TranslationTarget
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*

enum class MicUiState {
    IDLE,
    PREPARING,
    LISTENING,
    PROCESSING_AI
}

/**
 * DearTalk AI 스마트 음성 키보드 메인 화면 (Clean Architecture & Modular Compose)
 */
@Composable
fun DearTalkScreen(
    micUiState: MicUiState = MicUiState.IDLE,
    recognizedText: String,
    statusMessage: String,
    aiText: String,
    tones: List<CustomTone> = emptyList(),
    aiModes: List<AiModeItem> = emptyList(),
    onApplyTone: (CustomTone) -> Unit = {},
    onApplyAiMode: (AiModeItem) -> Unit = {},
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
    val rawStt = recognizedText.trim()
    val refinedAi = aiText.trim()
    val displayText = refinedAi.ifBlank { rawStt }

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
            // [1열] 🎙️ 음성 입력 메인 컨트롤 바
            MicControlHeader(
                micUiState = micUiState,
                haptic = haptic,
                onMainMicClick = onMainMicClick,
                onSwitchToKeyboardClick = onSwitchToKeyboardClick,
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(5.dp))

            // [2열] ✨ 스마트 DIFF 작업 캔버스
            SmartDiffCanvas(
                micUiState = micUiState,
                rawStt = rawStt,
                refinedAi = refinedAi,
                displayText = displayText,
                haptic = haptic,
                onApplyAiText = onApplyAiText,
                onClearAiTextClick = onClearAiTextClick
            )

            Spacer(modifier = Modifier.height(5.dp))

            // [3열] 👔 톤앤매너 및 🌐 다국어 번역 셀렉터
            ToneAndTranslationBar(
                tones = tones,
                haptic = haptic,
                onApplyTone = onApplyTone,
                onApplyAiMode = onApplyAiMode
            )

            Spacer(modifier = Modifier.height(6.dp))

            // [4열] ⚙️ 최하단 키보드 유틸리티 바
            KeyboardUtilityBar(
                haptic = haptic,
                onDeleteClick = onDeleteClick,
                onDeleteSentenceClick = onDeleteSentenceClick,
                onSpaceClick = onSpaceClick,
                onEnterClick = onEnterClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. [1열] 🎙️ 음성 입력 메인 컨트롤 바
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MicControlHeader(
    micUiState: MicUiState,
    haptic: HapticFeedback,
    onMainMicClick: () -> Unit,
    onSwitchToKeyboardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isListening = micUiState == MicUiState.LISTENING
    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 메인 마이크 버튼
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
                    MicUiState.PREPARING -> Color(0xFFD97706)
                    MicUiState.LISTENING -> Color(0xFFDC2626)
                    MicUiState.PROCESSING_AI -> Color(0xFF6366F1)
                    MicUiState.IDLE -> DearTalkPrimary
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 일반 키보드 전환 버튼
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

        // 앱 설정 버튼
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
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. [2열] ✨ 스마트 DIFF 작업 캔버스
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SmartDiffCanvas(
    micUiState: MicUiState,
    rawStt: String,
    refinedAi: String,
    displayText: String,
    haptic: HapticFeedback,
    onApplyAiText: (String) -> Unit,
    onClearAiTextClick: () -> Unit
) {
    val hasContent = displayText.isNotBlank()

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
            // 좌측: DIFF 텍스트 스크롤 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 60.dp, max = 120.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterStart
            ) {
                CanvasContent(
                    micUiState = micUiState,
                    rawStt = rawStt,
                    refinedAi = refinedAi
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 우측: [📥 입력] / [✕ 취소] 액션 버튼
            CanvasActionButtons(
                hasContent = hasContent,
                displayText = displayText,
                haptic = haptic,
                onApplyAiText = onApplyAiText,
                onClearAiTextClick = onClearAiTextClick
            )
        }
    }
}

@Composable
private fun CanvasContent(
    micUiState: MicUiState,
    rawStt: String,
    refinedAi: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            micUiState == MicUiState.PREPARING -> {
                Text(text = UiStrings.micConnecting, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                Text(text = UiStrings.micConnectingDesc, fontSize = 13.sp, color = DearTalkTextDim)
            }
            micUiState == MicUiState.LISTENING -> {
                Text(text = UiStrings.listeningLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DearTalkSecondary)
                Text(text = if (rawStt.isNotBlank()) rawStt else UiStrings.speakNowHint, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
            micUiState == MicUiState.PROCESSING_AI -> {
                DiffLabelRow(label = UiStrings.sttRaw, labelColor = Color(0xFF94A3B8), bgColor = Color(0xFF334155), text = rawStt, textColor = Color(0xFF94A3B8))
                DiffLabelRow(label = UiStrings.aiRefine, labelColor = Color(0xFF38BDF8), bgColor = Color(0xFF0C4A6E), text = UiStrings.aiRefiningContext, textColor = Color(0xFF38BDF8), isBold = true)
            }
            rawStt.isNotBlank() || refinedAi.isNotBlank() -> {
                if (rawStt.isNotBlank()) {
                    DiffLabelRow(label = UiStrings.sttRaw, labelColor = Color(0xFF94A3B8), bgColor = Color(0xFF334155), text = rawStt, textColor = Color(0xFF94A3B8))
                }
                DiffLabelRow(label = UiStrings.aiRefine, labelColor = Color(0xFF38BDF8), bgColor = Color(0xFF0C4A6E), text = refinedAi.ifBlank { rawStt }, textColor = Color.White, isBold = true, textSize = 14)
            }
            else -> {
                Text(text = UiStrings.canvasPlaceholder, fontSize = 13.sp, lineHeight = 18.sp, color = DearTalkTextDim.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun DiffLabelRow(
    label: String,
    labelColor: Color,
    bgColor: Color,
    text: String,
    textColor: Color,
    isBold: Boolean = false,
    textSize: Int = 12
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(bgColor)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = textSize.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 2
        )
    }
}

@Composable
private fun CanvasActionButtons(
    hasContent: Boolean,
    displayText: String,
    haptic: HapticFeedback,
    onApplyAiText: (String) -> Unit,
    onClearAiTextClick: () -> Unit
) {
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
            modifier = Modifier.height(34.dp).width(74.dp),
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

        // [✕ 취소]
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClearAiTextClick()
            },
            modifier = Modifier.height(30.dp).width(74.dp),
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

// ─────────────────────────────────────────────────────────────────────────────
// 3. [3열] 👔 4대 톤앤매너 및 🌐 다국어 번역 셀렉터
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ToneAndTranslationBar(
    tones: List<CustomTone>,
    haptic: HapticFeedback,
    onApplyTone: (CustomTone) -> Unit,
    onApplyAiMode: (AiModeItem) -> Unit
) {
    var isTranslationMenuExpanded by remember { mutableStateOf(false) }
    val translationTargets = CustomToneManager.DEFAULT_TRANSLATIONS
    var selectedTranslationTarget by remember { mutableStateOf(translationTargets.first()) }
    var selectedToneId by remember { mutableStateOf<String?>(null) }

    val displayTones = if (tones.isNotEmpty()) tones else CustomToneManager.DEFAULT_TONES

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 톤앤매너 칩 목록
        displayTones.forEach { tone ->
            val isSelected = selectedToneId == tone.id
            ToneChip(
                tone = tone,
                isSelected = isSelected,
                onClick = {
                    selectedToneId = tone.id
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onApplyTone(tone)
                }
            )
        }

        // 다국어 번역 드롭다운 셀렉터
        TranslationSelectorBox(
            selectedTarget = selectedTranslationTarget,
            isMenuExpanded = isTranslationMenuExpanded,
            targets = translationTargets,
            onExpandChange = { isTranslationMenuExpanded = it },
            onTargetSelect = { target ->
                selectedTranslationTarget = target
                isTranslationMenuExpanded = false
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onApplyAiMode(
                    AiModeItem(
                        id = target.id,
                        name = target.name,
                        icon = target.flag,
                        type = AiModeType.TRANSLATION,
                        translationTarget = target
                    )
                )
            },
            onDirectTranslateClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onApplyAiMode(
                    AiModeItem(
                        id = selectedTranslationTarget.id,
                        name = selectedTranslationTarget.name,
                        icon = selectedTranslationTarget.flag,
                        type = AiModeType.TRANSLATION,
                        translationTarget = selectedTranslationTarget
                    )
                )
            }
        )
    }
}

@Composable
private fun ToneChip(
    tone: CustomTone,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF4338CA) else DearTalkKey)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFF6366F1).copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
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

@Composable
private fun TranslationSelectorBox(
    selectedTarget: TranslationTarget,
    isMenuExpanded: Boolean,
    targets: List<TranslationTarget>,
    onExpandChange: (Boolean) -> Unit,
    onTargetSelect: (TranslationTarget) -> Unit,
    onDirectTranslateClick: () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF064E3B).copy(alpha = 0.45f))
                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clickable { onDirectTranslateClick() }
                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedTarget.flag, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = UiStrings.translationLabel(selectedTarget.name),
                    color = Color(0xFF6EE7B7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clickable { onExpandChange(true) }
                    .padding(start = 2.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "▾", color = Color(0xFF6EE7B7), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
        ) {
            Text(
                text = UiStrings.selectTranslationTarget,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6EE7B7),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            HorizontalDivider(color = Color(0xFF334155))

            targets.forEach { target ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(target.flag, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = target.name,
                                color = if (target.id == selectedTarget.id) Color(0xFF6EE7B7) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (target.id == selectedTarget.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = { onTargetSelect(target) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. [4열] ⚙️ 최하단 키보드 유틸리티 바
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun KeyboardUtilityBar(
    haptic: HapticFeedback,
    onDeleteClick: () -> Unit,
    onDeleteSentenceClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ⌫ 1글자 지우기
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

        // ⌫ 문장 단위 삭제
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteSentenceClick()
            },
            modifier = Modifier.height(38.dp),
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

        // Space 키
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSpaceClick()
            },
            modifier = Modifier.weight(1f).height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DearTalkKey),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            Text("Space", color = DearTalkText, fontSize = 12.sp)
        }

        // Enter 키
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
