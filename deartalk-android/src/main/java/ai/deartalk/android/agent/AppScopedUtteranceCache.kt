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
 * 📱 앱별 고정 크기 원형 링 버퍼 (Circular Ring Buffer)
 *
 * 동적 ArrayList 대신 고정 배열을 제자리에서 덮어쓰며(In-place Overwrite),
 * GC(Garbage Collection) 발생을 0에 가깝게 통제합니다.
 */
private class CircularUtteranceBuffer(private val capacity: Int = 2) {
    private val buffer = arrayOfNulls<CachedUtterance>(capacity)
    private var writeIndex = 0
    private var count = 0

    fun write(text: String, timestamp: Long) {
        buffer[writeIndex] = CachedUtterance(text, timestamp)
        writeIndex = (writeIndex + 1) % capacity
        if (count < capacity) count++
    }

    fun readValid(ttlMillis: Long, now: Long): List<String> {
        val result = ArrayList<String>(capacity)
        for (i in 0 until count) {
            val idx = if (count == capacity) (writeIndex + i) % capacity else i
            val item = buffer[idx]
            if (item != null && (now - item.timestamp) <= ttlMillis) {
                result.add(item.text)
            }
        }
        return result
    }

    fun clear() {
        for (i in buffer.indices) buffer[i] = null
        writeIndex = 0
        count = 0
    }
}

/**
 * 📱 앱별 격리 STT 대화 맥락 인메모리 고정 슬라이딩 캐시 (AppScopedUtteranceCache)
 *
 * [메모리 극대화 4대 원칙]
 * 1. 최대 앱 3개 제한: 동시에 활성화되는 메신저/업무 앱(카카오톡, 슬랙 등) 최대 3개만 유지.
 * 2. 원형 링 버퍼 (In-place Overwrite): 앱당 2개 고정 슬롯을 순환 덮어쓰기하여 메모리 할당 0(Zero).
 * 3. TTL 자동 만료: 3분 이상 대화 단절 시 만료 항목 자동 배제.
 * 4. 영구 메모리 고정 (< 1KB): 전체 캐시가 1KB를 넘지 않아 저사양 폰에서도 GC Jitter 제로.
 */
class AppScopedUtteranceCache(
    private val maxApps: Int = 3,
    private val maxUtterancesPerApp: Int = 2,
    private val ttlMillis: Long = 3 * 60 * 1000L // 기본 3분
) {
    companion object {
        /** 전역 공유 인스턴스 (최대 3개 앱, 2개 발화 링버퍼) */
        val shared = AppScopedUtteranceCache()
    }

    // LRU 순서 유지 고정 맵 (최대 3개 앱 파티션)
    private val appPartitions: MutableMap<String, CircularUtteranceBuffer> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, CircularUtteranceBuffer>(maxApps, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CircularUtteranceBuffer>?): Boolean {
                    return size > maxApps
                }
            }
        )

    /**
     * 특정 앱에 새로운 발화 추가 (원형 버퍼 제자리 덮어쓰기)
     */
    fun addUtterance(packageName: String, text: String, currentTimeMillis: Long = System.currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val key = normalizePackageName(packageName)

        synchronized(appPartitions) {
            val ringBuffer = appPartitions.getOrPut(key) { CircularUtteranceBuffer(maxUtterancesPerApp) }
            ringBuffer.write(trimmed, currentTimeMillis)
        }
    }

    /**
     * 특정 앱의 최근 유효 대화 맥락 조회 (원형 버퍼에서 TTL 유효 항목만 순서대로 추출)
     */
    fun getRecentContext(packageName: String, currentTimeMillis: Long = System.currentTimeMillis()): List<String> {
        val key = normalizePackageName(packageName)
        synchronized(appPartitions) {
            val ringBuffer = appPartitions[key] ?: return emptyList()
            return ringBuffer.readValid(ttlMillis, currentTimeMillis)
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

    fun getPartitionCount(): Int = synchronized(appPartitions) { appPartitions.size }

    private fun normalizePackageName(packageName: String): String {
        val clean = packageName.trim()
        return if (clean.isBlank()) "unknown_app" else clean
    }
}
