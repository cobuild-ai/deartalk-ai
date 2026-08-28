package ai.deartalk.android.agent

import android.content.Context
import ai.deartalk.android.util.DearTalkLog
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 6단계 AI 모델 라이프사이클 상태
 */
sealed interface AiModelLifecycleState {
    // 1. 확인 중
    data object Checking : AiModelLifecycleState

    // 2. 시스템 NPU 가속 즉시 준비 완료 (Gemini Nano 기기)
    data class SystemNanoReady(val profile: DeviceAiProfile) : AiModelLifecycleState

    // 3. Play Asset Delivery 에셋 다운로드 중
    data class PadDownloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : AiModelLifecycleState

    // 4. 모바일 데이터 보호를 위해 Wi-Fi 대기 중
    data object PadWifiWaiting : AiModelLifecycleState

    // 5. 온디바이스 SLM 모델 로드 및 추론 준비 완료
    data class PadReady(
        val modelPath: String,
        val profile: DeviceAiProfile
    ) : AiModelLifecycleState

    // 6. 오류 발생
    data class Error(val message: String) : AiModelLifecycleState
}

/**
 * 온디바이스 AI 모델의 생명주기 및 다운로드/로딩 상태 전역 관리자
 */
object AiModelLifecycleManager {
    private const val TAG = "AiModelLifecycleManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<AiModelLifecycleState>(AiModelLifecycleState.Checking)
    val state: StateFlow<AiModelLifecycleState> = _state.asStateFlow()

    private var assetPackManager: AssetPackManager? = null
    private var isListenerRegistered = false

    private val stateUpdatedListener = AssetPackStateUpdateListener { state: AssetPackState ->
        if (state.name() == ModelPathResolver.ASSET_PACK_NAME) {
            handleAssetPackState(state)
        }
    }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val profile = DeviceCapabilityDetector.detectProfile(appContext)

        // 1. 시스템 NPU 가속 기기인 경우 (Track 1)
        if (profile.engineType == DeviceAiEngineType.SYSTEM_NPU_ACCELERATED) {
            DearTalkLog.d(TAG, "⚡ 시스템 NPU 가속 단말 감지: ${profile.deviceModelName}")
            _state.value = AiModelLifecycleState.SystemNanoReady(profile)
            return
        }

        // 2. 자립형 SLM 기기인 경우 (Track 2)
        try {
            assetPackManager = AssetPackManagerFactory.getInstance(appContext)
            if (!isListenerRegistered) {
                assetPackManager?.registerListener(stateUpdatedListener)
                isListenerRegistered = true
            }

            checkCurrentAssetStatus(appContext, profile)
        } catch (t: Throwable) {
            DearTalkLog.e(TAG, "AssetPackManager 초기화 실패, 로컬 모델 fallback 점검", t)
            checkLocalFallback(appContext, profile)
        }
    }

    private fun checkCurrentAssetStatus(context: Context, profile: DeviceAiProfile) {
        scope.launch(Dispatchers.IO) {
            val candidates = ModelPathResolver.resolveCandidatePaths(context)
            val existingValidPath = candidates.firstOrNull { File(it).exists() && File(it).length() > 0 }

            if (existingValidPath != null) {
                DearTalkLog.d(TAG, "✅ 온디바이스 SLM 모델 파일 확인 완료: $existingValidPath")
                _state.value = AiModelLifecycleState.PadReady(existingValidPath, profile)
                return@launch
            }

            // 스토어 에셋 상태 조회
            try {
                val packLocation = assetPackManager?.getPackLocation(ModelPathResolver.ASSET_PACK_NAME)
                if (packLocation != null) {
                    val assetsPath = packLocation.assetsPath()
                    if (!assetsPath.isNullOrBlank()) {
                        _state.value = AiModelLifecycleState.PadReady(assetsPath, profile)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                DearTalkLog.e(TAG, "getPackLocation 에러", e)
            }

            // 다운로드가 필요한 경우 (on-demand / fast-follow 대응)
            _state.value = AiModelLifecycleState.PadDownloading(progressPercent = 0, bytesDownloaded = 0, totalBytes = 0)
        }
    }

    private fun checkLocalFallback(context: Context, profile: DeviceAiProfile) {
        val candidates = ModelPathResolver.resolveCandidatePaths(context)
        val existingValidPath = candidates.firstOrNull { File(it).exists() && File(it).length() > 0 }
        if (existingValidPath != null) {
            _state.value = AiModelLifecycleState.PadReady(existingValidPath, profile)
        } else {
            _state.value = AiModelLifecycleState.PadReady("", profile)
        }
    }

    private fun handleAssetPackState(state: AssetPackState) {
        when (state.status()) {
            AssetPackStatus.PENDING -> {
                _state.value = AiModelLifecycleState.PadDownloading(0, 0, state.totalBytesToDownload())
            }
            AssetPackStatus.DOWNLOADING -> {
                val downloaded = state.bytesDownloaded()
                val total = state.totalBytesToDownload()
                val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                DearTalkLog.d(TAG, "📦 모델 에셋 다운로드 중: $percent% ($downloaded / $total bytes)")
                _state.value = AiModelLifecycleState.PadDownloading(percent, downloaded, total)
            }
            AssetPackStatus.WAITING_FOR_WIFI -> {
                DearTalkLog.d(TAG, "📡 Wi-Fi 연결 대기 중")
                _state.value = AiModelLifecycleState.PadWifiWaiting
            }
            AssetPackStatus.COMPLETED -> {
                DearTalkLog.d(TAG, "🎉 모델 에셋 다운로드 완료!")
                _state.value = AiModelLifecycleState.PadReady(
                    modelPath = "",
                    profile = DeviceCapabilityDetector.detectProfile()
                )
            }
            AssetPackStatus.FAILED -> {
                DearTalkLog.e(TAG, "❌ 모델 에셋 다운로드 실패 (에러코드: ${state.errorCode()})")
                _state.value = AiModelLifecycleState.Error("다운로드 중 오류가 발생했습니다. (코드: ${state.errorCode()})")
            }
            else -> {}
        }
    }
}
