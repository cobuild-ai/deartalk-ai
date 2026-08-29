package ai.deartalk.android.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.IntentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DearTalkTestReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ai.deartalk.android.TEST_INFERENCE") {
            val prompt = intent.getStringExtra("prompt") ?: "내일 9시에 만나 공손하게 바꿔줘"
            Log.d("DearTalkAI", "🧪 [ADB Broadcast 테스트 시작]: '$prompt'")
            val engine = DearTalkIntentEngine(context)

            scope.launch {
                val result = engine.process(
                    voiceInput = prompt,
                    currentEditorText = "",
                    packageName = "ai.deartalk.android.test"
                )

                when (result) {
                    is IntentResult.Success -> {
                        Log.d("DearTalkAI", "🎉 [ADB 온디바이스 Gemma LLM 추론 성공]: '$prompt' ➔ '${result.text}' (메시지: ${result.message})")
                    }
                    is IntentResult.Error -> {
                        Log.e("DearTalkAI", "❌ [ADB 테스트 실패]: ${result.error}")
                    }
                }
            }
        }
    }
}
