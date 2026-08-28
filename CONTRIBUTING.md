# Contributing to DearTalkAI

Thank you for your interest in contributing to **DearTalkAI**! We welcome contributions from the community to help make 100% on-device AI communication accessible, respectful, and transparent for everyone.

---

## 🚨 Core Rules to Keep in Mind (The Zero Fake Rule)

Before writing any code, please review our inviolable principles in [GEMINI.md](GEMINI.md):
1. **Zero Fake Hardcoding:** Never write regex, string replacement, or heuristic rule hacks that mimic AI. All contextual transformations, tone alignments, and typo corrections must strictly rely on on-device neural network (LLM) inference.
2. **100% On-Device Privacy (Zero Network):** No network requests are allowed during inference or text processing. All models run strictly on local CPU/GPU/NPU.
3. **Engineering Transparency:** Always handle errors honestly instead of generating fake fallback text.

---

## 🌿 Standard Branching Strategy (Branch Rules)

We follow standard open-source branching rules with a protected `main` branch:

| Prefix | Purpose | Example |
| :--- | :--- | :--- |
| `feat/` | New features or capabilities | `feat/npu-acceleration`, `feat/vietnamese-locale` |
| `fix/` | Bug fixes or crash resolutions | `fix/hangul-backspace-boundary`, `fix/jamo-leak` |
| `docs/` | Documentation changes or diagrams | `docs/clean-docs`, `docs/update-roadmap` |
| `refactor/` | Clean code refactoring without behavior change | `refactor/modularize-compose`, `refactor/prompt-builder` |
| `test/` | Adding or updating unit/integration tests | `test/add-automata-tests`, `test/given-when-then` |
| `chore/` | Maintenance, dependencies, or CI scripts | `chore/update-gradle-sdk35`, `chore/clean-deps` |

---

## 🛠️ Development Setup & Verification

### Android (`deartalk-android`)
- **IDE:** Android Studio Ladybug or later / VS Code
- **JDK:** OpenJDK 17+
- **Build & Test:**
  ```bash
  # Run JVM Unit Tests
  ./gradlew :deartalk-android:testDebugUnitTest

  # Build Debug APK
  ./gradlew :deartalk-android:assembleDebug
  ```

---

## 📝 Commit Message Guidelines (Conventional Commits)

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
- `feat:` New feature or capability
- `fix:` Bug fix or crash resolution
- `docs:` Documentation changes
- `refactor:` Refactoring code without behavior alteration
- `test:` Adding or updating tests
- `chore:` Maintenance, dependency updates, packaging scripts

---

## 📬 Pull Request Workflow

1. Fork the repository and create your branch from the latest `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feat/your-feature-name
   ```
2. Implement your changes following Clean Code and Given-When-Then test standards.
3. Ensure all unit tests pass 100%:
   ```bash
   ./gradlew :deartalk-android:testDebugUnitTest
   ```
4. Commit your changes following Conventional Commits.
5. Push to your fork and submit a Pull Request describing your changes, motivation, and verification steps.
