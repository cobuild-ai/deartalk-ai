## 📝 Summary of Changes
Please include a concise summary of the changes, motivation, and link related issues.
- **Related Issue:** Closes #

---

## 🏷️ Type of Change
- [ ] 🐛 Bug fix (non-breaking fix for a defect)
- [ ] ✨ New feature / enhancement (non-breaking addition)
- [ ] 🌐 Localization & prompts (i18n / new language support)
- [ ] ⚡ Performance / LiteRT memory optimization
- [ ] 🧪 Testing / CI & automation scripts
- [ ] 📝 Documentation update (READMEs / docs)

---

## 📱 Platforms & Components Affected
- [ ] Jetpack Compose Keyboard UI (`deartalk-android/ime/ui`)
- [ ] On-Device AI & LiteRT Engine (`agent/DearTalkIntentEngine.kt`)
- [ ] Speech-to-Text & Automata (`stt/`, `ime/HangulComposer.kt`)
- [ ] Test Suite & Scripts (`verify.sh`, `scripts/`)
- [ ] Documentation (`README.md`, `README.ko.md`, `README.id.md`, `docs/`)

---

## 🧪 Verification & Testing Performed
Please check what tests you ran before submitting this PR:
- [ ] **Secret & Privacy Audit**: `./verify.sh audit` passed with 0 leaks.
- [ ] **JVM Unit Tests**: `./verify.sh unit` passed 100% locally.
- [ ] **Real-Device Stability Audit**: `./verify.sh device` or `./verify.sh all` passed (0 crashes, 0 ANRs).
- [ ] **Manual Device Testing**: Tested typing, STT voice input, and tone switching on a physical device / emulator.

---

## 📋 Contributor Checklist
- [ ] My code adheres to the project's **Zero Fake Rules Invariant** (100% on-device AI inference, no mock fallbacks).
- [ ] I have updated relevant documentation if this PR introduces public-facing changes.
- [ ] If modifying `README`, I updated all 3 languages (`README.md`, `README.ko.md`, `README.id.md`) in sync.
- [ ] My commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g. `feat: ...`, `fix: ...`).
