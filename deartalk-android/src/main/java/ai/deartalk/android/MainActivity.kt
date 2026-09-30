package ai.deartalk.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import ai.deartalk.android.agent.IntentResult
import ai.deartalk.android.data.pref.DearTalkSettings
import ai.deartalk.android.data.pref.UiStrings
import ai.deartalk.android.ime.ui.theme.DearTalkTheme
import ai.deartalk.android.ui.main.MainScreen
import ai.deartalk.android.ui.main.MainUiEvent
import ai.deartalk.android.ui.main.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 📱 DearTalk AI 메인 설정 및 온보딩 액티비티 (Clean Architecture View)
 * - 비즈니스 로직과 UI 상태는 MainViewModel 및 MainScreen(MVI)에 100% 위임
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val testLocale = intent?.getStringExtra("test_locale")
        if (!testLocale.isNullOrBlank()) {
            UiStrings.setLocale(Locale.forLanguageTag(testLocale))
        } else {
            UiStrings.setLocale(DearTalkSettings.getEffectiveLocale(this))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        handleTestIntent(intent)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(MainUiEvent.RefreshImeStatus)
            }
        })

        setContent {
            DearTalkTheme {
                val uiState by viewModel.uiState.collectAsState()
                MainScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onEnableIme = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onSelectIme = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    onOpenLive = {
                        val launchIntent = packageManager.getLaunchIntentForPackage("ai.deartalk.translator")
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        } else {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=ai.deartalk.translator")))
                            } catch (e: Throwable) {
                                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=ai.deartalk.translator")))
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val testLocale = intent.getStringExtra("test_locale")
        if (!testLocale.isNullOrBlank()) {
            UiStrings.setLocale(Locale.forLanguageTag(testLocale))
        }
        handleTestIntent(intent)
    }

    private fun handleTestIntent(intent: Intent?) {
        val testPrompt = intent?.getStringExtra("test_prompt")
        if (!testPrompt.isNullOrBlank()) {
            android.util.Log.d("DearTalkAI", "🧪 [테스트 프롬프트 수신]: '$testPrompt'")
            lifecycleScope.launch {
                val res = viewModel.intentEngine.process(testPrompt, "", "ai.deartalk.android.adb_test")
                when (res) {
                    is IntentResult.Success -> {
                        android.util.Log.d("DearTalkAI", "🎯 [AI 변환 성공]: '${res.text}' (${res.message})")
                    }
                    is IntentResult.Error -> {
                        android.util.Log.e("DearTalkAI", "⚠️ [AI 에러]: ${res.error}")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.sttManager.cancelListening()
    }
}
