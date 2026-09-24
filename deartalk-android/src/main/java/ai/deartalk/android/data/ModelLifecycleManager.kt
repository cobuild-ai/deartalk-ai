package ai.deartalk.android.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ModelPackState {
    object NotInstalled : ModelPackState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelPackState()
    data class Installed(val localPaths: Map<String, String>) : ModelPackState()
    data class Error(val message: String) : ModelPackState()
}

/**
 * 🌟 현재 활성화된 온디바이스 지능 등급
 */
enum class ActiveAiTier {
    GEMMA_4,     // 🌟 Gemma 4 E2B LiteRT (PAD 고성능 온디바이스 엔진)
    BASE_GEMMA,  // 🟢 Gemma 2B LiteRT (기본 내장 경량 엔진)
    STT_ONLY     // ⚡ 순수 음성인식 (LLM 미탑재 기기: STT 정상 동작 + 1-Tap PAD 다운로드 대기)
}

/**
 * 📦 Gemma 4 온디바이스 AI 모델 패키지 생명주기 관리자
 * Play Asset Delivery (PAD) on-demand 표준 및 로컬 ADB 경로(/data/local/tmp/llm/)와 호환
 */
class ModelLifecycleManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelLifecycleManager"
        const val GEMMA_PACK_NAME = "gemma_asset_pack"
        const val TOTAL_PACK_SIZE_BYTES = 1845493760L // 약 1.84 GB
        private const val MIN_VALID_MODEL_BYTES = 50 * 1024 * 1024L // 최소 50MB 이상

        const val KEY_STT = "stt"
        const val KEY_LLM = "llm"
        const val KEY_TTS = "tts"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _packState = MutableStateFlow<ModelPackState>(ModelPackState.NotInstalled)
    val packState: StateFlow<ModelPackState> = _packState.asStateFlow()

    private val _activeTier = MutableStateFlow<ActiveAiTier>(ActiveAiTier.STT_ONLY)
    val activeTier: StateFlow<ActiveAiTier> = _activeTier.asStateFlow()

    init {
        refreshState()
    }

    fun getModelDirectory(): File {
        val dir = File(context.filesDir, "models/gemma")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isInstalled(): Boolean {
        val paths = resolveModelPaths()
        return paths.isNotEmpty()
    }

    fun refreshState() {
        val paths = resolveModelPaths()
        if (paths.isNotEmpty()) {
            _packState.value = ModelPackState.Installed(paths)
            _activeTier.value = ActiveAiTier.GEMMA_4
        } else {
            if (_packState.value !is ModelPackState.Downloading) {
                _packState.value = ModelPackState.NotInstalled
            }
            // Gemma 기본 모델 존재 여부 확인 (최소 50MB 유효 바이너리 검증)
            val hasGemma = hasGemmaBaseModel()
            _activeTier.value = if (hasGemma) ActiveAiTier.BASE_GEMMA else ActiveAiTier.STT_ONLY
        }
    }

    private fun hasGemmaBaseModel(): Boolean {
        val gemmaCandidates = listOf(
            "/data/local/tmp/llm/model.litertlm",
            "/data/local/tmp/llm/gemma-2b-it.litertlm",
            "/data/local/tmp/llm/gemma-4-E2B-it.litertlm",
            "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin",
            "/data/local/tmp/llm/gemma-2b-it-cpu-int4.bin",
            File(context.filesDir, "models/model.litertlm").absolutePath,
            File(context.filesDir, "models/model.bin").absolutePath
        )
        return gemmaCandidates.any { path ->
            val f = File(path)
            f.exists() && f.length() >= MIN_VALID_MODEL_BYTES
        }
    }

    /**
     * 🔍 모델 로컬 경로 해석 (ADB 테스트 디렉토리 및 실존 바이너리 우선 검사)
     */
    fun resolveModelPaths(): Map<String, String> {
        val paths = mutableMapOf<String, String>()

        // 0. Google Play Asset Delivery (install-time) 경로 감지
        try {
            val assetPackManager = com.google.android.play.core.assetpacks.AssetPackManagerFactory.getInstance(context)
            val padLocation = assetPackManager.getPackLocation("gemma_asset_pack")
            if (padLocation != null) {
                val padAssetsPath = padLocation.assetsPath()
                if (!padAssetsPath.isNullOrBlank()) {
                    val padLlm = File(padAssetsPath, "models/gemma/gemma-4-E2B-it.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
                        ?: File(padAssetsPath, "models/gemma-4-E2B-it.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
                        ?: File(padAssetsPath, "models/model.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
                    if (padLlm != null) {
                        paths[KEY_LLM] = padLlm.absolutePath
                        return paths
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "PAD location query skipped or unavailable: ${e.message}")
        }

        // 1. ADB 개발자 로컬 경로 감지 (유효 크기 50MB 이상)
        val adbDir = File("/data/local/tmp/llm")
        if (adbDir.exists() && adbDir.isDirectory) {
            val llmFile = File(adbDir, "gemma-4-E2B-it.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
                ?: File(adbDir, "model.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
                ?: File(adbDir, "model.bin").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }

            if (llmFile != null) {
                paths[KEY_LLM] = llmFile.absolutePath
                return paths
            }
        }

        // 2. 앱 내부 On-Demand 다운로드 디렉토리 감지 (유효 크기 50MB 이상)
        val modelDir = getModelDirectory()
        val appLlm = File(modelDir, "gemma-4-E2B-it.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
            ?: File(modelDir, "model.litertlm").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }
            ?: File(modelDir, "model.bin").takeIf { it.exists() && it.length() >= MIN_VALID_MODEL_BYTES }

        if (appLlm != null) {
            paths[KEY_LLM] = appLlm.absolutePath
        }

        return paths
    }

    /**
     * 📥 Play Asset Delivery On-Demand 상태 알림 (Zero Fake Protocol)
     */
    fun startDownload(onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (_packState.value is ModelPackState.Downloading || isInstalled()) return

        downloadJob?.cancel()
        downloadJob = scope.launch {
            try {
                Log.d(TAG, "📥 Gemma 4 온디바이스 AI 팩 Play Asset Delivery 배포 준비 확인...")
                // 가짜 manifest 파일 생성을 엄격히 금지하고 정직하게 배포 대기 상태로 보고
                _packState.value = ModelPackState.Error("Play Asset Delivery (PAD) 패키지가 배포 준비 중입니다.")
                onError?.invoke("Play Asset Delivery 패키지 배포 준비 중")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 다운로드 실패", e)
                _packState.value = ModelPackState.Error(e.message ?: "다운로드 중 오류가 발생했습니다.")
                onError?.invoke(e.message ?: "다운로드 실패")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        refreshState()
    }

    /**
     * 🗑️ 1-Click 모델 삭제 (1.8GB 용량 즉시 환원 및 기본 모델 Fallback)
     */
    fun purgeModels(): Long {
        downloadJob?.cancel()
        val modelDir = getModelDirectory()
        var freedBytes = 0L

        if (modelDir.exists()) {
            modelDir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.delete()
            }
            modelDir.delete()
        }

        refreshState()
        Log.d(TAG, "🧹 Gemma 4 모델 패키지 삭제 완료 (환원된 용량: ${freedBytes / (1024 * 1024)}MB)")
        return freedBytes
    }
}
