package ai.deartalk.android.ime

import android.view.inputmethod.InputConnection

/**
 * 천지인(Cheonjiin) 한글 입력 오토마타
 * 3x4 키패드에서 자음 7개 연타 순환과 모음 3개(ㅣ, ㆍ, ㅡ) 합성 규칙을 통해
 * 온디바이스 스마트폰 환경에 최적화된 한손 타이핑 경험을 제공합니다.
 */
class CheonjiinComposer {

    private val chosungs = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private val jungsungs = listOf(
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    )
    private val jongsungs = listOf(
        "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    )

    // 복합 받침 매핑 (초성 인덱스 -> 종성 인덱스 결합)
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

    // 자음 키 그룹별 순환 목록
    private val consonantCycles = mapOf(
        'ㄱ' to listOf('ㄱ', 'ㅋ', 'ㄲ'),
        'ㄴ' to listOf('ㄴ', 'ㄹ'),
        'ㄷ' to listOf('ㄷ', 'ㅌ', 'ㄸ'),
        'ㅂ' to listOf('ㅂ', 'ㅍ', 'ㅃ'),
        'ㅅ' to listOf('ㅅ', 'ㅎ', 'ㅆ'),
        'ㅈ' to listOf('ㅈ', 'ㅊ', 'ㅉ'),
        'ㅇ' to listOf('ㅇ', 'ㅁ')
    )

    private var cho: Int = -1
    private var jung: Int = -1
    private var jong: Int = 0

    // 현재 자음 연타 상태 추적
    private var lastKeyGroup: Char? = null
    private var lastKeyIndex: Int = 0
    private var lastKeyTime: Long = 0L
    private val KEY_TIMEOUT_MS = 1200L

    // 천지인 모음 합성 상태 추적 ("" or "ㆍ", "ㆍㆍ", "ㅣ", "ㅡ" etc)
    private var vowelBuffer = StringBuilder()

    val isComposing: Boolean
        get() = cho != -1 || jung != -1 || vowelBuffer.isNotEmpty()

    fun makeSyllable(): String {
        if (cho != -1 && jung in 0..20) {
            val code = 0xAC00 + (cho * 21 + jung) * 28 + jong
            return code.toChar().toString()
        }
        if (cho != -1 && jung == -1) {
            if (vowelBuffer.isNotEmpty()) {
                return chosungs[cho] + vowelBuffer.toString()
            }
            return chosungs[cho].toString()
        }
        if (cho == -1 && jung in 0..20) {
            return jungsungs[jung]
        }
        if (vowelBuffer.isNotEmpty()) {
            return vowelBuffer.toString()
        }
        return ""
    }

    /**
     * 자음 키 입력 처리 ('ㄱ', 'ㄴ', 'ㄷ', 'ㅂ', 'ㅅ', 'ㅈ', 'ㅇ')
     */
    fun inputConsonantKey(ic: InputConnection?, keyGroup: Char) {
        val now = System.currentTimeMillis()
        val cycle = consonantCycles[keyGroup] ?: listOf(keyGroup)

        // 1. 이전 키와 동일한 키를 타임아웃 내에 연타한 경우: 순환 변경
        if (lastKeyGroup == keyGroup && (now - lastKeyTime) < KEY_TIMEOUT_MS) {
            lastKeyIndex = (lastKeyIndex + 1) % cycle.size
            val targetChar = cycle[lastKeyIndex]
            lastKeyTime = now
            cycleCurrentConsonant(ic, targetChar)
            return
        }

        // 2. 새로운 키 입력 또는 타임아웃 경과
        lastKeyGroup = keyGroup
        lastKeyIndex = 0
        lastKeyTime = now
        val targetChar = cycle[0]

        val choIdx = chosungs.indexOf(targetChar)

        // 상태 1: 아무것도 없는 상태 -> 초성 시작
        if (cho == -1 && jung == -1) {
            commitVowelBuffer(ic)
            cho = choIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        // 상태 2: 초성만 있는 상태에서 다른 자음 입력 -> 기존 초성 커밋 후 새 초성 시작
        if (cho != -1 && jung == -1) {
            commit(ic)
            cho = choIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        // 상태 3: 초성 + 중성 있는 상태 (종성 없음) -> 종성으로 시도
        if (cho != -1 && jung != -1 && jong == 0) {
            val jongIdx = jongsungs.indexOf(targetChar.toString())
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

        // 상태 4: 종성이 이미 있는 상태 -> 복합 받침 시도 또는 분리
        if (cho != -1 && jung != -1 && jong != 0) {
            val combined = doubleJong[Pair(jong, choIdx)]
            if (combined != null) {
                jong = combined
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                commit(ic)
                cho = choIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }
    }

    private fun cycleCurrentConsonant(ic: InputConnection?, nextChar: Char) {
        val nextChoIdx = chosungs.indexOf(nextChar)
        val nextJongIdx = jongsungs.indexOf(nextChar.toString())

        // 종성 자리에 있을 때 순환
        if (cho != -1 && jung != -1 && jong != 0) {
            if (nextJongIdx != -1) {
                jong = nextJongIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }

        // 초성 자리에 있을 때 순환
        if (cho != -1 && jung == -1) {
            cho = nextChoIdx
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        // 초성 + 중성 완성 상태에서 종성이 0이었는데 방금 종성으로 들어간 경우
        commit(ic)
        cho = nextChoIdx
        ic?.setComposingText(makeSyllable(), 1)
    }

    /**
     * 천지인 모음 키 입력 처리: 'ㅣ', 'ㆍ' (또는 '·'), 'ㅡ'
     */
    fun inputVowelKey(ic: InputConnection?, vowelChar: Char) {
        lastKeyGroup = null // 모음 입력 시 자음 연타 리셋

        // 1. 종성이 복합 받침인 경우: 뒷받침을 떼어 다음 글자 초성으로 이동
        if (cho != -1 && jung != -1 && jong != 0) {
            val (firstJong, secondCho) = splitJong(jong)
            jong = firstJong
            commit(ic)
            cho = secondCho
            jung = -1
            jong = 0
            vowelBuffer.clear()
        }

        // 2. 단일 종성이 있는 상태에서 모음 입력 시: 종성을 다음 글자 초성으로 이동
        else if (cho != -1 && jung != -1 && jong != 0) {
            val prevJongChar = jongsungs[jong]
            jong = 0
            commit(ic)
            cho = chosungs.indexOf(prevJongChar)
            jung = -1
            jong = 0
            vowelBuffer.clear()
        }

        // 모음 합성 시뮬레이션
        vowelBuffer.append(vowelChar)
        val synthesizedJung = synthesizeJung(vowelBuffer.toString())

        if (synthesizedJung in 0..20) {
            jung = synthesizedJung
            ic?.setComposingText(makeSyllable(), 1)
        } else if (synthesizedJung == -2) {
            // 아래아 단독/조합 전이 상태 (표시용)
            jung = -1
            ic?.setComposingText(makeSyllable(), 1)
        } else {
            // 합성 불가능한 조합이면 이전 글자 커밋 후 새 모음으로 시작
            if (cho != -1 || jung in 0..20 || vowelBuffer.length > 1) {
                vowelBuffer.deleteCharAt(vowelBuffer.length - 1)
                commit(ic)
            }
            vowelBuffer.clear()
            vowelBuffer.append(vowelChar)
            val singleJung = synthesizeJung(vowelBuffer.toString())
            if (singleJung in 0..20) {
                jung = singleJung
                ic?.setComposingText(makeSyllable(), 1)
            } else if (singleJung == -2) {
                jung = -1
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                jung = -1
                ic?.setComposingText(vowelBuffer.toString(), 1)
            }
        }
    }

    /**
     * 천지인 모음 버퍼 문자열을 표준 중성 인덱스로 합성
     */
    private fun synthesizeJung(buf: String): Int {
        // 기본 모음 및 합성 규칙
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

    private fun splitJong(j: Int): Pair<Int, Int> {
        for ((pair, result) in doubleJong) {
            if (result == j) {
                return Pair(pair.first, pair.second)
            }
        }
        val ch = jongsungs[j]
        val choIdx = chosungs.indexOf(ch)
        return Pair(0, choIdx)
    }

    fun delete(ic: InputConnection?) {
        lastKeyGroup = null

        if (jong != 0) {
            // 복합 받침 분리 시도
            val (first, _) = splitJong(jong)
            jong = first
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (vowelBuffer.isNotEmpty()) {
            vowelBuffer.deleteCharAt(vowelBuffer.length - 1)
            val newJung = synthesizeJung(vowelBuffer.toString())
            if (newJung in 0..20) {
                jung = newJung
            } else {
                jung = -1
            }
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (jung != -1) {
            jung = -1
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (cho != -1) {
            cho = -1
            ic?.setComposingText("", 1)
            return
        }

        ic?.deleteSurroundingText(1, 0)
    }

    fun space(ic: InputConnection?) {
        lastKeyGroup = null
        if (isComposing) {
            commit(ic)
        } else {
            ic?.commitText(" ", 1)
        }
    }

    fun commit(ic: InputConnection?) {
        lastKeyGroup = null
        if (isComposing) {
            val text = makeSyllable()
            if (text.isNotEmpty()) {
                ic?.commitText(text, 1)
            }
            ic?.finishComposingText()
            reset()
        }
    }

    private fun commitVowelBuffer(ic: InputConnection?) {
        if (vowelBuffer.isNotEmpty()) {
            val text = makeSyllable()
            if (text.isNotEmpty()) {
                ic?.commitText(text, 1)
            }
            ic?.finishComposingText()
            reset()
        }
    }

    fun reset() {
        cho = -1
        jung = -1
        jong = 0
        vowelBuffer.clear()
        lastKeyGroup = null
        lastKeyIndex = 0
        lastKeyTime = 0L
    }
}
