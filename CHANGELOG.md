# CHANGELOG

## [1.3.0] - 2026-08-22

### Added
- **macOS On-Device AI Runtime Setup & Diagnosis (`RuntimeSetupManager`)**: Added automated 3-step environment diagnosis detecting Gemma GGUF model files, Metal GPU runtime (`llama-server`/`llama.cpp`), Homebrew (`brew`), and local daemon health (Port 11435).
- **1-Click llama.cpp Auto-Installer**: Enabled one-click background installation of `llama.cpp` via Homebrew with real-time status reporting and auto engine hot-reloading.
- **Interactive Diagnosis UI**: Integrated diagnostic card with 1-click model download, runtime install, terminal command copying (`brew install llama.cpp`), and self-healing refresh in macOS `SandboxView` and `SettingsView`.
- **macOS Runner Verification**: Added environment diagnosis test suite in `DearTalkMacRunner`.

## [1.2.0] - 2026-08-22

### Added
- **Multiplatform Version & Build Info**: Added short version string (SemVer) and dynamic build timestamp retrieval across macOS, iOS, and Android platforms.
- **On-Device User Guides (About/Help)**: Designed and implemented localized User Guide (도움말) screens in macOS Settings tab view, iOS settings navigation screen, and Android app info card block.
- **Settings Access from Sandbox**: Added gear icon button to iOS Sandbox view for easy navigation to settings and guide.
- **Verification Assertions**: Added unit test assertions to macOS `DearTalkMacRunner`, iOS `DearTalkIOSRunner`, and Android `UiStringsLocaleTest` validating the new localizations.

## [1.1.0] - 2026-08-22

### Added
- **Android Model Downloader Implementation**: Added `ModelDownloader.kt` using `HttpURLConnection` and `Coroutine StateFlow` to support in-app one-click downloading of LiteRT Gemma models (GPU Int4).
- **iOS Model Downloader Enhancements**: Extended `ModelDownloader.swift` to support default download URL configuration, automated file management (directory moving), and auto-initialization hooks.
- **Android Reactive Model Flow**: Added `isModelLoadedFlow` to `DearTalkIntentEngine` to dynamically update Compose UI when on-device LLM completes loading.
- **One-click Download Cards UI**: Integrated card-based download/install/refresh UI panels in both Android `MainActivity.kt` and iOS `IOSSandboxView.swift`.
- **Integrated Unit Tests**: Added coverage tests in `DearTalkIntentEngineTest.kt` for asynchronous model reloading and downloader cancellation.

### Fixed
- Fixed Compose recomposition sync bugs related to local AI engine loading state.
