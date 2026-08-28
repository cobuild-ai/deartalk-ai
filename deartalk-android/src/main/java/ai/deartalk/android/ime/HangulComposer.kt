package ai.deartalk.android.ime

import android.view.inputmethod.InputConnection

/**
 * 한글 2벌식 오토마타 (Hangul 2-Set Automata)
 *
 * 터치 키보드의 자모(초성/중성/종성) 입력을 온디바이스 유니코드 완성형 음절로 실시간 조합 및 분해합니다.
 */
class HangulComposer {

    companion object {
        private const val HANGUL_BASE_CODE = 0xAC00
        private const val JUNGSUNG_COUNT = 21
        private const val JONGSUNG_COUNT = 28

        private const val CHOSUNGS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        private val JUNGSUNGS = listOf(
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ",
            "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
        )
        private val JONGSUNGS = listOf(
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ",
            "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ",
            "ㅋ", "ㅌ", "ㅍ", "ㅎ"
        )

        // 복합 모음 매핑 (ㅗ + ㅏ = ㅘ, ㅜ + ㅓ = ㅝ 등)
        private val DOUBLE_JUNG_MAP = mapOf(
            Pair(8, 0) to 9,   // ㅗ + ㅏ = ㅘ
            Pair(8, 1) to 10,  // ㅗ + ㅐ = ㅙ
            Pair(8, 20) to 11, // ㅗ + ㅣ = ㅚ
            Pair(13, 4) to 14, // ㅜ + ㅓ = ㅝ
            Pair(13, 5) to 15, // ㅜ + ㅔ = ㅞ
            Pair(13, 20) to 16,// ㅜ + ㅣ = ㅟ
            Pair(18, 20) to 19 // ㅡ + ㅣ = ㅢ
        )

        // 복합 받침 매핑 (ㄱ + ㅅ = ㄳ, ㄹ + ㄱ = ㄺ 등)
        private val DOUBLE_JONG_MAP = mapOf(
            Pair(1, 19) to 3,   // ㄱ + ㅅ = ㄳ
            Pair(4, 22) to 5,   // ㄴ + ㅈ = ㄵ
            Pair(4, 27) to 6,   // ㄴ + ㅎ = ㄶ
            Pair(8, 1) to 9,    // ㄹ + ㄱ = ㄺ
            Pair(8, 16) to 10,  // ㄹ + ㅁ = ㄻ
            Pair(8, 17) to 11,  // ㄹ + ㅂ = ㄼ
            Pair(8, 19) to 12,  // ㄹ + ㅅ = ㄽ
            Pair(8, 25) to 13,  // ㄹ + ㅌ = ㄾ
            Pair(8, 26) to 14,  // ㄹ + ㅍ = ㄿ
            Pair(8, 27) to 15,  // ㄹ + ㅎ = ㅀ
            Pair(17, 19) to 18  // ㅂ + ㅅ = ㅄ
        )
    }

    private var cho: Int = -1
    private var jung: Int = -1
    private var jong: Int = 0

    val isComposing: Boolean
        get() = cho != -1 || jung != -1

    /**
     * 현재 자모 상태를 완성형 한글 음절 또는 단일 자모 문자열로 합성합니다.
     */
    fun makeSyllable(): String {
        return when {
            cho != -1 && jung != -1 -> {
                val unicode = HANGUL_BASE_CODE + (cho * JUNGSUNG_COUNT + jung) * JONGSUNG_COUNT + jong
                unicode.toChar().toString()
            }
            cho != -1 && jung == -1 -> CHOSUNGS[cho].toString()
            cho == -1 && jung != -1 -> JUNGSUNGS[jung]
            else -> ""
        }
    }

    /**
     * 자모 문자 1개를 입력받아 오토마타 상태를 전이하고 InputConnection에 반영합니다.
     */
    fun inputJamo(ic: InputConnection?, jamo: Char) {
        val choIndex = CHOSUNGS.indexOf(jamo)
        val jungIndex = JUNGSUNGS.indexOf(jamo.toString())

        when {
            choIndex != -1 && jungIndex == -1 -> inputConsonant(ic, choIndex)
            jungIndex != -1 -> inputVowel(ic, jungIndex)
            else -> {
                commit(ic)
                ic?.commitText(jamo.toString(), 1)
            }
        }
    }

    private fun inputConsonant(ic: InputConnection?, choIdx: Int) {
        val jongIdx = JONGSUNGS.indexOf(CHOSUNGS[choIdx].toString())

        when {
            cho == -1 -> {
                cho = choIdx
                updateComposingText(ic)
            }
            cho != -1 && jung == -1 -> {
                commit(ic)
                cho = choIdx
                updateComposingText(ic)
            }
            cho != -1 && jung != -1 && jong == 0 -> {
                if (jongIdx != -1) {
                    jong = jongIdx
                    updateComposingText(ic)
                } else {
                    commit(ic)
                    cho = choIdx
                    updateComposingText(ic)
                }
            }
            cho != -1 && jung != -1 && jong != 0 -> {
                val combinedJong = DOUBLE_JONG_MAP[Pair(jong, jongIdx)]
                if (combinedJong != null) {
                    jong = combinedJong
                    updateComposingText(ic)
                } else {
                    commit(ic)
                    cho = choIdx
                    updateComposingText(ic)
                }
            }
        }
    }

    private fun inputVowel(ic: InputConnection?, jungIdx: Int) {
        when {
            cho == -1 && jung == -1 -> {
                jung = jungIdx
                updateComposingText(ic)
            }
            cho == -1 && jung != -1 -> {
                val combined = DOUBLE_JUNG_MAP[Pair(jung, jungIdx)]
                if (combined != null) {
                    jung = combined
                } else {
                    commit(ic)
                    jung = jungIdx
                }
                updateComposingText(ic)
            }
            cho != -1 && jung == -1 -> {
                jung = jungIdx
                updateComposingText(ic)
            }
            cho != -1 && jung != -1 && jong == 0 -> {
                val combined = DOUBLE_JUNG_MAP[Pair(jung, jungIdx)]
                if (combined != null) {
                    jung = combined
                } else {
                    commit(ic)
                    jung = jungIdx
                }
                updateComposingText(ic)
            }
            cho != -1 && jung != -1 && jong != 0 -> {
                splitJongAndTransition(ic, jungIdx)
            }
        }
    }

    private fun splitJongAndTransition(ic: InputConnection?, nextJungIdx: Int) {
        // 복합 받침 분리 탐색 (예: '닭' + 'ㅣ' ➔ '달' + '기')
        val matchedDoubleJong = DOUBLE_JONG_MAP.entries.firstOrNull { it.value == jong }

        if (matchedDoubleJong != null) {
            jong = matchedDoubleJong.key.first
            updateComposingText(ic)
            commit(ic)

            cho = CHOSUNGS.indexOf(JONGSUNGS[matchedDoubleJong.key.second])
            jung = nextJungIdx
            jong = 0
            updateComposingText(ic)
        } else {
            val singleJongChar = JONGSUNGS[jong]
            val newCho = CHOSUNGS.indexOf(singleJongChar)

            jong = 0
            updateComposingText(ic)
            commit(ic)

            cho = newCho
            jung = nextJungIdx
            jong = 0
            updateComposingText(ic)
        }
    }

    /**
     * 백스페이스 발생 시 조합 중인 음절을 종성 ➔ 중성 ➔ 초성 역순으로 안전하게 분해합니다.
     */
    fun delete(ic: InputConnection?): Boolean {
        if (!isComposing) return false

        if (jong != 0) {
            val matchedDouble = DOUBLE_JONG_MAP.entries.firstOrNull { it.value == jong }
            jong = matchedDouble?.key?.first ?: 0
            updateComposingText(ic)
            return true
        }

        if (jung != -1) {
            val matchedDouble = DOUBLE_JUNG_MAP.entries.firstOrNull { it.value == jung }
            if (matchedDouble != null) {
                jung = matchedDouble.key.first
                updateComposingText(ic)
            } else {
                jung = -1
                if (cho != -1) {
                    updateComposingText(ic)
                } else {
                    clearComposing(ic)
                }
            }
            return true
        }

        if (cho != -1) {
            clearComposing(ic)
            return true
        }

        return false
    }

    fun commit(ic: InputConnection?) {
        if (isComposing) {
            val syllable = makeSyllable()
            if (syllable.isNotEmpty()) {
                ic?.commitText(syllable, 1)
            }
            reset()
        }
    }

    fun reset() {
        cho = -1
        jung = -1
        jong = 0
    }

    private fun updateComposingText(ic: InputConnection?) {
        ic?.setComposingText(makeSyllable(), 1)
    }

    private fun clearComposing(ic: InputConnection?) {
        reset()
        ic?.setComposingText("", 1)
        ic?.finishComposingText()
    }
}
