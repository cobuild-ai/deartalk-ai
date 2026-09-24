package ai.deartalk.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelLifecycleManagerTest {

    @Test
    fun testPackStateConstants() {
        assertEquals("gemma_asset_pack", ModelLifecycleManager.GEMMA_PACK_NAME)
        assertTrue(ModelLifecycleManager.TOTAL_PACK_SIZE_BYTES > 1_000_000_000L) // 1GB+
        assertEquals("stt", ModelLifecycleManager.KEY_STT)
        assertEquals("llm", ModelLifecycleManager.KEY_LLM)
        assertEquals("tts", ModelLifecycleManager.KEY_TTS)
    }

    @Test
    fun testModelPaths_singleStandardGemma() {
        // 단일 표준 모델 Gemma 4 E2B 상수 및 우선순위 검증
        assertEquals("gemma-4-E2B-it.litertlm", ai.deartalk.android.agent.engine.ModelPaths.STANDARD_MODEL_FILENAME)
        assertEquals(
            "/data/local/tmp/llm/gemma-4-E2B-it.litertlm",
            ai.deartalk.android.agent.engine.ModelPaths.STANDARD_CANDIDATE_PATHS.first()
        )
        assertEquals(
            "models/gemma-4-E2B-it.litertlm",
            ai.deartalk.android.agent.engine.ModelPaths.APP_INTERNAL_MODELS.first()
        )
    }

    @Test
    fun testActiveAiTierValues() {
        val tiers = ActiveAiTier.values()
        assertTrue(tiers.contains(ActiveAiTier.BASE_GEMMA))
        assertTrue(tiers.contains(ActiveAiTier.GEMMA_4))
        assertTrue(tiers.contains(ActiveAiTier.STT_ONLY))
    }

    @Test
    fun testDeviceTierThresholds() {
        assertEquals(2L * 1024 * 1024 * 1024, SystemDiagnosticManager.MIN_REQUIRED_STORAGE_BYTES)
        assertEquals(3L * 1024 * 1024 * 1024, SystemDiagnosticManager.OPTIMAL_STORAGE_BYTES)
    }
}
