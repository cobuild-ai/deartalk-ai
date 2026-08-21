# CHANGELOG

## [1.1.0] - 2026-08-22

### Added
- **Android Model Downloader Implementation**: Added `ModelDownloader.kt` using `HttpURLConnection` and `Coroutine StateFlow` to support in-app one-click downloading of LiteRT Gemma models (GPU Int4).
- **iOS Model Downloader Enhancements**: Extended `ModelDownloader.swift` to support default download URL configuration, automated file management (directory moving), and auto-initialization hooks.
- **Android Reactive Model Flow**: Added `isModelLoadedFlow` to `DearTalkIntentEngine` to dynamically update Compose UI when on-device LLM completes loading.
- **One-click Download Cards UI**: Integrated card-based download/install/refresh UI panels in both Android `MainActivity.kt` and iOS `IOSSandboxView.swift`.
- **Integrated Unit Tests**: Added coverage tests in `DearTalkIntentEngineTest.kt` for asynchronous model reloading and downloader cancellation.

### Fixed
- Fixed Compose recomposition sync bugs related to local AI engine loading state.
