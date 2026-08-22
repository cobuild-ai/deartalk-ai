# 🗺️ DearTalkAI 10-Year Technology Roadmap

This document outlines the long-term vision, architectural evolution, and upcoming release milestones for **DearTalkAI**.

---

## 🎯 Core Mission & Inviolable Principles

DearTalkAI exists to provide **100% on-device, zero-network, honest AI communication assistance** across all modern computing platforms. We strictly adhere to our core principle in [[GEMINI.md]](file:///Users/smilelife/Projects/deartalk-ai/GEMINI.md):
- **Zero Fake Heuristics:** No string hacks or mock rules. All intelligence is 100% neural network inference.
- **Absolute Privacy (0% Network):** Keystrokes and voice never leave the device.
- **Engineering Transparency:** Honest states, no fabricated fallback text.

---

## 📍 Release Milestones

```
┌────────────────────────────┐    ┌────────────────────────────┐    ┌────────────────────────────┐
│ Milestone 1 (v1.3.0 Stable)│───►│  Milestone 2 (In Progress) │───►│   Milestone 3 (Next Gen)   │
│  - Android Keyboard (IME)  │    │  - iOS Physical App & IME  │    │  - Cross-Platform Shared   │
│  - macOS Floating Assistant│    │  - `feat/ios-app` Branch   │    │  - Linux & Windows Support │
└────────────────────────────┘    └────────────────────────────┘    └────────────────────────────┘
```

### ✅ Milestone 1: Foundation (Current - v1.3.0 Stable)
- [x] **Android IME Application (`deartalk-android`):**
  - Jetpack Compose dark-glass UI, Hangul automata, offline Speech-to-Text (STT).
  - LiteRT GPU on-device Gemma inference, 6 unified tone presets, one-click model downloader.
- [x] **macOS Floating Assistant (`deartalk-apple/DearTalk-macOS`):**
  - Global AXUIElement accessibility monitor with 2-line live token diff overlay.
  - Apple Silicon Metal GPU acceleration (`llama-server`), Tab auto-completion.
  - 3-step on-device environment diagnosis & 1-click Homebrew `llama.cpp` auto-installer.
- [x] **Automated 2-Way Gatekeepers:** Automated CI test suite ensuring 100% build integrity on every PR.

---

### 🚧 Milestone 2: iOS Ecosystem Integration (`feat/ios-app` branch)
> 📌 *Note: In accordance with our production-quality standards, iOS source code is actively developed in the dedicated [`feat/ios-app`](https://github.com/smilelife/deartalk-ai/tree/feat/ios-app) branch until physical device memory (Jetsam OOM) and hardware profiling are fully verified.*

- [x] iOS Core Package & Verification Runner (`DearTalkIOSRunner`)
- [x] SwiftUI Sandbox view, custom tone manager, and multilingual resources
- [ ] **Physical Device Profiling:**
  - Strict RAM footprint profiling (<1.8GB) on iPhone hardware (A16/A17/A18 Bionic).
  - Background memory management to guarantee 0% Jetsam OOM crashes.
- [ ] **App Extension Keyboard Bridge:**
  - App Groups shared container memory mapping for iOS Custom Keyboard Extension.
- [ ] **Merge into `main`:** Full release and App Store / TestFlight distribution.

---

### 🔮 Milestone 3: Shared Core & Platform Expansion (`deartalk-shared`)
- [ ] **Core Engine Abstraction:**
  - Extract LCS Diff algorithm, Prompt Builder, and Tokenizer into a unified native core (`deartalk-shared` via Rust / Kotlin Multiplatform).
- [ ] **Desktop Linux Support:**
  - Wayland / X11 input method engine integration (IBus / Fcitx5) with Vulkan acceleration.
- [ ] **Windows Support:**
  - DirectML GPU acceleration and Windows App SDK floating overlay.

---

### 🧠 Milestone 4: Next-Gen On-Device SLM (Small Language Models)
- [ ] Support for lightweight MoE (Mixture of Experts) and Gemma 3B quantizations (Q2_K / Q3_K / Q4_K_M).
- [ ] Dynamic quantization switching based on device battery and thermal state.
- [ ] Multi-turn context caching without disk persistence.

---

## 🤝 Contributing to the Roadmap

We welcome community feedback and proposals! If you want to contribute to an upcoming milestone or propose a new feature, please open an issue or start a discussion.
