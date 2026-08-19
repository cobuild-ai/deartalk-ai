package ai.deartalk.android.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceBackspaceTest {

    @Test
    fun `단일_문장_삭제_시_0_반환`() {
        val text = "안녕하세요 반갑습니다."
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(text)
        assertEquals(0, cutIndex)
    }

    @Test
    fun `마침표_없는_단일_문장_삭제_시_0_반환`() {
        val text = "안녕하세요 반갑습니다"
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(text)
        assertEquals(0, cutIndex)
    }

    @Test
    fun `여러_문장_중_마지막_문장만_정확히_절단`() {
        val text = "첫 번째 문장입니다. 두 번째 문장입니다! 세 번째 문장인가요?"
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(text)
        // 세 번째 문장 시작 전까지 남겨야 하므로 "첫 번째 문장입니다. 두 번째 문장입니다!" 뒤의 인덱스
        val expectedCut = "첫 번째 문장입니다. 두 번째 문장입니다!".length
        assertEquals(expectedCut, cutIndex)
        val remaining = text.substring(0, cutIndex).trimEnd()
        assertEquals("첫 번째 문장입니다. 두 번째 문장입니다!", remaining)
    }

    @Test
    fun `줄바꿈_기준_문장_절단_테스트`() {
        val text = "첫 번째 줄입니다\n두 번째 줄입니다"
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(text)
        val expectedCut = "첫 번째 줄입니다\n".length
        assertEquals(expectedCut, cutIndex)
        val remaining = text.substring(0, cutIndex).trimEnd()
        assertEquals("첫 번째 줄입니다", remaining)
    }

    @Test
    fun `빈_문자열_및_공백_처리`() {
        assertEquals(0, DearTalkIME.findLastSentenceCutIndex(""))
        assertEquals(0, DearTalkIME.findLastSentenceCutIndex("    "))
        assertEquals(0, DearTalkIME.findLastSentenceCutIndex("...!!!???"))
    }
}
