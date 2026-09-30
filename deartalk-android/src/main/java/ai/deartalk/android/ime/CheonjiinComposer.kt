package ai.deartalk.android.ime

import android.view.inputmethod.InputConnection
import ai.deartalk.android.ime.CheonjiinTables.CHOSUNGS
import ai.deartalk.android.ime.CheonjiinTables.CONSONANT_CYCLES
import ai.deartalk.android.ime.CheonjiinTables.DOUBLE_JONG
import ai.deartalk.android.ime.CheonjiinTables.JONGSUNGS
import ai.deartalk.android.ime.CheonjiinTables.JUNGSUNGS
import ai.deartalk.android.ime.CheonjiinTables.PUNCTUATION_CYCLE
import ai.deartalk.android.ime.CheonjiinTables.buildSingleSyllable
import ai.deartalk.android.ime.CheonjiinTables.canFormCompoundBatchim
import ai.deartalk.android.ime.CheonjiinTables.splitJong
import ai.deartalk.android.ime.CheonjiinTables.synthesizeJung

/**
 * 천지인(Cheonjiin) 한글 입력 오토마타
 * 3x4 키패드에서 자음 7개 연타 순환과 모음 3개(ㅣ, ㆍ, ㅡ) 합성 규칙을 통해
 * 온디바이스 스마트폰 환경에 최적화된 한손 타이핑 경험을 제공합니다.
 */
class CheonjiinComposer {

    private val chosungs get() = CHOSUNGS
    private val jungsungs get() = JUNGSUNGS
    private val jongsungs get() = JONGSUNGS
    private val doubleJong get() = DOUBLE_JONG
    private val consonantCycles get() = CONSONANT_CYCLES
    private val punctuationCycle get() = PUNCTUATION_CYCLE

    private var cho: Int = -1
    private var jung: Int = -1
    private var jong: Int = 0

    // 복합받침 2타 합성을 위한 이전 음절 대기 버퍼 (예: "만" + "ㅅ" -> 2타째 "많")
    private var pendingPrevCho: Int = -1
    private var pendingPrevJung: Int = -1
    private var pendingPrevJong: Int = 0

    // 복합받침 순환 시(예: ㄼ -> ㄿ, ㄽ -> ㅀ) 첫째 받침 인덱스 보존
    private var baseJongForCycle: Int = 0

    // 현재 자음 연타 상태 추적
    private var lastKeyGroup: Char? = null
    private var lastKeyIndex: Int = 0
    private var lastKeyTime: Long = 0L
    private val KEY_TIMEOUT_MS = 650L

    // 특수문자/구두점 (.,?!) 순환 연타 상태 추적
    private var lastPunctuationIndex: Int = -1
    private var lastPunctuationTime: Long = 0L

    val activePunctuationChar: Char?
        get() {
            val now = System.currentTimeMillis()
            return if (lastPunctuationIndex >= 0 && (now - lastPunctuationTime) < KEY_TIMEOUT_MS) {
                punctuationCycle[lastPunctuationIndex]
            } else {
                null
            }
        }

    // 천지인 모음 합성 상태 추적 ("" or "ㆍ", "ㆍㆍ", "ㅣ", "ㅡ" etc)
    private var vowelBuffer = StringBuilder()

    val isComposing: Boolean
        get() = cho != -1 || jung != -1 || vowelBuffer.isNotEmpty() || pendingPrevCho != -1

    fun makeSyllable(): String {
        val currentSyllable = buildSingleSyllable(cho, jung, jong, vowelBuffer)
        if (pendingPrevCho != -1 && pendingPrevJung in 0..20) {
            val prevSyllable = buildSingleSyllable(pendingPrevCho, pendingPrevJung, pendingPrevJong)
            return prevSyllable + currentSyllable
        }
        return currentSyllable
    }

    private fun commitPendingPrev(ic: InputConnection?) {
        if (pendingPrevCho != -1 && pendingPrevJung in 0..20) {
            val prevText = buildSingleSyllable(pendingPrevCho, pendingPrevJung, pendingPrevJong)
            if (prevText.isNotEmpty()) {
                ic?.commitText(prevText, 1)
            }
            pendingPrevCho = -1
            pendingPrevJung = -1
            pendingPrevJong = 0
            baseJongForCycle = 0
        }
    }

    /**
     * 자음 키 입력 처리 ('ㄱ', 'ㄴ', 'ㄷ', 'ㅂ', 'ㅅ', 'ㅈ', 'ㅇ')
     */
    fun inputConsonantKey(ic: InputConnection?, keyGroup: Char) {
        lastPunctuationIndex = -1
        val now = System.currentTimeMillis()
        val cycle = consonantCycles[keyGroup] ?: listOf(keyGroup)

        // 1. 이전 키와 동일한 키를 타임아웃 내에 연타한 경우: 순환 변경
        if (lastKeyGroup == keyGroup && (now - lastKeyTime) < KEY_TIMEOUT_MS) {
            val nextIndex = lastKeyIndex + 1
            if (nextIndex < cycle.size) {
                lastKeyIndex = nextIndex
                val targetChar = cycle[lastKeyIndex]
                lastKeyTime = now
                cycleCurrentConsonant(ic, targetChar)
                return
            } else {
                // 사이클을 모두 소진한 후 또 누른 경우:
                commitPendingPrev(ic)
                if (cho != -1 && jung != -1 && jong != 0) {
                    commit(ic)
                    val targetChar = cycle[0]
                    cho = chosungs.indexOf(targetChar)
                    lastKeyGroup = keyGroup
                    lastKeyIndex = 0
                    lastKeyTime = now
                    ic?.setComposingText(makeSyllable(), 1)
                    return
                } else {
                    // 초성 자리에 있을 때는 처음으로 순환
                    lastKeyIndex = 0
                    val targetChar = cycle[0]
                    lastKeyTime = now
                    cycleCurrentConsonant(ic, targetChar)
                    return
                }
            }
        }

        // 2. 새로운 키 입력 또는 타임아웃 경과
        commitPendingPrev(ic)
        val targetChar = cycle[0]
        val choIdx = chosungs.indexOf(targetChar)

        // 상태 1: 아무것도 없는 상태 -> 초성 시작
        if (cho == -1 && jung == -1) {
            commitVowelBuffer(ic)
            cho = choIdx
            lastKeyGroup = keyGroup
            lastKeyIndex = 0
            lastKeyTime = now
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        // 상태 2: 초성만 있는 상태에서 다른 자음 입력 -> 기존 초성 커밋 후 새 초성 시작
        if (cho != -1 && jung == -1) {
            commit(ic)
            cho = choIdx
            lastKeyGroup = keyGroup
            lastKeyIndex = 0
            lastKeyTime = now
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        // 상태 3: 초성 + 중성 있는 상태 (종성 없음) -> 종성으로 시도
        if (cho != -1 && jung != -1 && jong == 0) {
            val jongIdx = jongsungs.indexOf(targetChar.toString())
            if (jongIdx != -1) {
                jong = jongIdx
                baseJongForCycle = 0
                lastKeyGroup = keyGroup
                lastKeyIndex = 0
                lastKeyTime = now
                ic?.setComposingText(makeSyllable(), 1)
            } else {
                commit(ic)
                cho = choIdx
                lastKeyGroup = keyGroup
                lastKeyIndex = 0
                lastKeyTime = now
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }

        // 상태 4: 종성이 이미 있는 상태 -> 복합 받침 시도 또는 분리/대기
        if (cho != -1 && jung != -1 && jong != 0) {
            val combined = doubleJong[Pair(jong, choIdx)]
            if (combined != null) {
                baseJongForCycle = jong
                jong = combined
                lastKeyGroup = keyGroup
                lastKeyIndex = 0
                lastKeyTime = now
                ic?.setComposingText(makeSyllable(), 1)
                return
            } else if (canFormCompoundBatchim(jong, keyGroup)) {
                // 1타째에는 겹받침이 안 되지만, 2타째에 겹받침이 되는 경우 (예: "만" + "ㅅ" -> 2타째 "많")
                pendingPrevCho = cho
                pendingPrevJung = jung
                pendingPrevJong = jong
                baseJongForCycle = jong
                cho = choIdx
                jung = -1
                jong = 0
                vowelBuffer.clear()
                lastKeyGroup = keyGroup
                lastKeyIndex = 0
                lastKeyTime = now
                ic?.setComposingText(makeSyllable(), 1)
                return
            } else {
                commit(ic)
                cho = choIdx
                lastKeyGroup = keyGroup
                lastKeyIndex = 0
                lastKeyTime = now
                ic?.setComposingText(makeSyllable(), 1)
                return
            }
        }
    }

    private fun cycleCurrentConsonant(ic: InputConnection?, nextChar: Char) {
        val nextChoIdx = chosungs.indexOf(nextChar)
        val nextJongIdx = jongsungs.indexOf(nextChar.toString())

        // 1. 복합받침 대기 상태(pendingPrevCho != -1)에서의 순환 (예: "만ㅅ" -> "많")
        if (pendingPrevCho != -1) {
            val combined = doubleJong[Pair(pendingPrevJong, nextChoIdx)]
            if (combined != null) {
                cho = pendingPrevCho
                jung = pendingPrevJung
                jong = combined
                baseJongForCycle = pendingPrevJong
                pendingPrevCho = -1
                pendingPrevJung = -1
                pendingPrevJong = 0
                ic?.setComposingText(makeSyllable(), 1)
                return
            } else {
                cho = nextChoIdx
                ic?.setComposingText(makeSyllable(), 1)
                return
            }
        }

        // 2. 이미 복합받침 상태(baseJongForCycle != 0)에서의 순환 (예: ㄼ -> ㄿ, ㄽ -> ㅀ)
        if (cho != -1 && jung != -1 && baseJongForCycle != 0) {
            val combined = doubleJong[Pair(baseJongForCycle, nextChoIdx)]
            if (combined != null) {
                jong = combined
                ic?.setComposingText(makeSyllable(), 1)
                return
            }
        }

        // 3. 종성 자리에 있을 때 순환 (단일 받침 순환, 예: 안 -> 알, 잇 -> 잏 -> 있)
        if (cho != -1 && jung != -1 && jong != 0) {
            if (nextJongIdx != -1) {
                jong = nextJongIdx
                ic?.setComposingText(makeSyllable(), 1)
            }
            return
        }

        // 4. 초성 자리에 있을 때 순환 (예: ㄱ -> ㅋ -> ㄲ)
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
        lastPunctuationIndex = -1
        lastKeyGroup = null // 모음 입력 시 자음 연타 리셋

        // 복합받침 대기 중인 이전 음절이 있다면 먼저 확정 커밋
        commitPendingPrev(ic)

        // 종성이 있는 상태에서 모음 입력 시: 받침을 분리하여 다음 글자 초성으로 이동 (도깨비불 현상)
        if (cho != -1 && jung != -1 && jong != 0) {
            val (firstJong, secondCho) = splitJong(jong)
            jong = firstJong
            commit(ic)
            cho = secondCho
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

    fun delete(ic: InputConnection?) {
        lastPunctuationIndex = -1
        lastKeyGroup = null

        // 1. 복합받침 대기 상태에서 삭제 시 (예: "만ㅅ" -> "만")
        if (pendingPrevCho != -1) {
            cho = pendingPrevCho
            jung = pendingPrevJung
            jong = pendingPrevJong
            pendingPrevCho = -1
            pendingPrevJung = -1
            pendingPrevJong = 0
            baseJongForCycle = 0
            vowelBuffer.clear()
            ic?.setComposingText(makeSyllable(), 1)
            return
        }

        if (jong != 0) {
            // 복합 받침 분리 시도
            val (first, _) = splitJong(jong)
            jong = first
            baseJongForCycle = 0
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
        lastPunctuationIndex = -1
        lastKeyGroup = null
        if (isComposing) {
            commit(ic)
        } else {
            ic?.commitText(" ", 1)
        }
    }

    /**
     * 문장부호 순환 키 (.,?!) 입력 처리
     * - 연속 클릭 시: '.' -> ',' -> '?' -> '!' 순환 교체
     * - 타임아웃(1.2초) 경과 또는 다른 글자 작성 후 입력 시: 새 '.' 입력
     */
    fun inputPunctuationCycle(ic: InputConnection?) {
        val now = System.currentTimeMillis()
        lastKeyGroup = null

        // 1. 조합 중인 한글이 있다면 먼저 확정 커밋
        if (isComposing) {
            commit(ic)
        }

        // 2. 직전 입력이 구두점 순환이고 타임아웃(1.2초) 내 연타인 경우 직전문자 1글자 삭제 후 교체
        if (lastPunctuationIndex >= 0 && (now - lastPunctuationTime) < KEY_TIMEOUT_MS) {
            lastPunctuationIndex = (lastPunctuationIndex + 1) % punctuationCycle.size
            val targetChar = punctuationCycle[lastPunctuationIndex]
            lastPunctuationTime = now
            ic?.deleteSurroundingText(1, 0)
            ic?.commitText(targetChar.toString(), 1)
            return
        }

        // 3. 새로운 구두점 입력 시작 ('.'부터)
        lastPunctuationIndex = 0
        lastPunctuationTime = now
        val targetChar = punctuationCycle[0]
        ic?.commitText(targetChar.toString(), 1)
    }

    fun commit(ic: InputConnection?) {
        lastPunctuationIndex = -1
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
        pendingPrevCho = -1
        pendingPrevJung = -1
        pendingPrevJong = 0
        baseJongForCycle = 0
        vowelBuffer.clear()
        lastKeyGroup = null
        lastKeyIndex = 0
        lastKeyTime = 0L
        lastPunctuationIndex = -1
        lastPunctuationTime = 0L
    }
}
