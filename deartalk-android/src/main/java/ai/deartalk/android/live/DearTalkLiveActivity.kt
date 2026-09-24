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

import ai.deartalk.android.data.ModelLifecycleManager
import ai.deartalk.android.data.SystemDiagnosticManager

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
    private lateinit var modelLifecycleManager: ModelLifecycleManager
    private lateinit var diagnosticManager: SystemDiagnosticManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 실시간 대면 통역 중 화면 꺼짐 방지
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 전역 다국어 로케일 동기화 (테스트 인텐트 오버라이드 지원)
        val testLocale = intent?.getStringExtra("test_locale")
        if (!testLocale.isNullOrBlank()) {
            DearTalkSettings.setAutoLanguage(this, false)
            DearTalkSettings.setSelectedLanguageCode(this, testLocale)
            UiStrings.setLocale(java.util.Locale(testLocale))
        } else {
            UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))
        }

        // 컴포넌트 초기화
        intentEngine = DearTalkIntentEngine(this)
        ttsManager = TextToSpeechManager(this)
        sttManager = SpeechRecognitionManager(this)
        repository = LiveSessionRepository(this)
        modelLifecycleManager = ModelLifecycleManager(this)
        diagnosticManager = SystemDiagnosticManager(this)

        controller = DearTalkLiveController(
            context = this,
            repository = repository,
            intentEngine = intentEngine,
            ttsManager = ttsManager,
            sttManager = sttManager
        )

        // 언어 기본값 초기화 (내 앱 언어 기반 혹은 테스트 인텐트 오버라이드)
        val testMy = intent?.getStringExtra("test_my_lang")
        val testPartner = intent?.getStringExtra("test_partner_lang")
        val myAppLang = testMy ?: if (UiStrings.isKo) "KO" else if (UiStrings.isId) "ID" else "EN"
        val partnerAppLang = testPartner ?: if (UiStrings.isKo) "EN" else "KO"
        if (intent?.getBooleanExtra("test_clear_session", false) == true || testMy != null || testPartner != null) {
            controller.createNewSession(myAppLang, partnerAppLang)
        } else {
            controller.setLanguages(my = myAppLang, partner = partnerAppLang)
        }

        val autoStartDownload = intent?.getBooleanExtra("auto_start_download", false) ?: false
        val openSettings = intent?.getBooleanExtra("open_settings", false) ?: false
        val flipView = intent?.getBooleanExtra("flip_view", false) ?: false

        setContent {
            DearTalkTheme {
                DearTalkLiveScreen(
                    controller = controller,
                    modelLifecycleManager = modelLifecycleManager,
                    diagnosticManager = diagnosticManager,
                    intentEngine = intentEngine,
                    autoStartDownload = autoStartDownload,
                    openSettings = openSettings,
                    initialFlipView = flipView,
                    onBackClick = { finish() }
                )
            }
        }

        handleTestIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val testLocale = intent.getStringExtra("test_locale")
        if (!testLocale.isNullOrBlank()) {
            DearTalkSettings.setAutoLanguage(this, false)
            DearTalkSettings.setSelectedLanguageCode(this, testLocale)
            UiStrings.setLocale(java.util.Locale(testLocale))
        }
        val testMy = intent.getStringExtra("test_my_lang")
        val testPartner = intent.getStringExtra("test_partner_lang")
        if (testMy != null || testPartner != null) {
            val myAppLang = testMy ?: controller.myLang
            val partnerAppLang = testPartner ?: controller.partnerLang
            if (intent.getBooleanExtra("test_clear_session", false)) {
                controller.createNewSession(myAppLang, partnerAppLang)
            } else {
                controller.setLanguages(my = myAppLang, partner = partnerAppLang)
            }
        } else if (intent.getBooleanExtra("test_clear_session", false)) {
            controller.createNewSession(controller.myLang, controller.partnerLang)
        }
        handleTestIntent(intent)
    }

    /**
     * 🧪 [AI 에이전트/ADB 자동 검증 인터페이스]
     * --es test_speaker "ME|PARTNER" --es test_text "..." 수신 시
     * 온디바이스 SLM 번역 ➔ SQLite 적재 ➔ UI 렌더링 ➔ TTS 발화를 원스톱 실행
     */
    private fun handleTestIntent(intent: android.content.Intent?) {
        val speakerStr = intent?.getStringExtra("test_speaker") ?: return

        // 🌟 사후 의도 재작성 테스트 인텐트 지원
        val rephraseIntentStr = intent.getStringExtra("test_rephrase_intent")
        if (!rephraseIntentStr.isNullOrBlank()) {
            val rephraseSpeaker = when (speakerStr.uppercase()) {
                "PARTNER" -> ActiveSpeaker.PARTNER
                else -> ActiveSpeaker.ME
            }
            val rephraseIntent = when (rephraseIntentStr.uppercase()) {
                "QUESTION" -> ai.deartalk.android.live.data.SpeechIntent.QUESTION
                "STATEMENT" -> ai.deartalk.android.live.data.SpeechIntent.STATEMENT
                "REQUEST" -> ai.deartalk.android.live.data.SpeechIntent.REQUEST
                "CONFIRM" -> ai.deartalk.android.live.data.SpeechIntent.CONFIRM
                else -> ai.deartalk.android.live.data.SpeechIntent.AUTO
            }
            android.util.Log.d("DearTalkLive", "🧪 [테스트 사후 의도 재작성]: 화자=$rephraseSpeaker, 변경인텐트=$rephraseIntent")
            controller.setIntentByUser(rephraseSpeaker, rephraseIntent)
            return
        }

        val rawText = intent.getStringExtra("test_text") ?: return
        if (rawText.isBlank()) return

        val speaker = when (speakerStr.uppercase()) {
            "ME" -> ActiveSpeaker.ME
            "PARTNER" -> ActiveSpeaker.PARTNER
            else -> ActiveSpeaker.NONE
        }

        if (speaker != ActiveSpeaker.NONE) {
            val testRefined = intent.getStringExtra("test_refined_text")
            if (!testRefined.isNullOrBlank()) {
                val isDraft = intent.getBooleanExtra("test_is_draft", false)
                android.util.Log.d("DearTalkLive", "📸 [테스트 완성 메시지 인젝션]: 화자=$speaker, 원문='$rawText', 번역='$testRefined'")
                controller.insertCompletedTestMessage(speaker, rawText, testRefined, isDraft)
                return
            }

            val intentStr = intent.getStringExtra("test_intent")
            if (!intentStr.isNullOrBlank()) {
                val speechIntent = when (intentStr.uppercase()) {
                    "QUESTION" -> ai.deartalk.android.live.data.SpeechIntent.QUESTION
                    "STATEMENT" -> ai.deartalk.android.live.data.SpeechIntent.STATEMENT
                    "REQUEST" -> ai.deartalk.android.live.data.SpeechIntent.REQUEST
                    "CONFIRM" -> ai.deartalk.android.live.data.SpeechIntent.CONFIRM
                    else -> ai.deartalk.android.live.data.SpeechIntent.AUTO
                }
                if (speaker == ActiveSpeaker.ME) {
                    controller.myIntent = speechIntent
                } else {
                    controller.partnerIntent = speechIntent
                }
            }
            android.util.Log.d("DearTalkLive", "🧪 [테스트 대화 인젝션]: 화자=$speaker, 인텐트=${controller.myIntent}, 발화='$rawText'")
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
