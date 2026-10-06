package ai.deartalk.android.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.*
import ai.deartalk.android.ui.main.components.*
import ai.deartalk.android.ui.state.MainUiState

import ai.deartalk.android.ui.onboarding.OnboardingWizardScreen

/**
 * 📱 DearTalk AI 메인 설정 및 가이드 화면 (UDF / MVI Presentation)
 * - 불변 MainUiState를 구독하고 단일 MainUiEvent 채널로 사용자 동작을 위임
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onEvent: (MainUiEvent) -> Unit,
    onEnableIme: () -> Unit,
    onSelectIme: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isKorean = UiStrings.isKo
    val isIndonesian = UiStrings.isId

    // 🌟 온보딩 마법사 표시 여부 판별 (키보드 미활성화/미선택 또는 마이크 권한 미허용 상태이고, 아직 닫지 않았을 때)
    val showOnboarding = (!uiState.isImeSelected || !uiState.isImeEnabled || !uiState.hasMicPermission) && !uiState.isOnboardingDismissed

    if (showOnboarding) {
        OnboardingWizardScreen(
            isImeEnabled = uiState.isImeEnabled,
            isImeSelected = uiState.isImeSelected,
            hasMicPermission = uiState.hasMicPermission,
            isKorean = isKorean,
            isIndonesian = isIndonesian,
            onEnableIme = onEnableIme,
            onSelectIme = onSelectIme,
            onRequestMicPermission = onRequestMicPermission,
            onComplete = { onEvent(MainUiEvent.CompleteOnboarding) },
            onDismiss = { onEvent(MainUiEvent.DismissOnboarding) },
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isKorean) "디어톡(DearTalk) AI 음성키보드 설정 및 가이드"
                        else if (isIndonesian) "Pengaturan DearTalk AI"
                        else "DearTalk AI Settings & Guide",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DearTalkBackground,
                    titleContentColor = DearTalkText
                )
            )
        },
        containerColor = DearTalkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 키보드 빠른 시작 안내 카드
            ImeSetupGuideCard(
                isImeEnabled = uiState.isImeEnabled,
                isImeSelected = uiState.isImeSelected,
                isKorean = isKorean,
                isIndonesian = isIndonesian,
                onEnableIme = onEnableIme,
                onSelectIme = onSelectIme
            )

            // 2. AI 엔진 동작 상태 진단 카드
            PrivacyDiagnosticCard(
                isModelLoaded = uiState.isModelLoaded,
                isKorean = isKorean,
                isIndonesian = isIndonesian,
                onDetectAndInitModel = { onEvent(MainUiEvent.DetectAndInitModel) }
            )

            // 3. 실시간 음성 & AI 체험 샌드박스 + 모의 메신저 시뮬레이터
            VoiceSandboxSection(
                uiState = uiState,
                isKorean = isKorean,
                isIndonesian = isIndonesian,
                onEvent = onEvent
            )

            // 4. 기본 언어 및 자판 환경 설정
            KeyboardSettingsCard(
                isAutoLanguage = uiState.isAutoLanguage,
                selectedLanguageCode = uiState.selectedLanguageCode,
                selectedKoreanKeyboardType = uiState.selectedKoreanKeyboardType,
                isKorean = isKorean,
                isIndonesian = isIndonesian,
                onSetAutoLanguage = { onEvent(MainUiEvent.SetAutoLanguage(it)) },
                onSelectLanguageCode = { onEvent(MainUiEvent.SelectLanguageCode(it)) },
                onChangeKoreanKeyboardType = { onEvent(MainUiEvent.ChangeKoreanKeyboardType(it)) }
            )

            // 5. Cobuild AI 패밀리 앱 안내 카드 (DearTalk Voice Translator 출시 예정)
            FamilyAppsSectionCard(
                isKorean = isKorean,
                isIndonesian = isIndonesian,
                onComingSoonClick = {
                    val msg = if (isKorean) "🗣️ 디어톡(DearTalk) 음성 통역기는 곧 출시될 예정입니다!"
                              else if (isIndonesian) "🗣️ DearTalk Penerjemah Suara akan segera hadir!"
                              else "🗣️ DearTalk Voice Translator is coming soon!"
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            // 6. 사용 방법 안내
            UserGuideCard()

            // 7. 다른 키보드로 전환하기
            SwitchKeyboardCard(isKorean = isKorean, isIndonesian = isIndonesian, onSelectIme = onSelectIme)

            // 8. 앱 정보
            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) { null }

            val versionName = "v${packageInfo?.versionName ?: "1.0.0"} (${if (isKorean) "빌드" else "Build"} ${packageInfo?.longVersionCode ?: 1})"
            val buildTimestamp = packageInfo?.let {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                sdf.format(java.util.Date(it.lastUpdateTime))
            } ?: if (isKorean) "확인 불가" else if (isIndonesian) "Tidak diketahui" else "Unknown"

            AppAboutCard(
                versionName = versionName,
                buildTimestamp = buildTimestamp,
                isKorean = isKorean,
                isIndonesian = isIndonesian
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
