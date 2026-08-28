# Privacy Policy for DearTalk AI (개인정보처리방침)

> **Last Updated / 최종 수정일**: 2026-08-27  
> **App Name / 앱 이름**: DearTalk AI (디어톡 AI)  
> **Developer / 개발자**: cobuild-ai (`contact@cobuild-ai.org`)  
> **Public URL**: `https://github.com/cobuild-ai/deartalk-ai/blob/main/PRIVACY_POLICY.md`

---

## 🔒 English (Global Standard)

### 1. Overview & Core Privacy Commitment
**DearTalk AI** is an on-device AI-powered smart keyboard and conversational refinement application designed with **Zero-Data-Collection** and **Complete On-Device Privacy** at its core. 

We strongly believe that your keystrokes, personal chats, and tone adjustments belong exclusively to you. **DearTalk AI does NOT collect, store, transmit, or share any personal information, keystrokes, or telemetry data.**

---

### 2. Zero Network Permission (`INTERNET` Permission = 0)
- **No Internet Access**: DearTalk AI does **NOT** request or include the `android.permission.INTERNET` permission in its Android Manifest.
- **Architectural Guarantee**: Because the application possesses no network access capabilities, it is **technically and physically impossible** for DearTalk AI to transmit any user data, typed text, voice inputs, or device identifiers to external servers or third parties.

---

### 3. Data Handled Locally on Your Device

| Data Category | How It Is Processed | Storage & Retention |
| :--- | :--- | :--- |
| **Typed Text & Keystrokes** | Processed in volatile memory solely to render keyboard keys and perform real-time AI tone refinement. | **Zero Retention**. Immediately purged upon text submission or app switch. Never written to disk. |
| **Voice / Speech Input** | Processed locally on-device via Android SpeechRecognizer APIs for voice-to-text input. | **Zero Retention**. Audio buffers are discarded immediately after transcription. |
| **User Settings & Tones** | Selected tone preferences (e.g., Polite, Casual, Business) and keyboard layout preferences. | Stored locally on device via encrypted `DataStore`/`SharedPreferences`. Never uploaded. |
| **On-Device AI Models** | Pre-bundled or received via Google Play Asset Delivery (`install-time`). Runs purely on local NPU/CPU. | Stored in private app storage. No telemetry or analytics. |

---

### 4. Third-Party SDKs and Tracking
- **No Analytics / No Tracking**: DearTalk AI contains **zero** third-party trackers, ad SDKs, analytics tools (e.g., Firebase Analytics, Google Analytics, Facebook SDK, Adjust, AppsFlyer are strictly excluded).
- **Google Play Services**: The app utilizes official Google Play Core libraries strictly for `Play Asset Delivery` during app installation.

---

### 5. Google Play Data Safety Declarations

According to Google Play's Data Safety requirements:
- **Data Collected**: **None (No data collected)**
- **Data Shared**: **None (No data shared)**
- **Security Practices**: All processing occurs strictly on-device without network transmission.

---

### 6. Children's Privacy
DearTalk AI does not collect any personal information from anyone, including children under the age of 13.

---

### 7. Contact Us
If you have any questions or feedback regarding this Privacy Policy, please open an issue on our official GitHub repository or contact us:
- **GitHub**: [https://github.com/cobuild-ai/deartalk-ai](https://github.com/cobuild-ai/deartalk-ai)
- **Organization**: cobuild-ai

---

<br>

---

## 🇰🇷 한국어 (개인정보처리방침)

### 1. 개요 및 개인정보 보호 원칙
**DearTalk AI (디어톡 AI)**는 **완전한 온디바이스(On-Device) 로컬 인공지능 기반 키보드**로서, 사용자의 개인정보와 입력 데이터를 완벽히 보호하기 위해 **'데이터 수집 0건 (Zero-Data Collection)'** 원칙을 엄격히 준수합니다.

DearTalk AI는 사용자의 키보드 입력 내용, 음성 녹음 데이터, 텍스트 변환 내역, 기기 식별자 등의 어떠한 개인정보도 수집, 저장, 전송, 공유하지 않습니다.

---

### 2. 인터넷 권한 완전 배제 (`INTERNET` 권한 0개)
- **네트워크 권한 없음**: DearTalk AI는 `AndroidManifest.xml`에 인터넷 접속 권한(`android.permission.INTERNET`)을 전혀 포함하지 않습니다.
- **기술적 원천 차단**: 인터넷 연결 기능 자체가 존재하지 않으므로, 사용자가 입력한 대화 내용이나 데이터가 외부 서버로 유출되는 것이 **물리적·기술적으로 100% 불가능**합니다.

---

### 3. 처리하는 정보 및 처리 목적

1. **키보드 입력 텍스트 및 키스트로크**:
   - **처리 목적**: 키보드 자판 입력 및 온디바이스 SLM/NPU를 통한 6대 톤앤매너 문장 다듬기.
   - **보관 및 파기**: 휘발성 메모리(RAM)에서 즉시 처리 후 문장 입력 완료 시 **즉각 영구 파기(디스크 저장 0건)**.
2. **음성 입력(STT)**:
   - **처리 목적**: 키보드 음성 입력 기능 지원.
   - **보관 및 파기**: 안드로이드 내장 음성인식 엔진을 통해 텍스트로 변환된 직후 음성 데이터 즉시 파기.
3. **사용자 앱 설정**:
   - **처리 목적**: 사용자가 선택한 기본 톤앤매너, 키보드 레이아웃 유지.
   - **보관**: 기기 내 안전한 로컬 저장소(`DataStore`)에만 저장되며 외부 전송되지 않음.

---

### 4. 제3자 제공 및 외부 SDK 사용 여부
- 본 앱은 광고 SDK, 서드파티 분석 툴(Google Analytics, Firebase Analytics, Facebook SDK 등), 사용자 추적 라이브러리를 **일체 탑재하지 않았습니다.**
- 온디바이스 AI 모델 가중치는 Google Play의 공식 글로벌 배포 시스템(Play Asset Delivery)을 통해서만 안전하게 기기에 설치됩니다.

---

### 5. Google Play 데이터 보안 규정 충족
- **수집하는 데이터 항목**: **없음**
- **외부와 공유하는 데이터 항목**: **없음**
- **보안 조치**: 100% 로컬 온디바이스 하드웨어 가속 처리 및 네트워크 접근 원천 차단.

---

### 6. 문의처
개인정보 처리방침과 관련된 문의나 제안은 공식 GitHub 저장소 이슈를 통해 접수받고 있습니다:
- **공식 저장소**: [https://github.com/cobuild-ai/deartalk-ai](https://github.com/cobuild-ai/deartalk-ai)
- **개발 조직**: cobuild-ai
