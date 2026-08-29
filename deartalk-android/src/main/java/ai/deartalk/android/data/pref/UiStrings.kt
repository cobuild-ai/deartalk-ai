package ai.deartalk.android.data.pref

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * 중앙집중식 다국어 UI 문자열 관리 오브젝트
 * - 한국어(ko), 인도네시아어(id/in), 영어(default/en) 완벽 지원
 * - Compose Reactive State를 기반으로 하여 언어 변경 시 즉각 전체 UI 리컴포지션 트리거
 */
object UiStrings {
    var overrideLocaleState by mutableStateOf<Locale?>(null)

    fun setLocale(locale: Locale) {
        overrideLocaleState = locale
    }

    val currentLocale: Locale
        get() = overrideLocaleState ?: Locale.getDefault()

    val isKo: Boolean
        get() = currentLocale.language == "ko"

    val isId: Boolean
        get() {
            val lang = currentLocale.language.lowercase()
            return lang == "id" || lang == "in"
        }

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 마이크 버튼 상태
    // ═══════════════════════════════════════════════════
    val micPreparing get() = if (isKo) "⏳ 마이크 준비 중..." else if (isId) "⏳ Menyiapkan mikrofon..." else "⏳ Preparing mic..."
    val micListening get() = if (isKo) "🔴 듣고 있어요 (말씀이 끝나면 터치)" else if (isId) "🔴 Mendengarkan (ketuk saat selesai)" else "🔴 Listening (tap when finished)"
    val micProcessingAi get() = if (isKo) "🔒 AI가 문장을 다듬는 중..." else if (isId) "🔒 AI sedang merapikan kalimat..." else "🔒 Polishing sentence with AI..."
    val micIdle get() = if (isKo) "🎙️ AI 음성 입력" else if (isId) "🎙️ Masukan Suara AI" else "🎙️ AI Voice Input"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 키보드/설정 버튼
    // ═══════════════════════════════════════════════════
    val keyboard get() = if (isKo) "자판" else if (isId) "Papan Ketik" else "Keys"
    val keyboardContentDesc get() = if (isKo) "일반 자판으로 전환" else if (isId) "Beralih ke papan ketik biasa" else "Switch to standard keyboard"
    val settingsContentDesc get() = if (isKo) "DearTalk AI 설정 및 가이드" else if (isId) "Pengaturan & Panduan DearTalk AI" else "DearTalk AI Settings & Guide"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 스마트 DIFF 캔버스
    // ═══════════════════════════════════════════════════
    val micConnecting get() = if (isKo) "⏳ 마이크 연결 중..." else if (isId) "⏳ Menghubungkan mikrofon..." else "⏳ Connecting mic..."
    val micConnectingDesc get() = if (isKo) "음성 인식 준비 중입니다. 잠시만 기다려 주세요." else if (isId) "Menyiapkan pengenalan suara. Mohon tunggu sebentar." else "Getting ready to listen. Please wait a moment."
    val listeningLabel get() = if (isKo) "🎙️ 실시간으로 듣고 있어요..." else if (isId) "🎙️ Mendengarkan suara Anda..." else "🎙️ Listening to your voice..."
    val speakNowHint get() = if (isKo) "편하게 말씀하시면 AI가 문맥에 맞게 다듬어 드려요." else if (isId) "Bicaralah dengan santai, AI akan merapikan kalimat Anda." else "Speak naturally, and AI will polish your sentence."
    val sttRaw get() = if (isKo) "🎤 내가 말한 내용" else if (isId) "🎤 Yang Anda Katakan" else "🎤 What You Said"
    val aiRefine get() = if (isKo) "✨ AI가 다듬은 문장" else if (isId) "✨ Dirapikan oleh AI" else "✨ AI Polished"
    val aiRefiningContext get() = if (isKo) "🔒 AI가 문맥을 분석하여 다듬는 중..." else if (isId) "🔒 AI sedang menganalisis konteks..." else "🔒 Refining context with on-device AI..."
    val canvasPlaceholder get() = if (isKo) {
        "🎙️ 상단 마이크를 누르고 말씀하시면 [내가 말한 내용]과 [AI가 다듬은 문장]이 여기에 표시됩니다."
    } else if (isId) {
        "🎙️ Ketuk mikrofon di atas untuk berbicara. [Yang Anda Katakan] dan [Dirapikan AI] akan muncul di sini."
    } else {
        "🎙️ Tap the mic above to speak. [What You Said] and [AI Polished] will appear here."
    }

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 입력/취소 버튼
    // ═══════════════════════════════════════════════════
    val apply get() = if (isKo) "입력" else if (isId) "Terapkan" else "Apply"
    val applyContentDesc get() = if (isKo) "메시지 창에 입력" else if (isId) "Masukkan ke pesan" else "Insert into message"
    val cancel get() = if (isKo) "취소" else if (isId) "Batal" else "Cancel"
    val cancelContentDesc get() = if (isKo) "입력 취소" else if (isId) "Batal" else "Cancel"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 번역 셀렉트박스
    // ═══════════════════════════════════════════════════
    fun translationLabel(name: String) = if (isKo) "$name 번역" else if (isId) "Terjemahkan ke $name" else "Translate to $name"
    val selectTranslationTarget get() = if (isKo) "🌐 번역할 언어 선택" else if (isId) "🌐 Pilih Bahasa Tujuan" else "🌐 Select Target Language"

    // ═══════════════════════════════════════════════════
    // DearTalkScreen.kt — 유틸리티 바
    // ═══════════════════════════════════════════════════
    val deleteCharContentDesc get() = if (isKo) "한 글자 지우기" else if (isId) "Hapus satu karakter" else "Delete character"
    val deleteSentence get() = if (isKo) "문장삭제" else if (isId) "Hapus Kalimat" else "Del Sent"
    val deleteSentenceContentDesc get() = if (isKo) "방금 입력한 문장 전체 삭제" else if (isId) "Hapus seluruh kalimat sebelumnya" else "Delete current sentence"
    val space get() = if (isKo) "스페이스" else if (isId) "Spasi" else "Space"
    val enterContentDesc get() = if (isKo) "줄바꿈 또는 전송" else if (isId) "Kirim atau baris baru" else "Enter or send"

    // ═══════════════════════════════════════════════════
    // StandardKeyboardView.kt
    // ═══════════════════════════════════════════════════
    val standardKeyboardMode get() = if (isKo) "⌨️ 일반 키보드" else if (isId) "⌨️ Papan Ketik Biasa" else "⌨️ Standard Keyboard"
    val aiVoiceMode get() = if (isKo) "✨ AI 음성 모드" else if (isId) "✨ Mode Suara AI" else "✨ AI Voice Mode"
    val korEngToggle get() = if (isKo) "한/영" else "KO/EN"

    // ═══════════════════════════════════════════════════
    // CustomToneManager.kt — 톤앤매너 이름
    // ═══════════════════════════════════════════════════
    val toneRefine get() = if (isKo) "기본다듬기" else if (isId) "Rapikan" else "Refine"
    val tonePolite get() = if (isKo) "공손하게" else if (isId) "Sopan" else "Polite"
    val toneCasual get() = if (isKo) "친근하게" else if (isId) "Santai" else "Casual"
    val toneBusiness get() = if (isKo) "비즈니스" else if (isId) "Formal" else "Business"
    val toneFunny get() = if (isKo) "재미있게" else if (isId) "Lucu" else "Humorous"
    val toneCheeky get() = if (isKo) "당당하게" else if (isId) "Percaya Diri" else "Cheeky"

    // ═══════════════════════════════════════════════════
    // CustomToneManager.kt — 언어 이름
    // ═══════════════════════════════════════════════════
    val langEnglish get() = if (isKo) "영어" else if (isId) "Bahasa Inggris" else "English"
    val langIndonesian get() = if (isKo) "인도네시아어" else if (isId) "Bahasa Indonesia" else "Indonesian"
    val langJapanese get() = if (isKo) "일본어" else if (isId) "Bahasa Jepang" else "Japanese"
    val langChinese get() = if (isKo) "중국어" else if (isId) "Bahasa Mandarin" else "Chinese"
    val langSpanish get() = if (isKo) "스페인어" else if (isId) "Bahasa Spanyol" else "Spanish"
    val langFrench get() = if (isKo) "프랑스어" else if (isId) "Bahasa Prancis" else "French"
    val langGerman get() = if (isKo) "독일어" else if (isId) "Bahasa Jerman" else "German"
    val langVietnamese get() = if (isKo) "베트남어" else if (isId) "Bahasa Vietnam" else "Vietnamese"

    val defaultAi get() = if (isKo) "기본 AI" else if (isId) "AI Standar" else "Default AI"

    // ═══════════════════════════════════════════════════
    // DearTalkIME.kt — 상태 메시지
    // ═══════════════════════════════════════════════════
    val aiProcessing get() = if (isKo) "🔒 AI가 문장을 다듬는 중..." else if (isId) "🔒 AI sedang merapikan kalimat..." else "🔒 Polishing sentence with AI..."
    val errorOccurred get() = if (isKo) "준비 완료" else if (isId) "Siap" else "Ready"
    val aiTextComplete get() = if (isKo) "✨ 문장이 완성되었습니다 ([입력]을 눌러 전송)" else if (isId) "✨ Kalimat siap dikirim (ketuk [Terapkan])" else "✨ Ready to send (tap [Apply])"
    val sttComplete get() = if (isKo) "🎤 음성 인식 완료 ([입력]을 눌러 전송)" else if (isId) "🎤 Suara dikenali (ketuk [Terapkan])" else "🎤 Speech recognized (tap [Apply])"
    val noTextToTransform get() = if (isKo) "⚠️ 다듬을 내용이 없습니다." else if (isId) "⚠️ Tidak ada teks untuk dirapikan" else "⚠️ No text provided"
    fun modeProcessing(icon: String, name: String) = if (isKo) "🔒 $icon $name 적용 중..." else if (isId) "🔒 Menerapkan $name..." else "🔒 Applying $name..."
    fun modeComplete(name: String) = if (isKo) "✨ $name 적용 완료 ([입력] 터치)" else if (isId) "✨ $name siap (ketuk [Terapkan])" else "✨ $name ready (tap [Apply])"
    val transformFailed get() = if (isKo) "⚠️ 변환 실패 (원문이 유지됩니다)" else if (isId) "⚠️ Gagal mengubah (teks asli dipertahankan)" else "⚠️ Transformation failed (original kept)"
    fun toneConverting(icon: String, name: String) = if (isKo) "🔒 $icon $name 말투로 바꾸는 중..." else if (isId) "🔒 Mengubah ke gaya $name..." else "🔒 Changing tone to $name..."
    fun toneApplied(name: String) = if (isKo) "✨ $name 말투 적용 완료 ([입력] 터치)" else if (isId) "✨ Gaya $name siap (ketuk [Terapkan])" else "✨ $name tone ready (tap [Apply])"
    val textApplied get() = if (isKo) "✅ 입력창에 입력되었습니다" else if (isId) "✅ Dimasukkan ke pesan" else "✅ Inserted into message"
    val aiTextCleared get() = if (isKo) "🗑️ 내용을 지웠습니다" else if (isId) "🗑️ Teks dihapus" else "🗑️ Text cleared"
    val lastSentenceDeleted get() = if (isKo) "✂️ 방금 입력한 문장이 삭제되었습니다" else if (isId) "✂️ Kalimat sebelumnya telah dihapus" else "✂️ Previous sentence deleted"
    val editorSentenceDeleted get() = if (isKo) "✂️ 문장이 삭제되었습니다" else if (isId) "✂️ Kalimat dihapus" else "✂️ Sentence deleted"

    // ═══════════════════════════════════════════════════
    // DearTalkIntentEngine.kt — 결과 메시지
    // ═══════════════════════════════════════════════════
    val aiGenerationComplete get() = if (isKo) "🔒 온디바이스 AI 다듬기 완료" else if (isId) "🔒 AI On-Device Selesai Merapikan" else "🔒 On-Device AI Refinement Complete"
    val sttRawResult get() = if (isKo) "🎤 음성 인식 내용" else if (isId) "🎤 Teks Suara Asli" else "🎤 Recognized Speech"
    fun toneComplete(icon: String, name: String) = if (isKo) "🔒 $icon $name 완료" else if (isId) "🔒 $icon $name Selesai" else "🔒 $icon $name Complete"
    fun translationComplete(flag: String, name: String) = if (isKo) "🔒 $flag $name 번역 완료" else if (isId) "🔒 $flag Terjemahan $name Selesai" else "🔒 $flag $name Translation Complete"

    // ═══════════════════════════════════════════════════
    // MainActivity.kt — 설정 및 안내 화면
    // ═══════════════════════════════════════════════════
    val settingsListening get() = if (isKo) "말씀을 듣고 있어요... (편하게 말씀하세요)" else if (isId) "Mendengarkan suara Anda... (Bicara sekarang)" else "Listening to your voice... (Speak now)"
    val settingsAiAnalyzing get() = if (isKo) "🔒 AI가 문맥을 분석하여 문장을 다듬고 있습니다..." else if (isId) "🔒 Menganalisis konteks untuk merapikan kalimat..." else "🔒 Analyzing context to polish your sentence..."
    val settingsAiComplete get() = if (isKo) "🔒 AI 문장 다듬기 완료!" else if (isId) "🔒 Perapian AI Selesai!" else "🔒 AI Refinement Complete!"
    fun settingsSttError(code: Int) = if (isKo) "마이크 대기 중 (코드: $code)" else if (isId) "Mikrofon siap (kode: $code)" else "Microphone ready (code: $code)"
    val settingsAiRefining get() = if (isKo) "AI가 문장을 다듬는 중..." else if (isId) "AI sedang merapikan kalimat..." else "Polishing with On-Device AI..."
    val settingsAiRefined get() = if (isKo) "🔒 AI 문장 다듬기 완료" else if (isId) "🔒 Perapian AI Selesai" else "🔒 AI Refinement Complete"

    // ═══════════════════════════════════════════════════
    // MainActivity.kt — 정보 및 도움말
    // ═══════════════════════════════════════════════════
    val settingsTabAbout get() = if (isKo) "ℹ️ 앱 정보 및 도움말" else if (isId) "ℹ️ Informasi & Bantuan" else "ℹ️ About & Help"
    val appVersionLabel get() = if (isKo) "앱 버전" else if (isId) "Versi Aplikasi" else "App Version"
    val buildTimestampLabel get() = if (isKo) "업데이트 일시" else if (isId) "Terakhir Diperbarui" else "Last Updated"
    val userGuideTitle get() = if (isKo) "📖 DearTalk AI 쉽게 쓰는 법" else if (isId) "📖 Panduan Cepat DearTalk AI" else "📖 DearTalk AI Quick Guide"
    val userGuideHowToUseTitle get() = if (isKo) "💡 이렇게 사용해 보세요" else if (isId) "💡 Cara Penggunaan" else "💡 How to Use"
    val userGuideHowToUseContent get() = if (isKo) {
        "1. 위 🚀 [1단계] 및 [2단계] 버튼을 눌러 DearTalk AI 키보드를 활성화합니다.\n" +
        "2. 카카오톡, 문자, 이메일 등 원하는 대화창을 터치하여 키보드를 엽니다.\n" +
        "3. 키보드 상단의 마이크(🎙️) 버튼을 누르고 자연스럽게 말씀하세요.\n" +
        "4. [공손하게], [친근하게], [비즈니스] 등 톤 칩을 눌러 원하는 말투로 다듬습니다.\n" +
        "5. 완성된 문장을 확인한 후 오른쪽 [입력 📥] 버튼을 누르면 즉시 입력됩니다."
    } else if (isId) {
        "1. Ketuk tombol 🚀 [Langkah 1] dan [Langkah 2] di atas untuk mengaktifkan DearTalk AI.\n" +
        "2. Buka aplikasi pesan (WhatsApp, SMS, Email) dan buka papan ketik.\n" +
        "3. Ketuk tombol mikrofon (🎙️) di papan ketik dan bicaralah secara alami.\n" +
        "4. Ketuk tombol gaya seperti [Sopan], [Santai], atau [Formal] untuk merapikan kalimat.\n" +
        "5. Periksa kalimat yang dirapikan lalu ketuk [Terapkan 📥] untuk langsung memasukkan ke pesan."
    } else {
        "1. Tap Step 1 and Step 2 buttons above to enable and set DearTalk AI as default.\n" +
        "2. Tap any chat or text input area in KakaoTalk, Messages, or Email.\n" +
        "3. Tap the mic (🎙️) button and speak naturally.\n" +
        "4. Tap tone chips like [Polite], [Casual], or [Business] to polish your style.\n" +
        "5. Check the refined sentence and tap [Apply 📥] to insert it instantly."
    }
}
