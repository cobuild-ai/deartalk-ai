package ai.deartalk.android.agent

import android.content.Context
import android.os.Build

/**
 * 기기별 온디바이스 AI 구동 방식 구분
 */
enum class DeviceAiEngineType(
    val title: String,
    val badgeLabel: String,
    val description: String,
    val isZeroDownload: Boolean
) {
    SYSTEM_NPU_ACCELERATED(
        title = "⚡ 시스템 AI 하드웨어 가속 (Gemini Nano)",
        badgeLabel = "⚡ NPU 가속",
        description = "고객님의 기기는 최신 온디바이스 AI 가속 하드웨어(NPU)가 내장되어 있어, 별도 다운로드 없이 즉시 초고속으로 동작합니다.",
        isZeroDownload = true
    ),
    PLAY_ASSET_DELIVERY_SLM(
        title = "📦 온디바이스 SLM 자립형 엔진 (Gemma 2B)",
        badgeLabel = "🧠 온디바이스 SLM",
        description = "고객님의 기기에 최적화된 고성능 AI 모델이 Google Play를 통해 안전하게 전달됩니다. (인터넷 권한 0개로 100% 오프라인 보증)",
        isZeroDownload = false
    )
}

data class DeviceAiProfile(
    val deviceModelName: String,
    val manufacturer: String,
    val engineType: DeviceAiEngineType,
    val chipsetName: String = "",
    val isHighEndNpu: Boolean = false
)

/**
 * 기기별 AI 하드웨어 스펙 및 AICore/Gemini Nano 탑재 여부 판별기
 */
object DeviceCapabilityDetector {

    // 시스템 내장 AICore / Gemini Nano 가속을 기본 지원하는 플래그십 기기 패턴
    private val SYSTEM_AI_DEVICE_PATTERNS = listOf(
        // Google Pixel 8, 8 Pro, 8a, 9, 9 Pro, 9 Fold 등
        "Pixel 8", "Pixel 9",
        // Samsung Galaxy S24 시리즈, Z Fold6, Z Flip6 등 (Android 14+ AICore 탑재 단말)
        "SM-S921", "SM-S926", "SM-S928", // S24, S24+, S24 Ultra
        "SM-F956", "SM-F741",             // Z Fold6, Z Flip6
        "Galaxy S24", "Galaxy S25"
    )

    fun detectProfile(context: Context? = null): DeviceAiProfile {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val displayModelName = "$manufacturer $model"

        val isSystemAi = isSystemAiSupported(model, displayModelName)

        val engineType = if (isSystemAi) {
            DeviceAiEngineType.SYSTEM_NPU_ACCELERATED
        } else {
            DeviceAiEngineType.PLAY_ASSET_DELIVERY_SLM
        }

        return DeviceAiProfile(
            deviceModelName = displayModelName,
            manufacturer = manufacturer,
            engineType = engineType,
            isHighEndNpu = isSystemAi
        )
    }

    private fun isSystemAiSupported(model: String, displayModelName: String): Boolean {
        // 1. 모델명 화이트리스트 검사
        val matchesHardwarePattern = SYSTEM_AI_DEVICE_PATTERNS.any { pattern ->
            model.contains(pattern, ignoreCase = true) || displayModelName.contains(pattern, ignoreCase = true)
        }
        if (matchesHardwarePattern && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }

        // 2. Android 14+ AICore 시스템 패키지 존재 여부 확인 (미래 확장성)
        return false
    }
}
