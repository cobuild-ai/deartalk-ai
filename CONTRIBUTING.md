# 🤝 Contributing to DearTalkAI

Thank you for your interest in contributing to **DearTalkAI**! We welcome developers, testers, UI/UX designers, and translators from all around the world to help build a 100% on-device, privacy-preserving AI communication assistant.

---

## 🌟 Quick Start for Contributors (Zero-Friction DX)

We provide an interactive **One-Touch Verification Suite (`verify.sh`)** and **Standardized Makefile** so you can get started in seconds without struggling with manual virtual environment activations or complex Gradle flags:

### 1. Launch Interactive Menu
```bash
# Simply run verify.sh to launch the interactive helper menu
./verify.sh
```

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 🧪 DearTalk-AI Automated Testing & Verification Suite 
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1) 🚀 전체 일괄 검증 (All-in-One: Unit Tests + Real-Device Audit)
  2) 🧪 JVM 단위 테스트 (make test)
  3) 📱 실기기 자동화 안정성 검증 (make verify-device)
  4) 🔨 Debug APK 빌드 (make build)
  5) 📦 Release Bundle (AAB) 빌드 (make release)
  6) 🔍 Kotlin 코드 린트 (make lint)
  q) 종료 (Quit)
────────────────────────────────────────────────────────────
▶ 실행할 작업 번호를 입력하세요 [1-6, q]:
```

### 2. Direct CLI Commands
You can also run commands directly via `make` or `./verify.sh`:

```bash
# 🚀 Run All-in-One Verification (Unit Tests + Real-Device Audit)
make verify            # or ./verify.sh all

# 🧪 Run JVM Unit Tests
make test              # or ./verify.sh unit

# 📱 Run Real-Device Automated Stability & Leak Audit (Monkey Stress)
make verify-device     # or ./verify.sh device

# 📱 Run Real-Device Audit with custom parameters
./verify.sh device --events 1000 --throttle 20 --device 192.168.1.136:35859

# 🔨 Build & Install Debug APK on connected phone/emulator
make build             # or ./verify.sh build

# 📦 Build Signed Production Release Bundle (AAB)
make release           # or ./verify.sh release

# 🔍 Run Kotlin Linter & Static Analysis
make lint              # or ./verify.sh lint
```

---

## 🚨 Inviolable Core Principles (The Zero Fake Rule)

Before writing any code, please review our inviolable governance principles in [GEMINI.md](GEMINI.md):

1. **Zero Fake Hardcoding:** Never write regex, string replacement, or heuristic rule hacks that mimic AI. All contextual transformations, tone adjustments, typo corrections, and translations must strictly rely on on-device neural network (Google Gemma) inference.
2. **100% On-Device Privacy (Zero Network):** Zero external network traffic during inference. Audio and typed keystrokes must never leave the local device.
3. **Play Asset Delivery (PAD) & Local Models:** No in-app HTTP downloading of models from untrusted third-party servers. All models are packaged via Google Play Asset Delivery or loaded from secure local app storage.
4. **Engineering Transparency:** Handle errors honestly and return clean uncorrupted raw text instead of mock replies.

---

## 🛠️ Environment & Prerequisites

### Android Module (`deartalk-android`)
- **JDK:** OpenJDK 17 or higher
- **Android SDK:** `compileSdk = 35`, `targetSdk = 35` (Android 15 ready)
- **IDE:** Android Studio (Ladybug / Iguana or later) or VS Code
- **Python:** Python 3.8+ (Used for automated testing scripts; standard library only, automatically creates isolated `.venv`)

---

## 🧪 Contributor Testing & Verification Checklist

To maintain rock-solid reliability across billions of keystrokes, every Pull Request (PR) must fulfill this 3-step verification checklist:

### Step 1: JVM Unit Tests (`make test`)
Verifies Hangul automata composition, prompt templates, few-shot generation, and tone presets:
```bash
make test
```
*Expected: 100% tests passing in < 2 seconds.*

### Step 2: Real-Device Automated Stability Audit (`make verify-device`)
Injects 500 high-speed Monkey touches/gestures into the app, audits Logcat for FATAL exceptions/ANRs, and verifies that the On-Device Gemma SLM runtime does not leak RAM:
```bash
make verify-device
```
*Expected output:*
```text
  • App Crashes (0%):     0 (Clean)
  • ANRs / Hangs (0%):    0 (Smooth)
  • Memory Status:        Stable (No Leak)
🎉 [PASSED] DearTalk AI Passed Real-Device Automated Stability Audit!
```
> See [docs/TESTING.md](docs/TESTING.md) for full details on thresholds and debugging.

### Step 3: Lint & Code Quality (`make lint`)
```bash
make lint
```

---

## 🎯 Good First Issues & Contribution Areas

| Difficulty | Area | Target Files | Description |
| :--- | :--- | :--- | :--- |
| 🟢 **Easy** | Localization (i18n) | `UiStrings.kt`, `values-*/strings.xml` | Add new language dictionaries or refine translations (e.g. Japanese, Spanish, Indonesian, Vietnamese, German) |
| 🟢 **Easy** | UI / Theming | `ui/components/`, Compose views | Refine dark mode glassmorphism styling, animations, or keyboard haptic feedback |
| 🟡 **Medium** | Prompt Engineering | `DearTalkIntentEngine.kt` | Refine multilingual system prompts and few-shot examples for the 6 tone presets |
| 🟡 **Medium** | Diff & Punctuation | `DiffEngine.kt` | Enhance word-level tokenization boundary handling and punctuation preservation |
| 🔴 **Hard** | On-Device NPU Acceleration | LiteRT LM C++ bindings | Optimize LiteRT GPU/NPU delegates and KV-cache quantization on mobile chips |

---

## 📝 Commit Message Guidelines (Conventional Commits)

We follow the [Conventional Commits](https://www.conventionalcommits.org/) standard:

- `feat:` New feature or tone capability
- `fix:` Bug fix or crash resolution
- `docs:` Documentation improvements
- `style:` Code formatting and cleanups
- `refactor:` Refactoring without behavior changes
- `test:` Adding or improving unit/device tests
- `chore:` Tooling, dependency, and build script updates

---

## 📬 Pull Request (PR) Workflow

1. **Fork & Branch**:
   ```bash
   git clone https://github.com/<your-username>/deartalk-ai.git
   cd deartalk-ai
   git checkout -b feat/your-awesome-feature
   ```
2. **Develop & Test**:
   Make your changes, then verify with the all-in-one suite:
   ```bash
   ./verify.sh all    # or make verify
   ```
3. **Commit & Push**:
   ```bash
   git add .
   git commit -m "feat(i18n): add Vietnamese localization support"
   git push origin feat/your-awesome-feature
   ```
4. **Submit PR**:
   Open a Pull Request on GitHub. Detail your changes, attach testing screenshots or `verify-device` outputs, and our core maintainers will review promptly!

Thank you for building the future of private, on-device AI communication with us! 🚀
