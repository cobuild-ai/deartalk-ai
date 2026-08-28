# 🗺️ DearTalkAI Long-Term Technology Roadmap

This document outlines the architectural vision, sustainable development principles, and upcoming release milestones for **DearTalkAI**.

---

## 🎯 Core Mission & Inviolable Principles

DearTalkAI exists to provide **100% on-device, zero-network, honest AI communication assistance**. We strictly adhere to our core principles in [GEMINI.md](file:///Users/smilelife/Projects/OSSProject/01-production/deartalk-ai/GEMINI.md):
- **Zero Fake Heuristics:** No string hacks or mock rules. All intelligence is 100% neural network inference.
- **Absolute Privacy (0% Network):** Keystrokes and voice never leave the device.
- **Engineering Transparency:** Honest states, no fabricated fallback text.

---

## 📍 Release Milestones

```
┌────────────────────────────┐    ┌────────────────────────────┐    ┌────────────────────────────┐
│ Milestone 1 (v1.3.0 Stable)│───►│  Milestone 2 (In Progress) │───►│   Milestone 3 (Next Gen)   │
│  - Android Keyboard (IME)  │    │  - On-Device Quantizations │    │  - Cross-Platform Expansion│
│  - Offline STT & 6 Tones   │    │  - LiteRT NPU Acceleration │    │  - macOS & iOS Release     │
└────────────────────────────┘    └────────────────────────────┘    └────────────────────────────┘
```

### ✅ Milestone 1: Android IME Foundation (Current - v1.3.0 Stable)
- [x] **Android IME Application (`deartalk-android`):**
  - Jetpack Compose dark-glass UI, Hangul automata, offline Speech-to-Text (STT).
  - LiteRT GPU on-device Gemma inference, 6 unified tone presets, one-click model downloader.
- [x] **Automated CI Gatekeepers:** Automated CI test suite ensuring 100% build integrity on every PR.
- [x] **Google Play Store Release:** Active distribution and data safety compliance.

---

### 🚧 Milestone 2: On-Device Performance & Model Optimization
- [ ] **NPU Acceleration:**
  - QualComm Hexagon & MediaTek APU delegate optimization for LiteRT.
- [ ] **Dynamic Quantization:**
  - Support for Gemma 2B Int4/Int8 dynamic switching based on thermal and battery state.
- [ ] **Context Memory Optimization:**
  - Multi-turn conversation context caching with zero disk persistence.

---

### 🔮 Milestone 3: Cross-Platform Expansion (Future)
- [ ] **macOS System-Wide Floating Assistant:**
  - Apple Silicon Metal GPU acceleration and Accessibility integration.
- [ ] **iOS Custom Keyboard:**
  - iOS Custom Keyboard Extension and memory management.
- [ ] **Desktop Linux Support:**
  - Wayland / X11 input method engine integration (IBus / Fcitx5).

---

## 🤝 Contributing to the Roadmap

We welcome community feedback and proposals! If you want to contribute to an upcoming milestone or propose a new feature, please open an issue or start a discussion.
