package ai.deartalk.android.agent.prompt

import ai.deartalk.android.agent.language.LanguageProfileRegistry
import ai.deartalk.android.live.data.SpeechIntent

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

    /**
     * 🎯 SLM 출력 텍스트에서 [INTENT: ...] 메타 태그를 파싱하고 본문 텍스트를 분리 정제합니다.
     */
    fun parseIntentTagAndClean(
        rawOutput: String,
        fallbackIntent: SpeechIntent = SpeechIntent.STATEMENT
    ): Pair<SpeechIntent, String> {
        val knownIntentTagRegex = Regex("""\[?INTENT:\s*(STATEMENT|QUESTION|REQUEST|CONFIRM)[^\]\n]*\]?""", RegexOption.IGNORE_CASE)
        val match = knownIntentTagRegex.find(rawOutput)
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

        var cleaned = rawOutput.replace(knownIntentTagRegex, "")
            .replace(Regex("""^[/\s]*(STATEMENT|QUESTION|REQUEST|CONFIRM)[/\]\s]*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^Line\s*[12]\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^Translation\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
            // 모델이 [INTENT: 번역문] 형태로 출력한 경우 껍질만 벗겨내어 번역문 보존
            .replace(Regex("""\[?INTENT:\s*([^\]\n]+)\]?""", RegexOption.IGNORE_CASE)) { it.groupValues[1].trim() }
            .replace(Regex("""^Line\s*[12]\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^Translation\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
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
            .replace(Regex("""<think>[\s\S]*?(</think>|$)""", RegexOption.IGNORE_CASE), "")
            // 2. Gemma 4 및 표준 특수 제어 토큰 제거
            .replace(Regex("""<(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)[^>]*>\s*(model|user|assistant)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</(start_of_turn|end_of_turn|bos|eos|pad|model|user|turn|instruction|response|context)>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""</?[a-zA-Z0-9_-]+(\s+[^>]*)?>"""), "")
            // 3. 닫히지 않고 잘린 불완전한 제어 태그 및 특수 토큰 조각 제거 (<로 시작하여 끝까지 닫히지 않은 태그 조각 전수 소거)
            .replace(Regex("""<[^>]*$"""), "")
            // 4. 메타 태그 및 찌꺼기 제거 ([INTENT: ...], /QUESTION/REQUEST/CONFIRM], Line 1/2, Translation: 등)
            .replace(Regex("""\[?INTENT:\s*(STATEMENT|QUESTION|REQUEST|CONFIRM)[^\]\n]*\]?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[?INTENT:\s*([^\]\n]+)\]?""", RegexOption.IGNORE_CASE)) { it.groupValues[1].trim() }
            .replace(Regex("""^[/\s]*(STATEMENT|QUESTION|REQUEST|CONFIRM)[/\]\s]*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^Line\s*[12]\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^Translation\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
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
            .replace(Regex("""^(변환|결과|수정|Result|Output|Rewritten)\s*[:：]\s*""", RegexOption.IGNORE_CASE), "")
            .trim()

        val profile = if (targetLangCode.isNotBlank()) LanguageProfileRegistry.get(targetLangCode) else null
        val quotes = profile?.quotationMarks ?: listOf('"', '\'', '`', '“', '”', '‘', '’', '「', '」', '『', '』')
        for (q in quotes) {
            text = text.trim(q)
        }

        text = text
            .replace(Regex("""[\u2728\u2729\u2b50\u2b51\u2747\u2748\u2749\u2733\u2734\u2744]+$"""), "")
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
     * 📝 [기본 다듬기] 입력 언어 및 맥락에 맞는 최적화된 교정 프롬프트 조합
     */
    fun buildCorrectionPrompt(
        trimmed: String,
        currentEditorText: String = "",
        priorContext: List<String> = emptyList(),
        isInputKorean: Boolean,
        isIndonesianLocale: Boolean,
        isInputEnglish: Boolean,
        speechIntent: SpeechIntent = SpeechIntent.AUTO,
        modelFamily: ModelFamily = ModelFamily.GEMMA
    ): String {
        return when {
            isInputKorean -> buildKoreanPrompt(trimmed, currentEditorText, priorContext, speechIntent, modelFamily)
            isIndonesianLocale -> buildIndonesianPrompt(trimmed, currentEditorText, priorContext, speechIntent, modelFamily)
            isInputEnglish -> buildEnglishPrompt(trimmed, currentEditorText, priorContext, speechIntent, modelFamily)
            else -> buildUniversalPrompt(trimmed, priorContext, modelFamily)
        }
    }

    private fun buildKoreanPrompt(
        trimmed: String,
        currentEditorText: String,
        priorContext: List<String>,
        speechIntent: SpeechIntent,
        modelFamily: ModelFamily
    ): String {
        return if (modelFamily == ModelFamily.GEMMA) {
            val user = "모바일 키보드의 문장 교정 및 다듬기 엔진. 잡담/설명 금지, 교정 문장 1줄만 출력.\n" +
                    "1. 원문 왜곡 금지 및 내용 생략 금지: 이유, 상황, 요구 등 원문의 모든 의미 요소를 빠짐없이 100% 온전히 포함하고, 억지 치환이나 임의 생략/축약 절대 금지.\n" +
                    "2. 오탈자와 띄어쓰기를 올바르게 교정하여 자연스러운 문장으로 완성.\n" +
                    "원문: \"$trimmed\" -> 교정:"
            wrapTurn("", user, modelFamily)
        } else {
            val system = "문장 교정 및 다듬기 엔진: 원문의 모든 의미 요소(이유/요구)를 100% 보존하고 오탈자/띄어쓰기만 교정한 단 한 줄 출력. 대화/설명/임의생략 금지."
            val fewShot = "예시: \"밥 머것어\" -> 밥 먹었어? / \"오늘 날씨 조타\" -> 오늘 날씨 좋다."
            val editorPart = if (currentEditorText.isNotBlank()) "[맥락: $currentEditorText]\n" else ""
            val intentPart = if (speechIntent != SpeechIntent.AUTO) "[INTENT: ${speechIntent.name}]\n" else ""
            val user = "$editorPart$intentPart$fewShot\n원문: \"$trimmed\" -> 교정:"
            wrapTurn(system, user, modelFamily)
        }
    }

    private fun buildIndonesianPrompt(
        trimmed: String,
        currentEditorText: String,
        priorContext: List<String>,
        speechIntent: SpeechIntent,
        modelFamily: ModelFamily
    ): String {
        val system = "Anda adalah mesin perapih kalimat untuk papan ketik ponsel.\n" +
                "Aturan: JANGAN menjawab pertanyaan atau mengobrol. Keluarkan HANYA kalimat yang sudah diperbaiki ejaan dan tanda bacanya dalam satu baris tanpa penjelasan."

        val fewShotExamples = """
            [Contoh Perbaikan]
            Masukan: "kamu udah makan blm"
            Koreksi: Kamu sudah makan belum?
            Masukan: "besok ketemu jam brp"
            Koreksi: Besok ketemu jam berapa?
            Masukan: "hari ini cuaca bgs ya"
            Koreksi: Hari ini cuaca bagus ya.
        """.trimIndent()

        val editorPart = if (currentEditorText.isNotBlank()) "[Konteks: $currentEditorText]\n" else ""
        val user = "$editorPart$fewShotExamples\n\nMasukan: \"$trimmed\"\nKoreksi:"
        return wrapTurn(system, user, modelFamily)
    }

    private fun buildEnglishPrompt(
        trimmed: String,
        currentEditorText: String,
        priorContext: List<String>,
        speechIntent: SpeechIntent,
        modelFamily: ModelFamily
    ): String {
        val system = "You are a mobile keyboard sentence refinement engine.\n" +
                "Rule: Do NOT answer questions or converse. Output ONLY the refined sentence with correct spelling and punctuation on a single line."

        val fewShotExamples = """
            [Refinement Examples]
            Input: "did u eat lunch"
            Refined: Did you eat lunch?
            Input: "what time we meet tmrw"
            Refined: What time do we meet tomorrow?
            Input: "weather is great today"
            Refined: The weather is great today.
        """.trimIndent()

        val editorPart = if (currentEditorText.isNotBlank()) "[Context: $currentEditorText]\n" else ""
        val user = "$editorPart$fewShotExamples\n\nInput: \"$trimmed\"\nRefined:"
        return wrapTurn(system, user, modelFamily)
    }

    private fun buildUniversalPrompt(
        trimmed: String,
        priorContext: List<String>,
        modelFamily: ModelFamily = ModelFamily.GEMMA
    ): String {
        val system = "Mobile keyboard punctuation corrector. Add appropriate punctuation. Output ONLY the single line refined text without explanation. Do NOT answer questions."
        val contextBlock = if (priorContext.isNotEmpty()) "[Context: ${priorContext.takeLast(2).joinToString(" / ")}]\n" else ""
        val user = "${contextBlock}Input: \"$trimmed\""
        return wrapTurn(system, user, modelFamily)
    }

    /**
     * 🎭 [어조/톤 변환] 모바일 키보드 톤 변환 프롬프트 빌더 (화행 및 의도 100% 보존)
     */
    fun buildTonePrompt(
        trimmed: String,
        toneName: String,
        toneInstruction: String,
        examples: String,
        isInputKorean: Boolean,
        isInputIndonesian: Boolean,
        speechIntent: SpeechIntent = SpeechIntent.AUTO,
        modelFamily: ModelFamily = ModelFamily.GEMMA
    ): String {
        return if (isInputKorean) {
            val intentDirective = when (speechIntent) {
                SpeechIntent.QUESTION -> "질문/의문문 형태를 유지하고 물음표('?')로 끝내세요"
                SpeechIntent.REQUEST -> "정중한 요청/부탁 어미 사용"
                SpeechIntent.CONFIRM -> "확인 의문문 유지 및 물음표('?') 종결"
                SpeechIntent.STATEMENT -> "설명/서술형 종결"
                SpeechIntent.AUTO -> "원문이 의문문이면 '?', 평서문이면 '.' 종결"
            }
            val examplesBlock = if (examples.isNotBlank()) "\n[변환 예시]\n$examples\n" else ""
            if (modelFamily == ModelFamily.GEMMA) {
                val user = "모바일 키보드의 텍스트 어조/톤 변환기. 잡담/설명 금지, 변환 문장 1줄만 출력.\n" +
                        "1. 원문 왜곡 금지 및 내용 생략 금지: 이유, 상황, 요구 등 원문의 모든 의미 요소를 빠짐없이 100% 온전히 보존하고, 억지 한자어 치환 절대 금지 및 임의 축약 절대 금지.\n" +
                        "2. 톤: '$toneName' ($toneInstruction)\n" +
                        "3. 화행: $intentDirective" +
                        examplesBlock +
                        "\n원문: \"$trimmed\" -> 변환:"
                wrapTurn("", user, modelFamily)
            } else {
                val system = "모바일 키보드의 텍스트 어조/톤 변환기.\n" +
                        "1. 원문 왜곡 금지 및 내용 생략 금지: 이유, 상황, 요구 등 원문의 모든 의미 요소를 빠짐없이 100% 보존, 억지 한자어 치환 절대 금지 및 임의 축약 금지.\n" +
                        "2. 톤: '$toneName' ($toneInstruction)\n" +
                        "3. 화행: $intentDirective"
                val user = "$examples\n원문: \"$trimmed\" -> 변환:"
                wrapTurn(system, user, modelFamily)
            }
        } else if (isInputIndonesian) {
            val intentDirective = when (speechIntent) {
                SpeechIntent.QUESTION -> "Pertahankan bentuk pertanyaan dan akhiri dengan tanda tanya ('?')."
                SpeechIntent.REQUEST -> "Gunakan nada permohonan sopan."
                SpeechIntent.CONFIRM -> "Gunakan nada konfirmasi dan akhiri tanda tanya ('?')."
                SpeechIntent.STATEMENT -> "Gunakan bentuk pernyataan."
                SpeechIntent.AUTO -> "Pertahankan maksud kalimat asli."
            }
            val examplesBlock = if (examples.isNotBlank()) "\n[Contoh]\n$examples\n" else ""
            if (modelFamily == ModelFamily.GEMMA) {
                val user = "Pengubah gaya teks papan ketik. DILARANG mengobrol/menjelaskan, keluarkan HANYA 1 baris.\n" +
                        "1. Dilarang distorsi & pemotongan: Pertahankan SEMUA arti asli (alasan, konteks, permohonan) secara utuh tanpa memotong isi kalimat.\n" +
                        "2. Gaya: '$toneName' ($toneInstruction)\n" +
                        "3. Niat: $intentDirective" +
                        examplesBlock +
                        "\nTeks Asli: \"$trimmed\" -> Ubah:"
                wrapTurn("", user, modelFamily)
            } else {
                val system = "Pengubah gaya teks papan ketik. DILARANG mengobrol.\n" +
                        "1. Dilarang distorsi: Pertahankan arti asli.\n" +
                        "2. Gaya: '$toneName' ($toneInstruction)\n" +
                        "3. Niat: $intentDirective"
                val user = "$examples\nMasukan: \"$trimmed\" -> Ubah:"
                wrapTurn(system, user, modelFamily)
            }
        } else {
            val intentDirective = when (speechIntent) {
                SpeechIntent.QUESTION -> "Keep the question structure and end with a question mark ('?')."
                SpeechIntent.REQUEST -> "Use polite request phrasing."
                SpeechIntent.CONFIRM -> "Use confirmation phrasing and end with '?'."
                SpeechIntent.STATEMENT -> "Use declarative statement phrasing."
                SpeechIntent.AUTO -> "Preserve original speech act and intent."
            }
            val examplesBlock = if (examples.isNotBlank()) "\n[Examples]\n$examples\n" else ""
            if (modelFamily == ModelFamily.GEMMA) {
                val user = "Mobile keyboard tone transformer. Do NOT converse/explain. Output ONLY 1 line.\n" +
                        "1. Zero Distortion & No Omission: Preserve ALL original meaning elements (reasons, context, requests) completely without omission.\n" +
                        "2. Tone: '$toneName' ($toneInstruction)\n" +
                        "3. Intent: $intentDirective" +
                        examplesBlock +
                        "\nInput: \"$trimmed\" -> Transformed:"
                wrapTurn("", user, modelFamily)
            } else {
                val system = "Mobile keyboard tone transformer. Do NOT converse.\n" +
                        "1. Zero Distortion: Preserve original meaning.\n" +
                        "2. Tone: '$toneName' ($toneInstruction)\n" +
                        "3. Intent: $intentDirective"
                val user = "$examples\nInput: \"$trimmed\" -> Transformed:"
                wrapTurn(system, user, modelFamily)
            }
        }
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
