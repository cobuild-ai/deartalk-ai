package ai.deartalk.android.agent.engine

/**
 * 📦 온디바이스 단일 표준 모델 경로 상수 (Gemma 4 E2B)
 */
object ModelPaths {
    const val ADB_LLM_DIR = "/data/local/tmp/llm"
    const val STANDARD_MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    val STANDARD_CANDIDATE_PATHS = listOf(
        "$ADB_LLM_DIR/$STANDARD_MODEL_FILENAME",
        "$ADB_LLM_DIR/model.bin"
    )

    val APP_INTERNAL_MODELS = listOf(
        "models/$STANDARD_MODEL_FILENAME",
        "models/model.litertlm",
        "models/model.bin"
    )
}
