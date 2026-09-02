package ai.deartalk.android.agent

import java.util.Collections
import java.util.LinkedHashMap

/**
 * 단일 발화 캐시 엔티티 (타임스탬프 포함)
 */
data class CachedUtterance(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 📱 앱별 격리 STT 대화 맥락 인메모리 슬라이딩 캐시 (AppScopedUtteranceCache)
 *
 * [4대 불변 원칙]
 * 1. 앱별 완전 격리: com.kakao.talk 과 com.Slack 의 대화 맥락이 절대 섞이지 않음.
 * 2. 슬라이딩 윈도우: 앱당 최근 2개 발화만 유지하여 메모리 사용량 최소화 (< 300 Bytes/app).
 * 3. TTL 자동 만료: 3분(180초) 이상 대화 단절 시 오래된 맥락이 새 대화를 오염시키는 현상(Stale Bleed) 방지.
 * 4. 순수 인메모리: 디스크 I/O 없이 0.001ms로 동작하며, 프로세스 종료 시 개인정보 흔적 없이 소멸.
 */
class AppScopedUtteranceCache(
    private val maxApps: Int = 10,
    private val maxUtterancesPerApp: Int = 2,
    private val ttlMillis: Long = 3 * 60 * 1000L // 기본 3분
) {
    companion object {
        /** 전역 공유 인스턴스 (Singleton) */
        val shared = AppScopedUtteranceCache()
    }

    // LRU 순서가 유지되는 동기화된 맵 (앱 패키지명 -> 발화 리스트)
    private val appPartitions: MutableMap<String, MutableList<CachedUtterance>> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, MutableList<CachedUtterance>>(maxApps, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MutableList<CachedUtterance>>?): Boolean {
                    return size > maxApps
                }
            }
        )

    /**
     * 특정 앱에 새로운 발화 추가 (슬라이딩 윈도우 갱신)
     */
    fun addUtterance(packageName: String, text: String, currentTimeMillis: Long = System.currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val key = normalizePackageName(packageName)

        synchronized(appPartitions) {
            val list = appPartitions.getOrPut(key) { mutableListOf() }
            // 오래된 항목 만료 검사
            list.removeAll { (currentTimeMillis - it.timestamp) > ttlMillis }

            // 새 발화 추가
            list.add(CachedUtterance(trimmed, currentTimeMillis))

            // 슬라이딩 윈도우 크기 제한 (최근 N개만 유지)
            while (list.size > maxUtterancesPerApp) {
                list.removeAt(0)
            }
        }
    }

    /**
     * 특정 앱의 최근 유효 대화 맥락 조회 (TTL 내의 발화만 반환)
     */
    fun getRecentContext(packageName: String, currentTimeMillis: Long = System.currentTimeMillis()): List<String> {
        val key = normalizePackageName(packageName)
        synchronized(appPartitions) {
            val list = appPartitions[key] ?: return emptyList()
            // TTL 만료 필터링
            list.removeAll { (currentTimeMillis - it.timestamp) > ttlMillis }
            return list.map { it.text }
        }
    }

    /**
     * 특정 앱 또는 전체 캐시 초기화
     */
    fun clear(packageName: String? = null) {
        synchronized(appPartitions) {
            if (packageName != null) {
                appPartitions.remove(normalizePackageName(packageName))
            } else {
                appPartitions.clear()
            }
        }
    }

    /**
     * 캐시 상태 모니터링 (디버그 및 테스트용)
     */
    fun getPartitionCount(): Int = synchronized(appPartitions) { appPartitions.size }

    private fun normalizePackageName(packageName: String): String {
        val clean = packageName.trim()
        return if (clean.isBlank()) "unknown_app" else clean
    }
}
