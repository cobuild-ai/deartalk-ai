package ai.deartalk.android.live.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 🎙️ 발화 주체
 */
enum class LiveSender {
    ME,       // 내가 말함 (내 언어 -> 번역 -> 상대방 언어 TTS)
    PARTNER   // 상대방이 말함 (상대방 언어 -> 번역 -> 내가 텍스트로 읽음)
}

/**
 * 📁 1:1 대화 세션
 */
data class LiveSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = createDefaultTitle(),
    val createdAt: Long = System.currentTimeMillis(),
    val myLang: String = "KO",
    val partnerLang: String = "EN"
) {
    companion object {
        fun createDefaultTitle(): String {
            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            return "${sdf.format(Date())}-DearTalk-Live"
        }
    }
}

/**
 * 💬 대화 메시지 엔티티
 */
data class LiveMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sender: LiveSender,
    val rawText: String,
    val refinedText: String,
    val sourceLang: String,
    val targetLang: String,
    val tone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val originalRawText: String = rawText,
    val isDraft: Boolean = false,
    val draftText: String? = null
)

/**
 * 🎯 실시간 발화 의도 (Speech Pragmatics / 화행)
 * - 사용자가 탭하여 1회성(One-shot)으로 지정할 수 있는 발화 유형
 * - 발화 및 번역이 완료되면 자동으로 AUTO(스마트)로 복귀
 */
enum class SpeechIntent(val id: String) {
    AUTO("auto"),           // ✨ 스마트 (AI 자율 맥락 판단)
    QUESTION("question"),   // ❓ 질문? (의문문 강제)
    STATEMENT("statement"), // 💬 설명. (평서문/답변 강제)
    REQUEST("request"),     // 🙏 부탁! (정중한 요청/부탁 강제)
    CONFIRM("confirm")      // 🔁 확인? (되묻기/동의 구하기 강제)
}

fun SpeechIntent.getLabel(langCode: String? = null): String {
    val code = langCode?.uppercase() ?: ""
    return when (this) {
        SpeechIntent.AUTO -> when {
            code.startsWith("KO") -> "✨ 스마트"
            code.startsWith("ID") -> "✨ Cerdas"
            code.startsWith("JA") -> "✨ スマート"
            code.startsWith("ZH") -> "✨ 智能"
            code.startsWith("ES") -> "✨ Inteligente"
            code.startsWith("FR") -> "✨ Intelligent"
            code.startsWith("DE") -> "✨ Intelligent"
            code.startsWith("VI") -> "✨ Thông minh"
            code.startsWith("TH") -> "✨ ฉลาด"
            code.isNotBlank() -> "✨ Smart"
            else -> ai.deartalk.android.data.pref.UiStrings.liveIntentAuto
        }
        SpeechIntent.QUESTION -> when {
            code.startsWith("KO") -> "❓ 질문?"
            code.startsWith("ID") -> "❓ Tanya?"
            code.startsWith("JA") -> "❓ 質問？"
            code.startsWith("ZH") -> "❓ 提问？"
            code.startsWith("ES") -> "❓ ¿Pregunta?"
            code.startsWith("FR") -> "❓ Question ?"
            code.startsWith("DE") -> "❓ Frage?"
            code.startsWith("VI") -> "❓ Câu hỏi?"
            code.startsWith("TH") -> "❓ คำถาม?"
            code.isNotBlank() -> "❓ Question"
            else -> ai.deartalk.android.data.pref.UiStrings.liveIntentQuestion
        }
        SpeechIntent.STATEMENT -> when {
            code.startsWith("KO") -> "💬 설명."
            code.startsWith("ID") -> "💬 Jawaban."
            code.startsWith("JA") -> "💬 説明・回答"
            code.startsWith("ZH") -> "💬 陈述・回答"
            code.startsWith("ES") -> "💬 Declaración."
            code.startsWith("FR") -> "💬 Déclaration."
            code.startsWith("DE") -> "💬 Aussage."
            code.startsWith("VI") -> "💬 Trả lời."
            code.startsWith("TH") -> "💬 คำตอบ."
            code.isNotBlank() -> "💬 Statement"
            else -> ai.deartalk.android.data.pref.UiStrings.liveIntentStatement
        }
        SpeechIntent.REQUEST -> when {
            code.startsWith("KO") -> "🙏 부탁!"
            code.startsWith("ID") -> "🙏 Mohon!"
            code.startsWith("JA") -> "🙏 お願い！"
            code.startsWith("ZH") -> "🙏 拜托！"
            code.startsWith("ES") -> "🙏 ¡Petición!"
            code.startsWith("FR") -> "🙏 Demande !"
            code.startsWith("DE") -> "🙏 Bitte!"
            code.startsWith("VI") -> "🙏 Yêu cầu!"
            code.startsWith("TH") -> "🙏 ร้องขอ!"
            code.isNotBlank() -> "🙏 Request"
            else -> ai.deartalk.android.data.pref.UiStrings.liveIntentRequest
        }
        SpeechIntent.CONFIRM -> when {
            code.startsWith("KO") -> "🔁 확인?"
            code.startsWith("ID") -> "🔁 Benarkah?"
            code.startsWith("JA") -> "🔁 確認？"
            code.startsWith("ZH") -> "🔁 确认？"
            code.startsWith("ES") -> "🔁 ¿Confirmar?"
            code.startsWith("FR") -> "🔁 Confirmer ?"
            code.startsWith("DE") -> "🔁 Bestätigen?"
            code.startsWith("VI") -> "🔁 Xác nhận?"
            code.startsWith("TH") -> "🔁 ยืนยัน?"
            code.isNotBlank() -> "🔁 Confirm"
            else -> ai.deartalk.android.data.pref.UiStrings.liveIntentConfirm
        }
    }
}

/**
 * 🌐 1:1 대면 대칭 모드(180° Face-to-face) 및 실시간 라이브 UI 다국어 도우미
 */
object LiveUiHelper {
    fun getSpeakTitle(langCode: String): String {
        val code = langCode.uppercase()
        return when {
            code.startsWith("KO") -> "🗣️ 말하기"
            code.startsWith("ID") -> "🗣️ Bicara"
            code.startsWith("JA") -> "🗣️ 話す"
            code.startsWith("ZH") -> "🗣️ 说话"
            code.startsWith("ES") -> "🗣️ Hablar"
            code.startsWith("FR") -> "🗣️ Parler"
            code.startsWith("DE") -> "🗣️ Sprechen"
            code.startsWith("VI") -> "🗣️ Nói"
            code.startsWith("TH") -> "🗣️ พูด"
            else -> "🗣️ Speak"
        }
    }

    fun getSpeakSubtitle(langCode: String, langLabel: String = ""): String {
        val code = langCode.uppercase()
        val label = if (langLabel.isNotBlank()) " ($langLabel)" else ""
        return when {
            code.startsWith("KO") -> "탭하여 말씀하세요$label"
            code.startsWith("ID") -> "Ketuk untuk bicara$label"
            code.startsWith("JA") -> "タップして話す$label"
            code.startsWith("ZH") -> "点击开始说话$label"
            code.startsWith("ES") -> "Toca para hablar$label"
            code.startsWith("FR") -> "Appuyez pour parler$label"
            code.startsWith("DE") -> "Tippen zum Sprechen$label"
            code.startsWith("VI") -> "Chạm để nói$label"
            code.startsWith("TH") -> "แตะเพื่อพูด$label"
            else -> "Tap to speak$label"
        }
    }

    fun getFinishTitle(langCode: String): String {
        val code = langCode.uppercase()
        return when {
            code.startsWith("KO") -> "🛑 완료 (전송)"
            code.startsWith("ID") -> "🛑 Selesai (Kirim)"
            code.startsWith("JA") -> "🛑 完了 (送信)"
            code.startsWith("ZH") -> "🛑 完成 (发送)"
            code.startsWith("ES") -> "🛑 Listo (Enviar)"
            code.startsWith("FR") -> "🛑 Terminé (Envoyer)"
            code.startsWith("DE") -> "🛑 Fertig (Senden)"
            code.startsWith("VI") -> "🛑 Xong (Gửi)"
            code.startsWith("TH") -> "🛑 เสร็จสิ้น (ส่ง)"
            else -> "🛑 Finish (Send)"
        }
    }

    fun getFinishSubtitle(langCode: String): String {
        val code = langCode.uppercase()
        return when {
            code.startsWith("KO") -> "말씀이 끝나면 누르세요"
            code.startsWith("ID") -> "Ketuk jika selesai berbicara"
            code.startsWith("JA") -> "話し終わったらタップ"
            code.startsWith("ZH") -> "说完请点击发送"
            code.startsWith("ES") -> "Toca al terminar de hablar"
            code.startsWith("FR") -> "Appuyez une fois terminé"
            code.startsWith("DE") -> "Tippen wenn fertig"
            code.startsWith("VI") -> "Chạm khi nói xong"
            code.startsWith("TH") -> "แตะเมื่อพูดเสร็จ"
            else -> "Tap when finished speaking"
        }
    }

    fun getProcessingTitle(langCode: String): String {
        val code = langCode.uppercase()
        return when {
            code.startsWith("KO") -> "⏳ 번역/보정 중"
            code.startsWith("ID") -> "⏳ Menerjemahkan"
            code.startsWith("JA") -> "⏳ 翻訳・処理中"
            code.startsWith("ZH") -> "⏳ 正在翻译/润色"
            code.startsWith("ES") -> "⏳ Procesando"
            code.startsWith("FR") -> "⏳ Traduction en cours"
            code.startsWith("DE") -> "⏳ Übersetzung läuft"
            code.startsWith("VI") -> "⏳ Đang dịch"
            code.startsWith("TH") -> "⏳ กำลังแปล"
            else -> "⏳ Processing"
        }
    }

    fun getProcessingSubtitle(langCode: String): String {
        val code = langCode.uppercase()
        return when {
            code.startsWith("KO") -> "잠시만 기다려주세요"
            code.startsWith("ID") -> "Mohon tunggu sebentar"
            code.startsWith("JA") -> "少々お待ちください"
            code.startsWith("ZH") -> "请稍候"
            code.startsWith("ES") -> "Por favor espere un momento"
            code.startsWith("FR") -> "Veuillez patienter"
            code.startsWith("DE") -> "Bitte einen Moment warten"
            code.startsWith("VI") -> "Vui lòng chờ giây lát"
            code.startsWith("TH") -> "โปรดรอสักครู่"
            else -> "Please wait a moment"
        }
    }
}

/**
 * ✍️ 발화 의도(SpeechIntent) 및 번역 결과에 따른 원문 문장부호(? / .) 스마트 보정 (Zero Hardcoding)
 * - 하드코딩된 단어/어미 목록을 일체 사용하지 않고, 온디바이스 SLM의 문맥 분석 결과(translationHasQuestion)를 기준으로 동기화합니다.
 * - 사용자가 ❓ 질문? 인텐트를 선택했더라도 실수로 눌렀을 가능성을 고려하여:
 *   1) 문장 맥락상 의문문이 필요하지 않은 명백한 평서문(translationHasQuestion == false): '?'를 붙이지 않음.
 *   2) 모호한 문맥(SLM이 질문으로 해석)이거나 명시적 질문(translationHasQuestion == true): 번역과 내 카드 모두 '?' 부착.
 * - 이미 종결 문장부호(? ! .)로 끝나는 경우 기존 부호 100% 원형 보존.
 */
fun formatSourcePunctuation(
    text: String,
    intent: SpeechIntent,
    translationHasQuestion: Boolean = false
): String {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return trimmed

    // 이미 종결 문장부호(? ! .)로 끝나는 경우 기존 부호 보존
    if (trimmed.endsWith("?") || trimmed.endsWith("!") || trimmed.endsWith(".")) {
        return trimmed
    }

    return if (translationHasQuestion) {
        "$trimmed?"
    } else if (intent == SpeechIntent.STATEMENT) {
        "$trimmed."
    } else {
        trimmed
    }
}



