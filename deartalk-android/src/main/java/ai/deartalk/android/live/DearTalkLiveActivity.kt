package ai.deartalk.android.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.deartalk.android.agent.DearTalkIntentEngine
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkTheme
import ai.deartalk.android.live.data.LiveSessionRepository
import ai.deartalk.android.live.ui.DearTalkLiveScreen
import ai.deartalk.android.stt.SpeechRecognitionManager
import ai.deartalk.android.tts.TextToSpeechManager

/**
 * 🎙️ DearTalk Live Activity
 * - 1:1 실시간 대면 대화 및 오프라인 지능형 보이스 레코더 전용 액티비티
 * - Clean Code SRP: Activity는 생명주기 관리 및 의존성 조립에만 집중
 */
class DearTalkLiveActivity : ComponentActivity() {

    private lateinit var intentEngine: DearTalkIntentEngine
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var sttManager: SpeechRecognitionManager
    private lateinit var repository: LiveSessionRepository
    private lateinit var controller: DearTalkLiveController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전역 다국어 로케일 동기화
        UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))

        // 컴포넌트 초기화
        intentEngine = DearTalkIntentEngine(this)
        ttsManager = TextToSpeechManager(this)
        sttManager = SpeechRecognitionManager(this)
        repository = LiveSessionRepository(this)

        controller = DearTalkLiveController(
            context = this,
            repository = repository,
            intentEngine = intentEngine,
            ttsManager = ttsManager,
            sttManager = sttManager
        )

        // 언어 기본값 초기화 (내 앱 언어 기반)
        val myAppLang = if (UiStrings.isKo) "KO" else if (UiStrings.isId) "ID" else "EN"
        val partnerAppLang = if (UiStrings.isKo) "EN" else "KO"
        controller.setLanguages(my = myAppLang, partner = partnerAppLang)

        setContent {
            DearTalkTheme {
                DearTalkLiveScreen(
                    controller = controller,
                    onBackClick = { finish() }
                )
            }
        }

        handleTestIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTestIntent(intent)
    }

    /**
     * 🧪 [AI 에이전트/ADB 자동 검증 인터페이스]
     * --es test_speaker "ME|PARTNER" --es test_text "..." 수신 시
     * 온디바이스 SLM 번역 ➔ SQLite 적재 ➔ UI 렌더링 ➔ TTS 발화를 원스톱 실행
     */
    private fun handleTestIntent(intent: android.content.Intent?) {
        val speakerStr = intent?.getStringExtra("test_speaker") ?: return
        val rawText = intent.getStringExtra("test_text") ?: return
        if (rawText.isBlank()) return

        val speaker = when (speakerStr.uppercase()) {
            "ME" -> ActiveSpeaker.ME
            "PARTNER" -> ActiveSpeaker.PARTNER
            else -> ActiveSpeaker.NONE
        }

        if (speaker != ActiveSpeaker.NONE) {
            android.util.Log.d("DearTalkLive", "🧪 [테스트 대화 인젝션]: 화자=$speaker, 발화='$rawText'")
            controller.injectVoiceResult(speaker, rawText)
        }
    }

    override fun onPause() {
        super.onPause()
        controller.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.destroy()
        sttManager.destroy()
        ttsManager.shutdown()
    }
}
