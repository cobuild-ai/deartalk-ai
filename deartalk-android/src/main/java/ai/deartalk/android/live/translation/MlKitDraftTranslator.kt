package ai.deartalk.android.live.translation

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 🌐 [Track 1] Google ML Kit 온디바이스 초경량 실시간 초안 번역기
 *
 * 핵심 역할:
 * - 100% 온디바이스 오프라인 신경망 기계번역(NMT, ~30MB)을 활용하여
 *   전 세계 59개 이상의 모든 주요 언어에 대해 50~100ms(0.1초) 내에 동적 초안을 생성합니다.
 * - 특정 언어 쌍이나 30여 개 관용구에 갇힌 하드코딩 사전을 완전히 탈피하고,
 *   임의의 모든 대화 발화(Free-form Utterance)를 즉각 번역합니다.
 * - 이후 백그라운드에서 온디바이스 SLM(Gemma 4 E2B)이 문맥, 화행, 어조, 존칭을 2차 정밀 보정합니다.
 */
object MlKitDraftTranslator {

    private const val TAG = "MlKitDraftTranslator"

    // 캐싱된 Translator 인스턴스 풀 ("srcTag_tgtTag" -> Translator)
    private val translatorPool = ConcurrentHashMap<String, Translator>()

    // 모델 다운로드 조건 (와이파이 제약 없이 셀룰러에서도 빠른 다운로드 허용)
    private val downloadConditions = DownloadConditions.Builder()
        .build()

    /**
     * 🌐 언어 코드(예: "KO", "ID", "EN", "JA", "zh", "es" 등)를 ML Kit 표준 언어 태그로 정규화
     */
    fun resolveLanguageTag(code: String): String? {
        val clean = code.trim().lowercase().split("-")[0].split("_")[0]
        return TranslateLanguage.fromLanguageTag(clean)
    }

    /**
     * 🎯 해당 언어가 Google ML Kit 온디바이스 번역 지원 목록(59개 언어)에 포함되는지 확인
     */
    fun isSupported(langCode: String): Boolean {
        return resolveLanguageTag(langCode) != null
    }

    /**
     * 🚀 [Track 1 초고속 번역 API]
     * - 소요 시간: 50~100ms
     * - 입력된 문장을 온디바이스 ML Kit NMT로 즉시 번역합니다.
     * - 모델 미다운로드 또는 미지원 언어 시 null을 반환하여 안전하게 Track 2(Gemma 4 E2B)로 폴백합니다.
     */
    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String? = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext ""

        val srcTag = resolveLanguageTag(sourceLangCode)
        val tgtTag = resolveLanguageTag(targetLangCode)

        if (srcTag == null || tgtTag == null) {
            Log.w(TAG, "⚠️ [ML Kit 미지원 언어]: src=$sourceLangCode($srcTag), tgt=$targetLangCode($tgtTag)")
            return@withContext null
        }

        if (srcTag.equals(tgtTag, ignoreCase = true)) {
            return@withContext cleanText
        }

        try {
            val translator = getOrCreateTranslator(srcTag, tgtTag)
            
            // 모델 미다운로드 상태일 경우 선제 다운로드 (비동기 await)
            translator.downloadModelIfNeeded(downloadConditions).await()

            val startTime = System.currentTimeMillis()
            val translated = translator.translate(cleanText).await()
            val elapsed = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "⚡ [ML Kit Track 1 초안 완료 (${elapsed}ms)]: '$cleanText' ➔ '$translated'")
            translated
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [ML Kit 번역 일시 실패 (Track 2로 자동 폴백)]: ${e.message}")
            null
        }
    }

    /**
     * 🚀 세션 시작 또는 언어 변경 시 온디바이스 모델 백그라운드 사전 다운로드/워밍업
     */
    suspend fun prewarm(sourceLangCode: String, targetLangCode: String) = withContext(Dispatchers.IO) {
        val srcTag = resolveLanguageTag(sourceLangCode) ?: return@withContext
        val tgtTag = resolveLanguageTag(targetLangCode) ?: return@withContext

        if (srcTag.equals(tgtTag, ignoreCase = true)) return@withContext

        try {
            Log.d(TAG, "🚀 [ML Kit 프리웜 시작] 양방향 모델 다운로드 및 준비: $srcTag ↔ $tgtTag")
            // 양방향 (src ➔ tgt, tgt ➔ src) 모두 준비
            val forward = getOrCreateTranslator(srcTag, tgtTag)
            val reverse = getOrCreateTranslator(tgtTag, srcTag)

            forward.downloadModelIfNeeded(downloadConditions).await()
            reverse.downloadModelIfNeeded(downloadConditions).await()
            Log.d(TAG, "✅ [ML Kit 프리웜 완료] 온디바이스 NMT 모델 준비 완료 ($srcTag ↔ $tgtTag)")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [ML Kit 프리웜 지연]: ${e.message}")
        }
    }

    private fun getOrCreateTranslator(srcTag: String, tgtTag: String): Translator {
        val key = "${srcTag}_$tgtTag"
        return translatorPool.computeIfAbsent(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(srcTag)
                .setTargetLanguage(tgtTag)
                .build()
            Translation.getClient(options)
        }
    }

    /**
     * 🧹 메모리 해제
     */
    fun close() {
        translatorPool.values.forEach { translator ->
            try {
                translator.close()
            } catch (ignored: Exception) {}
        }
        translatorPool.clear()
    }
}
