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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.KoreanKeyboardType
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*

enum class KeyboardLayoutType {
    HANGUL,
    ENGLISH,
    SYMBOLS
}

@Composable
fun StandardKeyboardView(
    koreanKeyboardType: KoreanKeyboardType = KoreanKeyboardType.DUBEOLSIK,
    onKoreanKeyboardTypeChange: (KoreanKeyboardType) -> Unit = {},
    clipboardText: String? = null,
    onPasteClick: (String) -> Unit = {},
    onDismissClipboardClick: () -> Unit = {},
    onCharClick: (Char) -> Unit,
    onCheonjiinConsonantClick: (Char) -> Unit = {},
    onCheonjiinVowelClick: (Char) -> Unit = {},
    onDeleteClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSwitchToAiModeClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isKorean = UiStrings.isKo
    val initialLayout = if (isKorean) KeyboardLayoutType.HANGUL else KeyboardLayoutType.ENGLISH
    var layoutType by remember { mutableStateOf(initialLayout) }
    var currentKoreanType by remember(koreanKeyboardType) { mutableStateOf(koreanKeyboardType) }
    var isShiftActive by remember { mutableStateOf(false) }

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
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─────────────────────────────────────────────────────────────
            // [상단 툴바 1열]: 모드 표시 / [두벌식 | 천지인] 퀵 토글 / [✨ AI 모드로 복귀]
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = UiStrings.standardKeyboardMode,
                        color = DearTalkTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // 한글 레이아웃일 때: [두벌식 | 천지인] 퀵 토글 칩 (방안 A)
                    if (layoutType == KeyboardLayoutType.HANGUL) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DearTalkKeyActive)
                                .padding(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (currentKoreanType == KoreanKeyboardType.DUBEOLSIK) DearTalkPrimary else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentKoreanType = KoreanKeyboardType.DUBEOLSIK
                                        onKoreanKeyboardTypeChange(KoreanKeyboardType.DUBEOLSIK)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "두벌식",
                                    fontSize = 10.sp,
                                    fontWeight = if (currentKoreanType == KoreanKeyboardType.DUBEOLSIK) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentKoreanType == KoreanKeyboardType.DUBEOLSIK) Color.White else DearTalkTextDim
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (currentKoreanType == KoreanKeyboardType.CHEONJIIN) DearTalkPrimary else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentKoreanType = KoreanKeyboardType.CHEONJIIN
                                        onKoreanKeyboardTypeChange(KoreanKeyboardType.CHEONJIIN)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "천지인",
                                    fontSize = 10.sp,
                                    fontWeight = if (currentKoreanType == KoreanKeyboardType.CHEONJIIN) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentKoreanType == KoreanKeyboardType.CHEONJIIN) Color.White else DearTalkTextDim
                                )
                            }
                        }
                    }
                }

                // 우측: AI 음성 모드로 즉시 복귀 버튼
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchToAiModeClick()
                    },
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(UiStrings.aiVoiceMode, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ─────────────────────────────────────────────────────────────
            // [상단 툴바 2열]: 📋 클립보드 원터치 붙여넣기 제안 스트립 (클립보드에 내용이 있을 시 노출)
            // ─────────────────────────────────────────────────────────────
            if (!clipboardText.isNullOrBlank()) {
                val cleanClip = clipboardText.replace("\n", " ").trim()
                val displayClip = if (cleanClip.length > 20) cleanClip.take(20) + "..." else cleanClip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPasteClick(clipboardText)
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "붙여넣기",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "붙여넣기: \"$displayClip\"",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 수동 닫기 (×) 버튼
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DearTalkKeyActive)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismissClipboardClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "클립보드 닫기",
                            tint = DearTalkTextDim,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // ─────────────────────────────────────────────────────────────
            // [자판 본체]: 천지인 / 두벌식 / 영문 / 기호
            // ─────────────────────────────────────────────────────────────
            when (layoutType) {
                KeyboardLayoutType.HANGUL -> {
                    if (currentKoreanType == KoreanKeyboardType.CHEONJIIN) {
                        CheonjiinKeyboardLayout(
                            onConsonantClick = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCheonjiinConsonantClick(key)
                            },
                            onVowelClick = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCheonjiinVowelClick(key)
                            },
                            onDeleteClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteClick()
                            },
                            onEnterClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnterClick()
                            },
                            onSpaceClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSpaceClick()
                            },
                            onSymbolsClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                layoutType = KeyboardLayoutType.SYMBOLS
                            },
                            onToggleLangClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                layoutType = KeyboardLayoutType.ENGLISH
                            },
                            onSpecialCharClick = { char ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCharClick(char)
                            }
                        )
                    } else {
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

            val isCheonjiin = (layoutType == KeyboardLayoutType.HANGUL && currentKoreanType == KoreanKeyboardType.CHEONJIIN)
            if (!isCheonjiin) {
                Spacer(modifier = Modifier.height(4.dp))

                // ─────────────────────────────────────────────────────────────
                // [최하단 공통 행]: 기호(!#1), 한/영, 쉼표(,), Space, 마침표(.), Enter (두벌식/영문/기호용)
                // ─────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // !#1 기호/숫자 전환
                    KeyBox(
                        text = if (layoutType == KeyboardLayoutType.SYMBOLS) (if (isKorean) "한글" else "ABC") else "!#1",
                        modifier = Modifier.weight(1.1f),
                        bgColor = DearTalkKeyActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            layoutType = if (layoutType == KeyboardLayoutType.SYMBOLS) {
                                if (isKorean) KeyboardLayoutType.HANGUL else KeyboardLayoutType.ENGLISH
                            } else {
                                KeyboardLayoutType.SYMBOLS
                            }
                        }
                    )

                    // 한/영 전환
                    KeyBox(
                        text = if (isKorean) {
                            if (layoutType == KeyboardLayoutType.HANGUL) UiStrings.korEngToggle else "ENG"
                        } else {
                            if (layoutType == KeyboardLayoutType.ENGLISH) "KOR" else "ENG"
                        },
                        modifier = Modifier.weight(1.1f),
                        bgColor = DearTalkKeyActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            layoutType = if (layoutType == KeyboardLayoutType.HANGUL) KeyboardLayoutType.ENGLISH else KeyboardLayoutType.HANGUL
                        }
                    )

                    // 쉼표 (,) 키
                    KeyBox(
                        text = ",",
                        modifier = Modifier.weight(0.9f),
                        bgColor = DearTalkKeyActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharClick(',')
                        }
                    )

                    // Space 키
                    KeyBox(
                        text = "Space",
                        modifier = Modifier.weight(2.6f),
                        bgColor = DearTalkKey,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSpaceClick()
                        }
                    )

                    // 마침표 (.) 키
                    KeyBox(
                        text = ".",
                        modifier = Modifier.weight(0.9f),
                        bgColor = DearTalkKeyActive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCharClick('.')
                        }
                    )

                    // Enter 키
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
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
}

/**
 * 천지인(Cheonjiin) 4행 x 4열 삼성 순정 표준 레이아웃
 * - 좌측 3개 열: 글쓰기 자판 (1~9, 0 숫자 각인 및 천지인 자모)
 * - 우측 4번째 열: 백스페이스(⌫), 엔터(↵), 문장부호(.,?!), 마침표(.)
 */
@Composable
private fun CheonjiinKeyboardLayout(
    onConsonantClick: (Char) -> Unit,
    onVowelClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSymbolsClick: () -> Unit,
    onToggleLangClick: () -> Unit,
    onSpecialCharClick: (Char) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // ─────────────────────────────────────────────────────────────
        // Row 1: [ ㅣ (1) ] [ ㆍ (2) ] [ ㅡ (3) ] | [ ⌫ Backspace ]
        // ─────────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CheonjiinKey(mainText = "ㅣ", subText = "1", modifier = Modifier.weight(1f), onClick = { onVowelClick('ㅣ') })
            CheonjiinKey(mainText = "ㆍ", subText = "2", modifier = Modifier.weight(1f), onClick = { onVowelClick('ㆍ') })
            CheonjiinKey(mainText = "ㅡ", subText = "3", modifier = Modifier.weight(1f), onClick = { onVowelClick('ㅡ') })
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DearTalkKeyActive)
                    .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "삭제", tint = DearTalkText, modifier = Modifier.size(20.dp))
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Row 2: [ ㄱ ㅋ (4) ] [ ㄴ ㄹ (5) ] [ ㄷ ㅌ (6) ] | [ ↵ Enter ]
        // ─────────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CheonjiinKey(mainText = "ㄱ ㅋ", subText = "4", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㄱ') })
            CheonjiinKey(mainText = "ㄴ ㄹ", subText = "5", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㄴ') })
            CheonjiinKey(mainText = "ㄷ ㅌ", subText = "6", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㄷ') })
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DearTalkSecondary)
                    .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                    .clickable { onEnterClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Row 3: [ ㅂ ㅍ (7) ] [ ㅅ ㅎ (8) ] [ ㅈ ㅊ (9) ] | [ .,?! ]
        // ─────────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CheonjiinKey(mainText = "ㅂ ㅍ", subText = "7", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㅂ') })
            CheonjiinKey(mainText = "ㅅ ㅎ", subText = "8", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㅅ') })
            CheonjiinKey(mainText = "ㅈ ㅊ", subText = "9", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㅈ') })
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DearTalkKeyActive)
                    .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                    .clickable { onSymbolsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = ".,?!", color = DearTalkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Row 4: [ !#1 | 한/영 ] [ ㅇ ㅁ (0) ] [ ␣ Space ] | [ . ]
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Col 1: [!#1] + [한/영]
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DearTalkKeyActive)
                        .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                        .clickable { onSymbolsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "!#1", color = DearTalkText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DearTalkKeyActive)
                        .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                        .clickable { onToggleLangClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = UiStrings.korEngToggle, color = DearTalkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Col 2: [ ㅇ ㅁ (0) ]
            CheonjiinKey(mainText = "ㅇ ㅁ", subText = "0", modifier = Modifier.weight(1f), onClick = { onConsonantClick('ㅇ') })

            // Col 3: [ ␣ ] Space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DearTalkKey)
                    .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                    .clickable { onSpaceClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "␣", color = DearTalkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Col 4: [ . ] 마침표
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DearTalkKeyActive)
                    .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
                    .clickable { onSpecialCharClick('.') },
                contentAlignment = Alignment.Center
            ) {
                Text(text = ".", color = DearTalkText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CheonjiinKey(
    mainText: String,
    subText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DearTalkKey)
            .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!subText.isNullOrBlank()) {
            Text(
                text = subText,
                color = DearTalkTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 5.dp)
            )
        }
        Text(
            text = mainText,
            color = DearTalkText,
            fontSize = if (mainText.length > 2) 16.sp else 18.sp,
            fontWeight = FontWeight.SemiBold
        )
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
                textColor = DearTalkText,
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
    val row1 = if (isShift) "QWERTYUIOP" else "qwertyuiop"
    val row2 = if (isShift) "ASDFGHJKL" else "asdfghjkl"
    val row3 = if (isShift) "ZXCVBNM" else "zxcvbnm"

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
        // Row 3 (Shift + 알파벳 + Delete)
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
                textColor = DearTalkText,
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
    var symbolPage by remember { mutableStateOf(1) }

    val row1 = "1234567890"
    val row2 = if (symbolPage == 1) "@#$%&-+()/" else "[]{}₩€£¥^°"
    val row3 = if (symbolPage == 1) "*\"':;!?~\\" else "|`·…«»§±¡"
    val row4 = if (symbolPage == 1) listOf(',', '.', '_', '=', '<', '>') else listOf('¿', '÷', '×', '≠', '≤', '≥')

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Row 1: 숫자행
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row1.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        // Row 2: 주요 기호
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            row2.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
        }
        // Row 3: 문장 부호 + Delete
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            row3.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
            KeyBox(
                text = "⌫",
                modifier = Modifier.weight(1.3f),
                bgColor = DearTalkKeyActive,
                textColor = DearTalkText,
                onClick = onDeleteClick
            )
        }
        // Row 4: 페이지 전환 (1/2, 2/2) + 추가 기호
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            KeyBox(
                text = if (symbolPage == 1) "1/2" else "2/2",
                modifier = Modifier.weight(1.3f),
                bgColor = DearTalkPrimary,
                textColor = Color.White,
                onClick = { symbolPage = if (symbolPage == 1) 2 else 1 }
            )
            row4.forEach { char ->
                KeyBox(text = char.toString(), modifier = Modifier.weight(1f), onClick = { onKeyClick(char) })
            }
            Spacer(modifier = Modifier.weight(0.3f))
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
            .border(0.5.dp, DearTalkBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (text.length > 2) 11.sp else 16.sp,
            fontWeight = if (text.length > 2) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
