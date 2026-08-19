import Foundation
import DearTalkMacCore

print("==================================================")
print("🚀 [DearTalkMac Runner & Verification]")
print("==================================================")

// 1. UiStrings 검증
print("\n[1] UiStrings 다국어 지원 검증")
print("• 앱 이름: \(UiStrings.appName)")
print("• 메뉴바 타이틀: \(UiStrings.menuBarTitle)")
print("• 샌드박스 타이틀: \(UiStrings.sandboxTitle)")
print("• 단축키 안내: \(UiStrings.applySuggestionShortcut)")
assert(!UiStrings.appName.isEmpty, "앱 이름이 비어있으면 안 됩니다.")

// 2. CustomToneManager 검증
print("\n[2] 톤앤매너 및 번역 타깃 검증")
let toneManager = CustomToneManager.shared
let tones = toneManager.defaultTones
print("• 등록된 기본 톤 개수: \(tones.count)")
for tone in tones {
    print("  - \(tone.icon) [\(tone.name)]: \(tone.instruction)")
}
assert(tones.count == 6, "기본 톤은 6개여야 합니다.")

let translations = toneManager.defaultTranslations
print("• 등록된 번역 타깃 개수: \(translations.count)")
for trans in translations {
    print("  - \(trans.flag) [\(trans.name)] (\(trans.targetLanguage))")
}
assert(translations.count == 8, "기본 번역 타깃은 8개여야 합니다.")

// 3. DiffEngine 검증
print("\n[3] DiffEngine 단어 단위 LCS Diff 검증")
let original = "내일 아침 9시 만나"
let suggested = "내일 오전 9시에 뵙겠습니다."
let diff = DiffEngine.computeWordDiff(original: original, suggested: suggested)

print("• 원문: '\(original)'")
print("• AI 제안: '\(suggested)'")
print("• Diff 변경 감지 여부: \(diff.hasChanges)")
print("• 분해된 연산 토큰:")
for op in diff.operations {
    switch op {
    case .unchanged(let text):
        print("  [= 유지]: '\(text)'")
    case .removed(let text):
        print("  [- 삭제]: '\(text)'")
    case .added(let text):
        print("  [+ 추가]: '\(text)'")
    }
}
assert(diff.hasChanges, "원문과 제안문의 차이가 감지되어야 합니다.")

let reconstructed = diff.operations.compactMap { op -> String? in
    switch op {
    case .unchanged(let s), .added(let s): return s
    case .removed: return nil
    }
}.joined()
assert(reconstructed == suggested, "Diff 복원문은 AI 제안문과 완벽히 일치해야 합니다.")

// 3-1. DiffEngine 경계 조건 검증 (빈 문자열, 특수기호, 단일 문자)
let emptyDiff1 = DiffEngine.computeWordDiff(original: "", suggested: "")
assert(!emptyDiff1.hasChanges && emptyDiff1.operations.isEmpty, "빈 문자열 Diff는 변경사항이 없어야 합니다.")

let emptyDiff2 = DiffEngine.computeWordDiff(original: "", suggested: "테스트")
assert(emptyDiff2.hasChanges, "원문이 비어있고 제안문이 있으면 변경사항이 감지되어야 합니다.")

let symbolDiff = DiffEngine.computeWordDiff(original: "@smilelife", suggested: "@smilelife!")
assert(symbolDiff.hasChanges, "특수문자 Diff가 크래시 없이 정상 계산되어야 합니다.")
print("• DiffEngine 경계값 및 특수문자 안정성 검증: 통과 ✅")

// 4. DearTalkIntentEngine 온디바이스 AI 추론 검증
print("\n[4] DearTalkIntentEngine 온디바이스 AI 추론 검증")
let engine = DearTalkIntentEngine.shared
print("• 온디바이스 모델 로드 상태: \(engine.isModelLoaded ? "로드됨" : "준비 대기 중 (정직한 원문 보존 모드)")")

// 4-1. 빈 문자열 처리
let emptyResult = await engine.process(textInput: "   ")
if case .error(_, let errorMsg) = emptyResult {
    print("• 빈 문자열 방어 검증: 통과 (\(errorMsg))")
} else {
    assertionFailure("빈 문자열은 에러를 반환해야 합니다.")
}

// 4-2. 사용자 자유 입력 원문 보존성 및 왜곡 방지 검증 (제1철칙 준수)
let userInput = "부탁할께 너의 고양이를 가져와"
let userResult = await engine.process(textInput: userInput)
var resultMsg = ""
if case .success(_, let msg) = userResult {
    resultMsg = msg
}
print("• 사용자 자유 입력: '\(userInput)'")
print("• 엔진 출력: '\(userResult.text)' (메시지: '\(resultMsg)')")

// 모델 미로드 시 임의의 가짜 어미 결합이 절대 발생하지 않아야 함
assert(!userResult.text.hasSuffix("부탁드리겠습니다."), "하드코딩된 가짜 어미가 조립되면 안 됩니다.")
assert(userResult.text == userInput || engine.isModelLoaded, "모델 미로드 시 원문이 100% 보존되어야 합니다.")
print("• 가짜 어미 덧붙이기 방지 및 원문 보존 검증: 통과 ✅")

// 4-3. LLM 출력 클렌징 필터 검증
let rawLlmOutput = "<start_of_turn>model\n최종 문장: 내일 아침 9시에 뵙겠습니다.<end_of_turn>"
let cleanedOutput = engine.cleanLlmOutput(rawLlmOutput)
print("• LLM 출력 클렌징: '\(rawLlmOutput)' ➔ '\(cleanedOutput)'")
assert(cleanedOutput == "내일 아침 9시에 뵙겠습니다.", "특수 제어 토큰 및 라벨이 완벽히 제거되어야 합니다.")

// 5. DearTalkLogger 통합 로거 검증
print("\n[5] DearTalkLogger 통합 로깅 시스템 검증")
DearTalkLogger.info("🧪 DearTalkMacRunner 코어 엔진 테스트 실행 중", category: "TestRunner")
let logPath = (NSHomeDirectory() as NSString).appendingPathComponent(".deartalk/deartalk.log")
let logExists = FileManager.default.fileExists(atPath: logPath)
print("• 로그 파일 생성 여부: \(logExists ? "생성됨 (\(logPath))" : "확인 필요")")
assert(logExists, "로그 파일이 생성되어야 합니다.")
print("• 로깅 시스템 검증: 통과 ✅")

print("\n==================================================")
print("✅ DearTalkMac 모든 코어 엔진 검증 통과 (제1철칙 100% 준수)")
print("==================================================")




