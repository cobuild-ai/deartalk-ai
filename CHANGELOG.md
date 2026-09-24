# Changelog

All notable changes to **DearTalk AI (Android IME)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.5] - 2026-09-24

### Fixed & Improved
- **🎛️ 2-Tier 사용 모드 분기 (기본 모드 vs 프로 모드) 및 초직관 온보딩 다이얼로그 탑재**:
  - **기본 모드(BASIC)**: 3세 이상 전연령 초직관 경험. 번역/언어선택을 숨기고 단일 언어 톤앤매너 및 4대 화행에 집중.
  - **프로 모드(PRO)**: 글로벌 파워유저용. 59개국어 실시간 다국어 번역 및 2-Track Live 통역 완전 제공.
  - 앱 최초 실행 시 2-Card 비주얼 온보딩 다이얼로그(`ModeSelectionDialog`)로 1초 만에 모드 선택 지원.
- **🔄 화행(Intent) 일회성 AUTO 자동 리셋(One-shot Ephemeral) 라이프사이클 구현**:
  - 어조(Tone)는 세션 동안 유지되되, 화행(SpeechIntent)은 메시지 전송(입력 확정) 또는 삭제 시 즉시 AUTO(AI 자동 판단)로 자동 리셋되어 연속 발화 간섭을 원천 차단.
- **📸 3개 국어 21장 톤앤매너 고유 스크린샷 전수 캡처 및 중복 방지 파이프라인**:
  - 키보드 시스템 노출 감지 루프 및 SHA-256 고유성 자동 검증 Fail-Safe 탑재로 02(기본다듬기), 03(당당하게), 04(공손하게) 차별화 실증.
- **🏎️ 온디바이스 NPU 추론 블로킹 스톨 원천 해결 (Zero-Lock LiteRT Architecture)**:
  - `DearTalkIntentEngine`에서 네이티브 `session.close()`를 비동기 IO 코루틴 스코프로 격리하여, LiteRT C++ 락으로 인해 후속 추론이 8~10초간 대기하던 스톨 현상을 0ms로 완전 제거.
- **🛡️ 프롬프트 왜곡 방지 및 경량화 (Zero Distortion Protocol)**:
  - 군더더기 서술형 프롬프트를 컴팩트한 넘버링 규칙으로 압축하고, 최상단에 "원문 왜곡 금지 및 핵심 의미 100% 보존" 규칙을 명시하여 모델의 억지 의역 및 문맥 왜곡 원천 차단.
- **📱 메인 설정 및 온보딩 허브 전면 개편 (UI_SPEC.md v1.1.0 SSOT 준수)**:
  - 보안 상태 카드에 섞여 있던 한글 자판(두벌식/천지인) 선택기를 `키보드 환경 설정` 카드로 이전하여 설정 응집도 극대화.
  - `🎙️ DearTalk Live` 카드를 상시 노출하여 모드 전환 없이도 1:1 대면 실시간 통역을 바로 실행할 수 있도록 접근성(Discoverability) 개선.
- **🎙️ 음성 무음 감지 시간 단일 10초(10,000ms) 안전 대기 표준화**:
  - 사용자에게 인지 부하를 주던 설정 슬라이더를 완전히 제거하고, 단문/장문 구분 없이 단일 10초 안전 대기로 완전 자동화 (완료 시 [입력] 버튼으로 0ms 즉시 전송).
- **📖 전 화면 텍스트 반응형 가독성 최적화**:
  - 디바이스 너비에 따라 지저분하게 꺾이던 장문 설명들을 행동 중심의 단정한 1~2줄 컴팩트 카피로 전면 정제.

---

## [1.1.4] - 2026-09-20

### Added & Improved
- **🧠 Google Gemma 4 E2B LiteRT 단일 표준 고정 및 Qwen 잔재 전면 소거**:
  - 온디바이스 SLM을 Google Gemma 4 E2B 단일 표준으로 고정하고 프롬프트 파싱 및 설정 내 Qwen 잔재 완전 제거.
- **🔊 DearTalk Live TopAppBar 4대 직관적 액션 UX 완성**:
  - `[🔊/🔇 Auto-Speak (TTS)]` 상단 액션바 전진 배치로 통역 음성 즉시 음소거/재생 1-Tap 토글 지원.
  - `[🔄 180° 플립]`, `[🧹 대화 비우기]`, `[⚙️ 설정]` 4대 액션으로 현장 사용성 극대화.
- **📱 LiveSettingsBottomSheet 초슬림 미니멀 시트 개편**:
  - 200개 FIFO 링 버퍼 도입으로 불필요해진 레거시 보존 기간/세션 공유/스토리지 삭제 옵션 완전 소거.
  - 하드웨어 진단(RAM/스토리지 100% 오프라인 준비 완료) 및 TTS 재생 스위치만 남겨 스크롤 없는 완벽한 한눈 뷰 완성.

---

## [1.1.2] - 2026-09-13

### Fixed & Improved
- **🌐 3개 국어(KO, EN, ID) 완벽 다국어 로컬라이제이션 & 텍스트 잘림 방지 (Zero-Truncation Multilingual Polish)**:
  - Jetpack Compose 의도 칩(`softWrap = false`, `TextOverflow.Ellipsis`, 최적 폰트 크기 `10.sp`) 줄바꿈으로 인한 단어 실종 결함 원천 해결.
  - `FullScreenSymmetricStage`, `DearTalkLiveScreen`, `LiveSettingsBottomSheet` 내 잔여 하드코딩 문자열(화자 헤더, 상태 메시지, 다국어 드롭다운) 100% `UiStrings` 동적 다국어화 완료.
- **🔄 대면 180° 플립 뷰(Face-to-Face 180° Flip View) 완성 및 실기기 스토어 자산 확보**:
  - 상하 50:50 대칭 화면에서 맞은편 파트너를 위한 180도 역방향 뷰와 사용자 시야 정방향 뷰 간 완벽한 다국어 동기화.
  - 한국어, 영어, 인도네시아어 3개 언어 환경의 실제 Galaxy S22 Ultra 실기기 캡처 완결.
- **⚡ AI 문맥 의도 자동 인식 및 렌더링 안정화 (Stabilized Intent Chips & Zero Jitter)**:
  - 발화 의도(질문/설명/부탁/확인) 실시간 판별 및 렌더링 흔들림(Jitter) 현상 제거.

---

## [1.1.1] - 2026-09-13

### Added
- **🔄 180° 대면 회전 뷰 및 다국어 실시간 쇼케이스 검증**:
  - 맞은편 상대방을 위한 상하 대칭 회전 모드(`flip_view`) 및 3개 국어(한국어, 영어, 인도네시아어) 무결성 검증.
- **🛡️ 최초 발화 원음(`originalRawText`) 보존 아키텍처**:
  - SQLite `live_messages`에 `original_raw_text` 컬럼 신설(DB v2 마이그레이션)하여 사후 의도(질문/설명) 재작성 시에도 최초 발화 원음 영구 보존.
- **💬 영어 평서문 조동사 도치 보정 (`convertToDeclarativeEnglish`)**:
  - 온디바이스 SLM의 의문문 도치 편향을 교정하여 `STATEMENT` 선택 시 완벽한 평서문 어순 보장.

### Changed
- **🧹 레거시 `VoiceStudio` 완전 제거 및 `DearTalk Live` 단일화**:
  - 구버전 단일 마이크 화면(`VoiceStudioActivity`) 및 관련 서브시스템을 영구 삭제하고 `DearTalk Live`로 단일화.
  - 온디바이스 AI 모델 팩(Gemma 4) 관리 및 하드웨어 사양 진단을 `DearTalk Live` 설정 바텀시트(`LiveSettingsBottomSheet`)로 완전 흡수.
- **⚡ 키보드 AI 팩 원클릭 자동 다운로드**:
  - 키보드 배너 클릭 시 `DearTalk Live` 설정 시트로 직행하여 다운로드가 즉시 자동 시작되도록 사용자 경험(UX) 개선.

---

## [1.1.0] - 2026-09-12

### Added
- **🎙️ DearTalk Live (Real-Time 1:1 Face-to-Face Voice Messenger)**:
  - 여행 및 비즈니스 현장 특화 2-Way 실시간 통역 메신저 런칭 (`ai.deartalk.android.live`).
  - **대형 듀얼 마이크 액션 바**: `[🗣️ 내가 말하기]`(내 언어 STT ➔ AI 번역 ➔ 상대방 언어로 자동 TTS 발화) & `[👂 상대방 듣기]`(상대방 언어 STT ➔ 내 언어로 번역 ➔ 스크립트 텍스트 렌더링).
  - **100% 오프라인 SQLite 로컬 저장소**: 네트워크 연결 없이도 모든 대화 세션 및 메시지 무손실 영구 보존.
  - **대면 180도 플립 뷰 (Face-to-Face Flip View)**: 맞은편 외국인 파트너를 위해 상대방 말풍선 및 번역문을 180도 회전 렌더링.
  - **연속 청취 / 강의 레코더 모드 (Continuous Listening)**: 발화 종료 후 자동으로 다음 음성을 연속 청취하는 무인 루프.
  - **대화록 마크다운 내보내기 & 시스템 원터치 공유**: 전체 대화 세션을 마크다운 파일로 직렬화하여 카카오톡, 이메일, 클라우드로 공유.
- **⚡ 지능형 음성 인식(ASR) 오인식 문맥 보정 (Context-Aware Speech Repair)**:
  - 직전 5개 턴의 대화 맥락을 온디바이스 SLM(Gemma 4)에 주입하여 음향적 오인식(예: 호텔 대화 중 '포항 되나요' ➔ '포함 되나요')을 원래 의도대로 똑똑하게 복원.
  - **3대 메타 원칙(Fidelity-First)** 준수: 원형 보존(Tone & Voice), 최소 교정(Minimal Polish), 모호할 땐 평서문 기본값 처리.
- **⏱️ 10초 장문 무음 감지 확장 & 원터치 `[⏹️ 말씀 완료]` 토글**:
  - 상대방 장문 설명 중 숨을 고를 때 끊기지 않도록 VAD 무음 대기 한계를 10초로 대폭 확장.
  - 상대방 발화가 끝났을 때 탭 한 번으로 0.1초 만에 즉시 번역으로 직결되는 즉시 완료 토글 탑재.
- **📥 상단 언어 선택 시 온디바이스 STT 언어팩 자동 선제 다운로드 & TTS 프리웜**:
  - 언어 선택 즉시 시스템에 온디바이스 모델 다운로드를 트리거하여 첫 발화 대기 시간 0초 달성.

### Changed
- **2-Tier 계층화 & 2단 반응형 언어 선택 카드 UI**:
  - `✨ 공식 지원 3대 언어`(한국어, 영어, 인도네시아어)와 `🌐 교차 통역 언어` 분리.
  - 구글/애플 번역 스타일의 2단 카드(상단: 화자/뱃지, 하단: 언어명/드롭다운)로 가로 150dp 내에서 10글자 이상의 긴 언어명도 줄바꿈 0% 완벽 피팅.
- **공식 대외 이메일 및 테스터 커뮤니티 일원화**:
  - 모든 대외 문의 및 보안 창구를 `onthelogic@gmail.com`으로 통일.
  - 오픈소스 테스터 커뮤니티를 `aibuilder-testers@googlegroups.com` (`https://groups.google.com/g/aibuilder-testers`)로 갱신.

---

## [1.0.10] - 2026-09-10

### Fixed
- **Cheonjiin Automata Crash Resolution**: Eliminated `ArrayIndexOutOfBoundsException: length=21; index=-2` in `CheonjiinComposer.kt` by strictly bounding `jung` index to `0..20` and safely handling intermediate araea (`"ㆍ"`, `"ㆍㆍ"`) transitions.
- **IME Keystroke Circuit Breaker**: Wrapped all keyboard input event handlers with `runCatching` to prevent keyboard service termination and ensure instant self-healing on unexpected edge cases.

### Changed
- **Samsung-Style Cheonjiin Minimal Layout**: Removed legacy explanatory subtexts ("사람", "하늘", "땅", "ㄲ", "ㄸ") for a clean, distraction-free 18sp Korean 3x4 keypad mirroring genuine Samsung Cheonjiin.

### Added
- **Global Punctuation Access (Bottom Bar)**: Integrated permanent comma (`,`) and period (`.`) keys directly on the bottom common bar across all keyboards (`[!#1] [KOR/ENG] [,] [Space] [.] [Enter]`).
- **Rich 2-Page Special Symbols (`SymbolKeyboardLayout`)**: Expanded symbol keyboard into a 2-page pagination system (`1/2` and `2/2`) providing full punctuation (`.`, `,`, `?`, `!`), currencies (`₩`, `$`, `€`, `£`), math operators (`≠`, `≤`, `≥`, `÷`, `×`), and brackets.
- **Enterprise Crash Diagnostics (`CrashLogger`)**: Registered `DearTalkApplication` with a custom `UncaughtExceptionHandler` that writes persistent diagnostic crash dumps to `files/crash_logs/` for fail-safe post-mortem debugging.

---

## [1.0.9] - 2026-09-10

### Added
- **Voice Studio Sticky Bottom Bar**: Mic button now permanently fixed at screen bottom via `Scaffold(bottomBar)` + `navigationBarsPadding()`, eliminating system navigation bar overlap.
- **Mode Switch Auto Re-translation**: Switching between Tone Transform and Live Interpretation modes now automatically re-processes existing text through the new pipeline.
- **Model Purge Confirmation Dialog**: `AlertDialog` safety prompt before deleting on-device AI model package (~1.2GB).
- **Download Error Retry Button**: `OutlinedButton` with refresh icon in Error state, enabling immediate re-download without app restart.
- **Quick Translation Sample Sentences**: Mode-specific sample chips for both Tone Transform and Live Interpretation modes (3 languages: KO/EN/ID).

### Changed
- **UX Copy Refinement**: Replaced technical terminology "발화 음색(發話 音色)" → "🎙️ 목소리 설정" and "톤 매칭" → "🎚️ 목소리 톤:" for user-friendly language.
- **On-Device SLM Engine**: Standardized to Google Gemma 4 E2B across all UI labels and model detection paths.

### Fixed
- **Recording Button Label Wrapping**: Added `maxLines = 1` + `TextOverflow.Ellipsis` to prevent forced line breaks on narrow screens or large font accessibility settings.

### Accessibility
- **TalkBack Semantics**: Added `selectable(selected, role = Role.Tab)` to gender toggle and mode tabs, `Role.RadioButton` to pitch chips, and `semantics { role = Role.Button }` to main mic button for screen reader support.

---

## [1.0.8] - 2026-09-03

### Added
- **Human-Centered 2-Slot Utterance Cache (`AppScopedUtteranceCache`)**:
  - Implemented an app-scoped in-memory sliding cache based on human working memory limits (`maxApps = 2`: Primary Focus + Secondary Toggled App).
  - Eliminates subject/pronoun omission ("3박 4일 일정은요?" -> "발리 3박 4일 일정은요?") and homophone typos by injecting prior conversation context into SLM prompts.
  - Zero-Allocation Circular Ring Buffer (`CircularUtteranceBuffer`): Overwrites slots in-place to ensure zero GC overhead, zero keyboard jitter, and fixed memory footprint under 500 bytes.
  - 3-minute TTL auto-expiration and pure RAM residency for strict Google Play privacy compliance.

### Refactored
- **Zero Language Bias in Context Injection**:
  - Uniformly injected localized conversation context blocks across Korean, Indonesian, and English/global prompts.
  - Propagated target app `packageName` through `processIntent` and `applyTone` across all voice pipeline flows.
  - Cleaned prototype legacy parameter naming (`simulatedVoiceText` -> `voiceText`).

---

## [1.0.7] - 2026-09-02

### Fixed
- **Speech Recognition Infinite Sound Loop Prevention**:
  - Resolved `SpeechRecognitionManager` defect where `ERROR_NO_MATCH` and `ERROR_SPEECH_TIMEOUT` triggered an infinite rapid keep-alive reconnection loop and system beep noise cycle on silence.
  - Safely falls back to `VoiceState.FinalResult` when prior recognized text exists, or cleanly transitions to `VoiceState.Idle`.
- **Samsung One UI System Navigation Bar & Keyboard Switch Overlap**:
  - Added `.navigationBarsPadding()` and 8.dp bottom safety padding to `DearTalkScreen` and `StandardKeyboardView` root surfaces.
  - Completely eliminates touch collision between IME control buttons (`Clear`, `Backspace`) and Samsung Galaxy gesture handles or system keyboard switch icons.

---

## [1.0.6] - 2026-09-02

### Changed
- **Target SDK 36 (Android 16) Full Compliance**:
  - Upgraded `compileSdk` and `targetSdk` to API 36 to meet Google Play 2026 platform security and performance requirements.

---

## [1.0.5] - 2026-09-02

### Added
- **1-Tap Partner Language Pair UX (`TargetLanguageSelectorRow`)**:
  - Replaced legacy 2-way input/output dropdowns with a minimalist single-tap partner language selector.
  - Automatically binds primary language to device/app system locale and forms bi-directional conversation pairs (`🇰🇷 한국어 ⇄ 🇺🇸 English`).
- **Smart Bi-Directional Auto-Swap (`LanguageLocaleHelper.detectLanguageCode`)**:
  - Automatically identifies script types (Hangul, Latin/English, Japanese Kana, Chinese Hanzi, Thai) in voice speech and dynamically swaps interpretation direction without requiring manual button presses.
- **Script-Aware Multi-language TTS Engine**:
  - Dynamically binds the synthesized voice engine to match the actual script of the generated translation text for pristine vocal accuracy.
- **Unified High-Performance On-Device Model Sharing & Adaptive AI Tiers (`ActiveAiTier`)**:
  - Shared downloaded Gemma 4 model across Keyboard IME and Voice/Live pipelines.
  - Added 3-tier intelligence state machine (`GEMMA_4`, `BASE_GEMMA`, `STT_ONLY`) with zero-failure pure STT support on budget devices.

### Fixed
- **Multilingual Tone Instructions & Pipeline Error Localization**:
  - Converted tone instructions and pipeline exception messages into localized `UiStrings` properties across EN, KO, and ID.

---

## [1.0.4] - 2026-09-01

### Added
- **On-Device AI Voice Studio & Real-time Interpretation (`VoiceStudioActivity`)**:
  - Independent full-screen activity isolating memory-intensive STT/LLM/TTS operations from IME keyboard process.
  - **2-Way Language Selector & 1-Tap Swap**: `[ 🗣️ Spoken Input ] ⇄ [ 🌐 Target Output ]` architecture with one-touch reverse conversation direction.
  - **Zero Hardcoding Dynamic Prompt Engine**: Context-driven simultaneous interpreter prompt supporting real-time translation across 12 languages.
  - **Acoustic Model Language Binding**: Dynamically passes target `Locale` to `SpeechRecognitionManager` to eliminate acoustic recognition crosstalk.
  - **Voice Customizer & Pitch Control**: Female/Male voice selector and 4-tier pitch controls (`Normal`, `Deep Low`, `Warm Mid`, `Bright High`).
  - **Zero-Latency Audio Replay (`speakDirectly`)**: Direct TTS replay without LLM re-inference on speaker tap.
- **Hardware & RAM Diagnostic System (`SystemDiagnosticManager`)**:
  - Pre-flight 3-tier RAM & storage evaluation (🟢 Optimal 8GB+, 🟡 Caution/Sequential 6GB, 🔴 Constrained <=4GB).
- **Play Asset Delivery (PAD) On-Demand Model Lifecycle (`ModelLifecycleManager`)**:
  - Google Play standard On-Demand asset state machine with live progress tracking and 1-click package purge.

### Fixed
- **Locale-Aware Standard Keyboard Default Layout**: Dynamically selects standard keyboard layout based on active locale in `StandardKeyboardView.kt` (Hangul 2-set for Korean, standard Latin QWERTY for non-Korean locales).
- **Adaptive Key Labels**: Updated symbol return buttons (`ABC` vs `한글`) and language toggle chips (`ENG` vs `KOR`) to reactively match the active locale.
- **Speech Recognizer Keep-Alive**: Prevents premature voice cancellation during continuous speaking with RMS volume separation.

---

## [1.0.3] - 2026-08-30

### Fixed
- **Contextual Punctuation & Question Mark (`?`) Completion**: Enhanced on-device SLM Korean prompt instructions and few-shot examples to intelligently attach question marks (`?`), exclamation marks (`!`), and periods (`.`) based on speech context (e.g., schedule inquiry, opinion checking, gratitude) without artificial string manipulation (`Zero Fake Rules` compliant).

---

## [1.0.2] - 2026-08-30

### Fixed
- **Multilingual STT Code-Switching**: Removed `EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE` and enabled `en-US` as an auxiliary language in `SpeechRecognitionManager.kt`, resolving the issue where English words (e.g., "AI", "PPT", "API") in Korean mixed speech were distorted into Korean phonetic transliterations.
- **Proper Noun & Acronym LLM Principles**: Updated on-device SLM prompt instructions to strictly follow standard terminology and proper noun formatting rules without artificial string replacement hacks (`Zero Fake Rules` compliant).

### Added
- **Mixed-Language Detection & Fallback Unit Tests**: Added automated regression unit tests in `DearTalkIntentEngineTest.kt` verifying language classification and raw input preservation.

---

## [1.0.1] - 2026-08-29

### Fixed
- **Zero-Latency Reactive i18n**: Refactored `UiStrings` to Jetpack Compose `mutableStateOf` state, eliminating stale static UI labels and ensuring 0ms instant text updates for `[Apply]`, `[Cancel]`, and `[Space]` buttons upon language switching in settings.
- **Voice Button Label Truncation**: Streamlined voice recording button label from 22 characters (`🔴 듣고 있어요...`) to compact 12 characters (`듣는 중 (터치 시 완료)` / `Listening (Tap to finish)` / `Mendengarkan (Ketuk selesai)`), preventing text truncation across mobile and foldable screens.
- **Cheeky Tone Alignment**: Synchronized `[😼 당당하게]` (Cheeky) button label and prompt intent matching across `DearTalkIntentEngine.kt`, `UiStrings.kt`, and tri-lingual READMEs.

### Changed
- **Pure 6-Tone IME Bar**: Removed redundant `[🌐 번역 ▾]` dropdown menu from the keyboard top bar, establishing a clean and responsive 6 tone presets chip bar.

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
