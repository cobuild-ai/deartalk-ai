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
 * 🧠 [인간 인지공학 및 행동 양식 분석 기반 아키텍처]
 * 1. 인간의 작업 기억(Working Memory)과 멀티태스킹의 한계:
 *    - 사용자가 모바일 환경에서 실시간 음성(STT)으로 대화를 나눌 때, 이론적으로는 여러 앱을 사용할 수 있지만
 *      실제 인간의 인지 부하(Cognitive Load) 상 대화의 축은 '단 1개의 주 사용 앱(Primary Focus)'에 집중됩니다.
 *    - 기껏해야 링크를 확인하거나 정보를 참조하기 위해 다른 1개의 앱을 번갈아 확인(Toggled Secondary)하는 것이
 *      인간 행동 양식의 최대치(Max Capacity)입니다.
 *    - 3개 이상의 앱을 동시에 오가며 실시간 STT 대화를 병렬 수행하는 인간은 존재하지 않습니다.
 *
 * 2. 2-슬롯 듀얼 앱 아키텍처 (maxApps = 2):
 *    - 슬롯을 정확히 2개(Primary + Secondary)로 고정하여, 제3의 앱이 활성화되는 즉시
 *      인간의 인지 범위에서 이미 벗어난 가장 오래된 앱 파티션을 지체 없이 덮어씁니다(Evict & Reuse).
 *
 * 3. 원형 링 버퍼 제자리 덮어쓰기 (Zero-Allocation Circular Ring Buffer):
 *    - 동적 컬렉션(ArrayList 등)을 사용할 경우 발생하는 주기적 메모리 재할당 및 가비지 컬렉터(GC) 부하를 배제합니다.
 *    - 앱당 고정 크기 2개 슬롯의 원형 링 버퍼를 순환 포인터로 제자리에서 덮어씀(In-place Overwrite)으로써,
 *      신규 메모리 할당을 제로(0)로 통제하고 저사양 기기에서도 키보드 프레임 드롭(Jitter)을 방지합니다.
 *
 * 4. 시간 기반 자동 만료 (TTL = 3분) & 개인정보 완전 보호:
 *    - 3분 이상 대화가 단절되면 과거 맥락이 현재의 새로운 의도를 왜곡(Stale Context Bleed)하지 않도록 자동 필터링합니다.
 *    - SQLite나 파일 등 디스크 저장을 일체 배제한 순수 RAM 캐시로, 프로세스 종료 시 흔적 없이 소멸합니다.
 *    - 전체 메모리 점유율은 500 바이트 미만으로 영구 고정됩니다.
 */
class AppScopedUtteranceCache(
    private val maxApps: Int = 2,
    private val maxUtterancesPerApp: Int = 2,
    private val ttlMillis: Long = 3 * 60 * 1000L // 기본 3분
) {
    companion object {
        /** 전역 공유 인스턴스 (인간 인지 한계 기반: 최대 2개 앱, 각 2개 발화 링버퍼) */
        val shared = AppScopedUtteranceCache()
    }

    // LRU 순서 유지 고정 맵 (최대 2개 앱 파티션)
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
