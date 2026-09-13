package ai.deartalk.android.stt

/**
 * 🎙️ 언어학적 문맥 패턴 기반 의문문 판별기
 * - 안드로이드 SpeechRecognizer의 플랫폼 제약(원시 오디오 스트림 미제공)에 따라 무의미한 DSP 주파수 추정을 제거하고,
 * - 명백한 의문사 및 의문형 종결어미 패턴을 엄격하게 필터링하여 평서문이 의문문으로 왜곡되는 현상을 원천 방지합니다.
 */
object IntonationAnalyzer {

    /**
     * 언어학적 문맥 기반 의문문 패턴 분석
     * - 평서문과 형태소가 동일한 모호한 어미("어", "아", "있어", "괜찮아" 등)는 제외하고,
     *   명확한 의문사 및 전형적 의문 종결어미만 엄격하게 감지합니다.
     */
    fun isLikelyQuestion(text: String, languageCode: String = "KO"): Boolean {
        val trimmed = text.trim().removeSuffix(".").removeSuffix("?").trim()
        if (trimmed.isBlank()) return false

        when (languageCode.uppercase()) {
            "KO" -> {
                // 1. 1인칭 진술("나 지금...", "내가...", "저는...")은 명시적 의문사가 없는 한 평서문 우선
                val firstPersonPrefixes = listOf("나 ", "난 ", "내가 ", "나는 ", "나도 ", "저 ", "제가 ", "저는 ", "저도 ")
                val hasFirstPerson = firstPersonPrefixes.any { trimmed.startsWith(it) }

                // 2. 한국어 명시적 의문사
                val questionKeywords = listOf(
                    "혹시", "언제", "어디", "누구", "누가", "무엇", "뭐", "왜", "어떻게",
                    "얼마", "얼마나", "몇", "며칠", "몇일", "어느", "어떤", "무슨",
                    "어찌", "어째서"
                )
                val hasQuestionKeyword = questionKeywords.any { trimmed.contains(it) }

                if (hasQuestionKeyword) return true
                if (hasFirstPerson) return false

                // 3. 한국어 명백한 의문형 종결 어미 (평서문과 혼동되지 않는 전형적 질문 어미)
                val explicitQuestionEndings = listOf(
                    "까", "습니까", "십니까", "나요", "가요", "실까요", "을까요", "ㄹ까요",
                    "냐", "니", "나", "는가", "인가", "던가", "려나", "어때", "어때요", "맞나요", "맞죠"
                )

                val lastWord = trimmed.split(Regex("\\s+")).lastOrNull() ?: trimmed
                for (ending in explicitQuestionEndings) {
                    if (lastWord.endsWith(ending)) {
                        return true
                    }
                }
            }
            "ID" -> {
                // 인도네시아어 명시적 의문사 및 질문형 종결사
                val idKeywords = listOf(
                    "apakah", "kapan", "dimana", "di mana", "siapa", "mengapa", "kenapa",
                    "bagaimana", "berapa", "mana"
                )
                if (idKeywords.any { trimmed.lowercase().contains(it) }) return true

                val idEndings = listOf("kan", "kah", "ya")
                val lastWord = trimmed.split(Regex("\\s+")).lastOrNull()?.lowercase() ?: trimmed.lowercase()
                if (idEndings.any { lastWord == it || lastWord.endsWith(it) }) return true
            }
            "EN" -> {
                // 영어 의문사 및 조동사/부정조동사 의문문 시작
                val enWords = trimmed.lowercase().split(Regex("\\s+"))
                val firstWord = enWords.firstOrNull() ?: ""
                val lastWord = enWords.lastOrNull() ?: ""

                val questionStarters = listOf(
                    "what", "when", "where", "who", "why", "how",
                    "is", "are", "do", "does", "did", "can", "could", "will", "would", "should",
                    "can't", "couldn't", "won't", "wouldn't", "shouldn't", "don't", "doesn't", "didn't",
                    "isn't", "aren't", "hasn't", "haven't"
                )
                if (questionStarters.contains(firstWord)) return true

                val questionTags = listOf("right", "huh")
                if (questionTags.contains(lastWord)) return true
            }
        }

        return false
    }

    /**
     * 🔄 영어 도치 의문문 구조를 자연스러운 평서문(Declarative)으로 변환
     * 예: "Can't we just handle it with the rear camera." -> "We can't just handle it with the rear camera."
     * 예: "Are you at home?" -> "You are at home."
     */
    fun convertToDeclarativeEnglish(text: String): String {
        val trimmed = text.trim().removeSuffix("?").removeSuffix(".").trim()
        if (trimmed.isBlank()) return text

        val auxPattern = Regex("""^(Can't|Couldn't|Won't|Wouldn't|Shouldn't|Don't|Doesn't|Didn't|Isn't|Aren't|Hasn't|Haven't|Can|Could|Will|Would|Should|Do|Does|Did|Are|Is|Was|Were)\s+([wW]e|[yY]ou|[iI]|[tT]hey|[hH]e|[sS]he|[iI]t|[tT]his|[tT]hat)\b\s*(.*)$""", RegexOption.IGNORE_CASE)
        val match = auxPattern.find(trimmed)
        if (match != null) {
            val aux = match.groupValues[1].lowercase()
            val subjectRaw = match.groupValues[2]
            val rest = match.groupValues[3].trim()

            val subject = if (subjectRaw.equals("i", ignoreCase = true)) "I" else subjectRaw.replaceFirstChar { it.uppercase() }
            val auxDeclarative = when (aux) {
                "do", "does", "did" -> "" // 일반 조동사 do/does/did는 평서문에서 생략 가능하거나 본동사 결합
                else -> aux
            }

            val body = if (auxDeclarative.isNotBlank()) {
                if (rest.isNotBlank()) "$subject $auxDeclarative $rest" else "$subject $auxDeclarative"
            } else {
                if (rest.isNotBlank()) "$subject $rest" else subject
            }
            return "$body."
        }

        return "$trimmed."
    }

    /**
     * 하위 호환성 유지용 래퍼
     */
    fun detectIntonationQuestion(text: String, languageCode: String = "KO"): Boolean {
        return isLikelyQuestion(text, languageCode)
    }
}
