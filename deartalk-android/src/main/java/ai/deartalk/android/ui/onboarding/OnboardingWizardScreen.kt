package ai.deartalk.android.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.deartalk.android.ime.ui.theme.*

/**
 * 🚀 디어톡(DearTalk) AI 음성키보드 - 3단계 온보딩 마법사 화면 (Fullscreen Onboarding Wizard)
 * 1단계: 키보드 활성화 (시스템 설정)
 * 2단계: 기본 키보드로 선택 (입력기 피커)
 * 3단계: 마이크 권한 허용 (온디바이스 음성인식)
 */
@Composable
fun OnboardingWizardScreen(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    hasMicPermission: Boolean,
    isKorean: Boolean,
    isIndonesian: Boolean,
    onEnableIme: () -> Unit,
    onSelectIme: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val allCompleted = isImeEnabled && isImeSelected && hasMicPermission

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DearTalkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ─────────────────────────────────────────────────────────────
            // 🌟 상단 브랜드 헤더 & 타이틀
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF818CF8), DearTalkPrimary, Color(0xFF312E81))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isKorean) "디어톡(DearTalk) AI 음성키보드"
                else if (isIndonesian) "Keyboard Suara AI DearTalk"
                else "DearTalk AI Voice Keyboard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DearTalkText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isKorean) "간단한 3단계로 바로 사용해 보세요"
                else if (isIndonesian) "Selesaikan 3 langkah mudah untuk mulai"
                else "Complete 3 simple steps to get started",
                fontSize = 13.sp,
                color = DearTalkTextDim,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─────────────────────────────────────────────────────────────
            // 📋 3단계 온보딩 스텝 카드 목록
            // ─────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // STEP 1: 키보드 켜기 (활성화)
                OnboardingStepCard(
                    stepNumber = "1",
                    icon = Icons.Default.Settings,
                    title = if (isKorean) "1단계: 키보드 켜기 (활성화)"
                    else if (isIndonesian) "Langkah 1: Aktifkan Keyboard"
                    else "Step 1: Enable Keyboard",
                    description = if (isImeEnabled) {
                        if (isKorean) "✔ 디어톡 키보드가 켜져 있습니다"
                        else if (isIndonesian) "✔ Keyboard DearTalk sudah aktif"
                        else "✔ DearTalk keyboard is enabled"
                    } else {
                        if (isKorean) "시스템 설정에서 디어톡 스위치를 켜주세요"
                        else if (isIndonesian) "Aktifkan sakelar DearTalk di pengaturan"
                        else "Turn on DearTalk switch in settings"
                    },
                    isDone = isImeEnabled,
                    buttonText = if (isKorean) "키보드 켜기 ➔"
                    else if (isIndonesian) "Buka Pengaturan ➔"
                    else "Enable in Settings ➔",
                    onAction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnableIme()
                    }
                )

                // STEP 2: 기본 키보드로 선택
                OnboardingStepCard(
                    stepNumber = "2",
                    icon = Icons.Default.Keyboard,
                    title = if (isKorean) "2단계: 기본 키보드로 선택"
                    else if (isIndonesian) "Langkah 2: Pilih Keyboard Utama"
                    else "Step 2: Set as Default Keyboard",
                    description = if (isImeSelected) {
                        if (isKorean) "✔ 디어톡이 기본 키보드로 설정되었습니다"
                        else if (isIndonesian) "✔ DearTalk terpilih sebagai keyboard utama"
                        else "✔ DearTalk is selected as default"
                    } else {
                        if (isKorean) "기본 입력기를 디어톡(DearTalk)으로 지정해 주세요"
                        else if (isIndonesian) "Pilih DearTalk sebagai metode input utama"
                        else "Select DearTalk as your active input method"
                    },
                    isDone = isImeSelected,
                    buttonText = if (isKorean) "디어톡 선택하기 ➔"
                    else if (isIndonesian) "Pilih DearTalk ➔"
                    else "Select DearTalk ➔",
                    onAction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectIme()
                    }
                )

                // STEP 3: 마이크 권한 허용
                OnboardingStepCard(
                    stepNumber = "3",
                    icon = Icons.Default.Mic,
                    title = if (isKorean) "3단계: 마이크 권한 허용"
                    else if (isIndonesian) "Langkah 3: Izin Mikrofon"
                    else "Step 3: Microphone Permission",
                    description = if (hasMicPermission) {
                        if (isKorean) "✔ 마이크 음성 입력 준비가 완료되었습니다"
                        else if (isIndonesian) "✔ Mikrofon siap untuk input suara"
                        else "✔ Microphone is ready for voice input"
                    } else {
                        if (isKorean) "말씀하신 음성을 온디바이스 AI로 다듬기 위해 필요해요"
                        else if (isIndonesian) "Dibutuhkan untuk memproses suara dengan AI lokal"
                        else "Required to refine spoken words via local on-device AI"
                    },
                    isDone = hasMicPermission,
                    buttonText = if (isKorean) "마이크 권한 허용 ➔"
                    else if (isIndonesian) "Izinkan Mikrofon ➔"
                    else "Allow Microphone ➔",
                    onAction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRequestMicPermission()
                    }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ─────────────────────────────────────────────────────────────
            // 🎉 3단계 완료 축하 카드 & 시작 버튼
            // ─────────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = allCompleted,
                enter = fadeIn() + expandVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF131B2E))
                            )
                        )
                        .border(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isKorean) "🎉 모든 설정이 완료되었습니다!"
                        else if (isIndonesian) "🎉 Semua Pengaturan Selesai!"
                        else "🎉 All Setup Completed!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isKorean) "이제 카카오톡, 문자, 검색창 어디서든\n마이크를 누르고 편하게 말씀해 보세요."
                        else if (isIndonesian) "Sekarang Anda dapat menggunakan keyboard suara AI di aplikasi mana pun."
                        else "You can now use DearTalk AI voice keyboard in any messaging or search app.",
                        fontSize = 12.5.sp,
                        color = DearTalkText,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary)
                    ) {
                        Text(
                            text = if (isKorean) "🚀 디어톡 시작하기"
                            else if (isIndonesian) "🚀 Mulai DearTalk"
                            else "🚀 Start DearTalk",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 건너뛰기 / 둘러보기 버튼
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (isKorean) "나중에 설정하고 둘러보기 ➔"
                    else if (isIndonesian) "Atur nanti dan jelajahi ➔"
                    else "Setup later & explore ➔",
                    fontSize = 12.5.sp,
                    color = DearTalkTextDim
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 📦 개별 온보딩 스텝 카드 컴포넌트
 */
@Composable
private fun OnboardingStepCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String,
    isDone: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) DearTalkSurface.copy(alpha = 0.85f) else DearTalkSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isDone) Color(0xFF22C55E).copy(alpha = 0.6f) else DearTalkBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 스텝 번호 / 완료 체크 아이콘
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDone) Color(0xFF166534) else DearTalkKey
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = DearTalkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) Color(0xFF4ADE80) else DearTalkText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 11.5.sp,
                        color = if (isDone) Color(0xFF86EFAC) else DearTalkTextDim,
                        lineHeight = 15.sp
                    )
                }
            }

            if (!isDone) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DearTalkPrimary)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
