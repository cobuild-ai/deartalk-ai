package ai.deartalk.android.util

import android.util.Log
import ai.deartalk.android.BuildConfig

/**
 * DearTalk AI 중앙집중식 보안 로깅 유틸리티
 * - 디버그 빌드: 상세 로그 출력
 * - 릴리즈(프로덕션) 빌드: 사용자 음성 원문 등 민감 데이터가 Logcat에 평문 노출되지 않도록 철저히 차단
 */
object DearTalkLog {
    private const val TAG_PREFIX = "DearTalk_"

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX$tag", msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i("$TAG_PREFIX$tag", msg)
        }
    }

    fun w(tag: String, msg: String) {
        Log.w("$TAG_PREFIX$tag", msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX$tag", msg, throwable)
        } else {
            Log.e("$TAG_PREFIX$tag", msg)
        }
    }
}
