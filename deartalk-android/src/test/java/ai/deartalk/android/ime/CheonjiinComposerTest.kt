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
}
