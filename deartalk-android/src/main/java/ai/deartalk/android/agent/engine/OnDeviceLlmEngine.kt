package ai.deartalk.android.agent.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * 🧠 온디바이스 언어 모델 추론 추상 인터페이스 (DIP / OCP)
 * - 특정 LLM 런타임(LiteRT, MediaPipe, Mock)에 종속되지 않고 교체 가능한 추론 계약
 */
interface OnDeviceLlmEngine {
    val isModelLoaded: Boolean
    val isModelLoadedFlow: StateFlow<Boolean>

    suspend fun generate(prompt: String): String?
    fun reloadModel()
}
