package ai.deartalk.android.data

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

/**
 * 📱 기기 하드웨어(RAM, 저장공간) 사전 진단 결과 등급
 */
enum class DeviceTierRating {
    OPTIMAL,     // 🟢 최적: RAM 8GB+ 및 저장공간 3GB+ (Qwen 풀스택 무제한 허용)
    CAUTION,     // 🟡 주의: RAM 6GB 및 저장공간 2GB+ (순차 로딩 파이프라인으로 안전 구동)
    RESTRICTED   // 🔴 제한: RAM 4GB 이하 또는 저장공간 부족 (기기 멈춤 방지를 위해 설치 사전 차단)
}

data class SystemMetrics(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val isLowMemory: Boolean,
    val availableStorageBytes: Long,
    val tierRating: DeviceTierRating
) {
    val totalRamGb: Double get() = totalRamBytes / (1024.0 * 1024.0 * 1024.0)
    val availableRamGb: Double get() = availableRamBytes / (1024.0 * 1024.0 * 1024.0)
    val availableStorageGb: Double get() = availableStorageBytes / (1024.0 * 1024.0 * 1024.0)
}

/**
 * 🛡️ 시스템 하드웨어 및 메모리 사전 진단 관리자
 */
class SystemDiagnosticManager(private val context: Context) {

    companion object {
        const val MIN_REQUIRED_STORAGE_BYTES = 2L * 1024 * 1024 * 1024 // 최소 2GB 여유공간
        const val OPTIMAL_STORAGE_BYTES = 3L * 1024 * 1024 * 1024      // 최적 3GB 여유공간
        const val RAM_OPTIMAL_THRESHOLD_BYTES = (7.0 * 1024 * 1024 * 1024).toLong() // 7GB 이상 (8GB+ 기기)
        const val RAM_CAUTION_THRESHOLD_BYTES = (5.2 * 1024 * 1024 * 1024).toLong() // 5.2GB 이상 (6GB 기기)

        fun calculateTier(totalRam: Long, availableStorage: Long, isLowMem: Boolean = false): DeviceTierRating {
            if (availableStorage < MIN_REQUIRED_STORAGE_BYTES || isLowMem || totalRam < RAM_CAUTION_THRESHOLD_BYTES) {
                return DeviceTierRating.RESTRICTED
            }
            return if (totalRam >= RAM_OPTIMAL_THRESHOLD_BYTES && availableStorage >= OPTIMAL_STORAGE_BYTES) {
                DeviceTierRating.OPTIMAL
            } else {
                DeviceTierRating.CAUTION
            }
        }
    }

    fun diagnose(): SystemMetrics {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem
        val isLow = memInfo.lowMemory

        val internalDir = context.filesDir ?: Environment.getDataDirectory()
        val stat = StatFs(internalDir.path)
        val availStorage = stat.availableBlocksLong * stat.blockSizeLong

        val tier = calculateTier(totalRam, availStorage, isLow)

        return SystemMetrics(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            isLowMemory = isLow,
            availableStorageBytes = availStorage,
            tierRating = tier
        )
    }
}
