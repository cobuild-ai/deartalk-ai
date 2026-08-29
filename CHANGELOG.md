# Changelog

All notable changes to **DearTalk AI (Android IME)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
