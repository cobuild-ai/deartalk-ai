# 🧪 DearTalkAI Cross-Platform Testing Guide

This document defines the **Symmetric Testing Standard** for DearTalkAI across Android and macOS platforms.

---

## 🎯 The 5 Core Verification Principles

Every core feature must pass the identical set of test scenarios on both platforms:

| # | Test Scenario | Description | Target Function |
|---|---|---|---|
| 1 | **Tone Presets Matrix** | Verifies all 6 tone presets (`refine`, `polite`, `casual`, `business`, `funny`, `cheeky`) are defined with exact IDs and icons. | `CustomToneManager` |
| 2 | **Zero Fake Rules Preservation** | Verifies that unhandled inputs or missing models never append fake hardcoded suffixes, preserving 100% of raw user text. | `DearTalkIntentEngine.process` |
| 3 | **Empty & Whitespace Safety** | Verifies that blank inputs (`"   "`, `""`) return cleanly without throwing exceptions or crashing. | `DearTalkIntentEngine.process` |
| 4 | **Special Symbol Robustness** | Verifies that single identifiers and symbols (`@smilelife`, `#tag`) pass through DIFF and tokenization without range crashes. | `DiffEngine.computeWordDiff` |
| 5 | **LLM Tag Cleansing** | Verifies that `<start_of_turn>model\nLabel: ...<end_of_turn>` tags and labels are stripped cleanly. | `cleanLlmOutput` |

---

## 🚀 Running Tests

### 📱 Android
Run the full Android JVM test suite:
```bash
./gradlew :deartalk-android:testDebugUnitTest
```

### 🍏 macOS
Run the unified macOS Core Verification Runner:
```bash
cd deartalk-apple/DearTalk-macOS
swift run DearTalkMacRunner
```

---

## ✍️ Adding a New Tone Preset

When contributing a new tone preset:
1. Add the enum/ID in both `CustomToneManager.kt` and `CustomToneManager.swift`.
2. Provide bilingual names and instructions in `UiStrings.kt` and `UiStrings.swift`.
3. Add corresponding few-shot prompts in `DearTalkIntentEngine`.
4. Update `CommonCoreEngineTest.kt` and `DearTalkMacRunner/main.swift` to assert the new preset count and IDs.
