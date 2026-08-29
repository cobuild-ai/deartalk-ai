# Changelog

All notable changes to **DearTalk AI (Android IME)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-08-29

### Added
- **Initial Open Source Release of DearTalk AI**: 100% on-device AI Android Custom Keyboard (IME) powered by Google Gemma 2B via LiteRT GPU.
- **Android 15 & Target SDK 35 Ready**: Upgraded build toolchains to `compileSdk = 35` and `targetSdk = 35` with complete edge-to-edge Compose rendering.
- **6 Unified Tone Presets**: `✨ Refine`, `👔 Polite`, `😊 Casual`, `💼 Business`, `🤣 Humorous`, and `😼 Cheeky`.
- **Zero Fake Rules Invariant**: Strict fallback guarantees preserving 100% of raw user input without mock string hacks.
- **Offline Speech-to-Text (STT)**: Instant offline voice transcription with raw audio preservation across tone switches.
- **0ms Optimistic UI & Keyboard Switcher**: Instant visual feedback and quick one-tap switching to Samsung/Gboard keyboards.
- **Dynamic Multilingual Prompts**: Dynamic prompt generator for Indonesian (Bahasa Indonesia), Japanese, Spanish, Korean, and English.
- **Pre-PR Automated Testing Suite (`verify.sh`)**: Integrated zero-friction test launcher with automated `.venv` auto-provisioning, JVM unit testing, and real-device Monkey stability & memory leak auditing.
- **Play Asset Delivery (PAD) Model Architecture**: Official Google Play asset delivery (`install-time` / `fast-follow`) and local ADB sideloading support.
- **Tri-lingual Documentation Standard**: Pristine `README.md` (English), `README.ko.md` (Korean), and `README.id.md` (Indonesian) with a single English SSOT for internal technical specifications.
