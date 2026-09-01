# Changelog

All notable changes to **DearTalk AI (Android IME)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.4] - 2026-09-01

### Added
- **On-Device AI Voice Studio & Real-time Interpretation (`VoiceStudioActivity`)**:
  - Independent full-screen activity isolating memory-intensive STT/LLM/TTS operations from IME keyboard process.
  - **2-Way Language Selector & 1-Tap Swap**: `[ 🗣️ Spoken Input ] ⇄ [ 🌐 Target Output ]` architecture with one-touch reverse conversation direction.
  - **Zero Hardcoding Dynamic Prompt Engine**: Context-driven simultaneous interpreter prompt supporting real-time translation across 12 languages.
  - **Acoustic Model Language Binding**: Dynamically passes target `Locale` to `SpeechRecognitionManager` to eliminate acoustic recognition crosstalk.
  - **Voice Customizer & Pitch Control**: Female/Male voice selector and 4-tier pitch controls (`Normal`, `Deep Low`, `Warm Mid`, `Bright High`).
  - **Zero-Latency Audio Replay (`speakDirectly`)**: Direct TTS replay without LLM re-inference on speaker tap.
- **Hardware & RAM Diagnostic System (`SystemDiagnosticManager`)**:
  - Pre-flight 3-tier RAM & storage evaluation (🟢 Optimal 8GB+, 🟡 Caution/Sequential 6GB, 🔴 Constrained <=4GB).
- **Play Asset Delivery (PAD) On-Demand Model Lifecycle (`ModelLifecycleManager`)**:
  - Google Play standard On-Demand asset state machine with live progress tracking and 1-click package purge.

### Fixed
- **Locale-Aware Standard Keyboard Default Layout**: Dynamically selects standard keyboard layout based on active locale in `StandardKeyboardView.kt` (Hangul 2-set for Korean, standard Latin QWERTY for non-Korean locales).
- **Adaptive Key Labels**: Updated symbol return buttons (`ABC` vs `한글`) and language toggle chips (`ENG` vs `KOR`) to reactively match the active locale.
- **Speech Recognizer Keep-Alive**: Prevents premature voice cancellation during continuous speaking with RMS volume separation.

---

## [1.0.3] - 2026-08-30

### Fixed
- **Contextual Punctuation & Question Mark (`?`) Completion**: Enhanced on-device SLM Korean prompt instructions and few-shot examples to intelligently attach question marks (`?`), exclamation marks (`!`), and periods (`.`) based on speech context (e.g., schedule inquiry, opinion checking, gratitude) without artificial string manipulation (`Zero Fake Rules` compliant).

---

## [1.0.2] - 2026-08-30

### Fixed
- **Multilingual STT Code-Switching**: Removed `EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE` and enabled `en-US` as an auxiliary language in `SpeechRecognitionManager.kt`, resolving the issue where English words (e.g., "AI", "PPT", "API") in Korean mixed speech were distorted into Korean phonetic transliterations.
- **Proper Noun & Acronym LLM Principles**: Updated on-device SLM prompt instructions to strictly follow standard terminology and proper noun formatting rules without artificial string replacement hacks (`Zero Fake Rules` compliant).

### Added
- **Mixed-Language Detection & Fallback Unit Tests**: Added automated regression unit tests in `DearTalkIntentEngineTest.kt` verifying language classification and raw input preservation.

---

## [1.0.1] - 2026-08-29

### Fixed
- **Zero-Latency Reactive i18n**: Refactored `UiStrings` to Jetpack Compose `mutableStateOf` state, eliminating stale static UI labels and ensuring 0ms instant text updates for `[Apply]`, `[Cancel]`, and `[Space]` buttons upon language switching in settings.
- **Voice Button Label Truncation**: Streamlined voice recording button label from 22 characters (`🔴 듣고 있어요...`) to compact 12 characters (`듣는 중 (터치 시 완료)` / `Listening (Tap to finish)` / `Mendengarkan (Ketuk selesai)`), preventing text truncation across mobile and foldable screens.
- **Cheeky Tone Alignment**: Synchronized `[😼 당당하게]` (Cheeky) button label and prompt intent matching across `DearTalkIntentEngine.kt`, `UiStrings.kt`, and tri-lingual READMEs.

### Changed
- **Pure 6-Tone IME Bar**: Removed redundant `[🌐 번역 ▾]` dropdown menu from the keyboard top bar, establishing a clean and responsive 6 tone presets chip bar.

---

## [1.0.0] - 2026-08-29

### Added
- **Initial Open Source Release of DearTalk AI**: 100% on-device AI Android Custom Keyboard (IME) powered by Google Gemma 2B via LiteRT GPU.
- **Android 15 & Target SDK 35 Ready**: Upgraded build toolchains to `compileSdk = 35` and `targetSdk = 35` with complete edge-to-edge Compose rendering.
- **6 Unified Tone Presets**: `✨ Refine`, `👔 Polite`, `😊 Casual`, `💼 Business`, `🤣 Humorous`, and `😼 Cheeky`.
- **Zero Fake Rules Invariant**: Strict fallback guarantees preserving 100% of raw user input without mock string hacks.
- **Offline Speech-to-Text (STT)**: Instant offline voice transcription with raw audio preservation across tone switches.
- **0ms Optimistic UI & Keyboard Switcher**: Instant visual feedback and quick one-tap switching to Samsung/Gboard keyboards.
- **Dynamic Multilingual Prompts**: Dynamic prompt generator for Indonesian (Bahasa Indonesia), Japanese, Spanish, Korean, and English.
- **Pre-PR Automated Testing Suite (`verify.sh`)**: Integrated zero-friction test launcher with automated `.venv` auto-provisioning, JVM unit testing, and real-device Monkey stability & memory leak auditing.
- **Play Asset Delivery (PAD) Model Architecture**: Official Google Play asset delivery (`install-time` / `fast-follow`) and local ADB sideloading support.
- **Tri-lingual Documentation Standard**: Pristine `README.md` (English), `README.ko.md` (Korean), and `README.id.md` (Indonesian) with a single English SSOT for internal technical specifications.
