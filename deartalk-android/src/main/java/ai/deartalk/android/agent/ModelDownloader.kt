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

    // Google Gemma 2B Instruct GPU Int4 모델 (HuggingFace 공식 TFLite 경로)
    val defaultModelUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-gpu-int4.bin"

    fun startDownload(context: Context, intentEngine: DearTalkIntentEngine, urlString: String = defaultModelUrl) {
        if (_isDownloading.value) return

        _isDownloading.value = true
        _isCompleted.value = false
        _errorMessage.value = null
        _progress.value = 0.0f
        _statusMessage.value = if (Locale.getDefault().language == "ko") "연결 중..." else "Connecting..."

        downloadJob = downloadScope.launch {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("서버 응답 에러: ${connection.responseCode} ${connection.responseMessage}")
                }

                val fileLength = connection.contentLengthLong
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

                val input = BufferedInputStream(connection.inputStream)
                val output = FileOutputStream(tempFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                var lastUpdate = System.currentTimeMillis()

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100 || total == fileLength) {
                        lastUpdate = now
                        val currentProgress = if (fileLength > 0) total.toFloat() / fileLength else 0.0f
                        val writtenMB = total.toDouble() / (1024 * 1024)
                        _progress.value = currentProgress
                        _downloadedSizeMB.value = writtenMB
                        _statusMessage.value = String.format(
                            if (Locale.getDefault().language == "ko") "다운로드 중: %.1f MB / %.1f MB (%.1f%%)"
                            else "Downloading: %.1f MB / %.1f MB (%.1f%%)",
                            writtenMB, totalMBVal, currentProgress * 100f
                        )
                    }
                }

                output.flush()
                output.close()
                input.close()

                val targetFile = File(modelsDir, "model.bin")
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    throw Exception("임시 파일을 최종 경로로 이동하는 데 실패했습니다.")
                }

                _isDownloading.value = false
                _isCompleted.value = true
                _progress.value = 1.0f
                _statusMessage.value = if (Locale.getDefault().language == "ko") "다운로드 완료!" else "Download complete!"
                
                // 엔진에 새 모델 로드 트리거
                intentEngine.detectAndInitOnDeviceModel()

            } catch (e: Exception) {
                Log.e(TAG, "모델 다운로드 에러: ${e.message}", e)
                _isDownloading.value = false
                _errorMessage.value = e.localizedMessage ?: e.message ?: "다운로드 중 에러 발생"
                _statusMessage.value = if (Locale.getDefault().language == "ko") "다운로드 실패" else "Download failed"
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _isDownloading.value = false
        _statusMessage.value = if (Locale.getDefault().language == "ko") "다운로드 취소됨" else "Download cancelled"
    }
}
