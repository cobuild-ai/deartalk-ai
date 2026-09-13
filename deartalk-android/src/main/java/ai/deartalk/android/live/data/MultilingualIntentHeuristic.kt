package ai.deartalk.android.live.data

object MultilingualIntentHeuristic {
    
    /**
     * STT가 종료된 텍스트와 해당 언어 코드를 바탕으로 초고속으로 화행(Intent)을 휴리스틱 추론합니다.
     * 이 클래스는 AI 엔진 번역 전, UI 버튼 시각적 반응(미리 켜짐)을 위해 사용됩니다.
     */
    fun guessIntent(text: String, langCode: String): SpeechIntent {
        val t = text.trim()
        if (t.isBlank()) return SpeechIntent.STATEMENT
        val lowerT = t.lowercase()
        
        // 1. 공통 기호 (Universal)
        if (t.endsWith("?")) return SpeechIntent.QUESTION
        
        // 2. 언어별 특징 패턴 분석
        return when (langCode.take(2).uppercase()) {
            "KO" -> {
                when {
                    t.endsWith("까") || t.endsWith("나요") || t.endsWith("대요") || t.endsWith("어때") || t.endsWith("래") -> SpeechIntent.QUESTION
                    t.endsWith("주세요") || t.endsWith("바랍니다") || t.contains("부탁") || t.endsWith("해줘") || t.endsWith("해라") -> SpeechIntent.REQUEST
                    t.endsWith("지") || t.endsWith("맞지") || t.endsWith("그렇지") || t.endsWith("거지") -> SpeechIntent.CONFIRM
                    else -> SpeechIntent.STATEMENT
                }
            }
            "EN" -> {
                when {
                    lowerT.startsWith("could you") || lowerT.startsWith("can you") || lowerT.startsWith("would you") || lowerT.contains("please") || lowerT.startsWith("please") -> SpeechIntent.REQUEST
                    lowerT.startsWith("are ") || lowerT.startsWith("is ") || lowerT.startsWith("do ") || lowerT.startsWith("does ") || lowerT.startsWith("did ") || lowerT.startsWith("what ") || lowerT.startsWith("how ") || lowerT.startsWith("why ") || lowerT.startsWith("where ") || lowerT.startsWith("when ") || lowerT.startsWith("who ") -> SpeechIntent.QUESTION
                    lowerT.endsWith("right") || lowerT.endsWith("correct") -> SpeechIntent.CONFIRM
                    else -> SpeechIntent.STATEMENT
                }
            }
            "ID" -> {
                when {
                    lowerT.contains("tolong") || lowerT.contains("mohon") || lowerT.contains("silakan") || lowerT.startsWith("bisa") || lowerT.startsWith("bisakah") -> SpeechIntent.REQUEST
                    lowerT.startsWith("apakah") || lowerT.startsWith("bagaimana") || lowerT.startsWith("kenapa") || lowerT.startsWith("mengapa") || lowerT.startsWith("kapan") || lowerT.startsWith("siapa") || lowerT.startsWith("di mana") -> SpeechIntent.QUESTION
                    lowerT.endsWith("kan") || lowerT.endsWith("ya") || lowerT.endsWith("benar") -> SpeechIntent.CONFIRM
                    else -> SpeechIntent.STATEMENT
                }
            }
            "JA" -> {
                when {
                    t.endsWith("か") || t.endsWith("ですか") || t.endsWith("ますか") -> SpeechIntent.QUESTION
                    t.endsWith("ください") || t.endsWith("お願いします") || t.endsWith("ちょうだい") -> SpeechIntent.REQUEST
                    t.endsWith("ね") || t.endsWith("ですよね") || t.endsWith("確認") -> SpeechIntent.CONFIRM
                    else -> SpeechIntent.STATEMENT
                }
            }
            "ZH" -> {
                when {
                    t.endsWith("吗") || t.endsWith("嗎") || t.endsWith("呢") || lowerT.startsWith("为什么") || lowerT.startsWith("怎么") -> SpeechIntent.QUESTION
                    lowerT.startsWith("请") || lowerT.startsWith("請") || lowerT.contains("帮忙") || lowerT.contains("幫忙") -> SpeechIntent.REQUEST
                    t.endsWith("吧") || t.endsWith("对吧") || t.endsWith("對吧") -> SpeechIntent.CONFIRM
                    else -> SpeechIntent.STATEMENT
                }
            }
            "ES" -> {
                when {
                    lowerT.contains("por favor") -> SpeechIntent.REQUEST
                    lowerT.startsWith("cómo") || lowerT.startsWith("qué") || lowerT.startsWith("dónde") || lowerT.startsWith("cuándo") || lowerT.startsWith("quién") || lowerT.startsWith("por qué") || t.startsWith("¿") -> SpeechIntent.QUESTION
                    lowerT.endsWith("verdad") || lowerT.endsWith("cierto") -> SpeechIntent.CONFIRM
                    else -> SpeechIntent.STATEMENT
                }
            }
            else -> {
                SpeechIntent.STATEMENT
            }
        }
    }
}
