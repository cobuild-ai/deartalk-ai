package ai.deartalk.android.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*

enum class KeyboardLayoutType {
    HANGUL,
    ENGLISH,
    SYMBOLS
}

@Composable
fun StandardKeyboardView(
    onCharClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSwitchToAiModeClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var layoutType by remember { mutableStateOf(KeyboardLayoutType.HANGUL) }
    var isShiftActive by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = DearTalkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 툴바: 일반 키보드 상태 & [✨ AI 모드로 복귀] 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(UiStrings.standardKeyboardMode, color = DearTalkTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                // 우측: AI 음성 모드로 즉시 복귀 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchToAiModeClick()
                    },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(UiStrings.aiVoiceMode, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 키보드 자판 레이아웃
            when (layoutType) {
                KeyboardLayoutType.HANGUL -> {
                    HangulKeyboardLayout(
                        isShift = isShiftActive,
                        onKeyClick = { char ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharClick(char)
                            if (isShiftActive) isShiftActive = false
                        },
                        onShiftToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isShiftActive = !isShiftActive
                        },
                        onDeleteClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeleteClick()
                        }
                    )
                }
                KeyboardLayoutType.ENGLISH -> {
                    EnglishKeyboardLayout(
                        isShift = isShiftActive,
                        onKeyClick = { char ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharClick(char)
                            if (isShiftActive) isShiftActive = false
                        },
                        onShiftToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isShiftActive = !isShiftActive
                        },
                        onDeleteClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeleteClick()
                        }
                    )
                }
                KeyboardLayoutType.SYMBOLS -> {
                    SymbolKeyboardLayout(
                        onKeyClick = { char ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharClick(char)
                        },
                        onDeleteClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeleteClick()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 최하단 공통 액션 행 (기호전환, 한영전환, 스페이스, 엔터)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // !#1 기호/숫자 전환
                KeyBox(
                    text = if (layoutType == KeyboardLayoutType.SYMBOLS) "가/A" else "!#1",
                    modifier = Modifier.weight(1.2f),
                    bgColor = DearTalkKeyActive,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        layoutType = if (layoutType == KeyboardLayoutType.SYMBOLS) KeyboardLayoutType.HANGUL else KeyboardLayoutType.SYMBOLS
                    }
                )

                // 한/영 전환
                KeyBox(
                    text = if (layoutType == KeyboardLayoutType.HANGUL) UiStrings.korEngToggle else "ENG",
                    modifier = Modifier.weight(1.2f),
                    bgColor = DearTalkKeyActive,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        layoutType = if (layoutType == KeyboardLayoutType.HANGUL) KeyboardLayoutType.ENGLISH else KeyboardLayoutType.HANGUL
                    }
                )

                // Space 키
                KeyBox(
                    text = "Space",
                    modifier = Modifier.weight(3.5f),
                    bgColor = DearTalkKey,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpaceClick()
                    }
                )

                // Enter 키
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DearTalkSecondary)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEnterClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HangulKeyboardLayout(
    isShift: Boolean,
    onKeyClick: (Char) -> Unit,
    onShiftToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val row1 = if (isShift) listOf('ㅃ', 'ㅉ', 'ㄸ', 'ㄲ', 'ㅆ', 'ㅛ', 'ㅕ', 'ㅑ', 'ㅒ', 'ㅖ')
               else listOf('ㅂ', 'ㅈ', 'ㄷ', 'ㄱ', 'ㅅ', 'ㅛ', 'ㅕ', 'ㅑ', 'ㅐ', 'ㅔ')
    val row2 = listOf('ㅁ', 'ㄴ', 'ㅇ', 'ㄹ', 'ㅎ', 'ㅗ', 'ㅓ', 'ㅏ', 'ㅣ')
    val row3 = listOf('ㅋ', 'ㅌ', 'ㅊ', 'ㅍ', 'ㅠ', 'ㅜ', 'ㅡ')

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row1.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        // Row 2
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row2.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        // Row 3 (Shift + 자음 + Delete)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            KeyBox(
                text = "⇧",
                modifier = Modifier.weight(1.3f),
                bgColor = if (isShift) DearTalkPrimary else DearTalkKeyActive,
                textColor = if (isShift) Color.White else DearTalkText,
                onClick = onShiftToggle
            )
            row3.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
            KeyBox(
                text = "⌫",
                modifier = Modifier.weight(1.3f),
                bgColor = DearTalkKeyActive,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun EnglishKeyboardLayout(
    isShift: Boolean,
    onKeyClick: (Char) -> Unit,
    onShiftToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val row1 = if (isShift) "QWERTYUIOP".toList() else "qwertyuiop".toList()
    val row2 = if (isShift) "ASDFGHJKL".toList() else "asdfghjkl".toList()
    val row3 = if (isShift) "ZXCVBNM".toList() else "zxcvbnm".toList()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row1.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row2.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            KeyBox(
                text = "⇧",
                modifier = Modifier.weight(1.3f),
                bgColor = if (isShift) DearTalkPrimary else DearTalkKeyActive,
                textColor = if (isShift) Color.White else DearTalkText,
                onClick = onShiftToggle
            )
            row3.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
            KeyBox(
                text = "⌫",
                modifier = Modifier.weight(1.3f),
                bgColor = DearTalkKeyActive,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun SymbolKeyboardLayout(
    onKeyClick: (Char) -> Unit,
    onDeleteClick: () -> Unit
) {
    val row1 = "1234567890".toList()
    val row2 = listOf('@', '#', '$', '%', '&', '*', '-', '+', '=', '/')
    val row3 = listOf('!', '?', '"', '\'', ':', ';', ',', '.')

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row1.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row2.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.weight(0.5f))
            row3.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
            KeyBox(
                text = "⌫",
                modifier = Modifier.weight(1.3f),
                bgColor = DearTalkKeyActive,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun KeyBox(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = DearTalkKey,
    textColor: Color = DearTalkText,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
