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
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return "${sdf.format(Date())} - Live Voice"
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
    val createdAt: Long = System.currentTimeMillis()
)
