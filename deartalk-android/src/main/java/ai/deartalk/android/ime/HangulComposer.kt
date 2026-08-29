package ai.deartalk.android.ime

import android.view.inputmethod.InputConnection

/**
 * 한글 2벌식 오토마타 (Hangul 2-Set Automata)
 * 스마트폰 터치 키보드에서 한글 자모를 음절(초성+중성+종성)로 정확하게 조합하여 InputConnection에 전송합니다.
 */
class HangulComposer {

    private var cho: Int = -1
    private var jung: Int = -1
    private var jong: Int = 0

    private val chosungs = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private val jungsungs = listOf(
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    )
    private val jongsungs = listOf(
        "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    )

    // 복합 모음 매핑 (ㅗ + ㅏ = ㅘ 등)
    private val doubleJung = mapOf(
        Pair(8, 0) to 9,   // ㅗ + ㅏ = ㅘ
        Pair(8, 1) to 10,  // ㅗ + ㅐ = ㅙ
        Pair(8, 20) to 11, // ㅗ + ㅣ = ㅚ
        Pair(13, 4) to 14, // ㅜ + ㅓ = ㅝ
        Pair(13, 5) to 15, // ㅜ + ㅔ = ㅞ
        Pair(13, 20) to 16,// ㅜ + ㅣ = ㅟ
        Pair(18, 20) to 19 // ㅡ + ㅣ = ㅢ
    )

    // 복합 받침 매핑 (ㄱ + ㅅ = ㄳ 등)
    private val doubleJong = mapOf(
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

    val isComposing: Boolean
        get() = cho != -1 || jung != -1

    fun makeSyllable(): String {
        if (cho != -1 && jung != -1) {
            val code = 0xAC00 + (cho * 21 + jung) * 28 + jong
            return code.toChar().toString()
        }
        if (cho != -1 && jung == -1) {
            return chosungs[cho].toString()
        }
        if (cho == -1 && jung != -1) {
            return jungsungs[jung]
        }
        return ""
    }

    fun inputJamo(ic: InputConnection?, jamo: Char) {
        val choIndex = chosungs.indexOf(jamo)
        val jungIndex = jungsungs.indexOf(jamo.toString())

        if (choIndex != -1 && jungIndex == -1) {
            // 자음 입력
            inputConsonant(ic, choIndex)
        } else if (jungIndex != -1) {
            // 모음 입력
            inputVowel(ic, jungIndex)
        } else {
            // 기타 문자 (알파벳, 숫자, 특수문자)
            commit(ic)
            ic?.commitText(jamo.toString(), 1)
        }
    }

    private fun inputConsonant(ic: InputConnection?, choIdx: Int) {
        val jongIdx = jongsungs.indexOf(chosungs[choIdx].toString())

        if (cho == -1) {
            cho = choIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (cho != -1 && jung == -1) {
            commit(ic)
            cho = choIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (cho != -1 && jung != -1 && jong == 0) {
            if (jongIdx != -1) {
                jong = jongIdx
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                commit(ic)
                cho = choIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }

        if (cho != -1 && jung != -1 && jong != 0) {
            val combinedJong = doubleJong[Pair(jong, jongIdx)]
            if (combinedJong != null) {
                jong = combinedJong
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                commit(ic)
                cho = choIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
        }
    }

    private fun inputVowel(ic: InputConnection?, jungIdx: Int) {
        if (cho == -1 && jung == -1) {
            jung = jungIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (cho == -1 && jung != -1) {
            val combinedJung = doubleJung[Pair(jung, jungIdx)]
            if (combinedJung != null) {
                jung = combinedJung
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                commit(ic)
                jung = jungIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }

        if (cho != -1 && jung == -1) {
            jung = jungIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (cho != -1 && jung != -1 && jong == 0) {
            val combinedJung = doubleJung[Pair(jung, jungIdx)]
            if (combinedJung != null) {
                jung = combinedJung
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                commit(ic)
                jung = jungIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }

        if (cho != -1 && jung != -1 && jong != 0) {
            // 복합 받침 분리 여부 확인 (예: 닭 + 이 -> 달 + 기)
            var prevJong = 0
            var nextCho = -1

            for ((pair, result) in doubleJong) {
                if (result == jong) {
                    prevJong = pair.first
                    nextCho = chosungs.indexOf(jongsungs[pair.second])
                    break
                }
            }

            if (nextCho != -1) {
                jong = prevJong
                ic?.setComposingText(makeSyllable(), 1)
                commit(ic)
                cho = nextCho
                jung = jungIdx
                jong = 0
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                val jongChar = jongsungs[jong]
                val newCho = chosungs.indexOf(jongChar)
                jong = 0
                ic?.setComposingText(makeSyllable(), 1)
                commit(ic)
                cho = newCho
                jung = jungIdx
                jong = 0
                ic?.setComposingText(makeSyllable(), 1)
            }
        }
    }

    fun delete(ic: InputConnection?): Boolean {
        if (!isComposing) {
            return false
        }

        if (jong != 0) {
            for ((pair, result) in doubleJong) {
                if (result == jong) {
                    jong = pair.first
                    ic?.setComposingText(makeSyllable(), 1)
                    return true
                }
            }
            jong = 0
            ic?.setComposingText(makeSyllable(), 1)
            return true
        }

        if (jung != -1) {
            for ((pair, result) in doubleJung) {
                if (result == jung) {
                    jung = pair.first
                    ic?.setComposingText(makeSyllable(), 1)
                    return true
                }
            }
            jung = -1
            if (cho != -1) {
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                reset()
                ic?.setComposingText("", 1)
                ic?.finishComposingText()
            }
            return true
        }

        if (cho != -1) {
            reset()
            ic?.setComposingText("", 1)
            ic?.finishComposingText()
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
}
