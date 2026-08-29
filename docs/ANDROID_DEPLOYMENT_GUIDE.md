# 📱 DearTalk AI Android: Google Play Store 상용 배포 및 운영 가이드라인

이 문서는 `deartalk-ai`의 Android 모듈(`deartalk-android`)을 **Google Play Store에 안정적으로 출시하고 업데이트하기 위한 시니어 앱 개발자 표준 배포 가이드**입니다.

---

## 📋 1. Google Play 출시 전 필수 점검 체크리스트 (Pre-flight Checklist)

| 구분 | 검증 항목 | 점검 기준 | 상태 |
| :--- | :--- | :--- | :---: |
| **SDK 버전** | `compileSdk`, `targetSdk` | 최신 Google Play 기준 충족 (**`targetSdk = 35`**, Android 15) | ✅ Pass |
| **최소 지원 버전** | `minSdk` | Android 8.0 Oreo (`minSdk = 26`) | ✅ Pass |
| **앱 번들 형식** | 배포 아티팩트 | 반드시 `.aab` (Android App Bundle) 포맷 사용 | ✅ Pass |
| **코드 난독화 & 축소** | R8 / Proguard | `isMinifyEnabled = true`, `isShrinkResources = true`, JNI Keep 룰 적용 | ✅ Pass |
| **앱 서명 (Signing)** | Release Keystore | RSA 2048+ / PKCS12 키 분리 관리 (`keystore.properties`) | ✅ Pass |
| **보안 & 테스트 격리** | `DearTalkTestReceiver` | Release 빌드 매니페스트에서 완전 제외 | ✅ Pass |
| **앱 아이콘 & 그래픽** | Adaptive Icon | `@mipmap/ic_launcher` 프로덕션 에셋 적용 | ✅ Pass |
| **데이터 안전성** | Play Console Data Safety | 온디바이스 Zero-Network 처리 명시 (오디오/키스트로크 수집 0건) | ✅ 수립 완료 |

---

## 🔑 2. 안전한 Keystore 서명 관리 (`signingConfigs`)

릴리즈 서명 키(`release.keystore` / `*.jks`)와 비밀번호는 **절대 Git 저장소에 커밋해서는 안 됩니다.**

### A. 로컬 환경: `keystore.properties` 분리
`deartalk-android/keystore.properties` 파일을 생성하고 `.gitignore`에 등록하여 관리합니다:

```properties
# deartalk-android/keystore.properties (Git 커밋 절대 금지)
RELEASE_STORE_FILE=../secrets/deartalk-release.keystore
RELEASE_STORE_PASSWORD=your_secure_store_password
RELEASE_KEY_ALIAS=deartalk_release_key
RELEASE_KEY_PASSWORD=your_secure_key_password
```

### B. CI/CD 환경: 환경 변수 주입
GitHub Actions 또는 CI 빌드 환경에서는 Secret 환경변수를 통해 주입합니다:
- `DEARTALK_KEYSTORE_BASE64`
- `DEARTALK_KEYSTORE_PASSWORD`
- `DEARTALK_KEY_ALIAS`
- `DEARTALK_KEY_PASSWORD`

---

## 🛡️ 3. R8 / Proguard 최적화 및 C++ JNI Keep 규칙

온디바이스 AI 앱은 C++ 네이티브 바이너리(Google LiteRT, MediaPipe)와 JNI 인터페이스로 통신하므로, R8 난독화 시 네이티브 인터페이스가 삭제(Shrink)되면 런타임 크래시(`UnsatisfiedLinkError`)가 발생합니다.

### `proguard-rules.pro` 필수 보존 대상:
1. **Google LiteRT-LM & MediaPipe GenAI**:
   - `com.google.ai.edge.litertlm.**`
   - `com.google.mediapipe.tasks.genai.**`
2. **Jetpack Compose Runtime & UI State**:
   - Compose 내부 컴파일러 메타데이터 및 상태 클래스
3. **한글 오토마타 및 모델 데이터 클래스**:
   - `ai.deartalk.android.ime.HangulComposer`
   - `ai.deartalk.android.data.pref.**`

---

## 🧠 4. 온디바이스 AI 모델 배포 불변 원칙 (Play Asset Delivery & Local ADB)

Google Play Store의 **단일 AAB 기본 다운로드 제한은 200MB**입니다.  
Gemma 기반 온디바이스 모델(`.litertlm`, 약 1.0GB ~ 1.5GB)은 Google Play 공식 표준인 **Play Asset Delivery (PAD)** 또는 **로컬 ADB 스토리지**를 통해 배포됩니다:

```
┌─────────────────────────────────────────────────────────────┐
│ 🚀 Google Play 스토어 설치 (기본 AAB ~25MB 초경량)            │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────▼───────────────┐
               │ Play Asset Delivery (PAD) 에셋 팩│
               │ (install-time 또는 fast-follow)  │
               └───────────────┬───────────────┘
                               │
               ┌───────────────▼───────────────┐
               │ 📱 단말기 로컬 AI 엔진 바인딩  │
               │ (100% 온디바이스 오프라인 추론) │
               └───────────────────────────────┘
```

1. **Play Asset Delivery (PAD) 표준 (권장)**:
   - `install-time` 또는 `fast-follow` 에셋 팩을 통해 Google Play 인프라에서 공식 분할 번들링 제공.
2. **로컬 개발자 / 테스터 ADB 전송**:
   - `adb push gemma-2b-it-cpu-int4.litertlm /data/local/tmp/llm/`
   - 앱이 내부적으로 공용 및 앱 전용 `models/` 디렉토리를 자동 감지하여 연결.
3. **인앱 외부 HTTP 다운로드 금지**:
   - 보안 및 스토어 정책 준수를 위해 앱 내부에서 외부 HTTP 서버에 접속하여 대용량 파일을 내려받는 행위를 일체 배제합니다.

---

## 🔒 5. Play Console Data Safety & 권한 정책 작성 가이드

DearTalk AI는 **100% On-Device AI**이므로 구글 심사 시 매우 강력한 개인정보 보호 이점을 가집니다.

### A. 사용 권한 및 목적
- `android.permission.RECORD_AUDIO`: 키보드 내 음성 인식(STT) 마이크 입력용
- `android.permission.VIBRATE`: 키 입력 시 햅틱 피드백 제공용
- `android.permission.BIND_INPUT_METHOD`: 안드로이드 시스템 키보드(IME) 서비스 등록용

### B. Play Console "데이터 안전(Data Safety)" 답변 요령
- **데이터 수집 여부**: **수집 안 함 (No data collected)**
- **데이터 공유 여부**: **공유 안 함 (No data shared)**
- **오디오 데이터(Voice)**:
  - *“사용자의 음성 데이터는 기기 내부(On-device)에서 실시간으로 텍스트로 변환된 후 즉시 메모리에서 소멸되며, 외부 서버로 전송되거나 저장되지 않습니다.”*
- **키스트로크 데이터(Keystrokes)**:
  - *“사용자가 입력하는 모든 텍스트는 로컬 온디바이스 신경망에서만 처리되며 어떠한 원격 서버로도 전송되지 않습니다.”*

---

## 📦 6. 원터치 릴리즈 빌드 및 배포 절차

```bash
# 1. JVM 단위 테스트 및 실기기 자동화 안정성 감사
./start.sh test
./start.sh verify

# 2. 상용 서명 Release AAB (Android App Bundle) 빌드
./start.sh release    # 또는 make release

# 3. 생성된 배포 번들 확인
# -> deartalk-android/build/outputs/bundle/release/deartalk-android-release.aab (약 26MB)

# 4. Play Console 내부 테스트(Internal Test) 트랙에 업로드 및 검증
```

---

## 🏷️ 7. 버전닝 및 태그 전략

- `versionCode`: 매 릴리즈마다 `+1` 단조 증가 (예: `1`, `2`, `3`, `4` ...)
- `versionName`: `Semantic Versioning` 준수 (예: `1.4.0`)
- 배포 완료 시 Git 태그 생성:
  ```bash
  git tag -a v1.4.0 -m "Release v1.4.0: Target SDK 35, Compose UI modularization, full multilingual support"
  git push origin v1.4.0
  ```
