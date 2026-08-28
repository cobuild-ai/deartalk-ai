# 🏛️ DearTalkAI System Architecture

This document provides a comprehensive technical overview of **DearTalkAI**'s Android on-device AI keyboard architecture, data flow, and inference lifecycle.

---

## 🎯 Architecture Diagram

```mermaid
graph TD
    subgraph Client Layer
        A[Android Jetpack Compose IME] --> B[Hangul Composer & Offline STT]
    end

    subgraph Core Engine Layer
        B --> C[DearTalkIntentEngine]
        C --> D[CustomToneManager 6 Presets]
        C --> E[DiffEngine LCS Matrix]
    end

    subgraph Hardware Accelerated On-Device AI
        C --> F[Android: Google LiteRT GPU Delegate]
        F --> G[(Gemma 2B Quantized Model)]
    end

    subgraph Output Pipeline
        E --> H[Android: Compose Slate Keyboard & Live Diff View]
        H --> I[Android InputConnection Commit]
    end
```

---

## 🔄 End-to-End Execution Flow (Android IME)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant HostApp as 📱 Host App (KakaoTalk / Chrome / Notes)
    participant IME as ⌨️ DearTalk Keyboard (IME)
    participant Engine as 🧠 DearTalkIntentEngine
    participant LLM as ⚡ Local Neural Network (LiteRT GPU)
    participant Diff as 📊 DiffEngine (LCS)

    User->>IME: Types or Speaks "내일 아침 9시 만나"
    IME->>Engine: processWithTone(text, selectedTone)
    Engine->>LLM: Ingest System Prompt + Few-shots + Raw Text
    LLM-->>Engine: Returns "내일 오전 9시에 뵙겠습니다."
    Engine->>Diff: computeWordDiff(original, suggested)
    Diff-->>IME: Emits DiffResult (Unchanged, Added, Removed)
    IME->>User: Renders Slate Glass UI with Tone Chips & Live Preview
    User->>IME: Taps Suggested Text or Selects Different Tone
    IME->>HostApp: Commits refined text via Android InputConnection
```

---

## 🧩 Core Subsystems

### 1. `DearTalkIntentEngine`
- **Role:** Centralized orchestrator for local LLM inference.
- **Inference Backend:** Google LiteRT GPU delegate (`ai.deartalk.android.agent.LiteRtEngine`).
- **Safety Invariant (The 1st Principle):** When the model is loading or unavailable, it **honestly preserves 100% of the original text** without appending fake grammatical hacks.

### 2. `DiffEngine` (Longest Common Subsequence)
- **Role:** Word/token-level diff calculator that breaks sentences into `.unchanged`, `.removed`, and `.added` operations.
- **Complexity:** $O(N \times M)$ DP matrix with empty-token guards and consecutive operation merging.

### 3. `DearTalkIME` & `HangulComposer`
- **Role:** Android `InputMethodService` implementation providing custom Compose keyboard with real-time on-device tone suggestions.
- **Hangul Automata:** 2-Bulsik vowel/consonant composition and decomposition engine handling syllable formation and backspace boundaries.
- **In-App Model Lifecycle:** `ModelDownloader` dynamically loads Gemma `.litertlm` into RAM with GPU acceleration.

---

## 📚 Related Documentation
- [Android Google Play Deployment Guide](ANDROID_DEPLOYMENT_GUIDE.md)
- [Long-Term Technology Roadmap](ROADMAP.md)
- [On-Device Model Compatibility Specifications](MODELS.md)
- [Android Testing Specifications](TESTING.md)
