# 🧪 DearTalkAI Automated Testing & Verification Guide (Android)

This document defines the **Comprehensive Verification Standard** for DearTalkAI Android Custom Keyboard (IME), covering JVM unit tests and automated real-device Monkey stress testing with on-device SLM memory leak auditing (`verify.sh`).

---

## 🎯 1. The 5 Core Verification Principles

Every core feature must strictly pass these five foundational scenarios:

| # | Test Scenario | Description | Target Subsystem |
|---|---|---|---|
| 1 | **Tone Presets Matrix** | Verifies all 6 tone presets (`refine`, `polite`, `casual`, `business`, `funny`, `cheeky`) are registered with valid IDs and icons. | `CustomToneManager` |
| 2 | **Zero Fake Rules Invariant** | Verifies that unhandled inputs or loading models never append hardcoded string suffixes, preserving 100% of raw user text. | `DearTalkIntentEngine.process` |
| 3 | **Empty & Whitespace Safety** | Verifies that blank inputs (`"   "`, `""`) return cleanly without throwing exceptions or crashing. | `DearTalkIntentEngine.process` |
| 4 | **Special Symbol Robustness** | Verifies that single identifiers and symbols (`@smilelife`, `#tag`) pass through DIFF and tokenization without range crashes. | `DiffEngine.computeWordDiff` |
| 5 | **LLM Tag Cleansing** | Verifies that `<start_of_turn>model\nLabel: ...<end_of_turn>` tags and labels are stripped cleanly. | `cleanLlmOutput` |

---

## 🚀 2. Quick Start: Zero-Friction Verification Suite (`verify.sh`)

We provide an automated verification launcher (`verify.sh`) and standardized `Makefile` with zero external pip dependencies and automatic Python virtual environment (`.venv`) provisioning:

### Interactive Menu
```bash
# Launch interactive numbered menu
./verify.sh
```

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 🧪 DearTalk-AI Automated Testing & Verification Suite 
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1) 🚀 All-in-One Verification (Unit Tests + Real-Device Audit)
  2) 🧪 JVM Unit Tests (make test)
  3) 📱 Real-Device Stability & Stress Audit (make verify-device)
  4) 🔨 Build Debug APK (make build)
  5) 📦 Build Release Bundle AAB (make release)
  6) 🔍 Kotlin Static Lint (make lint)
  q) Quit
────────────────────────────────────────────────────────────
▶ Select action [1-6, q]:
```

### Direct CLI Commands
```bash
# 🚀 1. Run All-in-One Pre-PR Verification (Recommended)
make verify            # or ./verify.sh all

# 🧪 2. Run JVM Unit Tests Only (< 2 seconds)
make test              # or ./verify.sh unit

# 📱 3. Run Real-Device Stability & Memory Leak Audit
make verify-device     # or ./verify.sh device

# 📱 4. Run Real-Device Audit with Custom Flags
./verify.sh device --events 1000 --throttle 20 --device 192.168.1.136:35859
```

---

## 📱 3. Android Verification Suites

### A. JVM Fast Unit Tests (`make test`)
Executes the full suite of unit tests verifying Hangul automata, prompt generation, few-shots, and token diff calculations in < 2 seconds:
```bash
./gradlew :deartalk-android:testDebugUnitTest
```

### B. Automated Real-Device Stress & Memory Leak Audit (`make verify-device`)
For an on-device AI custom keyboard (IME), crashes and memory leaks are unacceptable. The automated tester executes a **5-step live audit** on connected USB or Wi-Fi Android devices:

```
[1/5] 🚀 Clean Launch & Warm-up
      • Ensures debug APK is installed (auto-installs if missing)
      • Force-stops previous instance and launches MainActivity
      • Waits for on-device Gemma LiteRT model mapping into RAM

[2/5] 📊 Baseline RAM Footprint
      • Measures initial Total PSS, Java Heap, and Native Heap via dumpsys meminfo

[3/5] 🐒 High-Speed Monkey Stress Engine
      • Injects 500 randomized touches (60%), gestures (20%), and nav events (20%) at 30ms intervals

[4/5] 🚨 Logcat Crash & ANR Audit
      • Scans Logcat buffer for FATAL EXCEPTION, AndroidRuntime errors, or ANR freezes

[5/5] 📈 Post-Stress Memory & Leak Calculation
      • Re-measures RAM and verifies that garbage collection recovers memory with zero native leaks
```

#### Pass / Fail Thresholds

| Metric | Pass Threshold | Typical Healthy Range | Assessment |
| :--- | :---: | :---: | :--- |
| **App Crashes** | **0 (Strict)** | 0 | 1+ crash fails immediately |
| **ANRs / Freezes** | **0 (Strict)** | 0 | 1+ ANR fails immediately |
| **Java Heap** | **< 80.0 MB** | 12 MB ~ 25 MB | Compose UI & STT memory |
| **RAM Growth Delta** | **< 120.0 MB** | -60 MB ~ +80 MB | Compose EGL graphics swap buffers |
| **Native Heap (SLM)** | **Stable** | ~1.0 GB | Gemma LiteRT model weights |

#### Sample Passing Output
```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 📋 STABILITY & PERFORMANCE VERIFICATION REPORT 
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Device Under Test:   192.168.1.136:35859
  • Events Injected:      500 actions
  • App Crashes (0%):     0 (Clean)
  • ANRs / Hangs (0%):    0 (Smooth)
  • Memory Status:        Stable (No Leak)
  • Final RAM Footprint:  2070.11 MB
────────────────────────────────────────────────────────────────────
🎉 [PASSED] DearTalk AI Passed Real-Device Automated Stability Audit!
```

---

## ✍️ 4. Adding New Tone Presets & Test Coverage

When contributing a new tone preset:
1. Add the enum/ID in `CustomToneManager.kt`.
2. Provide multilingual names and instructions in `UiStrings.kt`.
3. Add corresponding few-shot prompts in `DearTalkIntentEngine.kt`.
4. Update `CommonCoreEngineTest.kt` to assert the new preset count and IDs.
5. Verify via `./verify.sh all`.
