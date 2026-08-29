# 🧠 On-Device AI Models & Hardware Compatibility Specification (Android)

DearTalkAI runs **100% on-device neural language models** without sending any data over external networks. This document specifies supported model architectures, quantization formats, and Android hardware requirements.

---

## 📊 Supported Model Specifications

| Component | Specification |
| :--- | :--- |
| **Model Architecture** | Google Gemma 2 2B Instruct |
| **Execution Engine** | Google LiteRT (`ai.deartalk.android.agent.LiteRtEngine`) |
| **Hardware Acceleration** | Qualcomm Adreno / ARM Mali / Google Tensor GPU Delegate |
| **File Format** | `.litertlm` / `.tflite` |
| **Recommended Quantization** | Int4 (Dynamic range weights) |
| **Model Size on Storage** | ~1.3 GB |
| **Runtime Resident RAM** | ~1.0 GB Native PSS |

---

## ⚡ Distribution & Local Paths

### 1. Google Play Asset Delivery (PAD)
- **Standard:** Official Google Play feature delivery via `install-time` or `fast-follow` asset packs.
- **Security:** Fully signed, verified, and managed by the Google Play Core framework.

### 2. Local ADB / Sideload Path (For Testing & Contributors)
- **Local Path 1:** `/data/local/tmp/llm/gemma-2-2b-it-int4.litertlm`
- **Local Path 2:** `/data/data/ai.deartalk.android/files/models/model.litertlm`
- **Zero In-App Downloads:** 0% external HTTP network downloader dependencies.

---

## 💻 Hardware Requirements

### 📱 Android Devices
- **Minimum OS:** Android 10 (API Level 29)
- **Recommended OS:** Android 13+ / Android 14 / Android 15 (API Level 33 ~ 35)
- **RAM:** Minimum 6GB RAM (8GB+ recommended for multi-tasking)
- **Chipset:** Snapdragon 7+ / 8 Gen series, MediaTek Dimensity 8000+, or Google Tensor G2/G3/G4 with OpenCL/Vulkan GPU support.

---

## 🔒 0% Fake Rules Verification Guarantee
In accordance with our core engineering principles:
1. If the model file is not present or still initializing, the engine **honestly returns the exact original input** with an informative status label.
2. The engine **never substitutes missing models with hardcoded string manipulation, regex templates, or pseudo-AI heuristic rules**.
