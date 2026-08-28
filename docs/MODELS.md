# 🧠 On-Device AI Models & Hardware Compatibility Specification

DearTalkAI runs **100% on-device neural language models** without sending any data over external networks. This document specifies supported model architectures, quantization formats, and hardware requirements.

---

## 📊 Supported Model Matrix

| Platform | Engine Backend | Format | Model Architecture | Recommended Quantization | Memory (RAM) |
| :--- | :--- | :---: | :--- | :---: | :---: |
| **Android** | Google LiteRT GPU | `.litertlm` / `.tflite` | Google Gemma 2 2B Instruct | Int4 / GPU Delegate | ~1.5 GB |

---

## ⚡ Default Model Sources (Official CDN)

### Android (LiteRT / TFLite Format)
- **Model:** `gemma-2-2b-it-int4.litertlm`
- **Local Storage Path:** `/data/data/ai.deartalk.android/files/models/model.litertlm`
- **Runtime Execution:** `ai.deartalk.android.agent.LiteRtEngine`

---

## 💻 Hardware Requirements

### 📱 Android Devices
- **Minimum OS:** Android 10 (API Level 29)
- **Recommended OS:** Android 13+ (API Level 33+)
- **RAM:** Minimum 6GB RAM (8GB+ recommended)
- **Chipset:** Snapdragon 7+ / 8 Gen series, Dimensity 8000+, or Google Tensor G2/G3/G4 with GPU/NPU acceleration.

---

## 🔒 0% Fake Rules Verification Guarantee
In accordance with our core engineering principles:
1. If the model file is not present or still initializing, the engine **honestly returns the exact original input** with an informative status label.
2. The engine **never substitutes missing models with hardcoded string manipulation, regex templates, or pseudo-AI heuristic rules**.
