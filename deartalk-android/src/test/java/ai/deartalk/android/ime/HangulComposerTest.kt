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
}
