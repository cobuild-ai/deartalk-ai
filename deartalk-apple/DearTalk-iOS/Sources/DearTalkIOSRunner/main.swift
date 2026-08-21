import Foundation
import DearTalkIOSCore

@main
struct DearTalkIOSRunner {
    @MainActor
    static func main() async {
        print("========================================================")
        print("🚀 [DearTalkIOSRunner] Starting iOS Core Verification Suite")
        print("========================================================")

        // 1. Engine Initialization & Model Discovery Check
        let engine = DearTalkIntentEngine.shared
        print("🔒 [1/4] Engine state: isModelLoaded=\(engine.isModelLoaded)")

        // 2. DiffEngine Insertion & Deletion Tests
        let old = "오늘 날씨 좋다"
        let new = "오늘 날씨 정말 좋다"
        let diffs = DiffEngine.computeDiff(oldText: old, newText: new)
        let hasInsert = diffs.contains {
            if case .insert(let str) = $0 { return str.contains("정말") }
            return false
        }
        assert(hasInsert, "DiffEngine insert failed!")
        print("✨ [2/4] DiffEngine test PASSED (Found insert: '정말')")

        // 3. CustomToneManager Tests
        let toneManager = CustomToneManager.shared
        let initialCount = toneManager.systemTones.count
        assert(initialCount == 6, "Default system tone count should be 6")
        toneManager.addTone(title: "테스트톤", instruction: "지시사항")
        assert(toneManager.allTones.contains { $0.title == "테스트톤" }, "Custom tone add failed")
        if let added = toneManager.customTones.first(where: { $0.title == "테스트톤" }) {
            toneManager.removeTone(id: added.id)
            assert(!toneManager.customTones.contains { $0.id == added.id }, "Custom tone remove failed")
        }
        print("🎨 [3/4] CustomToneManager test PASSED")

        // 4. Zero Fake Rules Fallback Test
        let testInput = "내일 회의 일정 공유드립니다"
        let result = await engine.process(text: testInput)
        assert(result.text == testInput || engine.isModelLoaded, "Zero fake rules fallback failed: original text must be preserved when model is not loaded")
        print("🛡️ [4/4] Zero Fake Rules Honest Fallback test PASSED")

        print("========================================================")
        print("🎉 [DearTalkIOSRunner] ALL iOS CORE TESTS PASSED SUCCESSFULLY!")
        print("========================================================")
    }
}
