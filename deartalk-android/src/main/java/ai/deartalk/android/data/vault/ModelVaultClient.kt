package ai.deartalk.android.data.vault

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 🔄 Cobuild AI 온디바이스 모델 볼트 클라이언트 (P2P Mesh)
 * - 기기에 설치된 패밀리 앱(DearTalk Keyboard, DearTalk Translator, DearMind) 중
 *   온디바이스 LLM 모델을 보유한 앱을 자동 감지하고,
 *   추가 네트워크 다운로드 없이 ParcelFileDescriptor를 통해 무손실 제로카피 공유를 실현합니다.
 */
object ModelVaultClient {

    private const val TAG = "ModelVaultClient"

    // 🤝 온디바이스 AI 패밀리 앱 패키지 목록
    val FAMILY_PACKAGES = listOf(
        "ai.deartalk.android",     // DearTalk Voice Keyboard
        "ai.deartalk.translator",  // DearTalk Voice Translator
        "ai.dearmind.android"      // DearMind
    )

    data class VaultResult(
        val pfd: ParcelFileDescriptor,
        val sourcePackage: String,
        val procFdPath: String
    )

    /**
     * 🔍 패밀리 앱들로부터 온디바이스 모델 공유 획득 시도 (P2P Mesh)
     */
    fun acquireSharedModel(context: Context): VaultResult? {
        val currentPackage = context.packageName

        for (targetPackage in FAMILY_PACKAGES) {
            if (targetPackage == currentPackage) continue // 자기 자신은 ContentProvider 쿼리 스킵

            val uri = Uri.parse("content://$targetPackage.modelprovider/model")
            try {
                Log.d(TAG, "🔎 패밀리 앱 모델 탐색 중: $targetPackage ($uri)")
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null && pfd.statSize > 50 * 1024 * 1024L) {
                    val procPath = "/proc/self/fd/${pfd.fd}"
                    Log.i(TAG, "🎉 [P2P 모델 공유 성공!] 출처: $targetPackage | 크기: ${pfd.statSize} bytes | 경로: $procPath")
                    return VaultResult(pfd, targetPackage, procPath)
                } else {
                    pfd?.close()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "⚠️ 서명 불일치 또는 접근 거부 ($targetPackage): ${e.message}")
            } catch (e: Throwable) {
                // 앱 미설치 또는 모델 미보유 (자연스러운 상태)
                Log.d(TAG, "ℹ️ 모델 미보유 또는 미설치 ($targetPackage): ${e.message}")
            }
        }

        Log.d(TAG, "ℹ️ 패밀리 앱 중 공유 가능한 모델이 없습니다 (자체 로딩 진행 필요)")
        return null
    }

    /**
     * 📁 /proc/self/fd 경로를 지원하지 않는 일부 NPU 하드웨어 네이티브 라이브러리를 위한
     *    초고속 로컬 복사 유틸리티 (P2P 로컬 스트림 복사이므로 데이터 요금/인터넷 0MB 소모)
     */
    fun copyToLocalFile(pfd: ParcelFileDescriptor, destinationFile: File): Boolean {
        return try {
            destinationFile.parentFile?.mkdirs()
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")
            FileInputStream(pfd.fileDescriptor).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.channel.transferTo(0, pfd.statSize, output.channel)
                }
            }
            if (tempFile.renameTo(destinationFile)) {
                Log.i(TAG, "✅ [로컬 볼트 복사 완료]: ${destinationFile.absolutePath} (${destinationFile.length()} bytes)")
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "❌ 로컬 볼트 복사 실패: ${e.message}", e)
            false
        }
    }
}
