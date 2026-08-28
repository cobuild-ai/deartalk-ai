# 🤖 DearTalk AI: Android Custom Keyboard (IME) Module

`deartalk-android`는 Google **LiteRT-LM (Gemma on Android)** 기반의 100% 온디바이스 AI 비서 기능을 탑재한 **Android 커스텀 키보드(IME)** 모듈입니다.

사용자의 음성(오프라인 STT) 및 타이핑 텍스트를 실시간으로 분석하여 6가지 맞춤형 어조(Tone) 변환, 문맥 교정, 오탈자 수정을 외부 네트워크 통신 없이 기기 내부에서 즉시 수행합니다.

---

## 🏗️ 모듈 패키지 구조 (`ai.deartalk.android`)

```
deartalk-android/src/main/java/ai/deartalk/android/
├── ⌨️ ime/                          # InputMethodService 및 키보드 UI
│   ├── DearTalkIME.kt               # 안드로이드 IME 생명주기 및 텍스트 커밋 엔진
│   ├── HangulComposer.kt            # 한글 2벌식 음소 결합/분해 오토마타
│   └── ui/                          # Jetpack Compose 기반 글래스모피즘 키보드 뷰
│       ├── DearTalkScreen.kt        # 상단 어조 선택 칩 및 AI 추천 패널
│       ├── StandardKeyboardView.kt  # 쿼티/천지인/단모음 키보드 레이아웃
│       └── theme/                   # Slate Dark 테마 및 타이포그래피
│
├── 🧠 agent/                        # 온디바이스 AI 추론 코어
│   ├── DearTalkIntentEngine.kt      # Google LiteRT-LM (.litertlm) 모델 로드 및 추론
│   └── ModelDownloader.kt           # 대용량 모델 백그라운드 다운로더 & 무결성 검증
│
├── 🎤 stt/                          # 음성 인식
│   └── SpeechRecognitionManager.kt  # Android On-Device SpeechRecognizer 래퍼
│
├── 🔊 tts/                          # 음성 출력
│   └── TextToSpeechManager.kt       # 피드백 음성 안내 TTS 매니저
│
├── 💾 data/                         # 로컬 데이터 및 환경 설정
│   ├── repository/                  # SQLite Context 데이터 레이어
│   └── pref/                        # SharedPreferences 기반 사용자 설정 & 커스텀 톤
│
└── 📱 MainActivity.kt               # 키보드 활성화 마법사, 모델 다운로드 및 테스트 화면
```

---

## ⚙️ 핵심 아키텍처 및 동작 원리

### 1. IME 라이프사이클 & 한글 오토마타 (`ime/`)
- `DearTalkIME`는 `InputMethodService`를 상속받아 OS의 텍스트 필드 포커스를 감지합니다.
- `HangulComposer`는 초성/중성/종성 결합 규칙을 처리하며, 커서 이동 및 백스페이스 시 음소 단위의 안전한 분해를 보장합니다.
- 텍스트 입력 변경 시 300ms Debounce를 거쳐 `DearTalkIntentEngine`으로 전달됩니다.

### 2. 온디바이스 LiteRT-LM 추론 (`agent/`)
- Google LiteRT (구 TensorFlow Lite / MediaPipe GenAI) 런타임을 활용합니다.
- 사전 패키징되거나 다운로드된 `.litertlm` 모델을 RAM에 맵핑하여 GPU/NPU 가속을 통해 sub-300ms 내에 톤 변환 결과를 생성합니다.

### 3. 오프라인 음성인식 STT (`stt/`)
- Android 11+ (`API 30+`)의 온디바이스 음성 인식 서비스를 바인딩하여 0% 네트워크 트래픽으로 음성을 텍스트로 변환합니다.

---

## 🚀 로컬 빌드 및 디버깅 가이드

### 필수 요구사항
- **Android SDK**: `compileSdk = 35`, `minSdk = 26` (Android 8.0 이상)
- **JDK**: Java 17
- **Gradle**: 8.0+

### 빌드 및 실행 명령어

```bash
# 1. 단위 테스트 실행
./gradlew :deartalk-android:testDebugUnitTest

# 2. 디버그 APK 빌드 및 연결된 단말기(USB/Wi-Fi ADB)에 설치
./gradlew :deartalk-android:installDebug

# 3. 실시간 로그캣 모니터링 (IME & AI 엔진)
adb logcat -s "DearTalkIME" "DearTalkEngine" "LiteRTLM"
```

---

## 🔒 보안 및 기밀성 규칙

1. **절대 네트워크 전송 금지**: 키보드 입력 데이터(`InputConnection`)와 오디오 스트림은 절대 외부 서버로 전송되지 않습니다.
2. **서명 보호 테스트 리시버**: 디버깅용 `DearTalkTestReceiver`는 릴리즈 빌드에서 자동 제외되며, 디버그 빌드에서도 `signature` 레벨 권한으로 보호됩니다.
3. **Keystore 보호**: 상용 배포 키는 `keystore.properties` 또는 환경변수로만 주입되며 Git에 커밋되지 않습니다.
