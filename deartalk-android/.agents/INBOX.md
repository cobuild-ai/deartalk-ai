# 📥 Human Request Inbox & Backlog

> 인간(사용자)이 남긴 기능 요청 및 아이디어 보관함입니다.
> AI 에이전트는 주기적 실행 시 최상단 미완료(`- [ ]`) 항목을 최우선으로 처리합니다.

---

## 📌 상태 표기 규칙 (Status Protocol)
- `- [ ]`: **대기 중 (Pending)** - AI가 다음 루프에서 우선적으로 구현합니다.
- `- [?]`: **질문/설계 확인 필요 (Clarification Needed)** - 모호한 사항에 대해 AI가 옵션(A/B)을 남겨둔 상태입니다.
- `- [x]`: **완료됨 (Completed)** - AI가 구현 및 검증을 완료한 상태입니다.

---

## 🔥 우선 구현 요청 (Priority Tasks)

---

## 💡 아이디어 보관함 (Ideas & Backlog)


- [x] (완료: 2026-08-27 21:50) Google Play 출시 규격 충족, Zero-Permission 보안 아키텍처, Play Asset Delivery (PAD) 및 기기별 2-Track 하이브리드 UX 구현
  > 📌 완료 내역: `INTERNET` 권한 완전 제거, `compileSdk/targetSdk 35` 상향, `DearTalkLog` 보안 로깅 도입, `:deartalk_model_pack` 에셋 팩 모듈 연동, `DeviceCapabilityDetector` (Pixel/S24 NPU vs PAD SLM) 구축, `DearTalkScreen` 키보드 툴바 상태 머신 연동 및 메인 앱 기기 안내 카드 탑재. AAB(27.6MB)/APK(57.6MB) 릴리즈 빌드 100% 검증.

- [x] (완료: 2026-08-27 19:58) 리펙토링 가능여부 확인하고, 읽기 쉬운 코드로 수정할 것
  > 📌 완료 내역: `DearTalkIntentEngine.kt` 프롬프트 빌더 메서드 분리(`buildRefinePrompt`, `buildTonePrompt`, `buildTranslationPrompt`), LLM 마크다운 코드블록/태그 클렌징 로직 고도화, 가독성 향상 리팩토링 및 `CommonCoreEngineTest` TDD 검증 완료.

- [x] (완료: 2026-08-27 19:58) 다국어 처리 및 번영여부가 톤앤매너대로 처리되어야 하며 영어는 영어로 즉 작성 글 기준으로 톤앤매너가 적용되어야 함
  > 📌 완료 내역: `hasKorean`/`isEnglish` 언어 감지 기반으로 원문이 영어인 경우 영어 톤앤매너 변환 프롬프트 적용 및 한국어 번역 왜곡 방지, 다국어 입력 시 원문 보존성 보장 및 TDD 단위 테스트(Test 6, 7, 8) 100% 통과.
