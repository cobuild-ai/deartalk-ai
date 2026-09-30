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

    @Test
    fun testMultiTapCompoundBatchim_Manhi() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // 1. "만" (ㅁ: ㅇ 2회 + ㅏ: ㅣ ㆍ + ㄴ: ㄴ 1회)
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        assertEquals("마", handler.getFullText())

        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("만", handler.getFullText())

        // 2. [ㅅ ㅎ] 키 1타 -> "만ㅅ" (2타째 ㄶ 합성 대기 상태)
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        assertEquals("만ㅅ", handler.getFullText())

        // 3. [ㅅ ㅎ] 키 2타 -> "많" ('ㄴ' + 'ㅎ' = 'ㄶ' 복합받침 합성 성공!)
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        assertEquals("많", handler.getFullText())

        // 4. "이" (ㅇ: ㅇ 1회 + ㅣ: ㅣ 1회) -> "많이" 완성!
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        assertEquals("많ㅇ", handler.getFullText())

        composer.inputVowelKey(mockConnection, 'ㅣ')
        assertEquals("많이", handler.getFullText())
    }

    @Test
    fun testMultiTapCompoundBatchim_PendingSeparationWithVowel_Mansi() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "만" 입력
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("만", handler.getFullText())

        // [ㅅ ㅎ] 1타 -> "만ㅅ"
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        assertEquals("만ㅅ", handler.getFullText())

        // 2타를 치지 않고 바로 모음 [ㅣ] 입력 -> "만시"로 분리 완성!
        composer.inputVowelKey(mockConnection, 'ㅣ')
        assertEquals("만시", handler.getFullText())
    }

    @Test
    fun testMultiTapCompoundBatchim_Salm() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "살" (ㅅ: ㅅ 1회 + ㅏ: ㅣ ㆍ + ㄹ: ㄴ 2회)
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("살", handler.getFullText())

        // [ㅇ ㅁ] 1타 -> "살ㅇ" (2타째 ㄻ 합성 대기)
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        assertEquals("살ㅇ", handler.getFullText())

        // [ㅇ ㅁ] 2타 -> "삶" ('ㄹ' + 'ㅁ' = 'ㄻ' 복합받침 합성 성공!)
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        assertEquals("삶", handler.getFullText())
    }

    @Test
    fun testMultiTapCompoundBatchim_Ilhta() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "일" (ㅇ + ㅣ + ㄹ(ㄴ 2회))
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        assertEquals("일", handler.getFullText())

        // [ㅅ ㅎ] 1타 -> "잀" (ㄹ + ㅅ = ㄽ 1타 즉시 결합)
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        assertEquals("잀", handler.getFullText())

        // [ㅅ ㅎ] 2타 -> "잃" (ㄽ -> ㅀ 2타 순환 결합 성공!)
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        assertEquals("잃", handler.getFullText())
    }

    @Test
    fun testPendingDelete() {
        val handler = FakeInputConnection()
        val mockConnection = java.lang.reflect.Proxy.newProxyInstance(
            android.view.inputmethod.InputConnection::class.java.classLoader,
            arrayOf(android.view.inputmethod.InputConnection::class.java),
            handler
        ) as android.view.inputmethod.InputConnection

        // "만" 입력 후 [ㅅ] 1타 -> "만ㅅ"
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputConsonantKey(mockConnection, 'ㅇ')
        composer.inputVowelKey(mockConnection, 'ㅣ')
        composer.inputVowelKey(mockConnection, 'ㆍ')
        composer.inputConsonantKey(mockConnection, 'ㄴ')
        composer.inputConsonantKey(mockConnection, 'ㅅ')
        assertEquals("만ㅅ", handler.getFullText())

        // 삭제 키 -> "ㅅ"만 지워지고 "만"으로 복원
        composer.delete(mockConnection)
        assertEquals("만", handler.getFullText())
    }
}

