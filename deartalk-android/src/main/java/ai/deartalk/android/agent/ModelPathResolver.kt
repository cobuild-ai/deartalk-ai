package ai.deartalk.android.agent

import android.content.Context
import ai.deartalk.android.BuildConfig
import ai.deartalk.android.util.DearTalkLog
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import java.io.File

/**
 * 온디바이스 SLM 모델 파일 경로 해석기 (ModelPathResolver)
 * 
 * 탐색 우선순위:
 * 1. Google Play Asset Delivery (PAD) install-time 에셋 팩 (`deartalk-model-pack`)
 * 2. 앱 내부 저장소 (`context.filesDir/models/`)
 * 3. 디버그 환경 전용 임시 디렉터리 (`/data/local/tmp/llm/`)
 */
object ModelPathResolver {
    private const val TAG = "ModelPathResolver"
    const val ASSET_PACK_NAME = "deartalk_model_pack"

    private val SUPPORTED_EXTENSIONS = listOf(".litertlm", ".bin", ".task", ".tflite")

    fun resolveCandidatePaths(context: Context): List<String> {
        val candidates = mutableListOf<String>()

        // 1. Google Play Asset Delivery (PAD) 에셋 팩 경로 확인
        try {
            val assetPackManager = AssetPackManagerFactory.getInstance(context)
            val packLocation = assetPackManager.getPackLocation(ASSET_PACK_NAME)
            if (packLocation != null) {
                val assetsDirPath = packLocation.assetsPath()
                if (!assetsDirPath.isNullOrBlank()) {
                    val modelsDir = File(assetsDirPath, "models")
                    if (modelsDir.exists() && modelsDir.isDirectory) {
                        modelsDir.listFiles()?.filter { isSupportedModelFile(it) }?.forEach {
                            DearTalkLog.d(TAG, "📦 PAD 모델 발견: ${it.absolutePath} (${it.length() / 1024 / 1024}MB)")
                            candidates.add(it.absolutePath)
                        }
                    }
                    val rootAssets = File(assetsDirPath)
                    rootAssets.listFiles()?.filter { isSupportedModelFile(it) }?.forEach {
                        if (!candidates.contains(it.absolutePath)) {
                            candidates.add(it.absolutePath)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            DearTalkLog.e(TAG, "PAD 에셋 팩 탐색 중 예외 발생 (무시하고 fallback 진행)", t)
        }

        // 2. 앱 내부 로컬 저장소 (filesDir/models/)
        val localModelsDir = File(context.filesDir, "models")
        if (localModelsDir.exists() && localModelsDir.isDirectory) {
            localModelsDir.listFiles()?.filter { isSupportedModelFile(it) }?.forEach {
                if (!candidates.contains(it.absolutePath)) {
                    DearTalkLog.d(TAG, "📁 로컬 내부 저장소 모델 발견: ${it.absolutePath}")
                    candidates.add(it.absolutePath)
                }
            }
        }

        // 기본 표준 모델 파일명 fallback
        val defaultInternalModel = File(context.filesDir, "models/model.litertlm").absolutePath
        if (!candidates.contains(defaultInternalModel)) {
            candidates.add(defaultInternalModel)
        }
        val defaultInternalBin = File(context.filesDir, "models/model.bin").absolutePath
        if (!candidates.contains(defaultInternalBin)) {
            candidates.add(defaultInternalBin)
        }

        // 3. 디버그 전용 개발 경로 (/data/local/tmp/llm/)
        if (BuildConfig.DEBUG) {
            val debugLlmDir = File("/data/local/tmp/llm/")
            if (debugLlmDir.exists() && debugLlmDir.isDirectory) {
                debugLlmDir.listFiles()?.filter { isSupportedModelFile(it) }?.forEach {
                    if (!candidates.contains(it.absolutePath)) {
                        DearTalkLog.d(TAG, "🛠️ 디버그 경로 모델 발견: ${it.absolutePath}")
                        candidates.add(it.absolutePath)
                    }
                }
            }
            listOf(
                "/data/local/tmp/llm/model.litertlm",
                "/data/local/tmp/llm/gemma-2b-it.litertlm",
                "/data/local/tmp/llm/gemma-2b-it-cpu-int4.bin",
                "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin"
            ).forEach {
                if (!candidates.contains(it)) {
                    candidates.add(it)
                }
            }
        }

        return candidates
    }

    private fun isSupportedModelFile(file: File): Boolean {
        if (!file.isFile || file.length() == 0L) return false
        val name = file.name.lowercase()
        return SUPPORTED_EXTENSIONS.any { name.endsWith(it) }
    }
}
