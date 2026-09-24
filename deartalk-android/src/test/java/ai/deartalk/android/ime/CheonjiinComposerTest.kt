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

    class FakeInputConnection : java.lang.reflect.InvocationHandler {
        val composingText = StringBuilder()
        val committedText = StringBuilder()
        var finishComposingCallCount = 0

        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
            when (method.name) {
                "setComposingText" -> {
                    val text = args?.get(0) as? CharSequence ?: ""
                    composingText.setLength(0)
                    composingText.append(text)
                    return true
                }
                "commitText" -> {
                    val text = args?.get(0) as? CharSequence ?: ""
                    committedText.append(text)
                    composingText.setLength(0)
                    return true
                }
                "finishComposingText" -> {
                    finishComposingCallCount++
                    committedText.append(composingText)
                    composingText.setLength(0)
                    return true
                }
                "deleteSurroundingText" -> {
                    val before = args?.get(0) as? Int ?: 0
                    if (before > 0 && committedText.isNotEmpty()) {
                        val start = (committedText.length - before).coerceAtLeast(0)
                        committedText.delete(start, committedText.length)
                    }
                    return true
                }
            }
            val returnType = method.returnType
            if (returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.java) {
                return true
            }
            if (returnType == Int::class.javaPrimitiveType || returnType == Int::class.java) {
                return 0
            }
            return null
        }

        fun getFullText(): String = committedText.toString() + composingText.toString()
    }

    @Test
    fun testCommitCallsFinishComposingText() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "안" 입력 -> commit
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("안", handler.getFullText())

        composer.commit(mockConnection)
        // commitText 및 finishComposingText가 호출되어야 함
        assertEquals("안", handler.committedText.toString())
        assertEquals("", handler.composingText.toString())
        org.junit.Assert.assertTrue(handler.finishComposingCallCount >= 1)
        org.junit.Assert.assertFalse(composer.isComposing)
    }

    @Test
    fun testTypingAtMiddleOfTextAfterCursorMove() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // 1. 문장 끝에서 "요" 입력 중 상태
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputVowelKey(mockConnection, 'ㅡ')
        assertEquals("요", handler.composingText.toString())

        // 2. 사용자가 텍스트 중간을 터치하여 커서를 이동 -> IME onUpdateSelection 발생
        // finishComposingText 호출 및 composer.reset()
        mockConnection.finishComposingText()
        composer.reset()
        assertEquals("요", handler.committedText.toString())
        assertEquals("", handler.composingText.toString())
        org.junit.Assert.assertFalse(composer.isComposing)

        // 3. 커서가 위치한 중간에서 새 글자 "가" 입력 시작
        // "요"와 결합되지 않고 독립된 "가"로 정상 시작되어야 함!
        composer.inputConsonantKey(mockConnection, 'ㄱ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')

        assertEquals("가", handler.composingText.toString())
        assertEquals("요가", handler.getFullText())
    }

    @Test
    fun testPunctuationCycle_dotCommaQuestionExclamation() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // 1. 첫 번째 클릭 -> '.'
        composer.inputPunctuationCycle(mockConnection)
        assertEquals(".", handler.committedText.toString())
        assertEquals('.', composer.activePunctuationChar)

        // 2. 두 번째 연속 클릭 -> '.' 삭제 후 ','
        composer.inputPunctuationCycle(mockConnection)
        assertEquals(",", handler.committedText.toString())
        assertEquals(',', composer.activePunctuationChar)

        // 3. 세 번째 연속 클릭 -> ',' 삭제 후 '?'
        composer.inputPunctuationCycle(mockConnection)
        assertEquals("?", handler.committedText.toString())
        assertEquals('?', composer.activePunctuationChar)

        // 4. 네 번째 연속 클릭 -> '?' 삭제 후 '!'
        composer.inputPunctuationCycle(mockConnection)
        assertEquals("!", handler.committedText.toString())
        assertEquals('!', composer.activePunctuationChar)

        // 5. 다섯 번째 연속 클릭 -> '!' 삭제 후 다시 '.'로 순환
        composer.inputPunctuationCycle(mockConnection)
        assertEquals(".", handler.committedText.toString())
        assertEquals('.', composer.activePunctuationChar)
    }

    @Test
    fun testPunctuationCycle_commitsExistingComposingText() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "안" 조합 중 상태
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("안", handler.composingText.toString())

        // '.,?!' 키 클릭 -> "안" 확정(commit) 후 "." 입력
        composer.inputPunctuationCycle(mockConnection)
        assertEquals("안.", handler.committedText.toString())
        assertEquals("", handler.composingText.toString())

        // 다시 연속 클릭 -> "."이 ","로 교체 -> "안,"
        composer.inputPunctuationCycle(mockConnection)
        assertEquals("안,", handler.committedText.toString())
    }

    @Test
    fun testSameKeyConsonantAfterBatchimSeparation() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "안" (ㅇ + ㅣ + ㆍ + ㄴ) 입력
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("안", handler.getFullText())

        // 천지인 표준: 연속 동일 자음 키 분리를 위해 [간격(Space)] 키를 눌러 글자 확정
        composer.space(mockConnection)
        assertEquals("안", handler.committedText.toString())

        // 이어서 "녕"의 'ㄴ' 입력
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("안ㄴ", handler.getFullText())
        assertEquals("ㄴ", handler.composingText.toString())

        // 이어서 'ㅕ' (ㆍ + ㆍ + ㅣ)
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        assertEquals("안녀", handler.getFullText())

        // 'ㅇ' 받침
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        assertEquals("안녕", handler.getFullText())
    }

    @Test
    fun testHakgyoGukgaTyping() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "국" (ㄱ + ㅡ + ㆍ + ㄱ)
        composer.inputConsonantKey(mockConnection, 'ㄱ')
        composer.inputVowelKey(mockConnection, 'ㅡ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄱ')
        assertEquals("국", handler.getFullText())

        // 천지인 표준 [간격(Space)] 키로 글자 확정 후 '가'의 'ㄱ' 입력
        composer.space(mockConnection)
        composer.inputConsonantKey(mockConnection, 'ㄱ')
        assertEquals("국ㄱ", handler.getFullText())

        // 'ㅏ' (ㅣ + ㆍ)
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        assertEquals("국가", handler.getFullText())
    }

    @Test
    fun testDoubleBatchimDak() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "닭" (ㄷ + ㅣ + ㆍ + ㄹ(ㄴ 2번) + ㄱ)
        composer.inputConsonantKey(mockConnection, 'ㄷ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        assertEquals("다", handler.getFullText())

        // 'ㄴㄹ' 키 1번 -> '단'
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("단", handler.getFullText())

        // 'ㄴㄹ' 키 2번 (연타) -> '달'
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("달", handler.getFullText())

        // 'ㄱㅋ' 키 1번 -> '닭' (ㄹ + ㄱ = ㄺ 복합받침 합성!)
        composer.inputConsonantKey(mockConnection, 'ㄱ')
        assertEquals("닭", handler.getFullText())

        // '이' (ㅣ) 입력 시 복합받침 분리 -> "달기"
        composer.inputVowelKey(mockConnection, 'ㅣ')
        assertEquals("달기", handler.getFullText())
    }
}

