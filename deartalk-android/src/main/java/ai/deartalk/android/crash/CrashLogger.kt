package ai.deartalk.android.crash

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 키보드(IME) 및 애플리케이션 전역 크래시 로거
 * 키보드 앱의 특성상 크래시 발생 시 상세한 시스템/컴포넌트 상태를 영구 파일로 기록하고
 * 디버깅을 지원하며, 예외 상황에서도 프로세스가 비정상 종료되지 않도록 내결함성을 제공합니다.
 */
object CrashLogger {

    private const val TAG = "DearTalkCrash"
    private var isInitialized = false
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "CrashLogger successfully initialized")
    }

    /**
     * 핸들링된 예외를 파일 및 Logcat에 안전하게 영구 기록
     */
    fun logHandledException(contextTag: String, message: String, throwable: Throwable) {
        val stackTrace = getStackTraceString(throwable)
        val report = buildCrashReport("HANDLED EXCEPTION", contextTag, message, stackTrace)
        Log.e(TAG, report)
        saveReportToFile(report)
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        val stackTrace = getStackTraceString(throwable)
        val report = buildCrashReport("UNCAUGHT CRASH", "Thread: ${thread.name}", throwable.message ?: "No message", stackTrace)
        Log.e(TAG, report)
        saveReportToFile(report)
    }

    private fun buildCrashReport(header: String, contextTag: String, message: String, stackTrace: String): String {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.KOREA).format(Date())
        return buildString {
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("💥 DEARTALK IME CRASH REPORT [$header]")
            appendLine("⏰ Time: $timeStr")
            appendLine("🏷️ Context: $contextTag")
            appendLine("📱 Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT}, Android ${Build.VERSION.RELEASE})")
            appendLine("💬 Message: $message")
            appendLine("───────────────── STACK TRACE ─────────────────")
            appendLine(stackTrace)
            appendLine("═══════════════════════════════════════════════════════════════")
        }
    }

    private fun saveReportToFile(report: String) {
        try {
            val context = appContext ?: return
            val crashDir = File(context.filesDir, "crash_logs")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            val fileName = "crash_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.log"
            val file = File(crashDir, fileName)
            file.writeText(report)

            // 최신 10개 파일만 유지
            val files = crashDir.listFiles() ?: return
            if (files.size > 10) {
                files.sortedBy { it.lastModified() }
                    .take(files.size - 10)
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log to file: ${e.message}")
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    /**
     * 최근 크래시 리포트 내용 조회 (진단용)
     */
    fun getRecentCrashLogs(context: Context, maxCount: Int = 3): List<String> {
        return try {
            val crashDir = File(context.filesDir, "crash_logs")
            if (!crashDir.exists()) return emptyList()
            crashDir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.take(maxCount)
                ?.map { it.readText() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
