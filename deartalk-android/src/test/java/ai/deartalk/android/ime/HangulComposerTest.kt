package ai.deartalk.android.ime

import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * 한글 2벌식 오토마타 단위 테스트
 * - Given / When / Then 구조를 준수하여 자모 조합, 겹받침 분리, 백스페이스 분해, 비한글 처리를 검증합니다.
 */
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
    fun `초성과_중성과_종성을_순차_입력하면_음절이_정상_조합된다`() {
        // Given: 'ㄱ' 입력 후 'ㅏ' 입력
        composer.inputJamo(mockConnection, 'ㄱ')
        composer.inputJamo(mockConnection, 'ㅏ')

        // When: 종성 'ㅇ' 입력
        composer.inputJamo(mockConnection, 'ㅇ')

        // Then
        assertEquals("완성된 음절은 '강'이어야 합니다.", "강", fakeConnectionHandler.getFullText())
        assertTrue("현재 조합 중인 상태여야 합니다.", composer.isComposing)
    }

    @Test
    fun `복합_받침_자음을_입력하면_겹받침으로_조합된다`() {
        // Given: 'ㄷ' + 'ㅏ' + 'ㄹ' -> '달' 상태
        composer.inputJamo(mockConnection, 'ㄷ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄹ')

        // When: 겹받침 자음 'ㄱ' 입력
        composer.inputJamo(mockConnection, 'ㄱ')

        // Then
        assertEquals("ㄹ+ㄱ이 결합되어 '닭'이 되어야 합니다.", "닭", fakeConnectionHandler.getFullText())
    }

    @Test
    fun `겹받침_상태에서_모음_입력_시_받침이_다음_음절_초성으로_분리된다`() {
        // Given: '닭'이 조합된 상태
        composer.inputJamo(mockConnection, 'ㄷ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄹ')
        composer.inputJamo(mockConnection, 'ㄱ')
        assertEquals("닭", fakeConnectionHandler.getFullText())

        // When: 모음 'ㅣ' 입력
        composer.inputJamo(mockConnection, 'ㅣ')

        // Then
        assertEquals("'닭'+'ㅣ'는 '달기'로 분리 결합되어야 합니다.", "달기", fakeConnectionHandler.getFullText())
    }

    @Test
    fun `음절_완성_후_백스페이스_시_종성_중성_초성_순으로_단계별_분해된다`() {
        // Given: '닭' 완성 상태
        composer.inputJamo(mockConnection, 'ㄷ')
        composer.inputJamo(mockConnection, 'ㅏ')
        composer.inputJamo(mockConnection, 'ㄹ')
        composer.inputJamo(mockConnection, 'ㄱ')

        // When & Then 1: 겹받침 'ㄱ' 삭제 -> '달'
        assertTrue(composer.delete(mockConnection))
        assertEquals("달", fakeConnectionHandler.getFullText())

        // When & Then 2: 받침 'ㄹ' 삭제 -> '다'
        assertTrue(composer.delete(mockConnection))
        assertEquals("다", fakeConnectionHandler.getFullText())

        // When & Then 3: 중성 'ㅏ' 삭제 -> 'ㄷ'
        assertTrue(composer.delete(mockConnection))
        assertEquals("ㄷ", fakeConnectionHandler.getFullText())

        // When & Then 4: 초성 'ㄷ' 삭제 -> 빈 문자열
        assertTrue(composer.delete(mockConnection))
        assertEquals("", fakeConnectionHandler.getFullText())
        assertFalse(composer.isComposing)

        // When & Then 5: 빈 상태에서 삭제 시 false 반환
        assertFalse(composer.delete(mockConnection))
    }

    @Test
    fun `비한글_문자_입력_시_진행_중인_조합이_커밋되고_문자가_즉시_입력된다`() {
        // Given: '가' 조합 상태
        composer.inputJamo(mockConnection, 'ㄱ')
        composer.inputJamo(mockConnection, 'ㅏ')

        // When: 영문 대문자 'A' 입력
        composer.inputJamo(mockConnection, 'A')

        // Then
        assertEquals("'가'가 커밋되고 'A'가 추가되어 '가A'가 되어야 합니다.", "가A", fakeConnectionHandler.getFullText())
        assertFalse("조합이 종료되어 isComposing은 false여야 합니다.", composer.isComposing)
    }
}
