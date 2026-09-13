package ai.deartalk.android.live.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkBackground
import ai.deartalk.android.ime.ui.theme.DearTalkPrimary
import ai.deartalk.android.ime.ui.theme.DearTalkSurface
import ai.deartalk.android.ime.ui.theme.DearTalkText
import ai.deartalk.android.live.ActiveSpeaker
import ai.deartalk.android.live.data.LiveUiHelper
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.live.data.getLabel

/**
 * 🎯 실시간 발화 의도(Speech Pragmatics / 화행) 가로 스크롤 칩 바
 */
@Composable
fun SpeechIntentChipsBar(
    currentIntent: SpeechIntent,
    langCode: String? = null,
    onIntentSelected: (SpeechIntent) -> Unit,
    modifier: Modifier = Modifier,
    detectedIntent: SpeechIntent? = null
) {
    val intents = listOf(
        SpeechIntent.QUESTION,
        SpeechIntent.STATEMENT,
        SpeechIntent.REQUEST,
        SpeechIntent.CONFIRM
    )

    val effectiveActiveIntent = if (currentIntent != SpeechIntent.AUTO) {
        currentIntent
    } else {
        detectedIntent ?: SpeechIntent.STATEMENT
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🤖 맨 좌측: AI 지능형 의도 감지 캡슐 (AUTO 상태 표시 & 탭 시 스마트 모드로 리셋)
        val isAutoSelected = currentIntent == SpeechIntent.AUTO
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isAutoSelected) Color(0xFF0C4A6E).copy(alpha = 0.7f) else DearTalkSurface
                )
                .border(
                    width = if (isAutoSelected) 1.5.dp else 1.dp,
                    color = if (isAutoSelected) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color(0xFF6366F1).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable {
                    onIntentSelected(SpeechIntent.AUTO)
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

        intents.forEach { intent ->
            val isSelected = effectiveActiveIntent == intent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF4338CA) else DearTalkSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFF6366F1).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { 
                        if (currentIntent == intent) {
                            onIntentSelected(SpeechIntent.AUTO)
                        } else {
                            onIntentSelected(intent)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = intent.getLabel(langCode),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else DearTalkText,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 🔄 1:1 대면 대칭 모드(180° Face-to-Face) 전용 마이크 & 발화 의도 바 (슬림 & 컴팩트)
 * - 상단(상대방 180° 회전) 및 하단(내 쪽 0°)에 각각 배치되며, 대화 타임라인 영역 확보를 위해 높이를 대폭 슬림화(48dp)
 */
@Composable
fun SymmetricActionMicBar(
    speakerType: ActiveSpeaker,
    langCode: String,
    isActive: Boolean,
    isProcessing: Boolean,
    rmsDb: Float,
    currentIntent: SpeechIntent,
    onIntentSelected: (SpeechIntent) -> Unit,
    onActionClick: () -> Unit,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    languageLabel: String? = null,
    onLanguageSelect: ((String) -> Unit)? = null,
    supportedOfficialLangs: List<Pair<String, String>> = emptyList(),
    supportedCrossLangs: List<Pair<String, String>> = emptyList(),
    detectedIntent: SpeechIntent? = null
) {
    val title = when {
        isProcessing -> LiveUiHelper.getProcessingTitle(langCode)
        isActive -> LiveUiHelper.getFinishTitle(langCode)
        else -> LiveUiHelper.getSpeakTitle(langCode)
    }

    val subtitle = when {
        isProcessing -> LiveUiHelper.getProcessingSubtitle(langCode)
        isActive -> LiveUiHelper.getFinishSubtitle(langCode)
        else -> LiveUiHelper.getSpeakSubtitle(langCode)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DearTalkBackground,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 발화 의도(화행) 칩 (컴팩트)
            SpeechIntentChipsBar(
                currentIntent = currentIntent,
                langCode = langCode,
                onIntentSelected = onIntentSelected,
                detectedIntent = detectedIntent
            )

            Spacer(modifier = Modifier.height(3.dp))

            // 2. 가로 1열 슬림 대칭 액션 버튼 (48dp) + 언어 캡슐 버튼 (내 쪽에 태극기, 상대 쪽에 상대 국기 직관적 노출)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SymmetricActionButton(
                    modifier = Modifier.weight(1f),
                    title = title,
                    subtitle = subtitle,
                    isActive = isActive,
                    isProcessing = isProcessing,
                    rmsDb = if (isActive) rmsDb else 0f,
                    activeColor = accentColor,
                    idleColor = DearTalkSurface,
                    icon = icon,
                    onClick = onActionClick
                )

                // 🌐 내/상대 전용 언어 캡슐 버튼 (180도 대면 시 내 영역과 상대 영역에 각자 국기/언어를 완벽히 분리 배치)
                if (languageLabel != null && onLanguageSelect != null) {
                    var showLangMenu by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DearTalkSurface)
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { showLangMenu = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = languageLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DearTalkText,
                                maxLines = 1
                            )
                            Text(
                                text = "▾",
                                fontSize = 11.sp,
                                color = accentColor.copy(alpha = 0.8f)
                            )
                        }

                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false }
                        ) {
                            Text(
                                text = "✨ ${UiStrings.officialLangSection}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            supportedOfficialLangs.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    },
                                    onClick = {
                                        onLanguageSelect(code)
                                        showLangMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "🌐 ${UiStrings.crossLangSection}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            supportedCrossLangs.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = label, fontSize = 13.sp)
                                    },
                                    onClick = {
                                        onLanguageSelect(code)
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🔲 1:1 대면 대칭 전용 슬림 풀와이드 액션 버튼 (가로 1열 정렬로 48dp 내 완벽 피팅)
 */
@Composable
private fun SymmetricActionButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isActive: Boolean,
    isProcessing: Boolean,
    rmsDb: Float,
    activeColor: Color,
    idleColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dbScale = if (isActive) (1.0f + (rmsDb / 60f).coerceIn(0f, 0.2f)) else 1.0f
    val combinedScale = pulseScale * dbScale

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(if (isActive) combinedScale else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        colors = listOf(activeColor, activeColor.copy(alpha = 0.8f))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(idleColor, idleColor.copy(alpha = 0.9f))
                    )
                }
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !isProcessing) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White.copy(alpha = 0.25f) else DearTalkBackground),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) Color.White else DearTalkText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else DearTalkText,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "• $subtitle",
                fontSize = 11.sp,
                color = if (isActive) Color.White.copy(alpha = 0.85f) else Color.Gray,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 🎙️ 하단 듀얼 대형 마이크 & 실시간 발화 의도(화행) 칩 바 (핸드헬드 듀얼 모드 - 슬림 54dp)
 */
@Composable
fun DualActionMicBar(
    activeSpeaker: ActiveSpeaker,
    processingSpeaker: ActiveSpeaker = ActiveSpeaker.NONE,
    isProcessing: Boolean,
    rmsDb: Float,
    currentIntent: SpeechIntent = SpeechIntent.AUTO,
    onIntentSelected: (SpeechIntent) -> Unit,
    onSpeakMeClick: () -> Unit,
    onListenPartnerClick: () -> Unit,
    modifier: Modifier = Modifier,
    detectedIntent: SpeechIntent? = null,
    langCode: String = "KO"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DearTalkBackground,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 실시간 대화 발화 의도 선택 칩 바 (컴팩트)
            SpeechIntentChipsBar(
                currentIntent = currentIntent,
                langCode = langCode,
                onIntentSelected = onIntentSelected,
                detectedIntent = detectedIntent
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 발화자별 처리 중 상태 계산
            val isMeProcessing = isProcessing && (activeSpeaker == ActiveSpeaker.ME || processingSpeaker == ActiveSpeaker.ME)
            val isPartnerProcessing = isProcessing && (activeSpeaker == ActiveSpeaker.PARTNER || processingSpeaker == ActiveSpeaker.PARTNER)

            val meTitle = when {
                isMeProcessing -> UiStrings.liveProcessingTitle
                activeSpeaker == ActiveSpeaker.ME -> UiStrings.liveFinishRecording
                else -> UiStrings.liveSpeakMe
            }
            val meSub = when {
                isMeProcessing -> UiStrings.liveProcessingSub
                activeSpeaker == ActiveSpeaker.ME -> UiStrings.liveFinishRecordingSub
                else -> UiStrings.liveSpeakMeSub
            }

            val partnerTitle = when {
                isPartnerProcessing -> UiStrings.liveProcessingTitle
                activeSpeaker == ActiveSpeaker.PARTNER -> UiStrings.liveFinishRecording
                else -> UiStrings.liveListenPartner
            }
            val partnerSub = when {
                isPartnerProcessing -> UiStrings.liveProcessingSub
                activeSpeaker == ActiveSpeaker.PARTNER -> UiStrings.liveFinishRecordingSub
                else -> UiStrings.liveListenPartnerSub
            }

            // 2. 나란히 배치된 슬림 듀얼 액션 버튼 (54dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 🗣️ [내가 말하기] 버튼
                BigActionButton(
                    modifier = Modifier.weight(1f),
                    title = meTitle,
                    subtitle = meSub,
                    isActive = activeSpeaker == ActiveSpeaker.ME,
                    isProcessing = isMeProcessing,
                    rmsDb = if (activeSpeaker == ActiveSpeaker.ME) rmsDb else 0f,
                    activeColor = Color(0xFF10B981), // Emerald Green
                    idleColor = DearTalkSurface,
                    icon = if (activeSpeaker == ActiveSpeaker.ME) Icons.Default.Stop else Icons.Default.Mic,
                    height = 54.dp,
                    onClick = onSpeakMeClick
                )

                // 👂 [상대방 듣기] 버튼
                BigActionButton(
                    modifier = Modifier.weight(1f),
                    title = partnerTitle,
                    subtitle = partnerSub,
                    isActive = activeSpeaker == ActiveSpeaker.PARTNER,
                    isProcessing = isPartnerProcessing,
                    rmsDb = if (activeSpeaker == ActiveSpeaker.PARTNER) rmsDb else 0f,
                    activeColor = Color(0xFF6366F1), // Indigo Blue
                    idleColor = DearTalkSurface,
                    icon = if (activeSpeaker == ActiveSpeaker.PARTNER) Icons.Default.Stop else Icons.Default.Hearing,
                    height = 54.dp,
                    onClick = onListenPartnerClick
                )
            }
        }
    }
}

@Composable
private fun BigActionButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isActive: Boolean,
    isProcessing: Boolean,
    rmsDb: Float,
    activeColor: Color,
    idleColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    height: androidx.compose.ui.unit.Dp = 50.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dbScale = if (isActive) (1.0f + (rmsDb / 60f).coerceIn(0f, 0.2f)) else 1.0f
    val combinedScale = pulseScale * dbScale

    Box(
        modifier = modifier
            .height(height)
            .scale(if (isActive) combinedScale else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        colors = listOf(activeColor, activeColor.copy(alpha = 0.8f))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(idleColor, idleColor.copy(alpha = 0.9f))
                    )
                }
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !isProcessing) { onClick() }
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White.copy(alpha = 0.25f) else DearTalkBackground),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) Color.White else DearTalkText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else DearTalkText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = if (isActive) Color.White.copy(alpha = 0.85f) else Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
