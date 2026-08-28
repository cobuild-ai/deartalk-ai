# CHANGELOG

## [1.4.0] - 2026-08-28

### Changed
- **Platform Specialization (Android Focused)**: Refocused repository scope exclusively on the Android on-device IME application (`deartalk-android`).
- **Clean CI/CD Pipeline**: Streamlined GitHub Actions workflow to run Android Gradle build and unit tests only.
- **Documentation & Testing Alignment**: Updated architecture, test guides, and contribution guidelines for Android.

## [1.3.0] - 2026-08-22

### Added
- **Unified 6 Tone Presets**: Integrated `✨ Refine`, `👔 Polite`, `😊 Casual`, `💼 Business`, `🤣 Humorous`, and `😼 Cheeky` into Compose keyboard UI.
- **Offline Speech-to-Text (STT)**: Direct offline voice transcription preserving raw voice input across tone shifts.

## [1.2.0] - 2026-08-22

### Added
- **Version & Build Info**: Added short version string (SemVer) and dynamic build timestamp retrieval in Android settings.
- **On-Device User Guides (About/Help)**: Designed and implemented localized User Guide (도움말) screens in Android app info card block.
- **Verification Assertions**: Added unit test assertions to `UiStringsLocaleTest` and `CommonCoreEngineTest` validating localizations.

## [1.1.0] - 2026-08-22

### Added
- **Android Model Downloader Implementation**: Added `ModelDownloader.kt` using `HttpURLConnection` and `Coroutine StateFlow` to support in-app one-click downloading of LiteRT Gemma models (GPU Int4).
- **Android Reactive Model Flow**: Added `isModelLoadedFlow` to `DearTalkIntentEngine` to dynamically update Compose UI when on-device LLM completes loading.
- **One-click Download Cards UI**: Integrated card-based download/install/refresh UI panels in Android `MainActivity.kt`.
- **Integrated Unit Tests**: Added coverage tests in `DearTalkIntentEngineTest.kt` for asynchronous model reloading and downloader cancellation.

### Fixed
- Fixed Compose recomposition sync bugs related to local AI engine loading state.
