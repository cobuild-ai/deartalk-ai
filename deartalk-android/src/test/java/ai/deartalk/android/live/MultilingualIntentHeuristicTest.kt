package ai.deartalk.android.live

import ai.deartalk.android.live.data.MultilingualIntentHeuristic
import ai.deartalk.android.live.data.SpeechIntent
import org.junit.Assert.assertEquals
import org.junit.Test

class MultilingualIntentHeuristicTest {

    @Test
    fun testUniversalQuestionMark() {
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("안녕하세요?", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("Hello?", "EN"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("Apa kabar?", "ID"))
    }

    @Test
    fun testBlankOrEmptyInput() {
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("", "KO"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("   ", "EN"))
    }

    @Test
    fun testKoreanIntents() {
        // QUESTION
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("오늘 밥 먹을까", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("지금 가나요", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("이거 어때", "KO"))

        // REQUEST
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("문 좀 열어주세요", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("자료 확인 바랍니다", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("부탁드립니다", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("이것 좀 해줘", "KO"))

        // CONFIRM
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("내일 만나는 거지", "KO"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("그게 맞지", "KO"))

        // STATEMENT
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("나는 지금 공부를 하고 있어", "KO"))
    }

    @Test
    fun testEnglishIntents() {
        // REQUEST
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("could you please send the file", "EN"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("Can you check this", "EN"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("Please call me back", "EN"))

        // QUESTION
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("what time is the meeting", "EN"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("how are you doing", "EN"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("do you need anything", "EN"))

        // CONFIRM
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("you will be there right", "EN"))

        // STATEMENT
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("I have completed the task", "EN"))
    }

    @Test
    fun testIndonesianIntents() {
        // REQUEST
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("tolong bantu saya", "ID"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("mohon konfirmasinya", "ID"))

        // QUESTION
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("apakah anda sudah siap", "ID"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("kapan kita bertemu", "ID"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("di mana kantornya", "ID"))

        // CONFIRM
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("ini sudah selesai kan", "ID"))

        // STATEMENT
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("saya sedang bekerja", "ID"))
    }

    @Test
    fun testJapaneseAndChineseIntents() {
        // Japanese
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("これですか", "JA"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("お願いします", "JA"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("そうですよね", "JA"))

        // Chinese
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("你在忙吗", "ZH"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("请帮我一下", "ZH"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("好的对吧", "ZH"))
    }

    @Test
    fun testEmojiAndQuotationRobustness() {
        // 이모지(😊, 🤣, 😼 등)가 문장 끝에 붙어 있어도 화행이 100% 정상 판별되어야 함
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("밥은 잘 먹었어? 😊", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("내일 몇 시에 만날까? 🤣", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("자료 좀 보내줘 😊", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("문 좀 열어주세요 🙏", "KO"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("내일 3시 맞지? 😼", "KO"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("그게 맞죠 😊", "KO"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("지금 집에 가고 있어 😊", "KO"))

        // 따옴표 및 공백 래핑된 문장 정제 검증
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("\"식사하셨습니까?\"", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("'How are you?'", "EN"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("  \"Please send the report.\"  ", "EN"))
    }

    @Test
    fun testEnhancedKoreanIntentPatterns() {
        // 신규 어미 및 키워드 검증
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("식사하셨나요", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("언제 뵐 수 있을까요", "KO"))
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("밥 먹었니", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("파일 좀 보내줘", "KO"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("이것 좀 도와줘", "KO"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("내일 출발하는 거죠", "KO"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("오늘 다 끝났죠", "KO"))
    }

    @Test
    fun testEuropeanAndVietnameseIntents() {
        // Spanish (ES)
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("¿cómo estás?", "ES"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("por favor ayúdame", "ES"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("estás listo verdad", "ES"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("estoy en la oficina", "ES"))

        // French (FR)
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("est-ce que vous venez", "FR"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("aidez-moi s'il vous plaît", "FR"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("c'est bon n'est-ce pas", "FR"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("je suis prêt", "FR"))

        // German (DE)
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("wo bist du", "DE"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("bitte hilf mir", "DE"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("alles klar oder", "DE"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("ich arbeite gerade", "DE"))

        // Vietnamese (VI)
        assertEquals(SpeechIntent.QUESTION, MultilingualIntentHeuristic.guessIntent("bạn ăn cơm chưa", "VI"))
        assertEquals(SpeechIntent.REQUEST, MultilingualIntentHeuristic.guessIntent("làm ơn giúp tôi", "VI"))
        assertEquals(SpeechIntent.CONFIRM, MultilingualIntentHeuristic.guessIntent("hôm nay xong rồi đúng không", "VI"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("tôi đang bận", "VI"))
    }

    @Test
    fun testExtremeInputBoundaries() {
        // 단독 특수문자, 기호만 있는 경우 STATEMENT 안전 fallback
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("...", "KO"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("!@#$%", "EN"))
        assertEquals(SpeechIntent.STATEMENT, MultilingualIntentHeuristic.guessIntent("😊👍🎉", "KO"))
    }
}

