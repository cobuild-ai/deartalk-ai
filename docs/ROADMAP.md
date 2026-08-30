# 🗺️ DearTalkAI Long-Term Technology Roadmap (Android)

This document outlines the architectural vision, sustainable development principles, and upcoming release milestones for **DearTalkAI Android IME**.

---

## 🎯 Core Mission & Inviolable Principles

DearTalkAI exists to provide **100% on-device, zero-network, honest AI communication assistance** directly within the Android keyboard ecosystem:
- **Zero Fake Heuristics:** No string hacks or mock rules. All intelligence is 100% neural network inference via Google Gemma.
- **Absolute Privacy (0% Network):** Keystrokes, voice audio, and text never leave the device.
- **Engineering Transparency:** Honest states, no fabricated fallback text.

---

## 📍 Release Milestones

```
┌────────────────────────────┐    ┌────────────────────────────┐    ┌────────────────────────────┐
│ Milestone 1 (v1.0.3 Stable)│───►│  Milestone 2 (In Progress) │───►│   Milestone 3 (Next Gen)   │
└────────────────────────────┘    └────────────────────────────┘    └────────────────────────────┘
  - Android 15 & Target SDK 35      - Voice-to-Action (Agents)        - Multi-modal Vision Input
  - 100% On-Device Gemma 2B         - Dynamic Tool Calling            - Local Memory Graph
  - Offline Multilingual STT        - NPU Hardware Acceleration       - Zero-Latency Streaming
```

### ✅ Milestone 1: Foundation (Current - v1.0.3 Stable)
- [x] **Android IME Application (`deartalk-android`):**
  - Android 15 & Target SDK 35 compliance, Jetpack Compose modular UI architecture.
  - Offline Speech-to-Text (STT), LiteRT GPU on-device Gemma inference, 6 unified tone presets.
  - Full multilingual support (Korean, English, Indonesian, Japanese, Spanish) with dynamic prompt generation.
  - Automated real-device Monkey stability & memory leak verification suite (`verify.sh`).
  - Pre-PR quality gates guaranteeing zero crashes, zero ANRs, and zero secret leaks.

---

### 🚧 Milestone 2: Production Polish & Form Factors
- [ ] **Play Asset Delivery (PAD) Production Optimization:**
  - Automated fast-follow asset pack downloading and verification through Google Play Core.
- [ ] **Tablet & Foldable Layout Adaptations:**
  - Split keyboard mode for foldables (Galaxy Z Fold series) and large-screen Android tablets.
- [ ] **Custom Tone Builder UI:**
  - In-app custom prompt editor allowing users to craft and save personalized tone presets.

---

### 🧠 Milestone 3: Next-Gen On-Device SLM (Small Language Models)
- [ ] Support for lightweight MoE (Mixture of Experts) and Gemma 3B quantizations.
- [ ] Direct NPU acceleration delegate (Qualcomm QNN / MediaTek Neuropilot).
- [ ] Multi-turn conversational context caching with strict in-memory zero-persistence guarantees.

---

## 🤝 Contributing to the Roadmap

We welcome community feedback and proposals! If you want to contribute to an upcoming milestone or propose a new feature, please open an issue or start a discussion.
