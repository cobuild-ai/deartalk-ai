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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.launch

/**
 * 🪞 180° 대면 모드 전용 풀스크린 50:50 대칭 스테이지 (Full-Screen Symmetrical Stage)
 *
 * [UX 핵심 설계 원칙]:
 * 1. 50:50 풀스크린 균등 분할 & 가로 스와이프(Horizontal Swipe) 네비게이션:
 *    - 전체 화면을 좌우로 스와이프하여 과거 대화(과거)와 최신 대화(미래)를 물리적 카드 넘기듯 매끄럽게 탐색
 *    - 상단 50% (180° 회전): 맞은편 외국인 시야에서 완벽한 정방향으로 대형 24sp 헤드라인 폰트로 표시
 *    - 중앙 디바이더: 턴 인디케이터 (Turn 3 / 5) 및 이전/다음 턴 탐색 컨트롤
 *    - 하단 50% (0° 정방향): 내 시야에서 완벽한 정방향으로 대형 24sp 헤드라인 폰트로 표시
 * 2. 스포트라이트 반전 (Inverted Spotlight):
 *    - 발화 후 번역문을 읽어야 하는 청취자 쪽 절반 화면이 밝은 화이트/크림 반전 배경(#F8FAFC)으로 강조되어
 *      테이블 너머 1m 거리에서도 고개를 숙이지 않고 0.1초 만에 메시지를 직관적으로 즉시 인지!
 * 3. 듀얼 인터랙션:
 *    - 화면 어디서나 자연스러운 '가로 스와이프 제스처' + 정밀 조작을 위한 '중앙 턴 캡슐 버튼' 동시 지원
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenSymmetricStage(
    messages: List<LiveMessage>,
    streamingText: String,
    activeSpeaker: ActiveSpeaker,
    processingSpeaker: ActiveSpeaker,
    isProcessing: Boolean,
    onReplayClick: (LiveMessage) -> Unit,
    modifier: Modifier = Modifier,
    rephrasingMessageId: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isStreaming = streamingText.isNotBlank() || (isProcessing && (activeSpeaker != ActiveSpeaker.NONE || processingSpeaker != ActiveSpeaker.NONE))

    val pageCount = if (messages.isEmpty()) 1 else messages.size
    val pagerState = rememberPagerState(
        initialPage = (messages.size - 1).coerceAtLeast(0),
        pageCount = { pageCount }
    )

    // 새 메시지가 추가되거나 실시간 스트리밍 시작 시 자동으로 최신 페이지로 스크롤
    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty()) {
            pagerState.animateScrollToPage(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DearTalkBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val isCurrentStreamingPage = isStreaming && (pageIndex == pageCount - 1)
            val currentMessage = if (!isCurrentStreamingPage && messages.isNotEmpty()) {
                messages.getOrNull(pageIndex)
            } else null

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val isCurrentRephrasing = (currentMessage?.id == rephrasingMessageId)

                // =================================================================
                // 🔼 TOP HALF (50%): 상대방 시야 (180° 회전 대면)
                // =================================================================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    PartnerStageHalf(
                        message = currentMessage,
                        isStreaming = isCurrentStreamingPage,
                        streamingText = streamingText,
                        activeSpeaker = activeSpeaker,
                        processingSpeaker = processingSpeaker,
                        isProcessing = isProcessing,
                        isRephrasing = isCurrentRephrasing,
                        onReplayClick = { currentMessage?.let { onReplayClick(it) } },
                        onCopyClick = {
                            currentMessage?.let { msg ->
                                copyToClipboard(context, "${msg.rawText}\n➔ ${msg.refinedText}")
                            }
                        }
                    )
                }

                // =================================================================
                // ➖ CENTER DIVIDER & TURN CONTROLS (중앙 네비게이션 & 스와이프 인디케이터)
                // =================================================================
                CenterTurnBar(
                    totalCount = messages.size,
                    currentIndex = pagerState.currentPage,
                    isStreaming = isCurrentStreamingPage,
                    onPrevClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    },
                    onNextClick = {
                        if (pagerState.currentPage < messages.size - 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    onLatestClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(messages.size - 1) }
                    }
                )

                // =================================================================
                // 🔽 BOTTOM HALF (50%): 내 시야 (0° 정방향)
                // =================================================================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    UserStageHalf(
                        message = currentMessage,
                        isStreaming = isCurrentStreamingPage,
                        streamingText = streamingText,
                        activeSpeaker = activeSpeaker,
                        processingSpeaker = processingSpeaker,
                        isProcessing = isProcessing,
                        isRephrasing = isCurrentRephrasing,
                        onReplayClick = { currentMessage?.let { onReplayClick(it) } },
                        onCopyClick = {
                            currentMessage?.let { msg ->
                                copyToClipboard(context, "${msg.rawText}\n➔ ${msg.refinedText}")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 🔼 상단 50% 상대방 영역 (180° 회전)
 */
@Composable
private fun PartnerStageHalf(
    message: LiveMessage?,
    isStreaming: Boolean,
    streamingText: String,
    activeSpeaker: ActiveSpeaker,
    processingSpeaker: ActiveSpeaker,
    isProcessing: Boolean,
    isRephrasing: Boolean = false,
    onReplayClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val currentSpeaker = if (activeSpeaker != ActiveSpeaker.NONE) activeSpeaker else processingSpeaker
    val isMeSpeaking = currentSpeaker == ActiveSpeaker.ME
    val isPartnerSpeaking = currentSpeaker == ActiveSpeaker.PARTNER

    // 내가 말한 경우(isMe): 상대방이 번역문을 읽어야 하므로 밝은 반전 스포트라이트!
    val isPartnerSpotlight = message?.sender == LiveSender.ME

    // 🎨 AI 재점검 중일 때 부드러운 투명도 트랜지션 (흐리게)
    val rephraseAlpha by animateFloatAsState(
        targetValue = if (isRephrasing) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "partnerAlpha"
    )

    val containerBg = when {
        isStreaming && isMeSpeaking -> Color(0xFF1E293B).copy(alpha = 0.7f)
        isStreaming && isPartnerSpeaking -> Color(0xFF1E1B4B).copy(alpha = 0.85f)
        isPartnerSpotlight -> Color(0xFFF8FAFC) // 🌟 밝은 화이트 반전 배경
        message != null -> DearTalkSurface.copy(alpha = 0.75f)
        else -> DearTalkSurface.copy(alpha = 0.35f)
    }

    val containerBorderColor = when {
        isRephrasing -> DearTalkPrimary
        isStreaming && isPartnerSpeaking -> Color(0xFF818CF8)
        isStreaming && isMeSpeaking -> DearTalkPrimary.copy(alpha = 0.6f)
        isPartnerSpotlight -> Color(0xFF059669)
        message != null -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(if (isPartnerSpotlight || isStreaming || isRephrasing) 2.dp else 1.dp, containerBorderColor, RoundedCornerShape(20.dp))
            .graphicsLayer { rotationZ = 180f }
            .padding(14.dp)
    ) {
        // [회전 시 좌표 매핑]:
        // Column 내의 첫 항목(local top, y=0)은 회전 후 상대방 시야에서 하단(divider 쪽)으로 보입니다.
        // Column 내의 마지막 항목(local bottom, y=H)은 회전 후 상대방 시야에서 상단(폰 상단 쪽)으로 보입니다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 상대방 시야 기준 하단 (divider 부근): 보조 원문 텍스트
            if (message != null && !isStreaming) {
                Text(
                    text = if (message.sender == LiveSender.ME) "Original: ${message.rawText}" else "➔ ${message.refinedText}",
                    fontSize = 12.5.sp,
                    fontStyle = if (isRephrasing) FontStyle.Italic else FontStyle.Normal,
                    color = (if (isPartnerSpotlight) Color(0xFF64748B) else Color(0xFF94A3B8)).copy(alpha = rephraseAlpha),
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            } else if (isStreaming && isMeSpeaking) {
                Text(
                    text = "Translating speech to text...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 상대방 시야 기준 중앙: 대형 22~25sp 헤드라인 텍스트
            if (isStreaming) {
                Text(
                    text = if (isPartnerSpeaking) {
                        if (streamingText.isNotBlank()) streamingText else "Listening to your voice..."
                    } else {
                        if (isProcessing) "AI is translating into your language..." else "Partner is speaking..."
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPartnerSpeaking) Color(0xFFC7D2FE) else DearTalkPrimary,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (message != null) {
                // 내가 말한 경우: 상대방 언어로 번역된 정제문 (크고 선명하게)
                // 상대방이 말한 경우: 상대방이 말한 원문
                val mainText = if (message.sender == LiveSender.ME) message.refinedText else message.rawText
                val textColor = when {
                    message.isDraft && message.sender == LiveSender.ME -> Color(0xFFFBBF24)
                    isPartnerSpotlight -> Color(0xFF0F172A)
                    else -> DearTalkText
                }
                Text(
                    text = mainText,
                    fontSize = if (isPartnerSpotlight) 24.sp else 21.sp,
                    fontWeight = if (isPartnerSpotlight) FontWeight.Bold else FontWeight.SemiBold,
                    fontStyle = if (isRephrasing || message.isDraft) FontStyle.Italic else FontStyle.Normal,
                    color = textColor.copy(alpha = rephraseAlpha),
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (message.isDraft && message.sender == LiveSender.ME) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UiStrings.liveDraftToRefineStage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }

                // 🌟 AI 재점검 진행 인디케이터
                if (isRephrasing) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DearTalkPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            color = DearTalkPrimary,
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = "✨ AI is re-examining intent...",
                            fontSize = 11.5.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = DearTalkPrimary
                        )
                    }
                }
            } else {
                // 초기 빈 상태 안내
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ready to Talk",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the mic button above to speak in your language.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 상대방 시야 기준 상단 (폰 상단 쪽): 화자 뱃지 + 오디오/복사 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPartnerSpotlight) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF059669))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "READ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isStreaming) {
                            if (isPartnerSpeaking) "👤 You (Speaking...)" else "🗣️ Partner (Speaking...)"
                        } else if (message != null) {
                            if (message.sender == LiveSender.ME) "🗣️ To Partner (${message.targetLang})" else "👤 You (${message.sourceLang})"
                        } else {
                            "👤 Partner Screen"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPartnerSpotlight) Color(0xFF0F172A) else Color(0xFF818CF8)
                    )
                }

                if (message != null && !isStreaming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onReplayClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen",
                                tint = if (isPartnerSpotlight) Color(0xFF059669) else Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopyClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (isPartnerSpotlight) Color(0xFF64748B) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🔽 하단 50% 내 영역 (0° 정방향)
 */
@Composable
private fun UserStageHalf(
    message: LiveMessage?,
    isStreaming: Boolean,
    streamingText: String,
    activeSpeaker: ActiveSpeaker,
    processingSpeaker: ActiveSpeaker,
    isProcessing: Boolean,
    isRephrasing: Boolean = false,
    onReplayClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val currentSpeaker = if (activeSpeaker != ActiveSpeaker.NONE) activeSpeaker else processingSpeaker
    val isMeSpeaking = currentSpeaker == ActiveSpeaker.ME
    val isPartnerSpeaking = currentSpeaker == ActiveSpeaker.PARTNER

    // 상대방이 말한 경우(!isMe): 내가 한국어 번역문을 읽어야 하므로 밝은 반전 스포트라이트!
    val isUserSpotlight = message?.sender == LiveSender.PARTNER

    // 🎨 AI 재점검 중일 때 부드러운 투명도 트랜지션 (흐리게)
    val rephraseAlpha by animateFloatAsState(
        targetValue = if (isRephrasing) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "userAlpha"
    )

    val containerBg = when {
        isStreaming && isMeSpeaking -> Color(0xFF064E3B).copy(alpha = 0.5f)
        isStreaming && isPartnerSpeaking -> Color(0xFF1E293B).copy(alpha = 0.7f)
        isUserSpotlight -> Color(0xFFF8FAFC) // 🌟 밝은 화이트 반전 배경
        message != null -> DearTalkSurface.copy(alpha = 0.75f)
        else -> DearTalkSurface.copy(alpha = 0.35f)
    }

    val containerBorderColor = when {
        isRephrasing -> DearTalkPrimary
        isStreaming && isMeSpeaking -> DearTalkPrimary
        isStreaming && isPartnerSpeaking -> Color(0xFF818CF8).copy(alpha = 0.6f)
        isUserSpotlight -> Color(0xFF4F46E5)
        message != null -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(if (isUserSpotlight || isStreaming || isRephrasing) 2.dp else 1.dp, containerBorderColor, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 내 시야 기준 상단 (divider 부근): 화자 뱃지 + 오디오/복사 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUserSpotlight) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (UiStrings.isKo) "읽기" else if (UiStrings.isId) "BACA" else "READ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isStreaming) {
                            if (isMeSpeaking) {
                                if (UiStrings.isKo) "🙋 나 (실시간 말씀하시는 중...)" else if (UiStrings.isId) "🙋 Saya (Sedang berbicara...)" else "🙋 Me (Speaking...)"
                            } else {
                                if (UiStrings.isKo) "👤 상대방 (말씀하시는 중...)" else if (UiStrings.isId) "👤 Mitra (Sedang berbicara...)" else "👤 Partner (Speaking...)"
                            }
                        } else if (message != null) {
                            if (message.sender == LiveSender.ME) {
                                "${if (UiStrings.isKo) "🙋 나" else if (UiStrings.isId) "🙋 Saya" else "🙋 Me"} (${message.sourceLang})"
                            } else {
                                "🗣️ ${if (UiStrings.isKo) "상대방 ➔ 나" else if (UiStrings.isId) "Mitra ➔ Saya" else "Partner ➔ Me"} (${message.targetLang})"
                            }
                        } else {
                            if (UiStrings.isKo) "🙋 내 화면" else if (UiStrings.isId) "🙋 Layar Saya" else "🙋 My Screen"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUserSpotlight) Color(0xFF0F172A) else DearTalkPrimary
                    )
                }

                if (message != null && !isStreaming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onReplayClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (UiStrings.isKo) "다시 듣기" else if (UiStrings.isId) "Putar Ulang" else "Listen Again",
                                tint = if (isUserSpotlight) Color(0xFF4F46E5) else DearTalkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopyClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = if (UiStrings.isKo) "복사" else if (UiStrings.isId) "Salin" else "Copy",
                                tint = if (isUserSpotlight) Color(0xFF64748B) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 내 시야 기준 중앙: 대형 22~25sp 헤드라인 텍스트
            if (isStreaming) {
                Text(
                    text = if (isMeSpeaking) {
                        if (streamingText.isNotBlank()) streamingText else (if (UiStrings.isKo) "목소리를 듣고 있습니다..." else if (UiStrings.isId) "Mendengarkan suara Anda..." else "Listening to your voice...")
                    } else {
                        if (isProcessing) {
                            if (UiStrings.isKo) "AI가 번역 및 정제 중입니다..." else if (UiStrings.isId) "AI sedang menerjemahkan..." else "AI is refining and translating..."
                        } else {
                            if (UiStrings.isKo) "상대방이 말씀하고 계십니다..." else if (UiStrings.isId) "Mitra sedang berbicara..." else "Partner is speaking..."
                        }
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMeSpeaking) DearTalkPrimary else Color(0xFFC7D2FE),
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (message != null) {
                // 상대방이 말한 경우: 한국어로 번역된 정제문 (내가 읽고 이해할 내용)
                // 내가 말한 경우: 내가 발화한 한국어 원문
                val mainText = if (message.sender == LiveSender.PARTNER) message.refinedText else message.rawText
                val textColor = when {
                    message.isDraft && message.sender == LiveSender.PARTNER -> Color(0xFFFBBF24)
                    isUserSpotlight -> Color(0xFF0F172A)
                    else -> DearTalkText
                }
                Text(
                    text = mainText,
                    fontSize = if (isUserSpotlight) 24.sp else 21.sp,
                    fontWeight = if (isUserSpotlight) FontWeight.Bold else FontWeight.SemiBold,
                    fontStyle = if (isRephrasing || message.isDraft) FontStyle.Italic else FontStyle.Normal,
                    color = textColor.copy(alpha = rephraseAlpha),
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (message.isDraft && message.sender == LiveSender.PARTNER) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UiStrings.liveDraftToRefineStage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }

                // 🌟 AI 재점검 진행 인디케이터
                if (isRephrasing) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DearTalkPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            color = DearTalkPrimary,
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = if (UiStrings.isKo) "✨ AI가 문장 구조와 톤을 재점검하고 있습니다..." else if (UiStrings.isId) "✨ AI sedang meninjau tata bahasa dan nada..." else "✨ AI is reviewing grammar and tone...",
                            fontSize = 11.5.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = DearTalkPrimary
                        )
                    }
                }
            } else {
                // 초기 빈 상태 안내
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (UiStrings.isKo) "대화 준비 완료" else if (UiStrings.isId) "Siap untuk Berbicara" else "Ready to Talk",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (UiStrings.isKo) "하단의 마이크 버튼을 누르고 편하게 말씀하세요." else if (UiStrings.isId) "Ketuk tombol mikrofon di bawah untuk berbicara." else "Tap the mic button below to start speaking.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 내 시야 기준 하단 (마이크 바 부근): 보조 원문 텍스트
            if (message != null && !isStreaming) {
                val origLabel = if (UiStrings.isKo) "원문" else if (UiStrings.isId) "Asli" else "Original"
                Text(
                    text = if (message.sender == LiveSender.ME) "➔ ${message.refinedText}" else "$origLabel: ${message.rawText}",
                    fontSize = 12.5.sp,
                    fontStyle = if (isRephrasing) FontStyle.Italic else FontStyle.Normal,
                    color = (if (isUserSpotlight) Color(0xFF64748B) else Color(0xFF94A3B8)).copy(alpha = rephraseAlpha),
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            } else if (isStreaming && isPartnerSpeaking) {
                Text(
                    text = if (UiStrings.isKo) "실시간 음성 인식 중..." else if (UiStrings.isId) "Mengenali suara..." else "Listening...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

/**
 * ➖ 중앙 대칭 구분선 및 턴 네비게이션 컨트롤 (Center Turn Bar)
 */
@Composable
private fun CenterTurnBar(
    totalCount: Int,
    currentIndex: Int,
    isStreaming: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onLatestClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.12f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 중앙 플로팅 턴 캡슐
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, if (isStreaming) DearTalkPrimary else Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            if (isStreaming) {
                Text(
                    text = "✨ LIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DearTalkPrimary
                )
            } else if (totalCount > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onPrevClick,
                        enabled = currentIndex > 0,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "이전 대화",
                            tint = if (currentIndex > 0) DearTalkText else Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${currentIndex + 1} / $totalCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DearTalkText
                    )

                    IconButton(
                        onClick = onNextClick,
                        enabled = currentIndex < totalCount - 1,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "다음 대화",
                            tint = if (currentIndex < totalCount - 1) DearTalkText else Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (currentIndex < totalCount - 1) {
                        Text(
                            text = "최신",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DearTalkPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DearTalkPrimary.copy(alpha = 0.15f))
                                .clickable { onLatestClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "50 : 50 Stage",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.12f)
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("DearTalk Live", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, UiStrings.textCopiedFeedback, Toast.LENGTH_SHORT).show()
}
