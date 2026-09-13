package ai.deartalk.android.agent.language

import ai.deartalk.android.data.pref.TranslationTarget
import ai.deartalk.android.live.data.SpeechIntent
import org.junit.Assert.*
import org.junit.Test

class LanguageProfileTest {

    @Test
    fun `9대 주요 언어 프로필이 모두 유효하게 등록되어 있어야 한다`() {
        val expectedCodes = listOf("KO", "EN", "JA", "ZH", "ID", "ES", "FR", "DE", "VI")
        for (code in expectedCodes) {
            val profile = LanguageProfileRegistry.get(code)
            assertEquals(code, profile.code)
            assertTrue("English name should not be blank for $code", profile.englishName.isNotBlank())
            assertTrue("Flag should not be blank for $code", profile.flag.isNotBlank())
            assertTrue("Cleaning prefixes should not be empty for $code", profile.cleaningPrefixes.isNotEmpty())

            // 4대 화행 규칙 검증
            val intents = listOf(SpeechIntent.QUESTION, SpeechIntent.STATEMENT, SpeechIntent.REQUEST, SpeechIntent.CONFIRM)
            for (intent in intents) {
                val rule = profile.getIntentRule(intent)
                assertNotNull("Intent $intent must have a defined rule in $code", rule)
                assertTrue("Directive for $intent in $code should not be blank", rule!!.directive.isNotBlank())
            }
        }
    }

    @Test
    fun `미등록 언어 코드 조회 시 범용 DEFAULT 프로필이 안전하게 반환되어야 한다`() {
        val unknown = LanguageProfileRegistry.get("XYZ")
        assertEquals("XYZ", unknown.code)
        assertEquals("XYZ", unknown.englishName)
        assertEquals("🌐", unknown.flag)
        assertNotNull(unknown.getIntentRule(SpeechIntent.QUESTION))
        assertNotNull(unknown.getIntentRule(SpeechIntent.STATEMENT))
    }

    @Test
    fun `TranslationTarget으로부터 ISO 코드를 정확히 추출해야 한다`() {
        val targetWithCode = TranslationTarget(id = "custom_1", code = "JA", name = "일본어", targetLanguage = "일본어(日本語)")
        assertEquals("JA", LanguageProfileRegistry.resolveCode(targetWithCode))

        val targetLegacy = TranslationTarget(id = "trans_zh", code = "", name = "중국어", targetLanguage = "중국어(中文)")
        assertEquals("ZH", LanguageProfileRegistry.resolveCode(targetLegacy))

        val targetDefault = TranslationTarget(id = "trans_id", code = "DEFAULT", name = "인도네시아어", targetLanguage = "인도네시아어")
        assertEquals("ID", LanguageProfileRegistry.resolveCode(targetDefault))
    }

    @Test
    fun `언어별 불필요 접두어가 통합 정규식에 의해 완벽히 제거되어야 한다`() {
        val testCases = listOf(
            "Japanese: こんにちは" to "こんにちは",
            "日本語訳: こんにちは" to "こんにちは",
            "【訳】: こんにちは" to "こんにちは",
            "English: Hello there" to "Hello there",
            "Translation: How are you?" to "How are you?",
            "중국어: 你好" to "你好",
            "한국어: 반갑습니다" to "반갑습니다",
            "Bahasa Indonesia: Selamat pagi" to "Selamat pagi",
            "Traducción: Hola" to "Hola",
            "Output: Perfect result" to "Perfect result"
        )

        for ((input, expected) in testCases) {
            val cleaned = input.replace(LanguageProfileRegistry.allCleaningPrefixesRegex, "").trim()
            assertEquals(expected, cleaned)
        }
    }

    @Test
    fun `영어 평서문 화행 시 의문문 역위 구조가 평서문으로 정상 후처리되어야 한다`() {
        val enProfile = LanguageProfileRegistry.get("EN")
        assertNotNull(enProfile.postProcessor)

        val questionLikeOutput = "Are you at home?"
        val statementProcessed = enProfile.applyPostProcessing(questionLikeOutput, SpeechIntent.STATEMENT)
        // STATEMENT 화행이므로 의문문이 평서문("You are at home.")으로 변환되어야 함
        assertEquals("You are at home.", statementProcessed)

        // QUESTION 화행일 때는 변환 없이 그대로 유지
        val questionKept = enProfile.applyPostProcessing(questionLikeOutput, SpeechIntent.QUESTION)
        assertEquals("Are you at home?", questionKept)
    }

    @Test
    fun `일본어 프로필은 괄호 및 일본어 특수 따옴표를 정제 규칙에 포함해야 한다`() {
        val jaProfile = LanguageProfileRegistry.get("JA")
        assertTrue(jaProfile.quotationMarks.contains('「'))
        assertTrue(jaProfile.quotationMarks.contains('」'))
        assertTrue(jaProfile.quotationMarks.contains('『'))
        assertTrue(jaProfile.quotationMarks.contains('』'))
    }
}
