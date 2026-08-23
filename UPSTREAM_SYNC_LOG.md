# 🏛️ DearTalk AI: Upstream Sync & Audit History

이 문서는 Downstream(개인/배포판)에서 Upstream(`cobuild-ai` 오픈소스 코어)으로 코드를 동기화(환원)할 때 수행된 **사전 감사(Pre-flight Audit), 기밀성 점검, 라이선스 검증, 변경 내역**을 영구적으로 기록하는 표준 거버넌스 로그입니다.  
향후 **AI 기반 완전 자동화 업스트리밍 파이프라인**이 가동될 때 검증 기준(Ground Truth) 및 히스토리 스키마로 활용됩니다.

---

## 📌 Sync Metadata Index

| Sync ID | Date (UTC/KST) | Source Snapshot | Target Tag | Status | Auditor / Agent |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `SYNC-20260823-001` | 2026-08-23 20:40 KST | `downstream/deartalk-ai` | `v0.9.0-core-snapshot` | ✅ Passed & Synced | Antigravity AI + User |

---

## 📜 Detailed Audit Records

### [SYNC-20260823-001] Initial Core Upstreaming (Play Store Pre-release Baseline)

- **동기화 일시**: 2026-08-23T20:40:00+09:00
- **동기화 목적**: Play Store 상용 배포 설정(R8 난독화, Keystore 서명, 비공개 메타데이터 적용)에 들어가기 전, **순수 오픈소스 코어 및 아키텍처 원형을 Upstream에 안전하게 백업 및 동기화**.

#### 1. 🛡️ 보안 & 기밀성 감사 (Confidentiality & Sanitization)
- [x] **하드코딩 시크릿 검사**: API Key, 개인 비밀번호, Auth 토큰 없음 확인 (`pass`)
- [x] **개인 서명 키(`*.jks`, `*.keystore`) 검사**: 릴리즈 키스토어 미포함 확인 (`pass`)
- [x] **로컬 설정 및 경로 파일 제외**:
  - Android: `local.properties`, `.gradle/`, `build/`, `.kotlin/` 제외 완료
  - macOS: `.build/`, `DerivedData/` 제외 완료
  - OS 아티팩트: `.DS_Store`, `logs/` 제외 완료
- [x] **로컬 절대 경로 노출 검사**: 코드 내 하드코딩된 개발자 로컬 홈 경로 없음 확인 (`pass`)

#### 2. ⚖️ 오픈소스 라이선스 & 거버넌스 (Licensing & Governance)
- [x] **루트 라이선스 파일**: `LICENSE` (Apache License 2.0) 정상 유지 (`pass`)
- [x] **커뮤니티 가이드라인**: `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md` 포함 확인 (`pass`)
- [x] **문서화 동기화**: `README.md`, `docs/ARCHITECTURE.md`, `docs/MODELS.md`, `docs/ROADMAP.md` 포함 확인 (`pass`)

#### 3. 📦 동기화된 컴포넌트 내역 (Included Components)
1. **`deartalk-android/`**:
   - Google LiteRT-LM (`.litertlm`) & MediaPipe GenAI 기반 온디바이스 음성 키보드(IME) 코어
   - Android Intent Engine, STT/TTS 모듈, Room SQLite 데이터 레이어
   - `build.gradle.kts` (SDK 34 기반)
2. **`deartalk-apple/DearTalk-macOS/`**:
   - Swift/SwiftUI 기반 macOS 메뉴바 UI, 온보딩, 샌드박스
   - Accessibility API 기반 실시간 텍스트 대치(Diff Engine) 코어
3. **`deartalk-shared/`**:
   - 시스템 프롬프트(Prompt templates), 어조(Tone) 규칙, 디자인 에셋

#### 4. 🤖 AI 자동화 연동 메타데이터 (Machine-Readable Block)
```json
{
  "sync_id": "SYNC-20260823-001",
  "timestamp": "2026-08-23T20:40:00+09:00",
  "source_path": "OSSProject/downstream/deartalk-ai",
  "target_path": "OSSProject/upstream/deartalk-ai",
  "license": "Apache-2.0",
  "sanitization_rules_applied": [
    "exclude_build_artifacts",
    "exclude_local_properties",
    "exclude_keystores",
    "exclude_ds_store"
  ],
  "audit_result": "SUCCESS"
}
```

---
