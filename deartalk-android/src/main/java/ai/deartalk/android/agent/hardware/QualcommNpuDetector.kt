package ai.deartalk.android.agent.hardware

import android.os.Build
import android.util.Log
import java.io.File

/**
 * 🏎️ Qualcomm Hexagon NPU 및 Snapdragon SoC 물리 가속 하드웨어 감지기
 *
 * Truth-First & Zero Fake Protocol:
 * - 기기의 물리적 SoC 모델, 제조사, 하드웨어 식별자, 보드 플랫폼,
 *   그리고 Hexagon CDSP (Compute DSP) RPC 라이브러리(/vendor/lib64/libcdsprpc.so)의 실존 여부를
 *   다각도로 교차 검증하여 실제 Qualcomm Hexagon NPU 가속 가능 여부를 판별합니다.
 * - Qualcomm NPU가 탑재된 기기에서만 선택적으로 NPU 가속 바인딩을 시도하여,
 *   비퀄컴 기기(Exynos, Tensor, Dimensity 등)에서의 불필요한 시도 및 오버헤드를 원천 방지합니다.
 */
object QualcommNpuDetector {
    private const val TAG = "QualcommNpuDetector"

    val KNOWN_CDSP_RPC_PATHS = listOf(
        "/vendor/lib64/libcdsprpc.so",
        "/vendor/lib/libcdsprpc.so",
        "/system/vendor/lib64/libcdsprpc.so",
        "/system/vendor/lib/libcdsprpc.so"
    )

    data class NpuHardwareProfile(
        val isQualcommSoc: Boolean,
        val hasHexagonCdspLibrary: Boolean,
        val isNpuSupported: Boolean,
        val socModel: String,
        val socManufacturer: String,
        val hardware: String,
        val board: String,
        val cdspPath: String?
    )

    /**
     * 현재 기기의 하드웨어 정보를 수집하여 Qualcomm Hexagon NPU 지원 여부를 종합 판별합니다.
     */
    fun detect(
        hardwareOverride: String? = null,
        boardOverride: String? = null,
        socModelOverride: String? = null,
        socManufacturerOverride: String? = null,
        fileChecker: (String) -> Boolean = { File(it).exists() }
    ): NpuHardwareProfile {
        val hardware = hardwareOverride ?: Build.HARDWARE.orEmpty()
        val board = boardOverride ?: Build.BOARD.orEmpty()

        val socModel = socModelOverride ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.orEmpty()
        } else {
            ""
        }

        val socManufacturer = socManufacturerOverride ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.orEmpty()
        } else {
            ""
        }

        val isQcomHardware = hardware.lowercase().contains("qcom") ||
                hardware.lowercase().startsWith("sm") ||
                hardware.lowercase().startsWith("sdm") ||
                hardware.lowercase().startsWith("msm")

        val isQcomManufacturer = socManufacturer.lowercase().contains("qti") ||
                socManufacturer.lowercase().contains("qualcomm")

        val isQcomSoc = isQcomHardware || isQcomManufacturer ||
                socModel.lowercase().startsWith("sm") ||
                socModel.lowercase().startsWith("sdm")

        // Hexagon Compute DSP (CDSP) RPC 라이브러리 실제 존재 여부 검사
        val existingCdspPath = KNOWN_CDSP_RPC_PATHS.firstOrNull { path ->
            try {
                fileChecker(path)
            } catch (t: Throwable) {
                false
            }
        }

        val hasCdsp = existingCdspPath != null

        // 🏎️ Qualcomm SoC이면서 물리적 Hexagon CDSP 라이브러리가 확인된 경우에만 NPU 지원으로 판정
        val isNpuSupported = isQcomSoc && hasCdsp

        val profile = NpuHardwareProfile(
            isQualcommSoc = isQcomSoc,
            hasHexagonCdspLibrary = hasCdsp,
            isNpuSupported = isNpuSupported,
            socModel = socModel.ifEmpty { hardware },
            socManufacturer = socManufacturer.ifEmpty { if (isQcomSoc) "Qualcomm" else Build.MANUFACTURER.orEmpty() },
            hardware = hardware,
            board = board,
            cdspPath = existingCdspPath
        )

        try {
            Log.i(
                TAG,
                "🔍 [SoC 하드웨어 진단] Qualcomm: ${profile.isQualcommSoc}, " +
                        "Hexagon CDSP: ${profile.hasHexagonCdspLibrary} (${profile.cdspPath ?: "None"}), " +
                        "NPU 가속 가능: ${profile.isNpuSupported}, " +
                        "SoC: ${profile.socManufacturer} ${profile.socModel} (Board: ${profile.board})"
            )
        } catch (_: Throwable) {
            // Android Log unmocked in pure JVM tests
        }

        return profile
    }

    /**
     * Qualcomm NPU 가속 대상 기기인지 여부를 간편 확인
     */
    fun isQualcommNpuSupported(): Boolean = detect().isNpuSupported
}
