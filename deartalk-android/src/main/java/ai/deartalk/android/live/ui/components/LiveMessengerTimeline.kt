package ai.deartalk.android.live.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.live.ActiveSpeaker
import ai.deartalk.android.live.data.LiveMessage
import ai.deartalk.android.live.data.LiveSender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontStyle

/** 언어 코드 → 국기 이모지 변환 (UX 단순화: "KO" → "🇰🇷") */
fun langCodeToFlag(code: String): String = when (code.uppercase()) {
    "KO" -> "🇰🇷"
    "EN" -> "🇺🇸"
    "ID" -> "🇮🇩"
    "JA" -> "🇯🇵"
    "ZH" -> "🇨🇳"
    "ZH-CN", "ZH_CN" -> "🇨🇳"
    "ZH-TW", "ZH_TW" -> "🇹🇼"
    "ES" -> "🇪🇸"
    "FR" -> "🇫🇷"
    "DE" -> "🇩🇪"
    "PT" -> "🇵🇹"
    "RU" -> "🇷🇺"
    "AR" -> "🇸🇦"
    "HI" -> "🇮🇳"
    "VI" -> "🇻🇳"
    "TH" -> "🇹🇭"
    "MS" -> "🇲🇾"
    "TR" -> "🇹🇷"
    "IT" -> "🇮🇹"
    "NL" -> "🇳🇱"
    "PL" -> "🇵🇱"
    else -> code
}

/**
 * 💬 카카오톡 / Continuum 스타일 1:1 대화 타임라인
 */
@Composable
fun LiveMessengerTimeline(
    messages: List<LiveMessage>,
    streamingText: String,
    activeSpeaker: ActiveSpeaker,
    isProcessing: Boolean,
    onReplayClick: (LiveMessage) -> Unit,
    modifier: Modifier = Modifier,
    isFlipViewEnabled: Boolean = false,
    processingSpeaker: ActiveSpeaker = ActiveSpeaker.NONE,
    rephrasingMessageId: String? = null
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // 새 메시지나 스트리밍 수신 시 맨 아래로 부드럽게 스크롤
    LaunchedEffect(messages.size, streamingText, isProcessing) {
        val totalCount = messages.size + if (streamingText.isNotBlank() || isProcessing) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    if (messages.isEmpty() && streamingText.isBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "🎙️ DearTalk Live",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DearTalkPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = UiStrings.liveSubtitle,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        val latestMessageId = messages.lastOrNull()?.id

        LazyColumn(
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isLatest = (msg.id == latestMessageId)
                val isRephrasing = (msg.id == rephrasingMessageId)
                if (isFlipViewEnabled) {
                    // 🔄 180° 대면 모드: 상하 양면 대칭 대화 카드 (나와 상대방이 각자 언어로 정방향 동시 열람)
                    // 🌟 최신 카드는 밝게 반전(Inverted Spotlight), 지난 대화는 디엠페시스(Dimmed) 처리
                    SymmetricalDualFaceBubble(
                        message = msg,
                        isLatest = isLatest,
                        isRephrasing = isRephrasing,
                        onReplayClick = { onReplayClick(msg) },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("DearTalk Live", "${msg.rawText}\n➔ ${msg.refinedText}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, UiStrings.textCopiedFeedback, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // 📱 핸드헬드 일반 모드: 단방향 카카오톡/메신저 스타일 버블
                    MessageBubble(
                        message = msg,
                        isFlipViewEnabled = false,
                        isRephrasing = isRephrasing,
                        onReplayClick = { onReplayClick(msg) },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("DearTalk Live", "${msg.rawText}\n➔ ${msg.refinedText}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, UiStrings.textCopiedFeedback, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 실시간 스트리밍 중인 임시 버블 (번역 중에도 사라지지 않고 부드럽게 유지됨)
            val currentSpeaker = if (activeSpeaker != ActiveSpeaker.NONE) activeSpeaker else processingSpeaker
            if (streamingText.isNotBlank() || (isProcessing && currentSpeaker != ActiveSpeaker.NONE)) {
                item(key = "streaming_bubble") {
                    if (isFlipViewEnabled) {
                        SymmetricalStreamingBubble(
                            speaker = currentSpeaker,
                            text = if (streamingText.isNotBlank()) streamingText else "AI 문맥 번역 및 정제 중...",
                            isProcessing = isProcessing
                        )
                    } else {
                        StreamingBubble(
                            speaker = currentSpeaker,
                            text = if (streamingText.isNotBlank()) streamingText else "AI 문맥 번역 및 정제 중...",
                            isProcessing = isProcessing,
                            isFlipViewEnabled = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: LiveMessage,
    isFlipViewEnabled: Boolean,
    isRephrasing: Boolean = false,
    onReplayClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val isMe = message.sender == LiveSender.ME
    val shouldFlip = !isMe && isFlipViewEnabled
    val timeStr = remember(message.createdAt) {
        val sdf = SimpleDateFormat("a h:mm", UiStrings.currentLocale)
        sdf.format(Date(message.createdAt))
    }

    // 🎨 AI 재점검 중일 때 부드러운 투명도 트랜지션 (흐리게)
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isRephrasing) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "bubbleAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (shouldFlip) Modifier.graphicsLayer { rotationZ = 180f }
                else Modifier
            ),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // 발화자 라벨
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // 발화자 이름 + 국기 화살표 ("나  🇰🇷→🇮🇩" 형태로 직관적 표시)
            Text(
                text = if (isMe) {
                    "🙋 ${UiStrings.liveMePrefix}  ${langCodeToFlag(message.sourceLang)}→${langCodeToFlag(message.targetLang)}"
                } else {
                    "👤 ${UiStrings.livePartnerPrefix}  ${langCodeToFlag(message.sourceLang)}→${langCodeToFlag(message.targetLang)}"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMe) DearTalkPrimary else Color(0xFF818CF8)
            )
            // AI 재점검 중 표시 (Draft 태그는 버블 안 색상으로 구분하므로 헤더 제거)
            if (isRephrasing) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (UiStrings.isKo) "• 🔄" else "• 🔄",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = DearTalkPrimary
                )
            }
        }

        // 말풍선 본체
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) Color(0xFF064E3B).copy(alpha = 0.5f) else DearTalkSurface)
                .border(
                    width = if (isRephrasing || message.isDraft) 1.5.dp else 1.dp,
                    color = if (message.isDraft) Color(0xFFF59E0B).copy(alpha = 0.6f) else if (isRephrasing) DearTalkPrimary.copy(alpha = 0.75f) else if (isMe) DearTalkPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Column {
                // 1. 원문 (STT) - 마이크 아이콘 캡슐
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.rawText,
                        fontSize = 12.sp,
                        fontStyle = if (isRephrasing) FontStyle.Italic else FontStyle.Normal,
                        color = Color.LightGray.copy(alpha = animatedAlpha),
                        lineHeight = 16.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. 2-Track 하이브리드 번역문 표시
                if (message.isDraft) {
                    // ⚡ [변환전] 초경량 모델 초안 (앰버/골드 오렌지 색상)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF78350F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = UiStrings.liveDraftTitle,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = message.refinedText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFDE68A),
                                lineHeight = 19.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(9.dp),
                            color = Color(0xFFF59E0B),
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = UiStrings.liveRefiningProgress,
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF59E0B)
                        )
                    }
                } else {
                    // ✅ AI 완성 번역문 — 색상(에메랄드/스카이블루)으로 Draft와 구분, 라벨 없이 깔끔하게
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color(0xFF065F46) else Color(0xFF0C4A6E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (isMe) Color(0xFF6EE7B7) else Color(0xFF38BDF8),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.refinedText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = if (isRephrasing) FontStyle.Italic else FontStyle.Normal,
                            color = if (isMe) Color(0xFF6EE7B7) else Color(0xFF38BDF8),
                            lineHeight = 19.5.sp
                        )
                    }
                }

                // 🌟 AI 재점검 진행 인디케이터
                if (isRephrasing) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DearTalkPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            color = DearTalkPrimary,
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = "✨ AI가 문장 구조와 톤을 재점검하고 있습니다...",
                            fontSize = 10.5.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = DearTalkPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 3. 하단 액션 (시간, 다시듣기, 복사)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onReplayClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "다시 듣기",
                                tint = if (isMe) DearTalkPrimary else Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onCopyClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "복사",
                                tint = Color.Gray,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(
    speaker: ActiveSpeaker,
    text: String,
    isProcessing: Boolean,
    isFlipViewEnabled: Boolean
) {
    val isMe = speaker == ActiveSpeaker.ME
    val shouldFlip = !isMe && isFlipViewEnabled
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (shouldFlip) Modifier.graphicsLayer { rotationZ = 180f }
                else Modifier
            ),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isMe) (if (UiStrings.isKo) "🙋 나 (입력 중...)" else if (UiStrings.isId) "🙋 Saya (Bicara...)" else "🙋 Me (Speaking...)") else (if (UiStrings.isKo) "👤 상대방 (청취 중...)" else if (UiStrings.isId) "👤 Mitra (Mendengarkan...)" else "👤 Partner (Listening...)"),
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isMe) Color(0xFF065F46).copy(alpha = 0.35f) else DearTalkSurface.copy(alpha = 0.7f))
                .border(
                    width = 1.dp,
                    color = if (isMe) DearTalkPrimary.copy(alpha = 0.5f) else Color(0xFF818CF8).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 11.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isProcessing) Color(0xFF0C4A6E) else Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isProcessing) Icons.Default.SmartToy else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isProcessing) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(10.5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isProcessing) DearTalkPrimary else DearTalkText
                )
            }
        }
    }
}

/**
 * 🔄 180° 대면 모드 전용 양면 대칭 대화 카드 (Symmetrical Dual-Face Bubble)
 *
 * - 상단 영역 (180° 회전): 상대방(맞은편) 시야에서 완벽한 정방향으로 상대방 언어와 발화 내용을 표시
 * - 중앙 분할선 (HorizontalDivider): 두 사용자 영역을 우아하고 직관적으로 구분
 * - 하단 영역 (0° 정방향): 내 시야에서 완벽한 정방향으로 내 언어(한국어)와 발화 내용을 표시
 * - 나와 상대방이 고개를 숙이거나 폰을 돌리지 않고도 각자의 모국어로 실시간 대화 흐름을 동시에 열람 가능
 */
/**
 * 🔄 180° 대면 모드 전용 양면 대칭 대화 카드 (Symmetrical Dual-Face Bubble)
 *
 * 🌟 [Spotlight Inversion & Past Dimming]:
 * - isLatest == true: 현재 읽어야 할 최신 대화 카드는 밝게 반전(Inverted Spotlight)되어
 *   테이블 건너편의 외국인과 내 시야에서 0.1초 만에 눈에 확 띄도록 극적인 고대비(High-Contrast)로 강조!
 *   - 내가 말한 경우(isMe): 외국인이 읽어야 할 상단 영역(180°)이 화이트/크림 반전 배경 + 짙은 블랙 볼드 텍스트로 폭발적 시선 집중!
 *   - 상대방이 말한 경우(!isMe): 내가 읽어야 할 하단 영역(0°)이 화이트/크림 반전 배경 + 짙은 블랙 볼드 텍스트로 즉각 가독성 확보!
 * - isLatest == false: 지난 대화는 이미 소통이 끝난 맥락이므로 투명도 38%로 어둡게 디엠페시스(Dimmed)하여 시선 분산을 완벽히 차단!
 */
@Composable
private fun SymmetricalDualFaceBubble(
    message: LiveMessage,
    isLatest: Boolean = false,
    isRephrasing: Boolean = false,
    onReplayClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val isMe = message.sender == LiveSender.ME
    val timeStr = remember(message.createdAt) {
        val sdf = SimpleDateFormat("a h:mm", UiStrings.currentLocale)
        sdf.format(Date(message.createdAt))
    }

    // 🎨 AI 재점검 중일 때 부드러운 투명도 트랜지션
    val rephraseAlpha by animateFloatAsState(
        targetValue = if (isRephrasing) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "rephraseAlpha"
    )

    // 🎨 테두리 및 카드 전체 스타일
    val cardAlpha = (if (isLatest) 1.0f else 0.38f) * rephraseAlpha
    val cardBorderWidth = if (isRephrasing) 2.5.dp else if (isLatest) 2.dp else 0.8.dp
    val cardBorderColor = if (isRephrasing) {
        DearTalkPrimary
    } else if (isLatest) {
        if (isMe) DearTalkPrimary else Color(0xFF818CF8)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val cardBgColor = if (isLatest) {
        if (isMe) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFF1E1B4B).copy(alpha = 0.55f)
    } else {
        Color(0xFF0F172A).copy(alpha = 0.45f)
    }

    // ☀️ 상단 영역(180° 상대방 시야)의 반전 여부: 내가 말했을 때 최신 카드이면 상대방이 읽을 대상이므로 반전!
    val isTopInverted = isLatest && isMe
    // ☀️ 하단 영역(0° 내 시야)의 반전 여부: 상대방이 말했을 때 최신 카드이면 내가 읽을 대상이므로 반전!
    val isBottomInverted = isLatest && !isMe

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha }
            .clip(RoundedCornerShape(18.dp))
            .background(cardBgColor)
            .border(cardBorderWidth, cardBorderColor, RoundedCornerShape(18.dp))
    ) {
        // -------------------------------------------------------------
        // 🔼 TOP HALF: 상대방 시야 (180° 대면 회전)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = 180f }
                .then(
                    if (isTopInverted) {
                        Modifier
                            .background(Color(0xFFF8FAFC)) // 눈부시게 밝은 화이트 반전 배경
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    } else {
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    }
                )
        ) {
            // [상대방 시야 하단: 보조 텍스트 및 시간 (y=0 -> 회전 후 y=H)]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMe) message.rawText else message.refinedText,
                    fontSize = if (isTopInverted) 12.sp else 11.5.sp,
                    color = if (isTopInverted) Color(0xFF64748B) else if (isLatest) Color.LightGray.copy(alpha = 0.75f) else Color(0xFF94A3B8),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = if (isTopInverted) Color(0xFF64748B) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // [상대방 시야 본문 텍스트 (가장 중요한 메시지)]
            // 내가 말한 경우: 상대방에게 전달된 번역문 (예: 영어)
            // 상대방이 말한 경우: 상대방 본인이 말한 원문 (예: 영어 STT)
            Text(
                text = if (isMe) message.refinedText else message.rawText,
                fontSize = if (isTopInverted) 17.sp else if (isLatest) 15.5.sp else 14.sp,
                fontWeight = if (isTopInverted) FontWeight.Bold else if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                fontStyle = if (isRephrasing) FontStyle.Italic else FontStyle.Normal,
                color = if (isTopInverted) Color(0xFF0F172A) else if (isLatest) DearTalkText else Color(0xFFCBD5E1),
                lineHeight = if (isTopInverted) 23.sp else 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // [상대방 시야 최상단 헤더 (y=H -> 회전 후 y=0)]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLatest) {
                        // 🟢 최신 대화 강조 뱃지
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isTopInverted) Color(0xFF059669) else DearTalkPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NOW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isMe) "🗣️ To Partner (${message.targetLang})" else "👤 Partner (${message.sourceLang})",
                        fontSize = 11.5.sp,
                        fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTopInverted) Color(0xFF0F172A) else if (isLatest) (if (isMe) DearTalkPrimary else Color(0xFF818CF8)) else Color(0xFF94A3B8)
                    )
                    if (message.tone != null && !isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${message.tone}",
                            fontSize = 10.sp,
                            color = if (isTopInverted) Color(0xFF64748B) else Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = onReplayClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = if (isTopInverted) Color(0xFF059669) else if (isMe) DearTalkPrimary else Color(0xFF818CF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // ➖ 대칭 중앙 분할선 (HorizontalDivider)
        // -------------------------------------------------------------
        HorizontalDivider(
            thickness = if (isLatest) 1.5.dp else 0.8.dp,
            color = if (isLatest) cardBorderColor.copy(alpha = 0.6f) else cardBorderColor.copy(alpha = 0.2f)
        )

        // -------------------------------------------------------------
        // 🔽 BOTTOM HALF: 내 시야 (0° 정방향)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isBottomInverted) {
                        Modifier
                            .background(Color(0xFFF8FAFC)) // 눈부시게 밝은 화이트 반전 배경
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    } else {
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    }
                )
        ) {
            // [내 시야 최상단 헤더]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLatest) {
                        // 🟢 최신 대화 강조 뱃지
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isBottomInverted) Color(0xFF4F46E5) else DearTalkPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NOW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isMe) "🙋 ${UiStrings.liveMePrefix} (${message.sourceLang})" else "🗣️ ${UiStrings.livePartnerPrefix} ➔ ${UiStrings.liveMePrefix} (${message.targetLang})",
                        fontSize = 11.5.sp,
                        fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                        color = if (isBottomInverted) Color(0xFF0F172A) else if (isLatest) (if (isMe) DearTalkPrimary else Color(0xFF818CF8)) else Color(0xFF94A3B8)
                    )
                    if (message.tone != null && isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${message.tone}",
                            fontSize = 10.sp,
                            color = if (isBottomInverted) Color(0xFF64748B) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // [내 시야 본문 텍스트]
            // 내가 말한 경우: 내가 발화한 한국어 원문
            // 상대방이 말한 경우: 한국어로 번역된 정제문 (내가 읽고 이해할 내용)
            Text(
                text = if (isMe) message.rawText else message.refinedText,
                fontSize = if (isBottomInverted) 17.sp else if (isLatest) 15.5.sp else 14.sp,
                fontWeight = if (isBottomInverted) FontWeight.Bold else if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                fontStyle = if (isRephrasing) FontStyle.Italic else FontStyle.Normal,
                color = if (isBottomInverted) Color(0xFF0F172A) else if (isLatest) DearTalkText else Color(0xFFCBD5E1),
                lineHeight = if (isBottomInverted) 23.sp else 20.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            // [보조 텍스트 (상대방 언어/원문 매핑)]
            Text(
                text = if (isMe) "➔ ${message.refinedText}" else message.rawText,
                fontSize = if (isBottomInverted) 12.sp else 11.5.sp,
                color = if (isBottomInverted) Color(0xFF64748B) else if (isLatest) Color.LightGray.copy(alpha = 0.75f) else Color(0xFF94A3B8),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // [내 시야 하단 액션 (시간, 복사, 다시듣기)]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = if (isBottomInverted) Color(0xFF64748B) else Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onReplayClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "다시 듣기",
                            tint = if (isBottomInverted) Color(0xFF4F46E5) else if (isMe) DearTalkPrimary else Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "복사",
                            tint = if (isBottomInverted) Color(0xFF64748B) else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🔄 180° 대면 모드 전용 양면 실시간 스트리밍 버블
 */
@Composable
private fun SymmetricalStreamingBubble(
    speaker: ActiveSpeaker,
    text: String,
    isProcessing: Boolean
) {
    val isMe = speaker == ActiveSpeaker.ME
    val borderColor = if (isMe) DearTalkPrimary.copy(alpha = 0.5f) else Color(0xFF818CF8).copy(alpha = 0.5f)
    val bgColor = if (isMe) Color(0xFF065F46).copy(alpha = 0.3f) else DearTalkSurface.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        // Top Half (180° Partner)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = 180f }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Appears at Partner's bottom (near divider)
            Text(
                text = if (isMe) {
                    if (isProcessing) "AI 번역 및 정제 중... / Translating..." else "듣고 실시간 번역 대기 중... / Listening..."
                } else {
                    text
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isProcessing) DearTalkPrimary else DearTalkText
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Appears at Partner's top
            Text(
                text = if (isMe) "⏳ Translating for Partner..." else "👤 You (Speaking...)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMe) DearTalkPrimary else Color(0xFF818CF8)
            )
        }

        HorizontalDivider(
            thickness = 0.8.dp,
            color = borderColor.copy(alpha = 0.3f)
        )

        // Bottom Half (0° User)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isMe) "🙋 나 (실시간 음성 인식 중...)" else "⏳ 한국어로 실시간 번역 대기 중...",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMe) DearTalkPrimary else Color(0xFF818CF8)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isMe) {
                    text
                } else {
                    if (isProcessing) {
                        if (UiStrings.isKo) "AI 한국어 정제 및 번역 중..." else if (UiStrings.isId) "AI sedang menerjemahkan..." else "AI is refining and translating..."
                    } else {
                        if (UiStrings.isKo) "상대방이 말씀하시는 중입니다..." else if (UiStrings.isId) "Mitra sedang berbicara..." else "Partner is speaking..."
                    }
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isProcessing) DearTalkPrimary else DearTalkText
            )
        }
    }
}
