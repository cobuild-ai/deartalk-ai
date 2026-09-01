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
    // DearTalkScreen.kt — 마이크 버튼 상태 (안 짤리는 컴팩트 레이블)
    // ═══════════════════════════════════════════════════
    val micPreparing get() = if (isKo) "마이크 준비 중..." else if (isId) "Menyiapkan..." else "Preparing mic..."
    val micListening get() = if (isKo) "듣는 중 (터치 시 완료)" else if (isId) "Mendengarkan (Ketuk selesai)" else "Listening (Tap to finish)"
    val micProcessingAi get() = if (isKo) "AI 다듬는 중..." else if (isId) "AI merapikan..." else "AI Polishing..."
    val micIdle get() = if (isKo) "AI 음성 입력" else if (isId) "Masukan Suara AI" else "AI Voice Input"

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

    val instRefine get() = if (isKo) "문맥을 살려 중복과 어색한 끊김 없이 자연스럽고 유려한 완성형 문장으로 다듬어 작성하세요." else if (isId) "Rapikan menjadi satu kalimat lengkap yang alami dan lancar tanpa pengulangan kata yang janggal." else "Polish into a natural, fluent, and well-structured complete sentence preserving original intent."
    val instPolite get() = if (isKo) "상대방에게 정중하고 예의 바른 비즈니스 경어체로 다듬어 완성형 문장 하나로 작성하세요." else if (isId) "Ubah menjadi satu kalimat lengkap dengan gaya bahasa yang sopan, santun, dan formal." else "Refine into a polite, respectful, and courteous complete sentence suitable for professional communication."
    val instCasual get() = if (isKo) "친구에게 대화하듯 부드럽고 친근한 톤으로 자연스러운 완성형 문장 하나로 작성하세요." else if (isId) "Ubah menjadi kalimat santai, hangat, dan ramah seperti mengobrol dengan teman dekat." else "Transform into a friendly, casual, and conversational complete sentence like talking to a friend."
    val instBusiness get() = if (isKo) "격식 있는 이메일/업무 메신저에 어울리는 명확하고 신뢰감 있는 문장으로 작성하세요." else if (isId) "Tuliskan dalam gaya bisnis yang jelas, ringkas, dan profesional untuk pesan kerja/email." else "Write in a clear, professional, and concise tone ideal for business messaging and email."
    val instFunny get() = if (isKo) "재치 있고 위트와 유머가 넘치며 빵 터지는 센스 있는 유쾌한 어조로 작성하세요." else if (isId) "Tuliskan dengan gaya yang ceria, penuh humor, lucu, dan menyenangkan." else "Add witty humor and playful charm to create an entertaining and fun sentence."
    val instCheeky get() = if (isKo) "자신만만하고 쿨하며 살짝 얄밉고 거만하지만 밉지 않은 도도한 반말 어조로 작성하세요." else if (isId) "Tuliskan dengan gaya percaya diri, keren, sedikit nakal tapi tetap memikat." else "Write in a confident, cheeky, and cool attitude that is delightfully bold."

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
    // ═══════════════════════════════════════════════════
    // VoiceStudioActivity.kt — 온디바이스 보이스 스튜디오 & 실시간 통역기
    // ═══════════════════════════════════════════════════
    val voiceStudioTitle get() = if (isKo) "🎙️ DearTalk 보이스 스튜디오" else if (isId) "🎙️ Studio Suara DearTalk" else "🎙️ DearTalk Voice Studio"
    val backButtonContentDesc get() = if (isKo) "뒤로가기" else if (isId) "Kembali" else "Back"

    // 1. 하드웨어 진단 및 모델 상태
    fun diagOptimal(ram: String, storage: String) = if (isKo) "🟢 최적 사양 (RAM ${ram}GB / 여유 ${storage}GB)" else if (isId) "🟢 Spesifikasi Optimal (RAM ${ram}GB / Bebas ${storage}GB)" else "🟢 Optimal Spec (RAM ${ram}GB / Free ${storage}GB)"
    fun diagCaution(ram: String) = if (isKo) "🟡 주의 사양 (RAM ${ram}GB - 순차 파이프라인 구동)" else if (isId) "🟡 Perhatian (RAM ${ram}GB - Alur Kerja Sekuensial)" else "🟡 Caution (RAM ${ram}GB - Sequential Pipeline)"
    fun diagRestricted(ram: String) = if (isKo) "🔴 사양 제한 (RAM ${ram}GB - 기본 엔진 권장)" else if (isId) "🔴 Terbatas (RAM ${ram}GB - Disarankan Mesin Standar)" else "🔴 Restricted (RAM ${ram}GB - Standard Engine Recommended)"
    
    val diagModelTitle get() = if (isKo) "Qwen 고품질 온디바이스 패키지" else if (isId) "Paket AI Kualitas Tinggi Qwen" else "Qwen High-Quality AI Package"
    val diagModelSubtitle get() = if (isKo) "STT(0.6B) + LLM(1.7B) + TTS(0.6B) · 1.8GB" else if (isId) "STT(0.6B) + LLM(1.7B) + TTS(0.6B) · 1.8GB" else "STT(0.6B) + LLM(1.7B) + TTS(0.6B) · 1.8GB"
    val diagDownloadBtn get() = if (isKo) "다운로드" else if (isId) "Unduh" else "Download"
    val diagDownloadingLabel get() = if (isKo) "고품질 패키지 다운로드 중..." else if (isId) "Mengunduh paket kualitas tinggi..." else "Downloading High-Quality Package..."
    val diagActiveLabel get() = if (isKo) "Qwen 고성능 엔진 활성화됨" else if (isId) "Mesin Qwen Performa Tinggi Aktif" else "Qwen Neural Engine Active"
    val diagPurgeContentDesc get() = if (isKo) "모델 패키지 삭제" else if (isId) "Hapus paket model" else "Purge model package"
    fun diagErrorLabel(msg: String) = if (isKo) "⚠️ 다운로드 오류: $msg" else if (isId) "⚠️ Kesalahan unduhan: $msg" else "⚠️ Download error: $msg"

    // 2. 모드 탭
    val modeToneTransform get() = if (isKo) "✨ 고운말 톤 변환" else if (isId) "✨ Transformasi Gaya Bicara" else "✨ Tone Transformation"
    val modeLiveTranslation get() = if (isKo) "🌐 실시간 다국어 통역" else if (isId) "🌐 Penerjemah Langsung" else "🌐 Live Interpretation"

    // 3. 실시간 통역 상대방 언어 바
    val partnerLangSelectionTitle get() = if (isKo) "🌐 통역할 상대방 언어 선택" else if (isId) "🌐 Pilih Bahasa Lawan Bicara" else "🌐 Select Partner Language"
    fun conversationPairBadge(myLang: String, partnerLang: String) = if (isKo) "$myLang ⇄ $partnerLang (양방향 자동 통역)" else if (isId) "$myLang ⇄ $partnerLang (Otomatis 2 Arah)" else "$myLang ⇄ $partnerLang (Auto 2-Way)"
    val inputHeaderLabel get() = if (isKo) "🗣️ 입력: " else if (isId) "🗣️ Masukan: " else "🗣️ Input: "
    val outputHeaderLabel get() = if (isKo) "🌐 번역: " else if (isId) "🌐 Terjemahan: " else "🌐 Output: "
    val swapLangContentDesc get() = if (isKo) "입력/출력 언어 맞바꾸기" else if (isId) "Tukar bahasa masukan/keluaran" else "Swap input and output languages"
    val step1SpokenLang get() = if (isKo) "1️⃣ 내가 말할 언어 (마이크 입력):" else if (isId) "1️⃣ Bahasa yang diucapkan (Masukan):" else "1️⃣ Spoken Language (Mic Input):"
    val step2TargetLang get() = if (isKo) "2️⃣ AI가 번역할 언어 (스피커 출력):" else if (isId) "2️⃣ Bahasa terjemahan AI (Keluaran):" else "2️⃣ Target Language (Speaker Output):"

    // 4. 언어 표시명 (국기 + 현지화 이름)
    fun getLangDisplayName(code: String): String = when (code.uppercase()) {
        "KO" -> if (isKo) "🇰🇷 한국어" else if (isId) "🇰🇷 Bahasa Korea" else "🇰🇷 Korean"
        "EN" -> if (isKo) "🇺🇸 영어" else if (isId) "🇺🇸 Bahasa Inggris" else "🇺🇸 English"
        "JA" -> if (isKo) "🇯🇵 일본어" else if (isId) "🇯🇵 Bahasa Jepang" else "🇯🇵 Japanese"
        "ZH" -> if (isKo) "🇨🇳 중국어" else if (isId) "🇨🇳 Bahasa Mandarin" else "🇨🇳 Chinese"
        "ES" -> if (isKo) "🇪🇸 스페인어" else if (isId) "🇪🇸 Bahasa Spanyol" else "🇪🇸 Spanish"
        "FR" -> if (isKo) "🇫🇷 프랑스어" else if (isId) "🇫🇷 Bahasa Prancis" else "🇫🇷 French"
        "DE" -> if (isKo) "🇩🇪 독일어" else if (isId) "🇩🇪 Bahasa Jerman" else "🇩🇪 German"
        "ID" -> if (isKo) "🇮🇩 인도네시아어" else if (isId) "🇮🇩 Bahasa Indonesia" else "🇮🇩 Indonesian"
        "VI" -> if (isKo) "🇻🇳 베트남어" else if (isId) "🇻🇳 Bahasa Vietnam" else "🇻🇳 Vietnamese"
        "TL", "FIL" -> if (isKo) "🇵🇭 필리핀어" else if (isId) "🇵🇭 Bahasa Filipino" else "🇵🇭 Tagalog"
        "TH" -> if (isKo) "🇹🇭 태국어" else if (isId) "🇹🇭 Bahasa Thai" else "🇹🇭 Thai"
        "MS" -> if (isKo) "🇲🇾 말레이어" else if (isId) "🇲🇾 Bahasa Melayu" else "🇲🇾 Malay"
        else -> code
    }

    // 5. 음성 커스터마이저
    val voiceToneCustomizerTitle get() = if (isKo) "🎙️ 발화 음색" else if (isId) "🎙️ Karakter Suara" else "🎙️ Vocal Timbre"
    val voiceFemale get() = if (isKo) "👩 여성 음성" else if (isId) "👩 Suara Wanita" else "👩 Female Voice"
    val voiceMale get() = if (isKo) "👨 남성 음성" else if (isId) "👨 Suara Pria" else "👨 Male Voice"
    val pitchMatchingLabel get() = if (isKo) "🎚️ 톤 매칭:" else if (isId) "🎚️ Nada Suara:" else "🎚️ Pitch Match:"
    val pitchNormal get() = if (isKo) "보통" else if (isId) "Normal" else "Normal"
    val pitchDeepLow get() = if (isKo) "중후한 저음" else if (isId) "Bass Dalam" else "Deep Low"
    val pitchWarmMid get() = if (isKo) "부드러운 중음" else if (isId) "Sedang Hangat" else "Warm Mid"
    val pitchBrightHigh get() = if (isKo) "밝은 고음" else if (isId) "Tinggi Cerah" else "Bright High"

    // 6. 결과 표시 카드
    val rawSttTitle get() = if (isKo) "내가 말한 내용 (STT)" else if (isId) "Yang Anda Katakan (STT)" else "What You Said (STT)"
    val rawSttListening get() = if (isKo) "음성 인식 중..." else if (isId) "Mendengarkan..." else "Listening..."
    val aiResultTitle get() = if (isKo) "✨ AI 조율 및 번역 결과" else if (isId) "✨ Hasil AI & Terjemahan" else "✨ AI Refined & Translated"
    val replayButtonDesc get() = if (isKo) "다시 듣기" else if (isId) "Putar Ulang" else "Listen Again"
    val initialRawPrompt get() = if (isKo) "하단의 마이크 버튼을 누르고 말씀해 보세요." else if (isId) "Ketuk tombol mikrofon di bawah dan bicaralah." else "Tap the mic button below and start speaking."
    val initialAiPrompt get() = if (isKo) "AI가 정제한 결과가 이곳에 표시됩니다." else if (isId) "Hasil yang dirapikan AI akan muncul di sini." else "AI refined output will appear here."
    val noSpeechDetected get() = if (isKo) "음성이 감지되지 않았습니다. 다시 탭하고 말씀해 보세요." else if (isId) "Suara tidak terdeteksi. Silakan ketuk lagi dan bicara." else "No speech detected. Please tap again and speak."
    val micPermissionNeeded get() = if (isKo) "음성 인식을 위해 마이크 권한이 필요합니다." else if (isId) "Izin mikrofon diperlukan untuk pengenalan suara." else "Microphone permission is required for speech recognition."

    // 7. 메인 마이크 버튼
    val micBtnPreparing get() = if (isKo) "⏳ 마이크 준비 중..." else if (isId) "⏳ Menyiapkan mikrofon..." else "⏳ Preparing mic..."
    val micBtnListening get() = if (isKo) "🔴 녹음 중... (탭하여 완료)" else if (isId) "🔴 Merekam... (Ketuk untuk selesai)" else "🔴 Recording... (Tap to finish)"
    val micBtnProcessing get() = if (isKo) "✨ AI 정제 및 번역 중..." else if (isId) "✨ AI sedang merapikan & menerjemahkan..." else "✨ AI Refining & Translating..."
    val micBtnIdle get() = if (isKo) "탭하여 마이크로 말하기" else if (isId) "Ketuk untuk berbicara" else "Tap to speak"

    val contentDescPreparing get() = if (isKo) "준비 중" else if (isId) "Menyiapkan" else "Preparing"
    val contentDescStop get() = if (isKo) "녹음 중지" else if (isId) "Hentikan perekaman" else "Stop recording"
    val contentDescSpeak get() = if (isKo) "말하기" else if (isId) "Bicara" else "Speak"

    // 8. 빠른 테스트 문장 (로케일 맞춤형)
    val quickTestTitle get() = if (isKo) "💡 빠른 테스트 문장" else if (isId) "💡 Contoh Kalimat Cepat" else "💡 Quick Test Sentences"
    val quickSamples: List<String> get() = if (isKo) {
        listOf(
            "오늘 밥 같이 먹을래?",
            "차 막혀서 늦을 것 같아 미안해",
            "자료 검토 후 회신 부탁드립니다",
            "이 제품 가격이 어떻게 되나요?"
        )
    } else if (isId) {
        listOf(
            "Mau makan bareng hari ini?",
            "Maaf sepertinya saya terlambat karena macet",
            "Mohon balas setelah meninjau dokumen",
            "Berapa harga produk ini?"
        )
    } else {
        listOf(
            "Do you want to grab lunch today?",
            "Sorry, I might be late due to traffic",
            "Please reply after reviewing the documents",
            "How much is this item?"
        )
    }

    // 9. 온디바이스 AI 지능 등급 뱃지 & 안내
    val tierBadgeHigh get() = if (isKo) "🌟 Qwen 1.7B Pro" else if (isId) "🌟 Qwen 1.7B Pro" else "🌟 Qwen 1.7B Pro"
    val tierBadgeBase get() = if (isKo) "🟢 Gemma 2B Base" else if (isId) "🟢 Gemma 2B Base" else "🟢 Gemma 2B Base"
    val tierBadgeSttOnly get() = if (isKo) "⚡ STT 모드 (AI팩 필요)" else if (isId) "⚡ Mode STT (Perlu Paket AI)" else "⚡ STT Only (Needs AI Pack)"

    val tierBannerSttOnlyPrompt get() = if (isKo) "💡 AI 모델을 다운로드하면 문맥 정제 및 톤 변환이 활성화됩니다." else if (isId) "💡 Unduh model AI untuk mengaktifkan pemurnian & nada bicara." else "💡 Download AI model to enable context refinement & tone styles."
    val tierDownloadAction get() = if (isKo) "AI 팩 다운로드" else if (isId) "Unduh Paket AI" else "Download AI Pack"
}
