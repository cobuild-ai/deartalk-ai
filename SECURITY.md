# Security Policy

## 🔒 100% On-Device Privacy Guarantee

**DearTalkAI** is architected from day one as a zero-network, local-only communication assistant. 

### Key Privacy Invariants:
1. **Zero External Network Traffic:** Keystrokes, raw text, and transcribed audio never leave the local device.
2. **Local Model Execution:** All inference operations are executed strictly on the device's CPU/GPU/NPU via Apple Metal or Google LiteRT.
3. **Automatic History Pruning:** Local SQLite repositories enforce strict record caps (max 100 entries per app) with one-click manual deletion support.

---

## 🛡️ Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

---

## 🚨 Reporting a Vulnerability

If you discover a potential security vulnerability or unintentional network leakage in DearTalkAI, please notify us responsibly:

1. **Email:** Send details to `security@deartalk.ai` (or via private GitHub Security Advisory).
2. **Details to Include:**
   - Description of the vulnerability and affected platform (Android / macOS).
   - Steps to reproduce or proof-of-concept.
   - Any log captures or network packet inspection traces.
3. **Response Timeline:** We aim to acknowledge reports within 48 hours and provide a patch timeline within 7 days.
