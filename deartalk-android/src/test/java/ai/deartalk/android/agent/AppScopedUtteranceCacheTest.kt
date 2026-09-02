package ai.deartalk.android.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppScopedUtteranceCacheTest {

    private lateinit var cache: AppScopedUtteranceCache

    @Before
    fun setUp() {
        // 테스트용: 최대 3개 앱, 앱당 최대 2개 발화, TTL 3분
        cache = AppScopedUtteranceCache(
            maxApps = 3,
            maxUtterancesPerApp = 2,
            ttlMillis = 180_000L
        )
    }

    @Test
    fun testNamespaceIsolation_differentAppsDoNotLeakContext() {
        // Given
        val kakao = "com.kakao.talk"
        val slack = "com.Slack"

        // When
        cache.addUtterance(kakao, "오늘 저녁 삼겹살 어때?")
        cache.addUtterance(slack, "Q3 스프린트 일정입니다.")

        // Then: 카카오톡에는 삼겹살만, 슬랙에는 스프린트만 있어야 함 (격리 보장)
        val kakaoContext = cache.getRecentContext(kakao)
        val slackContext = cache.getRecentContext(slack)

        assertEquals(1, kakaoContext.size)
        assertEquals("오늘 저녁 삼겹살 어때?", kakaoContext[0])

        assertEquals(1, slackContext.size)
        assertEquals("Q3 스프린트 일정입니다.", slackContext[0])
    }

    @Test
    fun testSlidingWindow_keepsOnlyTwoMostRecentUtterances() {
        val app = "com.whatsapp"

        // 3개 발화 순차 추가
        cache.addUtterance(app, "첫 번째 발화: 안녕")
        cache.addUtterance(app, "두 번째 발화: 뭐해?")
        cache.addUtterance(app, "세 번째 발화: 밥 먹었어?")

        val context = cache.getRecentContext(app)

        // 슬라이딩 윈도우 크기 2 검증: 가장 오래된 첫 번째 발화는 탈락해야 함
        assertEquals(2, context.size)
        assertEquals("두 번째 발화: 뭐해?", context[0])
        assertEquals("세 번째 발화: 밥 먹었어?", context[1])
    }

    @Test
    fun testTtlExpiration_olderThanThreeMinutesIsFilteredOut() {
        val app = "com.google.android.gm"
        val baseTime = 1_000_000L

        // 1분 전 발화 추가
        cache.addUtterance(app, "견적서 송부드립니다.", currentTimeMillis = baseTime)

        // 2분 경과 시점: TTL(3분) 이내이므로 유지
        val validContext = cache.getRecentContext(app, currentTimeMillis = baseTime + 120_000L)
        assertEquals(1, validContext.size)

        // 4분 경과 시점: TTL(3분) 초과이므로 자동 소멸
        val expiredContext = cache.getRecentContext(app, currentTimeMillis = baseTime + 240_000L)
        assertTrue("3분 초과 발화는 자동 소멸되어야 함", expiredContext.isEmpty())
    }

    @Test
    fun testLruEviction_exceedingMaxAppsRemovesEldest() {
        // maxApps = 3
        cache.addUtterance("app.one", "1번 발화")
        cache.addUtterance("app.two", "2번 발화")
        cache.addUtterance("app.three", "3번 발화")

        assertEquals(3, cache.getPartitionCount())

        // 4번째 앱 추가 시 가장 오래된 app.one 파티션이 Evict 되어야 함
        cache.addUtterance("app.four", "4번 발화")

        assertEquals(3, cache.getPartitionCount())
        assertTrue("app.one은 Evict 되어 비어있어야 함", cache.getRecentContext("app.one").isEmpty())
        assertEquals(1, cache.getRecentContext("app.four").size)
    }
}
