import Foundation

public struct CustomTone: Identifiable, Codable, Equatable {
    public let id: String
    public var title: String
    public var instruction: String
    public var isSystemDefault: Bool

    public init(id: String = UUID().uuidString, title: String, instruction: String, isSystemDefault: Bool = false) {
        self.id = id
        self.title = title
        self.instruction = instruction
        self.isSystemDefault = isSystemDefault
    }
}

public final class CustomToneManager: ObservableObject {
    public static let shared = CustomToneManager()
    private let userDefaultsKey = "ai.deartalk.custom_tones"

    @Published public private(set) var customTones: [CustomTone] = []

    public init() {
        loadTones()
    }

    public var systemTones: [CustomTone] {
        [
            CustomTone(id: "refine", title: UiStrings.toneRefine, instruction: UiStrings.toneRefineInstruction, isSystemDefault: true),
            CustomTone(id: "polite", title: UiStrings.tonePolite, instruction: UiStrings.tonePoliteInstruction, isSystemDefault: true),
            CustomTone(id: "casual", title: UiStrings.toneCasual, instruction: UiStrings.toneCasualInstruction, isSystemDefault: true),
            CustomTone(id: "business", title: UiStrings.toneBusiness, instruction: UiStrings.toneBusinessInstruction, isSystemDefault: true),
            CustomTone(id: "funny", title: UiStrings.toneFunny, instruction: UiStrings.toneFunnyInstruction, isSystemDefault: true),
            CustomTone(id: "cheeky", title: UiStrings.toneCheeky, instruction: UiStrings.toneCheekyInstruction, isSystemDefault: true)
        ]
    }

    public var allTones: [CustomTone] {
        systemTones + customTones
    }

    public func addTone(title: String, instruction: String) {
        let newTone = CustomTone(title: title, instruction: instruction, isSystemDefault: false)
        customTones.append(newTone)
        saveTones()
    }

    public func removeTone(id: String) {
        customTones.removeAll { $0.id == id }
        saveTones()
    }

    private func loadTones() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let tones = try? JSONDecoder().decode([CustomTone].self, from: data) else {
            return
        }
        self.customTones = tones
    }

    private func saveTones() {
        guard let data = try? JSONEncoder().encode(customTones) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }
}
