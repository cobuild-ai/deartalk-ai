package ai.deartalk.android.agent.language

import ai.deartalk.android.data.pref.TranslationTarget
import ai.deartalk.android.live.data.SpeechIntent
import ai.deartalk.android.stt.IntonationAnalyzer

/**
 * 🌐 사전 정의 기반 다국어 화행 및 규칙 중앙 레지스트리 (Single Source of Truth)
 * - 새로운 언어 추가 또는 특정 언어의 화행/정제 규칙 수정 시 엔진 소스코드 수정 없이 프로필만 등록/수정합니다.
 */
object LanguageProfileRegistry {

    private val profiles: Map<String, LanguageProfile> = listOf(
        // 🇰🇷 한국어 (Korean)
        LanguageProfile(
            code = "KO",
            englishName = "Korean",
            localizedName = "한국어",
            flag = "🇰🇷",
            scriptGuidelines = "자연스러운 한국어 구어체로 작성하세요.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "화행 목표: [질문/의문문] 상대방에게 묻는 자연스러운 구어체 질문 어미로 바꾸고 물음표('?')로 끝내세요.",
                    endingPunctuation = "?",
                    examples = listOf(
                        "너는 집에 있어" to "너 집에 있어?",
                        "이거 복잡한 문제야" to "이거 복잡한 문제야?",
                        "지금 출발했어" to "지금 출발했어?"
                    ),
                    endings = listOf("까", "나요", "가요", "실까요", "을까요", "ㄹ까요", "어때", "어때요", "래", "니", "냐", "는가", "인가", "던가", "려나", "맞나요"),
                    keywords = listOf("혹시", "언제", "어디", "누구", "무엇", "왜", "어떻게", "몇")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "화행 목표: [설명/평서문] 명확한 평서문 종결 어미('~야', '~다', '~습니다', '~해요')로 바꾸고 마침표('.')로 끝내세요. 의문문 구조를 사용하지 마세요.",
                    endingPunctuation = ".",
                    examples = listOf(
                        "너 집에 있어?" to "너 집에 있어.",
                        "이거 복잡한 문제야?" to "이거 복잡한 문제야.",
                        "지금 출발했니?" to "지금 출발했어."
                    ),
                    endings = listOf("다", "습니다", "입니다", "해요", "야")
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "화행 목표: [정중한 부탁] 상대방에게 정중히 요청하거나 부탁하는 표현('~해줘', '~해주세요', '~부탁드립니다')으로 바꾸세요.",
                    endingPunctuation = ".",
                    endings = listOf("주세요", "바랍니다", "바래요", "해줘", "해라", "줘", "도와줘", "알려줘", "보내줘", "확인해줘", "주실래요", "주시겠어요", "주시겠습니까"),
                    keywords = listOf("부탁", "부탁드립니다", "부탁드려요", "부탁해")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "화행 목표: [확인/재확인] 상대방에게 사실이나 의사를 확인하는 어미('~맞지?', '~그렇지?', '~죠?')로 끝내세요.",
                    endingPunctuation = "?",
                    endings = listOf("지", "맞지", "맞죠", "그렇지", "그렇죠", "거지", "거죠", "알았지", "됐지", "됐죠", "죠")
                )
            ),
            cleaningPrefixes = listOf(
                "한국어", "한국어 번역", "수정된 문장", "최종 문장", "다듬은 문장", "변환된 문장",
                "결과", "답변", "제안", "답", "문장", "Korean"
            ),
            quotationMarks = listOf('"', '\'', '`', '“', '”', '「', '」')
        ),

        // 🇺🇸 영어 (English)
        LanguageProfile(
            code = "EN",
            englishName = "English",
            localizedName = "English",
            flag = "🇺🇸",
            scriptGuidelines = "Output natural conversational English with proper grammar and punctuation.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): The speaker intends to ask a question. The translation into English MUST be formulated as a grammatically complete interrogative sentence (using proper auxiliary verbs, subject-verb inversion, and question words like 'Are you...', 'Is this...', 'Do you...', 'Can we...', etc.) and MUST end with '?'.",
                    endingPunctuation = "?",
                    examples = listOf(
                        "너는 집에 있어" to "Are you at home?",
                        "이거 복잡한 문제야" to "Is this a complicated issue?",
                        "지금 출발했어" to "Did you leave now?",
                        "시간 돼" to "Do you have time?"
                    ),
                    prefixes = listOf(
                        "are ", "is ", "do ", "does ", "did ", "what ", "how ", "why ",
                        "where ", "when ", "who ", "which ", "can ", "could ", "would ",
                        "will ", "should ", "shall ", "am ", "were ", "was ", "have you ", "has "
                    )
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT/EXPLANATION): The speaker is making a declarative statement or explanation. Strictly structure the translated output as a clear declarative sentence in English ending with '.' (NOT a question, rhetorical suggestion, or inverted auxiliary structure). In English, strictly avoid inverted auxiliary question forms (do NOT start with 'Can't we...', 'Aren't you...', 'Do you...', 'Is this...'). Use standard declarative subject-verb word order (e.g., 'We cannot...', 'You shouldn't...', 'This is...', 'I am...').",
                    endingPunctuation = ".",
                    examples = listOf(
                        "후방 카메라로 다 하면 안 돼" to "We cannot do it all with the rear camera.",
                        "너는 집에 있어" to "You are at home.",
                        "이거 복잡한 문제야" to "This is a complicated problem.",
                        "지금 출발했어" to "I have departed now."
                    )
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): The speaker is asking for something or making a polite request/order. Strictly structure the translated output as a courteous request in English (e.g., using 'Could you please...', 'Please...').",
                    endingPunctuation = ".",
                    prefixes = listOf("could you", "can you", "would you", "will you", "please ", "kindly "),
                    keywords = listOf("please")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): The speaker is seeking confirmation or double-checking (asking 'Right?'). Structure the translated output to confirm in English, ending with '?'.",
                    endingPunctuation = "?",
                    endings = listOf("right", "correct", "isn't it", "aren't you", "don't you", "right?")
                )
            ),
            cleaningPrefixes = listOf(
                "English", "English Translation", "Translation", "Translated Text",
                "Output", "Result", "Sentence", "assistant", "model"
            ),
            postProcessor = { output, intent ->
                if (intent == SpeechIntent.STATEMENT && IntonationAnalyzer.isLikelyQuestion(output, "EN")) {
                    IntonationAnalyzer.convertToDeclarativeEnglish(output)
                } else {
                    output
                }
            }
        ),

        // 🇯🇵 일본어 (Japanese)
        LanguageProfile(
            code = "JA",
            englishName = "Japanese",
            localizedName = "日本語",
            flag = "🇯🇵",
            scriptGuidelines = "Output natural conversational Japanese using proper Kanji and Kana. Do NOT add Furigana in parentheses. Do NOT output romaji.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a natural polite question in Japanese ending with '〜ですか？', '〜ますか？', or '？'.",
                    endingPunctuation = "？",
                    endings = listOf("か", "ですか", "ますか", "でしょうか")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear polite declarative sentence in Japanese ending with '〜です。', '〜ます。', or '。'.",
                    endingPunctuation = "。"
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a courteous request in Japanese using '〜をお願いします。' or '〜てください。'.",
                    endingPunctuation = "。",
                    endings = listOf("ください", "お願いします", "ちょうだい", "頼む")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as confirmation in Japanese ending with '〜ですね？' or '〜でしょうか？'.",
                    endingPunctuation = "？",
                    endings = listOf("ね", "ですよね", "確認")
                )
            ),
            cleaningPrefixes = listOf(
                "Japanese", "日本語", "日本語訳", "訳文", "翻訳", "和訳", "【訳】"
            ),
            quotationMarks = listOf('「', '」', '『', '』', '"', '\'', '`')
        ),

        // 🇨🇳 중국어 (Chinese)
        LanguageProfile(
            code = "ZH",
            englishName = "Chinese",
            localizedName = "中文",
            flag = "🇨🇳",
            scriptGuidelines = "Output natural conversational Simplified Chinese (简体中文). Do NOT output Pinyin or English notes.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a natural question in Chinese ending with question particles like '吗？', '呢？', or '？'.",
                    endingPunctuation = "？",
                    endings = listOf("吗", "嗎", "呢"),
                    prefixes = listOf("为什么", "怎么", "怎麼", "谁", "誰", "哪")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear declarative sentence in Chinese ending with '。'.",
                    endingPunctuation = "。"
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a polite request in Chinese using '请...' or '麻烦您...'.",
                    endingPunctuation = "。",
                    prefixes = listOf("请", "請", "麻烦", "麻煩"),
                    keywords = listOf("帮忙", "幫忙", "协助", "協助")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as seeking confirmation ending with '对吧？', '是不是？', or '确认一下...'.",
                    endingPunctuation = "？",
                    endings = listOf("吧", "对吧", "對吧", "是不是")
                )
            ),
            cleaningPrefixes = listOf(
                "Chinese", "中文", "翻译", "译文", "结果"
            ),
            quotationMarks = listOf('“', '”', '‘', '’', '"', '\'', '`')
        ),

        // 🇮🇩 인도네시아어 (Indonesian)
        LanguageProfile(
            code = "ID",
            englishName = "Indonesian",
            localizedName = "Bahasa Indonesia",
            flag = "🇮🇩",
            scriptGuidelines = "Output natural conversational Bahasa Indonesia with standard spelling (EYD).",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Ubah struktur kalimat menjadi pertanyaan yang alami dalam bahasa Indonesia dan akhiri dengan '?'.",
                    endingPunctuation = "?",
                    prefixes = listOf("apakah", "bagaimana", "kenapa", "mengapa", "kapan", "siapa", "di mana", "dimana", "berapa", "mana")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Ubah menjadi kalimat berita/pernyataan yang jelas dan akhiri dengan '.' (hindari kalimat tanya).",
                    endingPunctuation = "."
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Gunakan kata permintaan sopan seperti 'tolong' atau 'mohon'.",
                    endingPunctuation = ".",
                    prefixes = listOf("bisa ", "bisakah ", "tolong ", "mohon "),
                    keywords = listOf("tolong", "mohon", "silakan")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Akhiri dengan konfirmasi seperti 'kan?' atau 'benar?'.",
                    endingPunctuation = "?",
                    endings = listOf("kan", "ya", "benar")
                )
            ),
            cleaningPrefixes = listOf(
                "Indonesian", "Bahasa Indonesia", "Terjemahan", "Hasil"
            )
        ),

        // 🇪🇸 스페인어 (Spanish)
        LanguageProfile(
            code = "ES",
            englishName = "Spanish",
            localizedName = "Español",
            flag = "🇪🇸",
            scriptGuidelines = "Output natural conversational Spanish with correct punctuation (including inverted marks ¿ if appropriate).",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a clear interrogative sentence in Spanish ending with '?'.",
                    endingPunctuation = "?",
                    prefixes = listOf("cómo", "qué", "dónde", "cuándo", "quién", "por qué", "¿")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear declarative statement in Spanish ending with '.'.",
                    endingPunctuation = "."
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a courteous request in Spanish (e.g., 'Por favor...', '¿Podrías...?').",
                    endingPunctuation = ".",
                    prefixes = listOf("podrías", "podría", "por favor"),
                    keywords = listOf("por favor")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as seeking confirmation in Spanish (e.g., '..., ¿verdad?', '¿cierto?').",
                    endingPunctuation = "?",
                    endings = listOf("verdad", "cierto")
                )
            ),
            cleaningPrefixes = listOf(
                "Spanish", "Español", "Traducción", "Resultado"
            )
        ),

        // 🇫🇷 프랑스어 (French)
        LanguageProfile(
            code = "FR",
            englishName = "French",
            localizedName = "Français",
            flag = "🇫🇷",
            scriptGuidelines = "Output natural conversational French.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a natural question in French ending with '?'.",
                    endingPunctuation = "?",
                    prefixes = listOf("est-ce que", "comment", "pourquoi", "où", "quand", "qui")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear declarative statement in French ending with '.'.",
                    endingPunctuation = "."
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a polite request in French (e.g., 'S'il vous plaît...').",
                    endingPunctuation = ".",
                    keywords = listOf("s'il vous plaît", "s'il te plaît", "svp")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as seeking confirmation in French (e.g., '..., n'est-ce pas ?').",
                    endingPunctuation = "?",
                    endings = listOf("n'est-ce pas")
                )
            ),
            cleaningPrefixes = listOf(
                "French", "Français", "Traduction", "Résultat"
            )
        ),

        // 🇩🇪 독일어 (German)
        LanguageProfile(
            code = "DE",
            englishName = "German",
            localizedName = "Deutsch",
            flag = "🇩🇪",
            scriptGuidelines = "Output natural conversational German with proper capitalization of nouns.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a question in German ending with '?'.",
                    endingPunctuation = "?",
                    prefixes = listOf("warum", "wie", "wo", "wann", "wer", "was", "können sie", "kannst du")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear declarative sentence in German ending with '.'.",
                    endingPunctuation = "."
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a polite request in German (e.g., 'Bitte...').",
                    endingPunctuation = ".",
                    keywords = listOf("bitte")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as seeking confirmation in German (e.g., '..., oder?', '..., nicht wahr?').",
                    endingPunctuation = "?",
                    endings = listOf("oder", "nicht wahr")
                )
            ),
            cleaningPrefixes = listOf(
                "German", "Deutsch", "Übersetzung", "Ergebnis"
            )
        ),

        // 🇻🇳 베트남어 (Vietnamese)
        LanguageProfile(
            code = "VI",
            englishName = "Vietnamese",
            localizedName = "Tiếng Việt",
            flag = "🇻🇳",
            scriptGuidelines = "Output natural conversational Vietnamese with proper tonal marks.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a question in Vietnamese ending with 'phải không?', 'sao?', or '?'.",
                    endingPunctuation = "?",
                    endings = listOf("phải không", "sao", "à", "chưa")
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear declarative sentence in Vietnamese ending with '.'.",
                    endingPunctuation = "."
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a polite request in Vietnamese (e.g., 'Làm ơn...', 'Xin vui lòng...').",
                    endingPunctuation = ".",
                    prefixes = listOf("làm ơn", "xin vui lòng")
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as seeking confirmation in Vietnamese (e.g., '...đúng không?').",
                    endingPunctuation = "?",
                    endings = listOf("đúng không", "đúng chứ")
                )
            ),
            cleaningPrefixes = listOf(
                "Vietnamese", "Tiếng Việt", "Bản dịch", "Kết quả"
            )
        )
    ).associateBy { it.code.uppercase() }

    /**
     * 🌐 언어 코드로 불변 프로필 조회 (대소문자 무관, 미등록 시 범용 DEFAULT 반환)
     */
    fun get(code: String): LanguageProfile {
        val normalized = code.trim().uppercase()
        return profiles[normalized] ?: createDefaultProfile(normalized)
    }

    /**
     * 🎯 TranslationTarget 객체로부터 공식 ISO 언어 코드 추출
     */
    fun resolveCode(target: TranslationTarget): String {
        if (target.code.isNotBlank() && target.code != "DEFAULT") {
            return target.code.uppercase()
        }
        val idOrName = (target.id + " " + target.name).lowercase()
        return when {
            idOrName.contains("en") || idOrName.contains("영어") || idOrName.contains("english") -> "EN"
            idOrName.contains("ja") || idOrName.contains("일본어") || idOrName.contains("japanese") -> "JA"
            idOrName.contains("zh") || idOrName.contains("중국어") || idOrName.contains("chinese") -> "ZH"
            idOrName.contains("id") || idOrName.contains("인도네시아") || idOrName.contains("indonesian") -> "ID"
            idOrName.contains("es") || idOrName.contains("스페인") || idOrName.contains("spanish") -> "ES"
            idOrName.contains("fr") || idOrName.contains("프랑스") || idOrName.contains("french") -> "FR"
            idOrName.contains("de") || idOrName.contains("독일") || idOrName.contains("german") -> "DE"
            idOrName.contains("vi") || idOrName.contains("베트남") || idOrName.contains("vietnamese") -> "VI"
            else -> target.id.replace("trans_", "").uppercase()
        }
    }

    /**
     * 🛡️ 미등록 ISO 코드를 위한 범용 폴백 프로필 생성
     */
    private fun createDefaultProfile(code: String): LanguageProfile {
        val langName = when (code) {
            "TH" -> "Thai"
            "TL", "FIL" -> "Filipino"
            "MS" -> "Malay"
            "RU" -> "Russian"
            "IT" -> "Italian"
            "PT" -> "Portuguese"
            "AR" -> "Arabic"
            "HI" -> "Hindi"
            else -> code
        }
        return LanguageProfile(
            code = code,
            englishName = langName,
            localizedName = langName,
            flag = "🌐",
            scriptGuidelines = "Output natural conversational $langName.",
            intentRules = mapOf(
                SpeechIntent.QUESTION to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (QUESTION): Formulate as a natural question in $langName ending with '?'.",
                    endingPunctuation = "?"
                ),
                SpeechIntent.STATEMENT to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (STATEMENT): Formulate as a clear declarative sentence in $langName ending with '.'.",
                    endingPunctuation = "."
                ),
                SpeechIntent.REQUEST to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (POLITE REQUEST): Formulate as a polite request in $langName.",
                    endingPunctuation = "."
                ),
                SpeechIntent.CONFIRM to SpeechIntentRule(
                    directive = "4. SPEECH INTENT (CONFIRMATION): Formulate as seeking confirmation in $langName ending with '?'.",
                    endingPunctuation = "?"
                )
            ),
            cleaningPrefixes = listOf(langName, "Translation", "Output", "Result")
        )
    }

    /**
     * 🧹 등록된 모든 언어의 접두어를 포괄하는 통합 정규식 패턴 생성
     */
    val allCleaningPrefixesRegex: Regex by lazy {
        val prefixes = (profiles.values.flatMap { it.cleaningPrefixes + listOf(it.englishName, it.localizedName) }.distinct() +
                listOf(
                    "Output", "Result", "Sentence", "model", "assistant", "AI", "Translation", "Translated Text",
                    "영어", "일본어", "중국어", "스페인어", "프랑스어", "독일어", "인도네시아어", "베트남어",
                    "최종 문장", "수정된 문장", "다듬은 문장", "변환된 문장", "결과", "답변", "제안", "답", "문장",
                    "교정", "교정문", "변환", "수정", "Refined", "Corrected", "Correction", "Koreksi", "Ubah"
                )).distinct()
        val pattern = "^(${prefixes.joinToString("|") { Regex.escape(it) }})\\s*[:：]\\s*"
        Regex(pattern, RegexOption.IGNORE_CASE)
    }
}
