package ai.deartalk.android.stt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntonationAnalyzerTest {

    @Test
    fun `한국어_의문문_판별_테스트`() {
        // 명시적 의문사 및 전형적 의문 종결형
        assertTrue(IntonationAnalyzer.isLikelyQuestion("내일 몇 시에 만날까", "KO"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("혹시 시간 있어", "KO"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("이 방향 맞나요", "KO"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("오늘 회식 가나", "KO"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("어떻게 생각해", "KO"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("언제 출발해", "KO"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("이거 어때요", "KO"))

        // 🌟 평서문 및 1인칭 진술은 의문문으로 오판되지 않아야 함 (False Positive 방지)
        assertFalse(IntonationAnalyzer.isLikelyQuestion("나 괜찮아", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("밥 먹었어", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("좋은 일 있었어", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("나 지금 출발했어", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("자료 보냈으니 확인해줘", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("지금 출발했습니다", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("내일 가겠습니다", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("자료 송부드립니다", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("감사합니다", "KO"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("수고하셨습니다", "KO"))
    }

    @Test
    fun `인도네시아어_의문문_판별_테스트`() {
        assertTrue(IntonationAnalyzer.isLikelyQuestion("sudah makan kan", "ID"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("apakah kamu siap", "ID"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("kamu mau pergi ke mana", "ID"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("siapa namamu", "ID"))

        assertFalse(IntonationAnalyzer.isLikelyQuestion("terima kasih banyak", "ID"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("saya sedang di jalan", "ID"))
    }

    @Test
    fun `영어_의문문_판별_테스트`() {
        assertTrue(IntonationAnalyzer.isLikelyQuestion("you leaving now right", "EN"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("what time do we meet", "EN"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("are you ready", "EN"))
        assertTrue(IntonationAnalyzer.isLikelyQuestion("can you help me", "EN"))

        assertFalse(IntonationAnalyzer.isLikelyQuestion("i am leaving now", "EN"))
        assertFalse(IntonationAnalyzer.isLikelyQuestion("thank you very much", "EN"))
    }
}
