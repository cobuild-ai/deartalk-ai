package ai.deartalk.android.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ModelDownloader private constructor() {
    companion object {
        private const val TAG = "DearTalkDownloader"
        val shared = ModelDownloader()
    }

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _progress = MutableStateFlow(0.0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _downloadedSizeMB = MutableStateFlow(0.0)
    val downloadedSizeMB: StateFlow<Double> = _downloadedSizeMB.asStateFlow()

    private val _totalSizeMB = MutableStateFlow(0.0)
    val totalSizeMB: StateFlow<Double> = _totalSizeMB.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var downloadJob: Job? = null
    private val downloadScope = CoroutineScope(Dispatchers.IO)

    // Google Gemma 2B 온디바이스 TFLite 바이너리 공개 엔드포인트
    val defaultModelUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-gpu-int4.bin"

    fun startDownload(context: Context, intentEngine: DearTalkIntentEngine, urlString: String = defaultModelUrl) {
        if (_isDownloading.value) return

        _isDownloading.value = true
        _isCompleted.value = false
        _errorMessage.value = null
        _progress.value = 0.0f
        _statusMessage.value = if (Locale.getDefault().language == "ko") "서버 연결 및 다운로드 준비 중..." else "Connecting and preparing download..."

        downloadJob = downloadScope.launch {
            try {
                var currentUrl = urlString
                var connection: HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 6

                // HTTP 301/302/307/308 리다이렉트 자동 추적 루프
                while (redirects < maxRedirects) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20000
                        readTimeout = 20000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) DearTalkAI/1.0")
                        setRequestProperty("Accept", "*/*")
                    }
                    connection.connect()

                    val status = connection.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!newUrl.isNullOrBlank()) {
                            currentUrl = newUrl
                            redirects++
                            continue
                        }
                    }
                    break
                }

                val finalConn = connection ?: throw Exception("네트워크 연결 생성 실패")
                val responseCode = finalConn.responseCode

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw Exception("401 인증 필요: 모델 호스팅 서버 권한이 필요합니다. 로컬 모델 감지를 사용하거나 수동 배치가 가능합니다.")
                }

                if (responseCode !in 200..299) {
                    throw Exception("서버 응답 오류 (HTTP $responseCode ${finalConn.responseMessage})")
                }

                val fileLength = finalConn.contentLengthLong
                val totalMBVal = if (fileLength > 0) fileLength.toDouble() / (1024 * 1024) else 1350.0
                _totalSizeMB.value = totalMBVal

                val modelsDir = File(context.filesDir, "models")
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs()
                }
                val tempFile = File(modelsDir, "model.bin.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                val input = BufferedInputStream(finalConn.inputStream)
                val output = FileOutputStream(tempFile)

                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                var lastUpdate = System.currentTimeMillis()

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 150 || total == fileLength) {
                        lastUpdate = now
                        val currentProgress = if (fileLength > 0) (total.toFloat() / fileLength).coerceIn(0f, 1f) else 0.0f
                        val writtenMB = total.toDouble() / (1024 * 1024)
                        _progress.value = currentProgress
                        _downloadedSizeMB.value = writtenMB
                        _statusMessage.value = String.format(
                            Locale.getDefault(),
                            if (Locale.getDefault().language == "ko") "다운로드 진행 중: %.1f MB / %.1f MB (%.1f%%)"
                            else "Downloading: %.1f MB / %.1f MB (%.1f%%)",
                            writtenMB, totalMBVal, currentProgress * 100f
                        )
                    }
                }

                output.flush()
                output.close()
                input.close()
                finalConn.disconnect()

                val targetFile = File(modelsDir, "model.bin")
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    throw Exception("임시 파일을 최종 모델 경로로 이동하는 데 실패했습니다.")
                }

                _isDownloading.value = false
                _isCompleted.value = true
                _progress.value = 1.0f
                _statusMessage.value = if (Locale.getDefault().language == "ko") "✨ 온디바이스 AI 모델 설치 완료!" else "✨ On-Device AI Model Installed!"

                // 엔진에 새 모델 로드 트리거
                intentEngine.detectAndInitOnDeviceModel()

            } catch (e: Exception) {
                Log.e(TAG, "모델 다운로드 에러: ${e.message}", e)
                _isDownloading.value = false
                _errorMessage.value = e.localizedMessage ?: e.message ?: "다운로드 중 오류가 발생했습니다."
                _statusMessage.value = if (Locale.getDefault().language == "ko") "다운로드 실패" else "Download Failed"
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _isDownloading.value = false
        _statusMessage.value = if (Locale.getDefault().language == "ko") "다운로드가 취소되었습니다." else "Download cancelled."
    }
}
