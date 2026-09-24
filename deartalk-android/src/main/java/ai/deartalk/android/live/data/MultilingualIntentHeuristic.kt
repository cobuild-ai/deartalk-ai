package ai.deartalk.android.live.data

import ai.deartalk.android.agent.language.LanguageProfileRegistry

object MultilingualIntentHeuristic {
    
    /**
     * 🌐 단일 진실 공급원(LanguageProfileRegistry SSOT) 기반 초고속 화행(Intent) 추론 엔진 (하드코딩 0%)
     * - 문장 끝에 붙은 이모지(😊, 😼 등)와 따옴표를 안전하게 정제한 후 프로필 데이터에 따라 판별합니다.
     */
    fun guessIntent(text: String, langCode: String): SpeechIntent {
        val t = text.trim()
        if (t.isBlank()) return SpeechIntent.STATEMENT
        
        // 1. 말미의 따옴표, 구두점 앞뒤 공백 및 이모지/특수기호 안전 정제
        val clean = t
            .trim('"', '\'', '`', '“', '”', '‘', '’', ' ')
            .replace(Regex("""[\s\p{So}\p{Sk}\p{Sm}\p{Sc}\p{C}]+$"""), "") // 말미 이모지 소거
            .trim()
            
        if (clean.isBlank()) return SpeechIntent.STATEMENT
        
        val normalizedCode = if (langCode.length >= 2) langCode.take(2).uppercase() else langCode.uppercase()
        val profile = LanguageProfileRegistry.get(normalizedCode)
        val lowerClean = clean.lowercase()
        val coreText = clean.removeSuffix("?").removeSuffix("？").trim()
        val lowerCore = coreText.lowercase()
        val lastWord = lowerCore.split(Regex("[\\s,.]+")).lastOrNull() ?: lowerCore

        // 2. REQUEST (부탁) 우선 검사 (예: "문 좀 열어주세요", "Could you please send the file?")
        val requestRule = profile.getIntentRule(SpeechIntent.REQUEST)
        if (requestRule != null) {
            val matchesPrefix = requestRule.prefixes.isNotEmpty() && requestRule.prefixes.any { lowerClean.startsWith(it.lowercase()) }
            val matchesKeyword = requestRule.keywords.isNotEmpty() && requestRule.keywords.any { lowerClean.contains(it.lowercase()) }
            val matchesEnding = requestRule.endings.isNotEmpty() && requestRule.endings.any { coreText.endsWith(it) || lowerCore.endsWith(it.lowercase()) }
            if (matchesPrefix || matchesKeyword || matchesEnding) {
                return SpeechIntent.REQUEST
            }
        }

        // 3. CONFIRM (확인) 검사 (예: "내일 3시 맞지?", "그렇죠?", "ini sudah selesai kan")
        val confirmRule = profile.getIntentRule(SpeechIntent.CONFIRM)
        if (confirmRule != null) {
            val matchesKeyword = confirmRule.keywords.isNotEmpty() && confirmRule.keywords.any { lowerClean.contains(it.lowercase()) }
            val matchesEnding = confirmRule.endings.isNotEmpty() && confirmRule.endings.any { ending ->
                val lowerEnding = ending.lowercase()
                if (normalizedCode == "KO" || normalizedCode == "JA" || normalizedCode == "ZH") {
                    coreText.endsWith(ending)
                } else {
                    lastWord == lowerEnding || lowerCore.endsWith(" $lowerEnding") || lowerCore.endsWith(", $lowerEnding")
                }
            }
            if (matchesKeyword || matchesEnding) {
                return SpeechIntent.CONFIRM
            }
        }

        // 4. 공통 문장부호 (Universal Question Mark) - REQUEST/CONFIRM이 아닌 일반 의문문 ("밥은 잘 먹었어?")
        if (clean.endsWith("?") || clean.endsWith("？")) return SpeechIntent.QUESTION

        // 5. 언어별 QUESTION (질문) 패턴 검사 (예: "오늘 밥 먹을까", "di mana kantornya")
        val questionRule = profile.getIntentRule(SpeechIntent.QUESTION)
        if (questionRule != null) {
            val matchesPrefix = questionRule.prefixes.isNotEmpty() && questionRule.prefixes.any { lowerClean.startsWith(it.lowercase()) }
            val matchesKeyword = questionRule.keywords.isNotEmpty() && questionRule.keywords.any { lowerClean.contains(it.lowercase()) }
            val matchesEnding = questionRule.endings.isNotEmpty() && questionRule.endings.any { coreText.endsWith(it) || lowerCore.endsWith(it.lowercase()) }
            if (matchesPrefix || matchesKeyword || matchesEnding) {
                return SpeechIntent.QUESTION
            }
        }

        return SpeechIntent.STATEMENT
    }
}
