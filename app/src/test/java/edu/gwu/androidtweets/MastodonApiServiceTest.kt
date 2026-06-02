package edu.gwu.androidtweets

import edu.gwu.androidtweets.api.MastodonApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MastodonApiServiceTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun service() = MastodonApi.create(
        baseUrl = mockWebServer.url("/api/").toString()
    )

    // ---------- tagTimeline ----------

    @Test
    fun `tagTimeline parses two statuses correctly`() = runTest {
        val json = javaClass.classLoader!!.getResource("mastodon_timeline.json")!!.readText()
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

        val statuses = service().tagTimeline("Android")

        assertEquals(2, statuses.size)

        val first = statuses[0]
        assertEquals("androiddev", first.account.username)
        assertEquals("androiddev@mastodon.social", first.account.acct)
        assertEquals("Android Developer", first.account.displayName)
        assertTrue("avatar URL should be set", first.account.avatar.isNotEmpty())
    }

    @Test
    fun `tagTimeline returns empty list for empty array response`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        val statuses = service().tagTimeline("Android")

        assertTrue(statuses.isEmpty())
    }

    @Test
    fun `tagTimeline sends correct path and query params`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        service().tagTimeline(hashtag = "Android", limit = 10)

        val request = mockWebServer.takeRequest()
        assertTrue("path should contain hashtag", request.path!!.contains("/v1/timelines/tag/Android"))
        assertTrue("path should include limit param", request.path!!.contains("limit=10"))
    }

    @Test
    fun `tagTimeline ignores unknown JSON fields`() = runTest {
        val jsonWithExtraFields = """
            [{"id":"1","content":"<p>Test</p>","uri":"https://example.com/1","created_at":"2024-01-01T00:00:00.000Z",
              "account":{"id":"1","username":"user","acct":"user@mastodon.social","display_name":"User",
              "avatar":"https://example.com/avatar.jpg","avatar_static":"https://example.com/avatar.jpg",
              "unknown_future_field":"some value"}}]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(jsonWithExtraFields).setResponseCode(200))

        val statuses = service().tagTimeline("Android")

        assertEquals(1, statuses.size)
        assertEquals("user", statuses[0].account.username)
    }
}
