package ai.deartalk.android.ime

import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class HangulComposerTest {

    private lateinit var composer: HangulComposer
    private lateinit var mockConnection: InputConnection
    private lateinit var fakeConnectionHandler: FakeInputConnection

    class FakeInputConnection : java.lang.reflect.InvocationHandler {
        val composingText = StringBuilder()
        val committedText = StringBuilder()

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
                    committedText.append(composingText)
                    composingText.setLength(0)
                    return true
                }
            }
            // Return safe default values for interface methods to prevent NPE
            val returnType = method.returnType
            if (returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.java) {
                return true
            }
            if (returnType == Int::class.javaPrimitiveType || returnType == Int::class.java) {
                return 0
            }
            return null
        }

        fun getFullText(): String {
            return committedText.toString() + composingText.toString()
        }
        
        fun clear() {
            composingText.setLength(0)
            committedText.setLength(0)
        }
    }

    @Before
    fun setUp() {
        composer = HangulComposer()
        fakeConnectionHandler = FakeInputConnection()
        mockConnection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java),
            fakeConnectionHandler
        ) as InputConnection
    }

    @Test
    fun testBasicSyllableComposition() {
        // "ㄱ" + "ㅏ" + "ㅇ" -> "강"
        composer.inputJamo(mockConnection, 'ㄱ')
        assertEquals("ㄱ", fakeConnectionHandler.getFullText())
        assertTrue(composer.isComposing)

        composer.inputJamo(mockConnection, 'ㅏ')
        assertEquals("가", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㅇ')
        assertEquals("강", fakeConnectionHandler.getFullText())
    }

    @Test
    fun testDoubleBatchimComposition() {
        // "ㄷ" + "ㅏ" + "ㄹ" + "ㄱ" -> "닭"
        composer.inputJamo(mockConnection, 'ㄷ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄹ')
        assertEquals("달", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㄱ')
        assertEquals("닭", fakeConnectionHandler.getFullText())
    }

    @Test
    fun testVowelSyllableSplitting() {
        // "닭" + "ㅣ" -> "달기"
        composer.inputJamo(mockConnection, 'ㄷ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄹ')
        composer.inputJamo(mockConnection, 'ㄱ')
        assertEquals("닭", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㅣ')
        assertEquals("달기", fakeConnectionHandler.getFullText())
    }

    @Test
    fun testSequentialDeletion() {
        // "닭" -> delete -> "달" -> delete -> "다" -> delete -> "ㄷ" -> delete -> ""
        composer.inputJamo(mockConnection, 'ㄷ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄹ')
        composer.inputJamo(mockConnection, 'ㄱ')
        assertEquals("닭", fakeConnectionHandler.getFullText())

        // 1. "닭" -> "달"
        assertTrue(composer.delete(mockConnection))
        assertEquals("달", fakeConnectionHandler.getFullText())

        // 2. "달" -> "다"
        assertTrue(composer.delete(mockConnection))
        assertEquals("다", fakeConnectionHandler.getFullText())

        // 3. "다" -> "ㄷ"
        assertTrue(composer.delete(mockConnection))
        assertEquals("ㄷ", fakeConnectionHandler.getFullText())

        // 4. "ㄷ" -> ""
        assertTrue(composer.delete(mockConnection))
        assertEquals("", fakeConnectionHandler.getFullText())
        assertFalse(composer.isComposing)

        // 5. Empty -> delete returns false
        assertFalse(composer.delete(mockConnection))
    }

    @Test
    fun testNonKoreanCharacterInput() {
        // Non-Korean characters should commit immediately and reset composition state
        composer.inputJamo(mockConnection, 'ㄱ')
        composer.inputJamo(mockConnection, 'ㅏ')
        assertEquals("가", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'A')
        assertEquals("가A", fakeConnectionHandler.getFullText())
        assertFalse(composer.isComposing)
    }

    @Test
    fun testDoubleSBatchimWithoutShift() {
        // Shift 없이 'ㅅ' 두 번 연타 시 'ㅆ' 받침 합성 검증 ("했")
        composer.inputJamo(mockConnection, 'ㅎ')
        composer.inputJamo(mockConnection, 'ㅐ')
        composer.inputJamo(mockConnection, 'ㅅ')
        assertEquals("햇", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㅅ')
        assertEquals("했", fakeConnectionHandler.getFullText())
    }

    @Test
    fun testThreeStepVowelSynthesis() {
        // 'ㅗ' + 'ㅏ' + 'ㅣ' -> 'ㅙ' (왜)
        composer.inputJamo(mockConnection, 'ㅇ')
        composer.inputJamo(mockConnection, 'ㅗ')
        assertEquals("오", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㅏ')
        assertEquals("와", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㅣ')
        assertEquals("왜", fakeConnectionHandler.getFullText())
    }

    @Test
    fun testDoubleBatchimSsSplitWithVowel() {
        // '있' + 'ㅓ' -> '이써' (도깨비불 쌍자음 분리)
        composer.inputJamo(mockConnection, 'ㅇ')
        composer.inputJamo(mockConnection, 'ㅣ')
        composer.inputJamo(mockConnection, 'ㅅ')
        composer.inputJamo(mockConnection, 'ㅅ')
        assertEquals("있", fakeConnectionHandler.getFullText())

        composer.inputJamo(mockConnection, 'ㅓ')
        assertEquals("이써", fakeConnectionHandler.getFullText())
    }

    @Test
    fun testCommonSentenceTyping() {
        // "안녕하세요" 입력 흐름 검증
        val text = "안녕하세요"
        fakeConnectionHandler.clear()
        composer.reset()
        
        // ㅇ ㅏ ㄴ
        composer.inputJamo(mockConnection, 'ㅇ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄴ')
        assertEquals("안", fakeConnectionHandler.getFullText())

        // ㄴ ㅕ ㅇ
        composer.inputJamo(mockConnection, 'ㄴ')
        composer.inputJamo(mockConnection, 'ㅕ')
        composer.inputJamo(mockConnection, 'ㅇ')
        assertEquals("안녕", fakeConnectionHandler.getFullText())

        // ㅎ ㅏ
        composer.inputJamo(mockConnection, 'ㅎ')
        composer.inputJamo(mockConnection, 'ㅏ')
        assertEquals("안녕하", fakeConnectionHandler.getFullText())

        // ㅅ ㅔ
        composer.inputJamo(mockConnection, 'ㅅ')
        composer.inputJamo(mockConnection, 'ㅔ')
        assertEquals("안녕하세", fakeConnectionHandler.getFullText())

        // ㅇ ㅛ
        composer.inputJamo(mockConnection, 'ㅇ')
        composer.inputJamo(mockConnection, 'ㅛ')
        assertEquals("안녕하세요", fakeConnectionHandler.getFullText())
    }
}
