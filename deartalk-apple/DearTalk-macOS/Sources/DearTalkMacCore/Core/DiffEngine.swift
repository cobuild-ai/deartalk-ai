import Foundation
import SwiftUI

public enum DiffOp: Equatable, Hashable {
    case unchanged(String)
    case removed(String)
    case added(String)

    public var text: String {
        switch self {
        case .unchanged(let s), .removed(let s), .added(let s):
            return s
        }
    }
}

public struct DiffResult: Equatable {
    public let original: String
    public let suggested: String
    public let operations: [DiffOp]
    public let hasChanges: Bool

    public init(original: String, suggested: String, operations: [DiffOp]) {
        self.original = original
        self.suggested = suggested
        self.operations = operations
        self.hasChanges = operations.contains {
            if case .unchanged = $0 { return false }
            return true
        }
    }
}

/// 단어/문자 단위 LCS (Longest Common Subsequence) 기반 스마트 DIFF 계산기
public enum DiffEngine {
    public static func computeWordDiff(original: String, suggested: String) -> DiffResult {
        let origTokens = tokenize(original)
        let suggTokens = tokenize(suggested)

        if original.trimmingCharacters(in: .whitespacesAndNewlines) == suggested.trimmingCharacters(in: .whitespacesAndNewlines) {
            return DiffResult(original: original, suggested: suggested, operations: [.unchanged(suggested)])
        }

        let lcsMatrix = computeLCSMatrix(origTokens, suggTokens)
        var ops: [DiffOp] = []

        var i = origTokens.count
        var j = suggTokens.count

        while i > 0 || j > 0 {
            if i > 0 && j > 0 && origTokens[i - 1] == suggTokens[j - 1] {
                ops.append(.unchanged(origTokens[i - 1]))
                i -= 1
                j -= 1
            } else if j > 0 && (i == 0 || lcsMatrix[i][j - 1] >= lcsMatrix[i - 1][j]) {
                ops.append(.added(suggTokens[j - 1]))
                j -= 1
            } else if i > 0 && (j == 0 || lcsMatrix[i][j - 1] < lcsMatrix[i - 1][j]) {
                ops.append(.removed(origTokens[i - 1]))
                i -= 1
            }
        }

        let mergedOps = mergeConsecutiveOps(ops.reversed())
        return DiffResult(original: original, suggested: suggested, operations: mergedOps)
    }

    private static func tokenize(_ text: String) -> [String] {
        var tokens: [String] = []
        var current = ""

        for char in text {
            if char.isWhitespace || char.isPunctuation {
                if !current.isEmpty {
                    tokens.append(current)
                    current = ""
                }
                tokens.append(String(char))
            } else {
                current.append(char)
            }
        }
        if !current.isEmpty {
            tokens.append(current)
        }
        return tokens
    }

    private static func computeLCSMatrix(_ a: [String], _ b: [String]) -> [[Int]] {
        let n = a.count
        let m = b.count
        var dp = Array(repeating: Array(repeating: 0, count: m + 1), count: n + 1)

        for i in 1...n {
            for j in 1...m {
                if a[i - 1] == b[j - 1] {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        return dp
    }

    private static func mergeConsecutiveOps(_ ops: [DiffOp]) -> [DiffOp] {
        var merged: [DiffOp] = []
        for op in ops {
            guard let last = merged.last else {
                merged.append(op)
                continue
            }

            switch (last, op) {
            case (.unchanged(let a), .unchanged(let b)):
                merged.removeLast()
                merged.append(.unchanged(a + b))
            case (.removed(let a), .removed(let b)):
                merged.removeLast()
                merged.append(.removed(a + b))
            case (.added(let a), .added(let b)):
                merged.removeLast()
                merged.append(.added(a + b))
            default:
                merged.append(op)
            }
        }
        return merged
    }
}
