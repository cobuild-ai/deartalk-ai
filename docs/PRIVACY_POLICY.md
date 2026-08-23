# 🔒 Privacy Policy for DearTalk AI

**Last Updated: August 23, 2026**

DearTalk AI ("we," "our," or "the Application") is committed to protecting your privacy. This Privacy Policy explains how our Android and macOS applications handle user information.

---

## 1. 100% On-Device Processing & Zero Data Collection

DearTalk AI is designed with a strict **Zero-Data / Privacy-First Architecture**:

1. **No External Transmission**: We **do not collect, store, or transmit** any personal data, typed keystrokes, audio recordings, or generated messages to external servers or third parties.
2. **On-Device Artificial Intelligence**: All artificial intelligence models (Google LiteRT-LM, MediaPipe GenAI, Apple Silicon Metal GPU engines) run **strictly offline and locally on your device**.
3. **Zero Telemetry & Analytics**: The application contains no third-party tracking SDKs, advertising frameworks, or user behavioral analytics.

---

## 2. Permissions and Their Uses

DearTalk AI requests only the minimal permissions required to function as an input method editor (IME) and communication assistant:

- **`RECORD_AUDIO` (Microphone)**:
  - Used solely for real-time speech-to-text (STT) transcription when the user explicitly taps the microphone button on the keyboard.
  - Audio data is processed in real time by the local on-device SpeechRecognizer and is **never recorded, saved to disk, or transmitted over the network**.
- **`VIBRATE` (Haptic Feedback)**:
  - Used exclusively to provide tactile feedback during key presses.
- **`BIND_INPUT_METHOD`**:
  - Required by the Android operating system to enable and register DearTalk AI as a system-wide custom keyboard.

---

## 3. Third-Party Libraries & Open Source Components

DearTalk AI utilizes open-source libraries from Google AndroidX, Jetpack Compose, and Google LiteRT. None of these components collect personal identifiable information (PII) during offline on-device inference.

---

## 4. Children's Privacy

DearTalk AI does not collect any data from anyone, including children under the age of 13.

---

## 5. Changes to This Privacy Policy

If we update this Privacy Policy, the revised version will be published here with an updated "Last Updated" date.

---

## 6. Contact Us

If you have any questions or suggestions about this Privacy Policy, please contact us at:
- **Email**: `privacy@deartalk.ai`
- **GitHub Repository**: `https://github.com/smilelife/deartalk-ai`
