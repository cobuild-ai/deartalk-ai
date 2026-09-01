package ai.deartalk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.agent.SequentialVoicePipeline
import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.data.SystemDiagnosticManager
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkTheme
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.tts.TextToSpeechManager
import ai.deartalk.android.voicestudio.ui.VoiceStudioScreen

/**
 * 🎙️ DearTalk AI Voice Studio Activity
 * - IME 키보드 프로세스와 메모리를 물리적으로 격리하여 고성능 음성 통역/고운말 파이프라인 안전 구동
 * - Clean Code SRP: Activity는 생명주기 관리 및 의존성 조립에만 집중
 */
class VoiceStudioActivity : ComponentActivity() {

    private lateinit var diagnosticManager: SystemDiagnosticManager
    private lateinit var modelLifecycleManager: ModelLifecycleManager
    private lateinit var intentEngine: DearTalkIntentEngine
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var voicePipeline: SequentialVoicePipeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전역 다국어 로케일 동기화
        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))

        // 컴포넌트 초기화
        diagnosticManager = SystemDiagnosticManager(this)
        modelLifecycleManager = ModelLifecycleManager(this)
        intentEngine = DearTalkIntentEngine(this)
        ttsManager = TextToSpeechManager(this)
        sttManager = SpeechRecognitionManager(this)
        voicePipeline = SequentialVoicePipeline(
            context = this,
            intentEngine = intentEngine,
            ttsManager = ttsManager,
            modelLifecycleManager = modelLifecycleManager
        )

        setContent {
            DearTalkTheme {
                VoiceStudioScreen(
                    diagnosticManager = diagnosticManager,
                    modelLifecycleManager = modelLifecycleManager,
                    sttManager = sttManager,
                    voicePipeline = voicePipeline,
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePipeline.releaseMemory()
        sttManager.destroy()
        ttsManager.shutdown()
    }
}
