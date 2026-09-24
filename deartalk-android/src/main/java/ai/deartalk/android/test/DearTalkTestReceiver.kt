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
        when (intent.action) {
            "ai.deartalk.android.TEST_INFERENCE" -> {
                val prompt = intent.getStringExtra("prompt") ?: "내일 9시에 만나 공손하게 바꿔줘"
                Log.d("DearTalkAI", "🧪 [ADB Broadcast 일반 추론 테스트 시작]: '$prompt'")
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
            "ai.deartalk.android.TEST_TONE" -> {
                val prompt = intent.getStringExtra("prompt") ?: "내일 9시에 만나"
                val toneName = intent.getStringExtra("tone") ?: "공손하게"
                val tone = ai.deartalk.android.data.pref.CustomToneManager.DEFAULT_TONES.find { it.name == toneName || it.id == toneName }
                    ?: ai.deartalk.android.data.pref.CustomTone(id = "tone_polite", name = "공손하게", instruction = "공손한 경어체")
                
                Log.d("DearTalkAI", "🧪 [ADB Broadcast 톤앤매너 테스트 시작]: '$prompt' (어조: ${tone.name})")
                val engine = DearTalkIntentEngine(context)

                scope.launch {
                    val result = engine.processWithTone(
                        voiceInput = prompt,
                        tone = tone,
                        packageName = "ai.deartalk.android.test"
                    )

                    when (result) {
                        is IntentResult.Success -> {
                            Log.d("DearTalkAI", "🎉 [ADB 톤 변환 검증 성공]: '$prompt' ➔ '${result.text}' (화행: ${result.detectedIntent}, 메시지: ${result.message})")
                        }
                        is IntentResult.Error -> {
                            Log.e("DearTalkAI", "❌ [ADB 톤 변환 실패]: ${result.error}")
                        }
                    }
                }
            }
        }
    }
}
