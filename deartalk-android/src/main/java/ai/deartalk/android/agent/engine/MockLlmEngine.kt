package ai.deartalk.android.agent.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 🧪 단위 테스트 및 AI 에이전트 신속 검증용 Mock LLM 엔진 (DIP)
 * - C-API / JNI native 바이너리 없이 순수 JVM / Robolectric 환경에서 즉시 동작
 */
class MockLlmEngine(
    private val defaultOutput: String = "✨ AI가 깔끔하게 다듬은 문장입니다."
) : OnDeviceLlmEngine {

    private val _isModelLoadedFlow = MutableStateFlow(true)
    override val isModelLoaded: Boolean = true
    override val isModelLoadedFlow: StateFlow<Boolean> = _isModelLoadedFlow.asStateFlow()

    override suspend fun generate(prompt: String): String? {
        return defaultOutput
    }

    override fun reloadModel() {
        _isModelLoadedFlow.value = true
    }
}
