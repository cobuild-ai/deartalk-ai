package ai.deartalk.android.agent.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualcommNpuDetectorTest {

    @Test
    fun detect_qualcommWithHexagonCdsp_returnsNpuSupportedTrue() {
        val profile = QualcommNpuDetector.detect(
            hardwareOverride = "qcom",
            boardOverride = "taro",
            socModelOverride = "SM8450",
            socManufacturerOverride = "QTI",
            fileChecker = { it == "/vendor/lib64/libcdsprpc.so" }
        )

        assertTrue(profile.isQualcommSoc)
        assertTrue(profile.hasHexagonCdspLibrary)
        assertTrue(profile.isNpuSupported)
        assertEquals("/vendor/lib64/libcdsprpc.so", profile.cdspPath)
        assertEquals("SM8450", profile.socModel)
        assertEquals("QTI", profile.socManufacturer)
    }

    @Test
    fun detect_qualcommWithoutHexagonCdsp_returnsNpuSupportedFalse() {
        val profile = QualcommNpuDetector.detect(
            hardwareOverride = "qcom",
            boardOverride = "taro",
            socModelOverride = "SM8450",
            socManufacturerOverride = "QTI",
            fileChecker = { false }
        )

        assertTrue(profile.isQualcommSoc)
        assertFalse(profile.hasHexagonCdspLibrary)
        assertFalse(profile.isNpuSupported)
    }

    @Test
    fun detect_nonQualcommSoc_returnsNpuSupportedFalse() {
        val exynosProfile = QualcommNpuDetector.detect(
            hardwareOverride = "s5e9925",
            boardOverride = "universal2200",
            socModelOverride = "Exynos 2200",
            socManufacturerOverride = "Samsung",
            fileChecker = { false }
        )
        assertFalse(exynosProfile.isQualcommSoc)
        assertFalse(exynosProfile.isNpuSupported)

        val tensorProfile = QualcommNpuDetector.detect(
            hardwareOverride = "zuma",
            boardOverride = "ripcurrent",
            socModelOverride = "Tensor G3",
            socManufacturerOverride = "Google",
            fileChecker = { false }
        )
        assertFalse(tensorProfile.isQualcommSoc)
        assertFalse(tensorProfile.isNpuSupported)
    }
}
