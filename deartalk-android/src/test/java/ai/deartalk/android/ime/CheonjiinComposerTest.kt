package ai.deartalk.android.ime

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CheonjiinComposerTest {

    private lateinit var composer: CheonjiinComposer

    @Before
    fun setup() {
        composer = CheonjiinComposer()
    }

    @Test
    fun testBasicConsonantAndVowel() {
        // 'ㄱ' + 'ㅣ' + 'ㆍ' -> '가'
        composer.inputConsonantKey(null, 'ㄱ')
        assertEquals("ㄱ", composer.makeSyllable())

        composer.inputVowelKey(null, 'ㅣ')
        composer.inputVowelKey(null, 'ㆍ')
        assertEquals("가", composer.makeSyllable())
    }

    @Test
    fun testVowelSynthesis() {
        // 'ㅣ' + 'ㆍ' -> 'ㅏ'
        composer.inputVowelKey(null, 'ㅣ')
        composer.inputVowelKey(null, 'ㆍ')
        assertEquals("ㅏ", composer.makeSyllable())

        // + 'ㅣ' -> 'ㅐ'
        composer.inputVowelKey(null, 'ㅣ')
        assertEquals("ㅐ", composer.makeSyllable())
    }

    @Test
    fun testConsonantCycle() {
        // 'ㄱ' 연타 시 'ㄱ' -> 'ㅋ' -> 'ㄲ'
        composer.inputConsonantKey(null, 'ㄱ')
        assertEquals("ㄱ", composer.makeSyllable())

        composer.inputConsonantKey(null, 'ㄱ')
        assertEquals("ㅋ", composer.makeSyllable())

        composer.inputConsonantKey(null, 'ㄱ')
        assertEquals("ㄲ", composer.makeSyllable())
    }

    @Test
    fun testBatchimCombination() {
        // 'ㄱ' + 'ㅏ' + 'ㄱ' -> '각'
        composer.inputConsonantKey(null, 'ㄱ')
        composer.inputVowelKey(null, 'ㅣ')
        composer.inputVowelKey(null, 'ㆍ')
        composer.inputConsonantKey(null, 'ㄱ')
        assertEquals("각", composer.makeSyllable())
    }

    @Test
    fun testAraeaAloneNoCrash() {
        // 단독 'ㆍ' 입력 시 ArrayIndexOutOfBoundsException 없이 안전하게 표시
        composer.inputVowelKey(null, 'ㆍ')
        assertEquals("ㆍ", composer.makeSyllable())

        // 연타 'ㆍ' + 'ㆍ' 입력 시에도 안전
        composer.inputVowelKey(null, 'ㆍ')
        assertEquals("ㆍㆍ", composer.makeSyllable())
    }

    @Test
    fun testConsonantAndAraea() {
        // 'ㄱ' + 'ㆍ' -> 초성 + 아래아 표시
        composer.inputConsonantKey(null, 'ㄱ')
        composer.inputVowelKey(null, 'ㆍ')
        assertEquals("ㄱㆍ", composer.makeSyllable())

        // + 'ㅣ' -> '개' (또는 'ㅓ' 계열 합성)
        composer.inputVowelKey(null, 'ㅣ')
        assertEquals("거", composer.makeSyllable())
    }

    @Test
    fun testAraeaThenVowelSynthesis() {
        // 'ㆍ' + 'ㅡ' -> 'ㅗ'
        composer.inputVowelKey(null, 'ㆍ')
        composer.inputVowelKey(null, 'ㅡ')
        assertEquals("ㅗ", composer.makeSyllable())
    }

    @Test
    fun testDeleteWithAraea() {
        // 'ㄱ' + 'ㆍ' 후 삭제 시 'ㄱ' 유지
        composer.inputConsonantKey(null, 'ㄱ')
        composer.inputVowelKey(null, 'ㆍ')
        composer.delete(null)
        assertEquals("ㄱ", composer.makeSyllable())
    }
}
