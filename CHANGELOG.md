# Changelog

All notable changes to **DearTalk AI (Android IME)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.10] - 2026-09-10

### Fixed
- **Cheonjiin Automata Crash Resolution**: Eliminated `ArrayIndexOutOfBoundsException: length=21; index=-2` in `CheonjiinComposer.kt` by strictly bounding `jung` index to `0..20` and safely handling intermediate araea (`"ㆍ"`, `"ㆍㆍ"`) transitions.
- **IME Keystroke Circuit Breaker**: Wrapped all keyboard input event handlers with `runCatching` to prevent keyboard service termination and ensure instant self-healing on unexpected edge cases.

### Changed
- **Samsung-Style Cheonjiin Minimal Layout**: Removed legacy explanatory subtexts ("사람", "하늘", "땅", "ㄲ", "ㄸ") for a clean, distraction-free 18sp Korean 3x4 keypad mirroring genuine Samsung Cheonjiin.

### Added
- **Global Punctuation Access (Bottom Bar)**: Integrated permanent comma (`,`) and period (`.`) keys directly on the bottom common bar across all keyboards (`[!#1] [KOR/ENG] [,] [Space] [.] [Enter]`).
- **Rich 2-Page Special Symbols (`SymbolKeyboardLayout`)**: Expanded symbol keyboard into a 2-page pagination system (`1/2` and `2/2`) providing full punctuation (`.`, `,`, `?`, `!`), currencies (`₩`, `$`, `€`, `£`), math operators (`≠`, `≤`, `≥`, `÷`, `×`), and brackets.
- **Enterprise Crash Diagnostics (`CrashLogger`)**: Registered `DearTalkApplication` with a custom `UncaughtExceptionHandler` that writes persistent diagnostic crash dumps to `files/crash_logs/` for fail-safe post-mortem debugging.

---

## [1.0.9] - 2026-09-10

### Added
- **Voice Studio Sticky Bottom Bar**: Mic button now permanently fixed at screen bottom via `Scaffold(bottomBar)` + `navigationBarsPadding()`, eliminating system navigation bar overlap.
- **Mode Switch Auto Re-translation**: Switching between Tone Transform and Live Interpretation modes now automatically re-processes existing text through the new pipeline.
- **Model Purge Confirmation Dialog**: `AlertDialog` safety prompt before deleting on-device AI model package (~1.2GB).
- **Download Error Retry Button**: `OutlinedButton` with refresh icon in Error state, enabling immediate re-download without app restart.
- **Quick Translation Sample Sentences**: Mode-specific sample chips for both Tone Transform and Live Interpretation modes (3 languages: KO/EN/ID).

### Changed
- **UX Copy Refinement**: Replaced technical terminology "발화 음색(發話 音色)" → "🎙️ 목소리 설정" and "톤 매칭" → "🎚️ 목소리 톤:" for user-friendly language.
- **On-Device SLM Engine**: Standardized to `Qwen 0.5B Nano` across all UI labels and model detection paths.

### Fixed
- **Recording Button Label Wrapping**: Added `maxLines = 1` + `TextOverflow.Ellipsis` to prevent forced line breaks on narrow screens or large font accessibility settings.

### Accessibility
- **TalkBack Semantics**: Added `selectable(selected, role = Role.Tab)` to gender toggle and mode tabs, `Role.RadioButton` to pitch chips, and `semantics { role = Role.Button }` to main mic button for screen reader support.

---

## [1.0.8] - 2026-09-03

### Added
- **Human-Centered 2-Slot Utterance Cache (`AppScopedUtteranceCache`)**:
  - Implemented an app-scoped in-memory sliding cache based on human working memory limits (`maxApps = 2`: Primary Focus + Secondary Toggled App).
  - Eliminates subject/pronoun omission ("3박 4일 일정은요?" -> "발리 3박 4일 일정은요?") and homophone typos by injecting prior conversation context into SLM prompts.
  - Zero-Allocation Circular Ring Buffer (`CircularUtteranceBuffer`): Overwrites slots in-place to ensure zero GC overhead, zero keyboard jitter, and fixed memory footprint under 500 bytes.
  - 3-minute TTL auto-expiration and pure RAM residency for strict Google Play privacy compliance.

### Refactored
- **Zero Language Bias in Context Injection**:
  - Uniformly injected localized conversation context blocks across Korean, Indonesian, and English/global prompts.
  - Propagated target app `packageName` through `processIntent` and `applyTone` across all voice pipeline flows.
  - Cleaned prototype legacy parameter naming (`simulatedVoiceText` -> `voiceText`).

---

## [1.0.7] - 2026-09-02

### Fixed
- **Speech Recognition Infinite Sound Loop Prevention**:
  - Resolved `SpeechRecognitionManager` defect where `ERROR_NO_MATCH` and `ERROR_SPEECH_TIMEOUT` triggered an infinite rapid keep-alive reconnection loop and system beep noise cycle on silence.
  - Safely falls back to `VoiceState.FinalResult` when prior recognized text exists, or cleanly transitions to `VoiceState.Idle`.
- **Samsung One UI System Navigation Bar & Keyboard Switch Overlap**:
  - Added `.navigationBarsPadding()` and 8.dp bottom safety padding to `DearTalkScreen` and `StandardKeyboardView` root surfaces.
  - Completely eliminates touch collision between IME control buttons (`Clear`, `Backspace`) and Samsung Galaxy gesture handles or system keyboard switch icons.

---

## [1.0.6] - 2026-09-02

### Changed
- **Target SDK 36 (Android 16) Full Compliance**:
  - Upgraded `compileSdk` and `targetSdk` to API 36 to meet Google Play 2026 platform security and performance requirements.

---

## [1.0.5] - 2026-09-02

### Added
- **1-Tap Partner Language Pair UX (`TargetLanguageSelectorRow`)**:
  - Replaced legacy 2-way input/output dropdowns with a minimalist single-tap partner language selector.
  - Automatically binds primary language to device/app system locale and forms bi-directional conversation pairs (`🇰🇷 한국어 ⇄ 🇺🇸 English`).
- **Smart Bi-Directional Auto-Swap (`LanguageLocaleHelper.detectLanguageCode`)**:
  - Automatically identifies script types (Hangul, Latin/English, Japanese Kana, Chinese Hanzi, Thai) in voice speech and dynamically swaps interpretation direction without requiring manual button presses.
- **Script-Aware Multi-language TTS Engine**:
  - Dynamically binds the synthesized voice engine to match the actual script of the generated translation text for pristine vocal accuracy.
- **Unified High-Performance Qwen Sharing & Adaptive AI Tiers (`ActiveAiTier`)**:
  - Shared downloaded Qwen 1.7B Instruct model across Keyboard IME and Voice Studio.
  - Added 3-tier intelligence state machine (`HIGH_QWEN`, `BASE_GEMMA`, `STT_ONLY`) with zero-failure pure STT support on budget devices.

### Fixed
- **Multilingual Tone Instructions & Pipeline Error Localization**:
  - Converted tone instructions and pipeline exception messages into localized `UiStrings` properties across EN, KO, and ID.

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
