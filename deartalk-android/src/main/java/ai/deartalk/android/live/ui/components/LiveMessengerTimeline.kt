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
    isFlipViewEnabled: Boolean = false
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
                    text = "외국인과 마주보고 서로 말해보세요.\n하단의 [말하기]와 [듣기] 버튼으로\nAI가 실시간 통역 및 스크립트를 작성합니다.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    isFlipViewEnabled = isFlipViewEnabled,
                    onReplayClick = { onReplayClick(msg) },
                    onCopyClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("DearTalk Live", "${msg.rawText}\n➔ ${msg.refinedText}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, UiStrings.textCopiedFeedback, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 실시간 스트리밍 중인 임시 버블
            if (streamingText.isNotBlank() || (isProcessing && activeSpeaker != ActiveSpeaker.NONE)) {
                item(key = "streaming_bubble") {
                    StreamingBubble(
                        speaker = activeSpeaker,
                        text = if (streamingText.isNotBlank()) streamingText else "AI 문맥 번역 및 정제 중...",
                        isProcessing = isProcessing,
                        isFlipViewEnabled = isFlipViewEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: LiveMessage,
    isFlipViewEnabled: Boolean,
    onReplayClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val isMe = message.sender == LiveSender.ME
    val shouldFlip = !isMe && isFlipViewEnabled
    val timeStr = remember(message.createdAt) {
        val sdf = SimpleDateFormat("a h:mm", Locale.getDefault())
        sdf.format(Date(message.createdAt))
    }

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
            Text(
                text = if (isMe) "🙋 나 (${message.sourceLang} ➔ ${message.targetLang})" else "👤 상대방 (${message.sourceLang} ➔ ${message.targetLang})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMe) DearTalkPrimary else Color(0xFF818CF8)
            )
            if (message.tone != null && isMe) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${message.tone}",
                    fontSize = 10.sp,
                    color = Color.Gray
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
                    width = 1.dp,
                    color = if (isMe) DearTalkPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                // 1. 원문 (STT)
                Text(
                    text = message.rawText,
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. AI 정제 / 번역문
                Text(
                    text = message.refinedText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DearTalkText,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

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
            text = if (isMe) "🙋 나 (입력 중...)" else "👤 상대방 (청취 중...)",
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
                .padding(12.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isProcessing) DearTalkPrimary else DearTalkText
            )
        }
    }
}
