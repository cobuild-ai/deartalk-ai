package ai.deartalk.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelLifecycleManagerTest {

    @Test
    fun testPackStateConstants() {
        assertEquals("qwen_voice_pack", ModelLifecycleManager.QWEN_PACK_NAME)
        assertTrue(ModelLifecycleManager.TOTAL_PACK_SIZE_BYTES > 1_000_000_000L) // 1GB+
        assertEquals("stt", ModelLifecycleManager.KEY_STT)
        assertEquals("llm", ModelLifecycleManager.KEY_LLM)
        assertEquals("tts", ModelLifecycleManager.KEY_TTS)
    }

    @Test
    fun testDeviceTierThresholds() {
        assertEquals(2L * 1024 * 1024 * 1024, SystemDiagnosticManager.MIN_REQUIRED_STORAGE_BYTES)
        assertEquals(3L * 1024 * 1024 * 1024, SystemDiagnosticManager.OPTIMAL_STORAGE_BYTES)
    }
}
