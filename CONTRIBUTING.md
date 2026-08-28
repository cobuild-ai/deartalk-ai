# Contributing to DearTalkAI

Thank you for your interest in contributing to **DearTalkAI**! We welcome contributions from the community to help make 100% on-device AI communication accessible, respectful, and transparent for everyone.

---

## 🚨 Core Rules to Keep in Mind (The Zero Fake Rule)

Before writing any code, please review our inviolable principle in [GEMINI.md](GEMINI.md):
1. **Zero Fake Hardcoding:** Never write regex, string replacement, or heuristic rule hacks that mimic AI. All contextual transformations, tone alignments, and typo corrections must strictly rely on on-device neural network (LLM) inference.
2. **100% On-Device Privacy (Zero Network):** No network requests are allowed during inference or text processing. All models run strictly on local CPU/GPU/NPU.
3. **Engineering Transparency:** Always handle errors honestly instead of generating fake fallback text.

---

## 🛠️ Development Setup

### Android (`deartalk-android`)
- **IDE:** Android Studio Ladybug or later / VS Code
- **JDK:** OpenJDK 17+
- **Build & Test:**
  ```bash
  ./gradlew :deartalk-android:testDebugUnitTest
  ./gradlew :deartalk-android:assembleDebug
  ```


---

## 🎯 Good First Issues & Contribution Areas

| Difficulty | Area | Description | Target Files |
| :--- | :--- | :--- | :--- |
| 🟢 **Easy** | Localization | Add new translation target languages or improve bilingual UI texts | `UiStrings.kt` |
| 🟢 **Easy** | UI / Theming | Enhance dark mode styling, animations, or keyboard haptics | `DearTalkScreen.kt` |
| 🟡 **Medium**| Prompt Engineering | Optimize few-shot examples in system prompts for 6 tone presets | `DearTalkIntentEngine.kt` |
| 🟡 **Medium**| Diff Engine | Improve word-level tokenization boundary and performance | `DiffEngine.kt` |
| 🔴 **Hard** | Hardware Acceleration | Optimize LiteRT NPU delegates or GPU context caching | `LiteRtEngine.kt` |

---

## 📝 Commit Message Guidelines (Conventional Commits)

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` New feature or capability
- `fix:` Bug fix or crash resolution
- `docs:` Documentation changes
- `style:` Code style / formatting (no logic changes)
- `refactor:` Refactoring code without behavior alteration
- `test:` Adding or updating tests
- `chore:` Maintenance, dependency updates, packaging scripts

---

## 📬 Pull Request Workflow

1. Fork the repository and create your branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Ensure all unit tests and platform runners pass 100%:
   ```bash
   # Android Tests
   ./gradlew :deartalk-android:testDebugUnitTest
   ```
3. Commit your changes following Conventional Commits.
4. Push to your fork and submit a Pull Request describing your changes, motivation, and test verification.
