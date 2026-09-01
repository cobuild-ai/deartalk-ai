package ai.deartalk.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemDiagnosticManagerTest {

    @Test
    fun testOptimalTier_highRamAndStorage() {
        val ram8Gb = 8L * 1024 * 1024 * 1024
        val storage10Gb = 10L * 1024 * 1024 * 1024

        val tier = SystemDiagnosticManager.calculateTier(
            totalRam = ram8Gb,
            availableStorage = storage10Gb,
            isLowMem = false
        )
        assertEquals(DeviceTierRating.OPTIMAL, tier)
    }

    @Test
    fun testCautionTier_midRamAndStorage() {
        val ram6Gb = (5.8 * 1024 * 1024 * 1024).toLong()
        val storage3Gb = 3L * 1024 * 1024 * 1024

        val tier = SystemDiagnosticManager.calculateTier(
            totalRam = ram6Gb,
            availableStorage = storage3Gb,
            isLowMem = false
        )
        assertEquals(DeviceTierRating.CAUTION, tier)
    }

    @Test
    fun testRestrictedTier_lowRam() {
        val ram4Gb = 4L * 1024 * 1024 * 1024
        val storage10Gb = 10L * 1024 * 1024 * 1024

        val tier = SystemDiagnosticManager.calculateTier(
            totalRam = ram4Gb,
            availableStorage = storage10Gb,
            isLowMem = false
        )
        assertEquals(DeviceTierRating.RESTRICTED, tier)
    }

    @Test
    fun testRestrictedTier_lowStorage() {
        val ram8Gb = 8L * 1024 * 1024 * 1024
        val storage1Gb = 1L * 1024 * 1024 * 1024 // 2GB 미만

        val tier = SystemDiagnosticManager.calculateTier(
            totalRam = ram8Gb,
            availableStorage = storage1Gb,
            isLowMem = false
        )
        assertEquals(DeviceTierRating.RESTRICTED, tier)
    }

    @Test
    fun testRestrictedTier_isLowMemory() {
        val ram8Gb = 8L * 1024 * 1024 * 1024
        val storage10Gb = 10L * 1024 * 1024 * 1024

        val tier = SystemDiagnosticManager.calculateTier(
            totalRam = ram8Gb,
            availableStorage = storage10Gb,
            isLowMem = true
        )
        assertEquals(DeviceTierRating.RESTRICTED, tier)
    }
}
