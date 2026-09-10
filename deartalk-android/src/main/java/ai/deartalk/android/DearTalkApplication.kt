package ai.deartalk.android

import android.app.Application
import ai.deartalk.android.crash.CrashLogger

class DearTalkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 키보드 앱 전역 크래시 로거 및 모니터링 초기화
        CrashLogger.init(this)
    }
}
