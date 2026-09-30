package ai.deartalk.android.ime

/**
 * 천지인(Cheonjiin) 한글 입력 오토마타를 위한 정적 테이블 및 합성/분리 순수 함수 유틸리티
 */
internal object CheonjiinTables {
    const val CHOSUNGS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    val JUNGSUNGS = listOf(
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    )

    val JONGSUNGS = listOf(
        "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    )

    // 복합 받침 매핑 (Pair(첫째받침 종성 인덱스, 둘째자음 초성 인덱스) -> 복합받침 종성 인덱스)
    val DOUBLE_JONG = mapOf(
        Pair(1, 9) to 3,   // ㄱ(jong 1) + ㅅ(cho 9) = ㄳ(jong 3)
        Pair(4, 12) to 5,  // ㄴ(jong 4) + ㅈ(cho 12) = ㄵ(jong 5)
        Pair(4, 18) to 6,  // ㄴ(jong 4) + ㅎ(cho 18) = ㄶ(jong 6)
        Pair(8, 0) to 9,   // ㄹ(jong 8) + ㄱ(cho 0) = ㄺ(jong 9)
        Pair(8, 6) to 10,  // ㄹ(jong 8) + ㅁ(cho 6) = ㄻ(jong 10)
        Pair(8, 7) to 11,  // ㄹ(jong 8) + ㅂ(cho 7) = ㄼ(jong 11)
        Pair(8, 9) to 12,  // ㄹ(jong 8) + ㅅ(cho 9) = ㄽ(jong 12)
        Pair(8, 16) to 13, // ㄹ(jong 8) + ㅌ(cho 16) = ㄾ(jong 13)
        Pair(8, 17) to 14, // ㄹ(jong 8) + ㅍ(cho 17) = ㄿ(jong 14)
        Pair(8, 18) to 15, // ㄹ(jong 8) + ㅎ(cho 18) = ㅀ(jong 15)
        Pair(17, 9) to 18  // ㅂ(jong 17) + ㅅ(cho 9) = ㅄ(jong 18)
    )

    // 자음 키 그룹별 순환 목록
    val CONSONANT_CYCLES = mapOf(
        'ㄱ' to listOf('ㄱ', 'ㅋ', 'ㄲ'),
        'ㄴ' to listOf('ㄴ', 'ㄹ'),
        'ㄷ' to listOf('ㄷ', 'ㅌ', 'ㄸ'),
        'ㅂ' to listOf('ㅂ', 'ㅍ', 'ㅃ'),
        'ㅅ' to listOf('ㅅ', 'ㅎ', 'ㅆ'),
        'ㅈ' to listOf('ㅈ', 'ㅊ', 'ㅉ'),
        'ㅇ' to listOf('ㅇ', 'ㅁ')
    )

    val PUNCTUATION_CYCLE = listOf('.', ',', '?', '!')

    fun canFormCompoundBatchim(baseJong: Int, keyGroup: Char): Boolean {
        val cycle = CONSONANT_CYCLES[keyGroup] ?: return false
        return cycle.any { ch ->
            val choIdx = CHOSUNGS.indexOf(ch)
            choIdx != -1 && DOUBLE_JONG.containsKey(Pair(baseJong, choIdx))
        }
    }

    fun splitJong(j: Int): Pair<Int, Int> {
        for ((pair, result) in DOUBLE_JONG) {
            if (result == j) {
                return Pair(pair.first, pair.second)
            }
        }
        val ch = JONGSUNGS[j]
        val choIdx = CHOSUNGS.indexOf(ch)
        return Pair(0, choIdx)
    }

    fun buildSingleSyllable(c: Int, j: Int, jng: Int, vowelBuffer: CharSequence = ""): String {
        if (c != -1 && j in 0..20) {
            val code = 0xAC00 + (c * 21 + j) * 28 + jng
            return code.toChar().toString()
        }
        if (c != -1 && j == -1) {
            if (vowelBuffer.isNotEmpty()) {
                return CHOSUNGS[c] + vowelBuffer.toString()
            }
            return CHOSUNGS[c].toString()
        }
        if (c == -1 && j in 0..20) {
            return JUNGSUNGS[j]
        }
        if (vowelBuffer.isNotEmpty()) {
            return vowelBuffer.toString()
        }
        return ""
    }

    /**
     * 천지인 모음 버퍼 문자열을 표준 중성 인덱스로 합성
     */
    fun synthesizeJung(buf: String): Int {
        return when (buf) {
            "ㅣ" -> 20 // ㅣ
            "ㅡ" -> 18 // ㅡ
            "ㆍ", "·", "." -> -2 // 아래아 1개 (단독/조합 전이)
            "ㆍㆍ", "··", "..", "ㆍ.", ".ㆍ" -> -2 // 아래아 2개 (ㅑ, ㅕ, ㅛ, ㅠ 전이)

            "ㅣㆍ", "ㅣ·", "ㅣ." -> 0  // ㅏ
            "ㅣㆍㆍ", "ㅣ··", "ㅣ..", "ㅣㆍ.", "ㅣ.ㆍ" -> 2 // ㅑ
            "ㆍㅣ", "·ㅣ", ".ㅣ" -> 4  // ㅓ
            "ㆍㆍㅣ", "··ㅣ", "..ㅣ", "ㆍ.ㅣ", ".ㆍㅣ" -> 6 // ㅕ

            "ㆍㅡ", "·ㅡ", ".ㅡ" -> 8  // ㅗ
            "ㆍㆍㅡ", "··ㅡ", "..ㅡ", "ㆍ.ㅡ", ".ㆍㅡ" -> 12 // ㅛ
            "ㅡㆍ", "ㅡ·", "ㅡ." -> 13 // ㅜ
            "ㅡㆍㆍ", "ㅡ··", "ㅡ..", "ㅡㆍ.", "ㅡ.ㆍ" -> 17 // ㅠ
            "ㅡㅣ" -> 19 // ㅢ

            // ㅐ, ㅒ, ㅔ, ㅖ
            "ㅣㆍㅣ", "ㅣ·ㅣ", "ㅣ.ㅣ" -> 1  // ㅐ (ㅏ + ㅣ)
            "ㅣㆍㆍㅣ", "ㅣ··ㅣ", "ㅣ..ㅣ", "ㅣㆍ.ㅣ", "ㅣ.ㆍㅣ" -> 3 // ㅒ (ㅑ + ㅣ)
            "ㆍㅣㅣ", "·ㅣㅣ", ".ㅣㅣ" -> 5  // ㅔ (ㅓ + ㅣ)
            "ㆍㆍㅣㅣ", "··ㅣㅣ", "..ㅣㅣ", "ㆍ.ㅣㅣ", ".ㆍㅣㅣ" -> 7 // ㅖ (ㅕ + ㅣ)

            // ㅘ, ㅙ, ㅚ
            "ㆍㅡㅣㆍ", "·ㅡㅣ·", ".ㅡㅣ." -> 9   // ㅘ (ㅗ + ㅏ)
            "ㆍㅡㅣㆍㅣ", "·ㅡㅣ·ㅣ", ".ㅡㅣ.ㅣ" -> 10 // ㅙ (ㅗ + ㅐ)
            "ㆍㅡㅣ", "·ㅡㅣ", ".ㅡㅣ" -> 11   // ㅚ (ㅗ + ㅣ)

            // ㅝ, ㅞ, ㅟ
            "ㅡㆍㆍㅣ", "ㅡ··ㅣ", "ㅡ..ㅣ", "ㅡㆍ.ㅣ", "ㅡ.ㆍㅣ" -> 14  // ㅝ (ㅜ + ㅓ)
            "ㅡㆍㆍㅣㅣ", "ㅡ··ㅣㅣ", "ㅡ..ㅣㅣ", "ㅡㆍ.ㅣㅣ", "ㅡ.ㆍㅣㅣ" -> 15 // ㅞ (ㅜ + ㅔ)
            "ㅡㆍㅣ", "ㅡ·ㅣ", "ㅡ.ㅣ" -> 16    // ㅟ (ㅜ + ㅣ)

            else -> -1
        }
    }
}
