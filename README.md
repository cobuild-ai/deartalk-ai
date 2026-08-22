# ✨ DearTalkAI: 100% On-Device AI Communication Assistant

<div align="center">

[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#-android-keyboard)
[![Platform: macOS](https://img.shields.io/badge/Platform-macOS-000000?logo=apple&logoColor=white)](#-macos-floating-assistant)
[![AI: Google Gemma On-Device](https://img.shields.io/badge/LLM-Gemma%20On--Device-4285F4?logo=google&logoColor=white)](#-core-principles)
[![Zero Network](https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero%20Network)-success)](#-privacy--security-guarantee)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI Status](https://img.shields.io/badge/CI-Passing-brightgreen)](https://github.com/smilelife/deartalk-ai/actions)

**DearTalkAI** is an open-source, privacy-first, 100% on-device AI communication assistant available as an **Android Custom Keyboard (IME)** and a **macOS System-Wide Floating Assistant**.

It refines typed text and spoken voice (STT) with real-time, context-aware tone adjustments, typo correction, and multilingual translations — powered strictly by local on-device neural networks without any network connection.

[Key Features](#-key-platform-features) • [Before & After Samples](#-tone-transformation-samples) • [Architecture](docs/ARCHITECTURE.md) • [10-Year Roadmap](docs/ROADMAP.md) • [Model Specs](docs/MODELS.md) • [Contributing](CONTRIBUTING.md)

</div>

---

## 📊 Platform Release Status

| Platform | Current Version | Status | Primary Features |
| :--- | :---: | :---: | :--- |
| 🤖 **Android** | `v1.3.0` | **Stable (Production)** | Custom Keyboard (IME), Offline STT, 6 Tones, In-App Model Downloader |
| 🍏 **macOS** | `v1.3.0` | **Stable (Production)** | Floating Overlay, Metal GPU, AXUIElement, 1-Click Runtime Installer |
| 📱 **iOS** | `feat/ios-app` | **In Development** | Core Engine & SwiftUI Sandbox (Physical Device Profiling in Progress) |

---

## 🌟 Key Platform Features

### 📱 Android Keyboard (`deartalk-android`)
- **Offline Speech-to-Text (STT):** Instant offline voice recognition directly inside the keyboard.
- **6 Unified Tone Presets:** `✨ Refine`, `👔 Polite`, `😊 Casual`, `💼 Business`, `🤣 Humorous`, and `😼 Cheeky`.
- **Raw STT Preservation:** Multiple tone changes keep the original voice input intact while dynamically swapping AI outputs.
- **Compose-based Slate UI:** Modern dark glassmorphism keyboard with dedicated settings, test receiver, and haptic feedback.
- **In-App 1-Click Model Setup:** Automatic download and reactive state management for Gemma LiteRT models.

### 🍏 macOS Floating Assistant (`deartalk-apple/DearTalk-macOS`)
- **System-Wide Accessibility Overlay:** Detects focused text fields across KakaoTalk, Chrome, Antigravity/VS Code, Slack, and Notes.
- **Metal GPU Hardware Acceleration:** Sub-100ms real-time neural inference powered by Apple Silicon GPU.
- **3-Step Environment Diagnosis & 1-Click Installer:** Automatically diagnoses Gemma GGUF model files, Metal runtime (`llama-server`), and Homebrew, offering one-click automated background installation.
- **2-Line Live Diff Layout:** Line 1 shows immutable original text, Line 2 highlights real-time word-level diff badges.
- **Interactive Control Toolbar:** One-click Tone chips, Copy (`📋`), Regenerate (`🔄`), Apply (`✅`), and Tab Key completion toggle (`⇥`).
- **Raycast-Style Onboarding Wizard:** Seamless accessibility setup with automatic TCC stale permission cleaner.

---

## 🎭 Tone Transformation Samples

| Tone Preset | Original Input (원문) | AI Refined Output (AI 제안문) |
|---|---|---|
| **✨ Refine (기본다듬기)** | 내일 아침 9시 만나 | 내일 아침 9시에 만나요. |
| **👔 Polite (공손하게)** | 식사 같이 하실래요? | 혹시 식사 함께 하실 수 있으실까요? |
| **😊 Casual (친근하게)** | 지금 어디야? | 지금 어디쯤이야? 😊 |
| **💼 Business (비즈니스)** | 자료 정리해서 보냈어 확인해 | 요청하신 업무 자료 송부해 드렸으니 확인 부탁드립니다. |
| **🤣 Funny (재미있게)** | 밥 먹으러 가자 | 밥 먹으러 안 가면 유죄! 같이 맛있는 거 먹으러 가요 🤣 |
| **😼 Cheeky (건방지게)** | 오늘 나랑 놀자 | 오늘 시간 비워둬, 내가 특별히 만나줄 테니까 😼 |

---

## 🔄 How It Works

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant HostApp as 📱 App (Slack / Chrome / Notes)
    participant Monitor as 🔍 Accessibility Monitor / IME
    participant Engine as 🧠 DearTalkIntentEngine
    participant LLM as ⚡ Local Neural Network (Metal/LiteRT)
    participant Diff as 📊 DiffEngine (LCS)
    participant Overlay as ✨ Floating Diff UI

    User->>HostApp: Types "내일 아침 9시 만나"
    HostApp->>Monitor: Text change detected (Debounce 300ms)
    Monitor->>Engine: processWithTone(text, tone)
    Engine->>LLM: Ingest System Prompt + Few-shots + Raw Text
    LLM-->>Engine: Returns "내일 오전 9시에 뵙겠습니다."
    Engine->>Diff: computeWordDiff(original, suggested)
    Diff-->>Overlay: Emits DiffResult (Unchanged, Added, Removed)
    Overlay->>User: Displays 2-Line Live Diff Panel
    User->>Overlay: Presses [Tab] or clicks [✅ Apply]
    Overlay->>HostApp: Directly replaces text via AXUIElement / InputConnection
```

---

## 🔒 Privacy & Security Guarantee

1. **Zero Fake Hardcoding (The 1st Principle):**
   - No `if/else`, substring matching, or regex-based grammatical hacks. All context, tones, and corrections are generated strictly through on-device LLM inference (Google Gemma).
2. **100% On-Device Privacy (Zero Network):**
   - 0% external network traffic. Keystrokes, voice audio, and text never leave the user's device.
3. **Engineering Transparency:**
   - Honest error handling and status indicators instead of fallback mock replies.

---

## 📁 Repository Structure

```
deartalk-ai/
├── .github/                    # CI/CD workflows and issue/PR templates
│   ├── workflows/build.yml     # Automated Android & macOS test/build pipeline
│   └── ISSUE_TEMPLATE/         # Bug report & feature request templates
│
├── deartalk-android/           # Android Keyboard Application (Kotlin, Jetpack Compose, LiteRT)
│   ├── src/main/java/ai/deartalk/android/
│   │   ├── agent/              # On-device Gemma LLM engine & prompt templates
│   │   ├── ime/                # Keyboard InputMethodService & Hangul automata
│   │   ├── data/               # SQLite repo with auto-pruning & preferences
│   │   └── stt/                # Offline SpeechRecognizer Manager
│   └── src/test/               # Common core JVM unit tests
│
├── deartalk-apple/
│   └── DearTalk-macOS/         # macOS Menu Bar & Floating Assistant (Swift, SwiftUI, AppKit)
│       ├── Sources/DearTalkMacCore/
│       │   ├── Core/           # Accessibility monitor, Llama Metal engine, DiffEngine
│       │   └── UI/             # FloatingDiffOverlayView, OnboardingGuideView, SandboxView
│       ├── Sources/DearTalkMacRunner/ # Standalone core verification test runner
│       └── scripts/            # Standalone .app packaging & codesigning scripts
│
└── docs/                       # Architecture diagrams, testing guides, and daily journals
    ├── ARCHITECTURE.md         # System architecture and data flow specifications
    └── TESTING.md              # Cross-platform symmetric testing standards
```

---

## 🚀 Quick Start & Build

### 📱 Android
```bash
# Run unit tests
./gradlew :deartalk-android:testDebugUnitTest

# Build and install on connected Android device (USB or Wi-Fi ADB)
./gradlew :deartalk-android:installDebug
```

### 🍏 macOS
```bash
# Run core verification test runner
cd deartalk-apple/DearTalk-macOS
swift run DearTalkMacRunner

# Package standalone .app bundle
bash scripts/package_macos_app.sh

# Launch DearTalk macOS
open build/DearTalk.app
```

---

## 🤝 Contributing & Community

We warmly welcome community contributions! Please read our:
- [Contributing Guide](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Testing Guide](docs/TESTING.md)
- [Security Policy](SECURITY.md)

---

## 📄 License
This project is licensed under the **Apache 2.0 License** - see the [LICENSE](LICENSE) file for details.
