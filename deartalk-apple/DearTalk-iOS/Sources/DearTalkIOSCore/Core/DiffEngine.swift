import Foundation

public enum DiffOperation: Equatable {
    case equal(String)
    case insert(String)
    case delete(String)
}

public final class DiffEngine {
    public static func computeDiff(oldText: String, newText: String) -> [DiffOperation] {
        let oldChars = Array(oldText)
        let newChars = Array(newText)

        let m = oldChars.count
        let n = newChars.count

        var dp = Array(repeating: Array(repeating: 0, count: n + 1), count: m + 1)

        for i in 0..<m {
            for j in 0..<n {
                if oldChars[i] == newChars[j] {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = max(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        var i = m
        var j = n
        var operations: [DiffOperation] = []

        while i > 0 || j > 0 {
            if i > 0 && j > 0 && oldChars[i - 1] == newChars[j - 1] {
                operations.append(.equal(String(oldChars[i - 1])))
                i -= 1
                j -= 1
            } else if j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) {
                operations.append(.insert(String(newChars[j - 1])))
                j -= 1
            } else if i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j]) {
                operations.append(.delete(String(oldChars[i - 1])))
                i -= 1
            }
        }

        operations.reverse()
        return coalesceOperations(operations)
    }

    private static func coalesceOperations(_ operations: [DiffOperation]) -> [DiffOperation] {
        var result: [DiffOperation] = []
        for op in operations {
            if let last = result.last {
                switch (last, op) {
                case (.equal(let a), .equal(let b)):
                    result[result.count - 1] = .equal(a + b)
                case (.insert(let a), .insert(let b)):
                    result[result.count - 1] = .insert(a + b)
                case (.delete(let a), .delete(let b)):
                    result[result.count - 1] = .delete(a + b)
                default:
                    result.append(op)
                }
            } else {
                result.append(op)
            }
        }
        return result
    }
}
