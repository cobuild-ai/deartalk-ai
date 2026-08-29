# DearTalkAI 프로젝트 불변의 철칙 (Core Principles)

## 🚨 제1철칙: 꼼수는 절대 금지한다 (Zero Fake Rules)
1. **하드코딩 흉내 내기 영구 금지:**
   - `if/else`, 문자열 `contains()`, `replace()`, 정규식 기반 어미 조립 등 인공지능을 흉내 내려는 어떠한 가짜 규칙도 코드베이스에 작성하지 않는다.
2. **100% 순수 신경망(LLM) 온디바이스 추론:**
   - 문맥 파악, 지시어 분리, 오탈자 교정, 톤앤매너 완성은 오직 Google Gemma 온디바이스 LLM 신경망 모델의 실시간 추론(Inference)으로만 처리한다.
3. **정직한 엔지니어링 (Transparency):**
   - 모델 로딩이나 런타임에 문제가 있을 경우 가짜 대체 로직으로 속이지 않고, 정직하게 문제를 해결하고 정석대로 구현한다.
4. **완벽한 온디바이스 프라이버시 (Zero Network):**
   - 키보드의 보안 특성상 외부 네트워크 통신은 0%를 유지하며, 모든 AI 연산은 스마트폰 기기 내부 NPU/GPU/CPU에서만 실행한다.
5. **Play Asset Delivery (PAD) 모델 배포 표준 (Zero In-App Download):**
   - 대용량 온디바이스 모델 바이너리는 Google Play 공식 표준인 Play Asset Delivery(PAD) 및 기기 로컬 에셋 경로를 통해 로드하며, 인앱 HTTP 네트워크 다운로더를 일체 사용하지 않는다.

---

## 🏛️ 엔터프라이즈 4대 불변 거버넌스 원칙 (Enterprise 4 Axioms)
1. 🛡️ **`main` 브랜치 보호 (Protected Branch)**: `01-production`의 `main` 브랜치는 직접 커밋/푸시가 금지되며, 항상 배포 가능한 상태를 유지한다.
2. 📦 **원자적 PR (Atomic Pull Request)**: 단일 목적의 기능/수정만 분리된 작업 브랜치(`feat/`, `refactor/`)에서 구현하고 PR로 머지한다.
3. 🛡️ **3-Tier 아키텍처 (3-Tier Governance)**: `01-production` ➔ `02-oss-ready` ➔ `03-oss-public` 단계를 거쳐 시크릿 정제 및 독립 빌드 검증을 보장한다.
4. 🔄 **역방향 백포트 (Backporting -x)**: 외부 기여 커밋은 `git cherry-pick -x`로 원본 작성자 맥락을 보존하며 프로덕션으로 흡수한다.
