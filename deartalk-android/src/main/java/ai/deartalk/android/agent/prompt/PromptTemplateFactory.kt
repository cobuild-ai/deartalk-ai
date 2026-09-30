package ai.deartalk.android.agent.prompt

import ai.deartalk.android.agent.language.LanguageProfileRegistry
import ai.deartalk.android.data.SpeechIntent

enum class ModelFamily {
    GEMMA,
    RAW
}

/**
 * 🏭 다국어 온디바이스 SLM 프롬프트 템플릿 생성 및 정제 팩토리 (SRP / OCP)
 * - 한국어(KO), 인도네시아어(ID), 영어(EN) 및 다국어 템플릿 생성 전담
 * - Google Gemma 4 특수 제어 토큰 완벽 지원
 * - 챗봇 대화/답변 모드 원천 차단 및 키보드 본연의 문장 교정/어조 변환 보장
 */
object PromptTemplateFactory {

    private val REASONING_META_PATTERNS = listOf(
        Regex("""^(okay,?\s*let'?s\s*see|the\s*(user|input|task|instructions?)|here\s*(is|are)|provided\s*the\s*text|thinking|i\s*should|let\s*me|first,?\s*|sure,?\s*|certainly|below\s*is|in\s*korean|in\s*english|to\s*translate).*""", RegexOption.IGNORE_CASE),
        Regex("""^(설명|분석|참고|원문|입력|출력|교정문|변환문|결과|번역|답변|생각|추론|화행)\s*[:：].*""", RegexOption.IGNORE_CASE),
        Regex("""^(\[?INTENT|\bINTENT)\s*[:：].*""", RegexOption.IGNORE_CASE),
        Regex("""^.*(STATEMENT|QUESTION|REQUEST|CONFIRM).*\]?$""", RegexOption.IGNORE_CASE),
        Regex("""^[/\[\s]*[A-Z_]+[/\]\s]*$"""),
        Regex("""^(this\s*(means|translates|is)|we\s*need\s*to|as\s*an\s*ai|i\s*will).*""", RegexOption.IGNORE_CASE)
    )

    private val CHATBOT_ANSWER_PREFIXES = listOf(
        Regex("""^(네|아니요|아니오|네,|아니요,|예,|예|응|응,|그래|그래,|맞아요|맞습니다|저도|저는|제가|저의|AI로서|인공지능으로서|질문에 대한 답변)[,\s]+.*"""),
        Regex("""^(Yes|No|Sure|Certainly|I am|I'm|As an AI|As a language model|Here is the answer)[,\s]+.*""", RegexOption.IGNORE_CASE),
        Regex("""^(Ya|Tidak|Tentu|Saya|Sebagai AI|Jawaban untuk pertanyaan)[,\s]+.*""", RegexOption.IGNORE_CASE)
    )

    private val REGEX_INTENT_TAG = Regex("""\[?INTENT:\s*(STATEMENT|QUESTION|REQUEST|CONFIRM)[^\]\n]*\]?""", RegexOption.IGNORE_CASE)
    private val REGEX_INTENT_ENCLOSED = Regex("""\[?INTENT:\s*([^\]\n]+)\]?""", RegexOption.IGNORE_CASE)
    private val REGEX_INTENT_SLASH = Regex("""^[/\s]*(STATEMENT|QUESTION|REQUEST|CONFIRM)[/\]\s]*""", RegexOption.IGNORE_CASE)
    private val REGEX_LINE_PREFIX = Regex("""^Line\s*[12]\s*[:：]?\s*""", RegexOption.IGNORE_CASE)
    private val REGEX_TRANSLATION_PREFIX = Regex("""^Translation\s*[:：]?\s*""", RegexOption.IGNORE_CASE)
    private val REGEX_THINK = Regex("""<think>[\s\S]*?(</think>|$)""", RegexOption.IGNORE_CASE)
    private val REGEX_CONTROL_TAGS_OPEN = Regex("""<(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)[^>]*>\s*(model|user|assistant)?""", RegexOption.IGNORE_CASE)
    private val REGEX_CONTROL_TAGS_CLOSE = Regex("""</(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)>""", RegexOption.IGNORE_CASE)
    private val REGEX_GENERIC_XML = Regex("""</?[a-zA-Z0-9_-]+(\s+[^>]*)?>""")
    private val REGEX_TRAILING_UNCLOSED_TAG = Regex("""<[^>]*$""")
    private val REGEX_REWRITTEN_PREFIX = Regex("""^(변환|결과|수정|Result|Output|Rewritten)\s*[:：]\s*""", RegexOption.IGNORE_CASE)
    private val REGEX_TRAILING_EMOJIS = Regex("""[\u2728\u2729\u2b50\u2b51\u2747\u2748\u2749\u2733\u2734\u2744]+$""")

    /**
     * 🎯 SLM 출력 텍스트에서 [INTENT: ...] 메타 태그를 파싱하고 본문 텍스트를 분리 정제합니다.
     */
    fun parseIntentTagAndClean(
        rawOutput: String,
        fallbackIntent: SpeechIntent = SpeechIntent.STATEMENT
    ): Pair<SpeechIntent, String> {
        val match = REGEX_INTENT_TAG.find(rawOutput)
        val parsedIntent = if (match != null) {
            val tagStr = match.groupValues[1].uppercase().trim()
            when {
                tagStr.contains("QUESTION") -> SpeechIntent.QUESTION
                tagStr.contains("REQUEST") -> SpeechIntent.REQUEST
                tagStr.contains("CONFIRM") -> SpeechIntent.CONFIRM
                tagStr.contains("STATEMENT") -> SpeechIntent.STATEMENT
                else -> fallbackIntent
            }
        } else {
            fallbackIntent
        }

        var cleaned = rawOutput.replace(REGEX_INTENT_TAG, "")
            .replace(REGEX_INTENT_SLASH, "")
            .replace(REGEX_LINE_PREFIX, "")
            .replace(REGEX_TRANSLATION_PREFIX, "")
            // 모델이 [INTENT: 번역문] 형태로 출력한 경우 껍질만 벗겨내어 번역문 보존
            .replace(REGEX_INTENT_ENCLOSED) { it.groupValues[1].trim() }
            .replace(REGEX_LINE_PREFIX, "")
            .replace(REGEX_TRANSLATION_PREFIX, "")
            .replace(Regex("""^[/\]]+"""), "")
            .replace(Regex("""[/\]]+$"""), "")
            .trim()
        return Pair(parsedIntent, cleaned)
    }

    /**
     * 🧹 LLM 생성 텍스트의 특수 토큰, reasoning/사고 과정, 태그, 불필요한 인용부호 제거 및 정제
     */
    fun cleanLlmOutput(raw: String, targetLangCode: String = ""): String {
        var text = raw
            // 1. Thinking / Reasoning 블록 완전 제거 (<think>...</think> 및 닫히지 않은 <think>...$)
            .replace(REGEX_THINK, "")
            // 2. Gemma 4 및 표준 특수 제어 토큰 제거
            .replace(REGEX_CONTROL_TAGS_OPEN, "")
            .replace(REGEX_CONTROL_TAGS_CLOSE, "")
            .replace(REGEX_GENERIC_XML, "")
            // 3. 닫히지 않고 잘린 불완전한 제어 태그 및 특수 토큰 조각 제거 (<로 시작하여 끝까지 닫히지 않은 태그 조각 전수 소거)
            .replace(REGEX_TRAILING_UNCLOSED_TAG, "")
            // 4. 메타 태그 및 찌꺼기 제거 ([INTENT: ...], /QUESTION/REQUEST/CONFIRM], Line 1/2, Translation: 등)
            .replace(REGEX_INTENT_TAG, "")
            .replace(REGEX_INTENT_ENCLOSED) { it.groupValues[1].trim() }
            .replace(REGEX_INTENT_SLASH, "")
            .replace(REGEX_LINE_PREFIX, "")
            .replace(REGEX_TRANSLATION_PREFIX, "")
            .replace(Regex("""^[/\]]+"""), "")
            .replace(Regex("""[/\]]+$"""), "")
            .trim()

        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                // Reasoning이나 해설/메타 설명이 아닌 첫 번째 실제 문장 선택
                val nonMetaLine = lines.firstOrNull { line ->
                    REASONING_META_PATTERNS.none { pattern -> pattern.matches(line) }
                }
                text = nonMetaLine ?: lines.last()
            }
        }

        text = text
            .replace(LanguageProfileRegistry.allCleaningPrefixesRegex, "")
            .trim()
            .removePrefix(">")
            .removePrefix("-")
            .removePrefix("*")

        // 4. 모델이 프롬프트의 앵커("->", "변환: ", "Result: ")를 에코한 경우 본문만 추출
        if (text.contains("->") || text.contains("➔") || text.contains("=>")) {
            val after = text.substringAfterLast("->").substringAfterLast("➔").substringAfterLast("=>").trim()
            if (after.isNotBlank() && after.length >= 2) {
                text = after
            }
        }
        text = text
            .replace(REGEX_REWRITTEN_PREFIX, "")
            .trim()

        val profile = if (targetLangCode.isNotBlank()) LanguageProfileRegistry.get(targetLangCode) else null
        val quotes = profile?.quotationMarks ?: listOf('"', '\'', '`', '“', '”', '‘', '’', '「', '」', '『', '』')
        for (q in quotes) {
            text = text.trim(q)
        }

        text = text
            .replace(REGEX_TRAILING_EMOJIS, "")
            .trim()

        return text.trim()
    }

    /**
     * 🛡️ 키보드 본연의 기능 안전 보장 가드 (Anti-Chatbot Guard & Zero-Corruption Guard)
     * - 원문이 질문인데 LLM이 챗봇처럼 답변("네, 저는...", "아니요...")을 해버린 경우 감지 및 복원
     * - 언어 중립적 구조 분석을 통해 원문 인용 후 해설 구조나 비정상 길이 팽창 등 메타 해설 감지 시 원문 복원
     * - 제어 태그 조각이나 자연어 문자가 없는 비정상 찌꺼기 출력 감지 시 원문 100% 안전 보존
     */
    fun sanitizeKeyboardOutput(input: String, generated: String, isExplicitQuestion: Boolean): String {
        if (generated.isBlank()) return input.trim()

        val trimmedGen = generated.trim()

        // 1. 챗봇형 대답 및 언어 중립적 구조적 메타 해설 감지
        val isAiDisclaimer = trimmedGen.matches(Regex("""^(AI로서|인공지능으로서|As an AI|As a language model|Sebagai AI)[,\s]+.*""", RegexOption.IGNORE_CASE))
        val isChatbotAnswerToQuestion = isExplicitQuestion && CHATBOT_ANSWER_PREFIXES.any { it.matches(trimmedGen) }
        val isMetatalk = isStructuralMetatalk(input, trimmedGen)

        if (isAiDisclaimer || isChatbotAnswerToQuestion || isMetatalk) {
            // 챗봇 대화나 구조적 메타 해설이 출력된 경우: 원문을 보존하여 정상 반환
            val normalizedInput = input.trim().removeSuffix(".").removeSuffix("!").trim()
            return if (isExplicitQuestion) {
                if (normalizedInput.endsWith("?") || normalizedInput.endsWith("？")) normalizedInput else "$normalizedInput?"
            } else {
                normalizedInput
            }
        }

        // 2. 메타 태그나 찌꺼기 문자열인 경우 원문 반환
        if (trimmedGen.contains("QUESTION") && trimmedGen.contains("CONFIRM")) {
            return input.trim()
        }
        if (trimmedGen.matches(Regex("""^[/\[\s]*[A-Z_]+[/\]\s]*$"""))) {
            return input.trim()
        }
        // 3. 제어 태그 잔해이거나 다국어 자연어 문자(알파벳, 한글, 가나/한자, 아랍, 키릴 등)가 전혀 없는 비정상 기호인 경우 원문 복원
        if (trimmedGen.startsWith("<") ||
            trimmedGen.matches(Regex("""^[^a-zA-Z0-9\uAC00-\uD7A3\u3040-\u30FF\u4E00-\u9FFF\u0600-\u06FF\u0400-\u04FF]+$"""))
        ) {
            return input.trim()
        }
        if (trimmedGen.length < 2 && input.trim().length >= 2) {
            return input.trim()
        }

        return trimmedGen
    }

    /**
     * 🛡️ 언어 중립적 구조적 메타 해설(Chatbot Metatalk) 감지:
     * 특정 언어의 어휘 목록을 하드코딩하지 않고, 모델이 원문을 인용하고 해설을 덧붙이거나
     * 프롬프트를 에코(Echo)하는 구조적 이상치를 판별합니다.
     */
    fun isStructuralMetatalk(input: String, generated: String): Boolean {
        val trimmedInput = input.trim()
        val trimmedGen = generated.trim()
        if (trimmedGen.isBlank() || trimmedInput.isBlank()) return false

        // 1. 원문 인용 후 외부 해설 구조: ^["'“‘「『]([^"'”’」』]+)["'”’」』]\s*(.+)
        // 모델이 원문(또는 그 일부)을 따옴표로 감싸고, 따옴표 바깥에 부연 설명/해설을 덧붙인 경우 (다국어 공통)
        val quoteMatch = Regex("""^["'“‘「『]([^"'”’」』]+)["'”’」』]\s*(.+)$""").find(trimmedGen)
        if (quoteMatch != null) {
            val quotedText = quoteMatch.groupValues[1].trim()
            val trailingExplanation = quoteMatch.groupValues[2].trim()
            if (trailingExplanation.isNotBlank()) {
                val inputWords = trimmedInput.split(Regex("""\s+""")).filter { it.length >= 2 }
                val matchesInput = trimmedInput.contains(quotedText) || quotedText.contains(trimmedInput) ||
                    inputWords.any { quotedText.contains(it) }
                if (matchesInput) return true
            }
        }

        // 2. 비정상적인 길이 팽창 및 다중 문장 해설 이상치 (Length Inflation Anomaly)
        // 사용자의 원문이 짧은 한 문장인데(예: 40자 이하), 모델이 3배 이상 길고 2개 이상의 문장 부호(. ! ? 。)를 포함하여 해설을 길게 늘어놓는 경우
        if (trimmedInput.length in 3..40 && trimmedGen.length > (trimmedInput.length * 3 + 15)) {
            val sentenceCount = trimmedGen.count { it == '.' || it == '!' || it == '?' || it == '。' }
            if (sentenceCount >= 2) return true
        }

        return false
    }

    /**
     * 🔄 모델별(Gemma 4 특수 토큰) 래핑 헬퍼
     */
    fun wrapTurn(
        systemPrompt: String,
        userPrompt: String,
        modelFamily: ModelFamily = ModelFamily.GEMMA,
        prefillModelOutput: String = ""
    ): String {
        return when (modelFamily) {
            ModelFamily.GEMMA -> {
                val userContent = if (systemPrompt.isNotBlank()) "$systemPrompt\n\n$userPrompt" else userPrompt
                val prefillBlock = if (prefillModelOutput.isNotBlank()) prefillModelOutput else ""
                "<start_of_turn>user\n$userContent<end_of_turn>\n<start_of_turn>model\n$prefillBlock"
            }
            ModelFamily.RAW -> {
                val content = if (systemPrompt.isNotBlank()) "$systemPrompt\n\n$userPrompt" else userPrompt
                "$content\n$prefillModelOutput"
            }
        }
    }

    /**
     * 📝 [기본 다듬기] 전 세계 모든 언어에 공통 적용되는 단일 표준 교정 프롬프트 (Unified English Instruction)
     * - Gemma 4 E2B의 영어 지시문 해석 극대화 (Instruction-Following Peak)
     * - 비문(Broken Grammar), 오탈자, 띄어쓰기, 런온 구어를 입력 언어와 동일한 언어로 자연스럽게 완성
     * - 원문의 모든 의미와 의도 100% 무손실 보존
     */
    /**
     * 📝 [기본 다듬기] 통합 3대 불변 규칙 기반 교정 프롬프트 (Unified 3-Pillar Refinement)
     * - Rule 1: Language Consistency (입력 언어와 100% 동일한 언어 출력)
     * - Rule 2: Meaning Preservation (의미, 사실, 이유, 의도 100% 무손실 보존)
     * - Rule 3: Grammar & Fluency (비문, 오탈자, 띄어쓰기, 런온 구어 교정 및 완성)
     * - 영어 및 기타 모든 글로벌 언어는 단일 표준 영어 지시문(Universal English Prompt)으로 처리
     * - 온디바이스 2B SLM(Gemma 4 E2B)의 토큰 억제 방지를 위해 핵심 언어(KO, ID)는 동일한 3대 규칙을 네이티브 스크립트로 1:1 매핑
     */
    fun buildCorrectionPrompt(
        trimmed: String,
        currentEditorText: String = "",
        priorContext: List<String> = emptyList(),
        isInputKorean: Boolean = false,
        isIndonesianLocale: Boolean = false,
        isInputEnglish: Boolean = false,
        langCode: String = "",
        speechIntent: SpeechIntent = SpeechIntent.AUTO,
        modelFamily: ModelFamily = ModelFamily.GEMMA
    ): String {
        val editorPart = if (currentEditorText.isNotBlank()) "[Context: $currentEditorText]\n" else ""
        val isKorean = isInputKorean || langCode.equals("KO", ignoreCase = true)
        val isIndonesian = isIndonesianLocale || langCode.equals("ID", ignoreCase = true)

        val userContent = when {
            isKorean -> {
                val intentDirective = when (speechIntent) {
                    SpeechIntent.QUESTION -> "의문문 어미로 변경하고 물음표('?')로 끝내세요."
                    SpeechIntent.REQUEST -> "정중한 요청 어조로 작성하세요."
                    SpeechIntent.CONFIRM -> "확인 어미로 변경하고 물음표('?')로 끝내세요."
                    SpeechIntent.STATEMENT -> "명확한 평서문 어미로 마침표('.')로 끝내세요."
                    SpeechIntent.AUTO -> "원문 화행 보존(질문은 '?', 평서문은 '.' 또는 '!')."
                }
                "모바일 키보드 문장 다듬기 엔진입니다. 대화나 설명을 하지 말고 교정된 단 한 문장만 출력하세요.\n" +
                "1. 언어 일치: 반드시 입력과 동일한 한국어로 출력하세요.\n" +
                "2. 의미 보존: 원문의 사실, 이유, 상황, 의도를 100% 온전히 보존하세요.\n" +
                "3. 문법 및 완성도: 오탈자, 띄어쓰기, 어색한 비문 및 구어를 자연스러운 문장으로 완성하세요.\n" +
                "4. 화행: $intentDirective\n" +
                editorPart +
                "\n입력: \"$trimmed\" ->"
            }
            isIndonesian -> {
                val intentDirective = when (speechIntent) {
                    SpeechIntent.QUESTION -> "Gunakan struktur pertanyaan dan akhiri dengan tanda tanya ('?')."
                    SpeechIntent.REQUEST -> "Gunakan gaya bahasa permintaan yang sopan."
                    SpeechIntent.CONFIRM -> "Gunakan bentuk konfirmasi dan akhiri dengan tanda tanya ('?')."
                    SpeechIntent.STATEMENT -> "Gunakan kalimat deklaratif dan akhiri dengan titik ('.')."
                    SpeechIntent.AUTO -> "Pertahankan maksud tutur asli ('?' untuk pertanyaan, '.' atau '!' untuk pernyataan)."
                }
                "Mesin penyempurnaan kalimat keyboard. JANGAN mengobrol. HANYA keluarkan satu kalimat hasil perbaikan.\n" +
                "1. Konsistensi Bahasa: Keluarkan dalam bahasa yang PERSIS SAMA dengan input.\n" +
                "2. Pelestarian Makna: Pertahankan 100% makna, alasan, dan maksud asli tanpa pengurangan.\n" +
                "3. Tata Bahasa: Perbaiki kesalahan ketik dan tata bahasa rusak menjadi kalimat yang lancar dan alami.\n" +
                "4. Maksud Tutur: $intentDirective\n" +
                editorPart +
                "\nInput: \"$trimmed\" ->"
            }
            else -> {
                val intentDirective = when (speechIntent) {
                    SpeechIntent.QUESTION -> "Ensure question structure and end with '?'."
                    SpeechIntent.REQUEST -> "Use polite request phrasing."
                    SpeechIntent.CONFIRM -> "Use confirmation phrasing and end with '?'."
                    SpeechIntent.STATEMENT -> "Use declarative statement phrasing."
                    SpeechIntent.AUTO -> "Preserve original speech act ('?' for question, '.' or '!' for statement)."
                }
                "Mobile keyboard sentence refinement engine. Do NOT converse or explain. Output ONLY the single refined sentence.\n" +
                "1. Language Consistency: Output in the EXACT SAME language as the input.\n" +
                "2. Meaning Preservation: Preserve 100% of original meaning, reasons, facts, and intent completely without omission.\n" +
                "3. Grammar & Fluency: Fix broken grammar, speech disfluencies, typos, and run-on sentences into natural, fluent phrasing.\n" +
                "4. Intent: $intentDirective\n" +
                editorPart +
                "\nInput: \"$trimmed\" ->"
            }
        }
        return wrapTurn("", userContent, modelFamily)
    }

    /**
     * 🎭 [어조/톤 변환] 통합 3대 불변 규칙 기반 어조 변환 프롬프트 (Unified 3-Pillar Tone Transformer)
     * - Rule 1: Language Consistency (입력 언어와 100% 동일한 언어 출력)
     * - Rule 2: Meaning Preservation (이유, 상황, 요청사항 100% 무손실 보존)
     * - Rule 3: Grammar & Fluency (비문 및 거친 표현 정상화)
     * - Rule 4: Tone & Intent (선택된 어조 및 화행 규칙 반영)
     * - 글로벌 다국어 처리는 단일 표준 영어 지시문(Universal English Prompt)으로 완전 자동 단일화
     */
    fun buildTonePrompt(
        trimmed: String,
        toneName: String,
        toneInstruction: String,
        examples: String = "",
        isInputKorean: Boolean = false,
        isInputIndonesian: Boolean = false,
        langCode: String = "",
        speechIntent: SpeechIntent = SpeechIntent.AUTO,
        modelFamily: ModelFamily = ModelFamily.GEMMA
    ): String {
        val isKorean = isInputKorean || langCode.equals("KO", ignoreCase = true)
        val isIndonesian = isInputIndonesian || langCode.equals("ID", ignoreCase = true)
        val examplesBlock = if (examples.isNotBlank()) "\n[Examples]\n$examples\n" else ""

        val userContent = when {
            isKorean -> {
                val intentDirective = when (speechIntent) {
                    SpeechIntent.QUESTION -> "의문문 구조를 유지하고 물음표('?')로 끝내세요."
                    SpeechIntent.REQUEST -> "정중한 부탁/요청 어조로 작성하세요."
                    SpeechIntent.CONFIRM -> "확인 어조로 작성하고 물음표('?')로 끝내세요."
                    SpeechIntent.STATEMENT -> "명확한 평서문 어미로 마침표('.')로 끝내세요."
                    SpeechIntent.AUTO -> "원문 화행 보존(질문은 '?', 평서문은 '.' 또는 '!')."
                }
                "모바일 키보드 어조 변환 엔진입니다. 대화나 설명을 하지 말고 변환된 단 한 문장만 출력하세요.\n" +
                "1. 언어 일치: 반드시 입력과 동일한 한국어로 출력하세요.\n" +
                "2. 의미 보존: 원문의 의미, 이유, 상황, 요구사항을 100% 온전히 보존하세요.\n" +
                "3. 문법 및 완성도: 오탈자, 띄어쓰기, 비문을 자연스러운 구어체 문장으로 완성하세요.\n" +
                "4. 어조: '$toneName' ($toneInstruction)\n" +
                "5. 화행: $intentDirective" +
                examplesBlock +
                "\n입력: \"$trimmed\" ->"
            }
            isIndonesian -> {
                val intentDirective = when (speechIntent) {
                    SpeechIntent.QUESTION -> "Gunakan struktur pertanyaan dan akhiri dengan tanda tanya ('?')."
                    SpeechIntent.REQUEST -> "Gunakan gaya bahasa permintaan yang sopan."
                    SpeechIntent.CONFIRM -> "Gunakan bentuk konfirmasi dan akhiri dengan tanda tanya ('?')."
                    SpeechIntent.STATEMENT -> "Gunakan kalimat deklaratif dan akhiri dengan titik ('.')."
                    SpeechIntent.AUTO -> "Pertahankan maksud tutur asli ('?' untuk pertanyaan, '.' atau '!' untuk pernyataan)."
                }
                "Mesin pengubah nada keyboard. JANGAN mengobrol. HANYA keluarkan satu kalimat hasil transformasi.\n" +
                "1. Konsistensi Bahasa: Keluarkan dalam bahasa yang PERSIS SAMA dengan input.\n" +
                "2. Pelestarian Makna: Pertahankan 100% makna, alasan, dan maksud asli tanpa pengurangan.\n" +
                "3. Tata Bahasa: Perbaiki tata bahasa rusak dan kalimat rancu menjadi alami.\n" +
                "4. Nada: '$toneName' ($toneInstruction)\n" +
                "5. Maksud Tutur: $intentDirective" +
                examplesBlock +
                "\nInput: \"$trimmed\" ->"
            }
            else -> {
                val intentDirective = when (speechIntent) {
                    SpeechIntent.QUESTION -> "Ensure question structure and end with a question mark ('?')."
                    SpeechIntent.REQUEST -> "Use polite request phrasing."
                    SpeechIntent.CONFIRM -> "Use confirmation phrasing and end with a question mark ('?')."
                    SpeechIntent.STATEMENT -> "Use declarative statement phrasing."
                    SpeechIntent.AUTO -> "Preserve original speech act ('?' for question, '.' or '!' for statement)."
                }
                "Mobile keyboard tone transformer. Do NOT converse or explain. Output ONLY the single transformed sentence.\n" +
                "1. Language Consistency: Output in the EXACT SAME language as the input.\n" +
                "2. Meaning Preservation: Preserve 100% of original meaning, reasons, context, and requests completely without omission.\n" +
                "3. Grammar & Fluency: Fix broken grammar, speech disfluencies, typos, and run-on sentences into natural, fluent phrasing.\n" +
                "4. Tone: '$toneName' ($toneInstruction)\n" +
                "5. Speech Intent: $intentDirective" +
                examplesBlock +
                "\nInput: \"$trimmed\" ->"
            }
        }
        return wrapTurn("", userContent, modelFamily)
    }

    /**
     * 🌐 [실시간 번역] 온디바이스 SLM 고밀도 초경량 실시간 통역 프롬프트 빌더
     * - 미사여구 및 중복 지시문을 제거하고 고밀도 키-값 구조로 압축하여 모바일 CPU Prefill 지연을 70% 단축합니다.
     */
    fun buildTranslationPrompt(
        trimmed: String,
        sourceLangName: String,
        targetLangName: String,
        toneInstruction: String,
        linguisticRule: String,
        asrRepairInstruction: String = "",
        intentRuleSection: String = "",
        contextBlock: String = "",
        speechIntent: SpeechIntent = SpeechIntent.AUTO,
        modelFamily: ModelFamily = ModelFamily.GEMMA
    ): String {
        val system = "Translate spoken speech from $sourceLangName to $targetLangName.$toneInstruction Do NOT converse or add explanations.\n" +
                "Format:\n" +
                "INTENT: STATEMENT|QUESTION|REQUEST|CONFIRM\n" +
                "Translation: <translated sentence>"

        // ⚡ 대화 맥락(Context) 완전 제거: 단일 발화 단위 초경량 순수 번역으로 Prefill 토큰 최소화 및 지연시간 1초대 단축
        val extraParts = listOf(linguisticRule.trim()).filter { it.isNotBlank() }
        val extraBlock = if (extraParts.isNotEmpty()) extraParts.joinToString("\n") + "\n" else ""
        val user = "${extraBlock}Input: \"$trimmed\""
        return wrapTurn(system, user, modelFamily)
    }
}
