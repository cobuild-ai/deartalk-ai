package ai.deartalk.android.data.vault

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

/**
 * 🏛️ Cobuild AI 온디바이스 공통 모델 제공자 (ContentProvider)
 * - 동일한 배포 서명 키(Release Keystore, protectionLevel="signature")로 서명된
 *   패밀리 앱들(DearTalk Keyboard, DearTalk Translator, DearMind)에게
 *   온디바이스 LLM 모델 파일의 읽기 전용 ParcelFileDescriptor를 제로 카피로 제공합니다.
 */
class CobuildModelProvider : ContentProvider() {

    companion object {
        private const val TAG = "CobuildModelVault"
        const val MODEL_PATH_SEGMENT = "model"
        private const val MIN_VALID_MODEL_BYTES = 50 * 1024 * 1024L // 50MB 이상 유효 가중치
    }

    override fun onCreate(): Boolean {
        Log.i(TAG, "🏛️ CobuildModelProvider 초기화 완료 (${context?.packageName})")
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            throw SecurityException("CobuildModelProvider는 읽기 전용(r) 모드만 허용합니다.")
        }

        val ctx = context ?: throw IllegalStateException("Context가 null입니다.")
        val segment = uri.lastPathSegment
        if (segment != MODEL_PATH_SEGMENT) {
            throw FileNotFoundException("알 수 없는 모델 요청 URI: $uri")
        }

        // 1. Google Play Asset Delivery (PAD) 경로 탐색
        try {
            val assetPackManager = com.google.android.play.core.assetpacks.AssetPackManagerFactory.getInstance(ctx)
            val padLocation = assetPackManager.getPackLocation("gemma_asset_pack")
            if (padLocation != null) {
                val padAssetsPath = padLocation.assetsPath()
                if (!padAssetsPath.isNullOrBlank()) {
                    val padCandidates = listOf(
                        File(padAssetsPath, "models/gemma/gemma-4-E2B-it.litertlm"),
                        File(padAssetsPath, "models/gemma-4-E2B-it.litertlm"),
                        File(padAssetsPath, "models/model.litertlm")
                    )
                    for (cand in padCandidates) {
                        if (cand.exists() && cand.length() >= MIN_VALID_MODEL_BYTES) {
                            Log.i(TAG, "📦 [PAD 모델 제공]: ${cand.absolutePath} (${cand.length()} bytes)")
                            return ParcelFileDescriptor.open(cand, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "PAD location query fallback: ${e.message}")
        }

        // 2. 앱 내부 On-Demand 다운로드 모델 디렉토리 탐색
        val internalCandidates = listOf(
            File(ctx.filesDir, "models/gemma/gemma-4-E2B-it.litertlm"),
            File(ctx.filesDir, "models/gemma-4-E2B-it.litertlm"),
            File(ctx.filesDir, "models/model.litertlm"),
            File(ctx.filesDir, "models/model.bin")
        )
        for (cand in internalCandidates) {
            if (cand.exists() && cand.length() >= MIN_VALID_MODEL_BYTES) {
                Log.i(TAG, "📦 [내부 저장소 모델 제공]: ${cand.absolutePath} (${cand.length()} bytes)")
                return ParcelFileDescriptor.open(cand, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }

        // 3. ADB 로컬 개발 경로 탐색
        val adbCandidates = listOf(
            File("/data/local/tmp/llm/gemma-4-E2B-it.litertlm"),
            File("/data/local/tmp/llm/model.litertlm"),
            File("/data/local/tmp/llm/model.bin")
        )
        for (cand in adbCandidates) {
            if (cand.exists() && cand.length() >= MIN_VALID_MODEL_BYTES) {
                Log.i(TAG, "📦 [ADB 로컬 모델 제공]: ${cand.absolutePath} (${cand.length()} bytes)")
                return ParcelFileDescriptor.open(cand, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }

        Log.e(TAG, "❌ [모델 부재]: 유효한 온디바이스 모델 가중치 파일을 찾을 수 없습니다.")
        throw FileNotFoundException("호스트 앱에 유효한 온디바이스 모델 파일이 존재하지 않습니다.")
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String = "application/octet-stream"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
