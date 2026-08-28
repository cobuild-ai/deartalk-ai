package ai.deartalk.android.ime

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 문장 단위 백스페이스(Sentence Backspace) 절단 로직 단위 테스트
 * - Given / When / Then 패턴을 준수하여 마침표/느낌표/물음표 및 개행 기반 문장 절단을 검증합니다.
 */
class SentenceBackspaceTest {

    @Test
    fun `단일_문장_삭제_시_0을_반환하여_전체_삭제_기준점을_제공한다`() {
        // Given
        val singleSentence = "안녕하세요 반갑습니다."

        // When
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(singleSentence)

        // Then
        assertEquals("단일 문장은 시작점(0)이 절단 위치여야 합니다.", 0, cutIndex)
    }

    @Test
    fun `마침표_없는_단일_문장_삭제_시_0을_반환한다`() {
        // Given
        val textWithoutPeriod = "안녕하세요 반갑습니다"

        // When
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(textWithoutPeriod)

        // Then
        assertEquals("마침표가 없는 단일 문장은 시작점(0)이 절단 위치여야 합니다.", 0, cutIndex)
    }

    @Test
    fun `여러_문장_중_마지막_문장만_정확히_절단한다`() {
        // Given
        val multiSentence = "첫 번째 문장입니다. 두 번째 문장입니다! 세 번째 문장인가요?"
        val expectedCut = "첫 번째 문장입니다. 두 번째 문장입니다!".length

        // When
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(multiSentence)

        // Then
        assertEquals("두 번째 문장 끝 위치가 절단 인덱스여야 합니다.", expectedCut, cutIndex)
        val remaining = multiSentence.substring(0, cutIndex).trimEnd()
        assertEquals("앞선 문장들은 보존되어야 합니다.", "첫 번째 문장입니다. 두 번째 문장입니다!", remaining)
    }

    @Test
    fun `줄바꿈_기준으로_마지막_줄만_정확히_절단한다`() {
        // Given
        val multiLineText = "첫 번째 줄입니다\n두 번째 줄입니다"
        val expectedCut = "첫 번째 줄입니다\n".length

        // When
        val cutIndex = DearTalkIME.findLastSentenceCutIndex(multiLineText)

        // Then
        assertEquals("개행 문자 바로 뒤가 절단 인덱스여야 합니다.", expectedCut, cutIndex)
        val remaining = multiLineText.substring(0, cutIndex).trimEnd()
        assertEquals("첫 번째 줄 내용이 보존되어야 합니다.", "첫 번째 줄입니다", remaining)
    }

    @Test
    fun `빈_문자열_공백_구두점만_있는_경우_0을_반환한다`() {
        // Given & When & Then
        assertEquals("빈 문자열은 0을 반환해야 합니다.", 0, DearTalkIME.findLastSentenceCutIndex(""))
        assertEquals("공백 문자열은 0을 반환해야 합니다.", 0, DearTalkIME.findLastSentenceCutIndex("    "))
        assertEquals("구두점 연속 문자열은 0을 반환해야 합니다.", 0, DearTalkIME.findLastSentenceCutIndex("...!!!???"))
    }
}
