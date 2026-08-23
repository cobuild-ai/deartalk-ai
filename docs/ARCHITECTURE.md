# 🏛️ DearTalkAI System Architecture

This document provides a comprehensive technical overview of **DearTalkAI**'s cross-platform architecture, data flow, and on-device inference lifecycle.

---

## 🎯 Architecture Diagram

```mermaid
graph TD
    subgraph Client Layer
        A1[Android Compose IME] --> B1[Hangul Composer & STT]
        A2[macOS AppKit & SwiftUI] --> B2[AXUIElement Text Monitor]
    end

    subgraph Core Engine Layer
        B1 --> C[DearTalkIntentEngine]
        B2 --> C
        C --> D[CustomToneManager 6 Presets]
        C --> E[DiffEngine LCS Matrix]
    end

    subgraph Hardware Accelerated On-Device AI
        C --> F1[Android: Google LiteRT NPU/GPU]
        C --> F2[macOS: Apple Silicon Metal GPU]
        F1 --> G[(Gemma 2B Quantized Model)]
        F2 --> G
    end

    subgraph Output Pipeline
        E --> H1[Android: 2-Line Diff Canvas]
        E --> H2[macOS: Floating Overlay + AX Injection]
    end
```

---

## 🔄 End-to-End Execution Flow (macOS & Android)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant HostApp as 📱 Host App (Slack / KakaoTalk)
    participant Monitor as 🔍 Accessibility Monitor / IME
    participant Engine as 🧠 DearTalkIntentEngine
    participant LLM as ⚡ Local Neural Network (Metal/LiteRT)
    participant Diff as 📊 DiffEngine (LCS)
    participant Overlay as ✨ Floating Diff UI

    User->>HostApp: Types or Speaks "내일 아침 9시 만나"
    HostApp->>Monitor: Text change detected (Debounce 300ms)
    Monitor->>Engine: processWithTone(text, selectedTone)
    Engine->>LLM: Ingest System Prompt + Few-shots + Raw Text
    LLM-->>Engine: Returns "내일 오전 9시에 뵙겠습니다."
    Engine->>Diff: computeWordDiff(original, suggested)
    Diff-->>Overlay: Emits DiffResult (Unchanged, Added, Removed)
    Overlay->>User: Displays 2-Line Live Diff Panel
    User->>Overlay: Presses [Tab] or clicks [✅ Apply]
    Overlay->>HostApp: Directly replaces text via AXUIElement / InputConnection
```

---

## 🧩 Core Subsystems

### 1. `DearTalkIntentEngine`
- **Role:** Centralized orchestrator for local LLM inference across platforms.
- **Inference Backends:**
  - **macOS:** Apple Silicon Metal GPU hardware acceleration via `llama-server` on port 11435.
  - **Android:** Google LiteRT GPU delegate (`ai.deartalk.android.agent.LiteRtEngine`).
- **Safety Invariant (The 1st Principle):** When the model is loading or unavailable, it **honestly preserves 100% of the original text** without appending fake grammatical hacks.

### 2. `DiffEngine` (Longest Common Subsequence)
- **Role:** Word/token-level diff calculator that breaks sentences into `.unchanged`, `.removed`, and `.added` operations.
- **Complexity:** $O(N \times M)$ DP matrix with empty-token guards and consecutive operation merging.

### 3. `AccessibilityMonitor` & `TextReplacementService` (macOS)
- **Role:** Queries focused elements across macOS applications using `AXUIElementCopyAttributeValue` and translates global Top-Left coordinates to Cocoa Bottom-Left space.
- **Awakening Mechanism:** Emits `AXEnhancedUserInterface = true` to activate lazy accessibility trees in Electron/Chromium apps (VS Code, Antigravity IDE, Slack, Chrome).

### 4. `UiStrings` (Multilingual Engine)
- **Role:** Automatically determines whether to render Korean (`ko`) or English fallback based on system preferences (`Locale.preferredLanguages` / `Locale.getDefault()`).

---

## 📚 Related Documentation
- [Long-Term Technology Roadmap](ROADMAP.md)
- [On-Device Model Compatibility Specifications](MODELS.md)
- [Cross-Platform Testing Specifications](TESTING.md)
