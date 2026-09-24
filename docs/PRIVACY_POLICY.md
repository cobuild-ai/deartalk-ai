# 🔒 Privacy Policy for DearTalk AI

**Last Updated: September 24, 2026**

DearTalk AI ("we," "our," or "the Application") is an open-source, on-device AI custom keyboard and communication assistant. We are committed to absolute user privacy. This Privacy Policy explains our strict zero-data handling practices for the DearTalk AI Android application.

---

## 1. 100% On-Device Processing & Zero Data Collection

DearTalk AI is architected with a strict **Zero-Data / Privacy-First Architecture**:

1. **Physical Network Isolation (Zero INTERNET Permission)**:  
   The Application does **NOT** declare or request the `android.permission.INTERNET` permission in its Android manifest. Therefore, it is technically and physically impossible for the application to transmit typed keystrokes, voice recordings, clipboard content, or generated text to any external server or third party.
2. **Local Neural Inference**:  
   All generative AI and language models (Google LiteRT-LM, Gemma 4, MediaPipe GenAI) execute **100% locally and offline on your device** utilizing your smartphone's CPU, GPU, or Qualcomm Hexagon NPU.
3. **No Telemetry, Analytics, or Advertising**:  
   The application contains zero third-party tracking SDKs, zero advertising frameworks, zero diagnostic crash reporters, and zero user behavioral analytics.

---

## 2. Permissions Requested and Purpose

DearTalk AI requests only the absolute minimum permissions strictly necessary to function as an Android Input Method Editor (IME):

* **`RECORD_AUDIO` (Microphone)**:
  * **Purpose**: Used solely for on-device real-time Speech-to-Text (STT) transcription when the user explicitly taps the microphone button.
  * **Data Handling**: Audio is processed in memory by the local Android on-device `SpeechRecognizer` API. Audio buffers are immediately discarded after transcription and are **never saved to storage, recorded as files, or transmitted over the network**.
* **`VIBRATE` (Haptic Feedback)**:
  * **Purpose**: Used exclusively to provide tactile vibration feedback when keys are pressed.
* **`BIND_INPUT_METHOD` (Android System)**:
  * **Purpose**: Required by the Android OS to register DearTalk AI as an active system-wide keyboard.

---

## 3. Data Retention and Deletion (Google Play Data Safety)

* **No Server Storage**: Because DearTalk AI operates with 0% network access and has no cloud backend, **we do not store or retain any personal data**.
* **Ephemeral Memory Life Cycle**: Ephemeral speech recognition drafts and tone suggestions are maintained strictly in volatile RAM and automatically cleared when a message is dispatched or discarded.
* **Complete User Control**: Uninstalling the Application or clearing its app storage via Android Settings immediately and irreversibly destroys any locally cached user preferences.

---

## 4. Children's Privacy

DearTalk AI does not collect, process, or share personal information from anyone, including children under the age of 13 (or under the applicable age in your jurisdiction).

---

## 5. Open Source Transparency

DearTalk AI is fully open-source under the **Apache License 2.0**. Anyone can independently audit our complete source code, manifest, build scripts, and dependencies on GitHub to verify our absolute zero-network privacy claims:
- **Source Code**: [https://github.com/cobuild-ai/deartalk-ai](https://github.com/cobuild-ai/deartalk-ai)

---

## 6. Changes to This Privacy Policy

If we update this Privacy Policy, the revised version will be published in this repository with an updated "Last Updated" date.

---

## 7. Contact Us

If you have questions, feedback, or security concerns regarding this Privacy Policy, please contact our core maintainer team at:
* **Official Contact Email**: `onthelogic@gmail.com`
* **Community Testers**: `aibuilder-testers@googlegroups.com`
* **GitHub Issues**: [https://github.com/cobuild-ai/deartalk-ai/issues](https://github.com/cobuild-ai/deartalk-ai/issues)

---

<details>
<summary><b>🇰🇷 [한국어] 개인정보처리방침 요약 안내 (펼치기)</b></summary>

DearTalk AI는 사용자의 사생활을 철저히 보호하는 **100% 온디바이스 AI 키보드**입니다:
1. **인터넷 권한 0개 (Zero Permission)**: 앱 매니페스트에 인터넷 권한(`INTERNET`) 자체가 없어 어떤 텍스트나 음성도 외부로 전송될 수 없습니다.
2. **완전한 로컬 연산**: Google LiteRT 기반 온디바이스 SLM이 스마트폰 내부에서만 작동합니다.
3. **오디오 데이터 미저장**: 마이크 음성은 기기 내 실시간 음성인식 즉시 소멸되며 파일로 저장되지 않습니다.
4. **문의처**: `onthelogic@gmail.com`
</details>
