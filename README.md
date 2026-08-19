# ✨ DearTalkAI: 100% On-Device AI Communication Assistant

<div align="center">

[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#-android-keyboard)
[![Platform: macOS](https://img.shields.io/badge/Platform-macOS-000000?logo=apple&logoColor=white)](#-macos-floating-assistant)
[![AI: Google Gemma On-Device](https://img.shields.io/badge/LLM-Gemma%20On--Device-4285F4?logo=google&logoColor=white)](#-core-principles)
[![Zero Network](https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero%20Network)-success)](#-privacy--security)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DearTalkAI** is a privacy-first, 100% on-device AI communication assistant available as an **Android Keyboard (IME)** and a **macOS System-Wide Floating Assistant**.

It refines speech (STT) and typed text with real-time, context-aware tone adjustments, typo correction, and multilingual translations — powered purely by local on-device neural networks without any network connection.

</div>

---

## 🌟 Key Platform Features

### 📱 Android Keyboard (`deartalk-android`)
- **Offline Speech-to-Text (STT):** Instant offline voice recognition directly inside the keyboard.
- **6 Unified Tone Presets:** `✨ Refine`, `👔 Polite`, `😊 Casual`, `💼 Business`, `🤣 Humorous`, and `😼 Cheeky`.
- **Raw STT Preservation:** Multiple tone changes keep the original voice input intact while dynamically swapping AI outputs.
- **Compose-based Slate UI:** Modern dark glassmorphism keyboard with dedicated settings, test receiver, and haptic feedback.

### 🖥️ macOS Floating Assistant (`deartalk-apple/DearTalk-macOS`)
- **System-Wide Accessibility Overlay:** Detects focused text fields across KakaoTalk, Chrome, Antigravity/VS Code, Slack, and Notes.
- **Metal GPU Hardware Acceleration:** Sub-100ms real-time inference via Apple Silicon GPU.
- **2-Line Live Diff Layout:** Line 1 shows immutable original text, Line 2 highlights real-time word-level diff badges.
- **Interactive Control Toolbar:** One-click Tone chips, Copy (`📋`), Regenerate (`🔄`), Apply (`✅`), and Tab Key completion toggle (`⇥`).
- **Raycast-Style Onboarding Wizard:** Seamless accessibility setup with automatic TCC stale permission cleaner.

---

## 🔒 Core Principles (Zero Fake Rules)

1. **Zero Fake Hardcoding:**
   - No `if/else`, substring matching, or regex-based grammatical hacks. All context, tones, and corrections are generated strictly through on-device LLM inference (Google Gemma).
2. **100% On-Device Privacy (Zero Network):**
   - 0% external network traffic. Keystrokes, voice audio, and text never leave the user's device.
3. **Engineering Transparency:**
   - Honest error handling and status indicators instead of fallback mock replies.

---

## 📁 Architecture Overview

```
deartalk-ai/
├── deartalk-android/           # Android Keyboard Application (Kotlin, Jetpack Compose, LiteRT)
│   ├── src/main/java/ai/deartalk/android/
│   │   ├── agent/              # On-device Gemma LLM engine & prompt templates
│   │   ├── ime/                # Keyboard InputMethodService & Hangul automata
│   │   ├── data/               # SQLite repo with auto-pruning & preferences
│   │   └── stt/                # Offline SpeechRecognizer Manager
│   └── build.gradle.kts
│
├── deartalk-apple/
│   └── DearTalk-macOS/         # macOS Menu Bar & Floating Assistant (Swift, SwiftUI, AppKit)
│       ├── Sources/DearTalkMacCore/
│       │   ├── Core/           # Accessibility monitor, Llama Metal engine, DiffEngine
│       │   └── UI/             # FloatingDiffOverlayView, OnboardingGuideView, SandboxView
│       └── scripts/            # Standalone .app packaging & codesigning scripts
│
└── docs/                       # Daily development journals and technical specs
```

---

## 🚀 Getting Started

### Android
```bash
# Build and install on connected Android device (USB or Wi-Fi ADB)
./gradlew :deartalk-android:installDebug
```

### macOS
```bash
# Package standalone .app bundle
cd deartalk-apple/DearTalk-macOS
bash scripts/package_macos_app.sh

# Launch DearTalk macOS
open build/DearTalk.app
```

---

## 📄 License
This project is licensed under the Apache 2.0 License.
