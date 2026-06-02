package edu.gwu.androidtweets

import android.location.Address
import edu.gwu.androidtweets.api.MastodonApi
import edu.gwu.androidtweets.api.MastodonApiService
import edu.gwu.androidtweets.viewmodel.fetchTweets
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [fetchTweets] — the pure business logic extracted from TweetsViewModel.
 * Tests the city-filtered search and fallback behaviour without lifecycle/dispatcher complexity.
 */
class TweetsViewModelTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: MastodonApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        service = MastodonApi.create(mockWebServer.url("/api/").toString())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun mockAddress(
        locality: String? = null,
        subAdminArea: String? = null,
        adminArea: String? = null,
        countryName: String? = null
    ): Address = mockk<Address>().also {
        every { it.locality } returns locality
        every { it.subAdminArea } returns subAdminArea
        every { it.adminArea } returns adminArea
        every { it.countryName } returns countryName
    }

    private val timelineJson get() =
        javaClass.classLoader!!.getResource("mastodon_timeline.json")!!.readText()

    // ---------- happy path ----------

    @Test
    fun `fetchTweets returns mapped tweets on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        val tweets = fetchTweets(mockAddress(locality = "Berlin"), service)

        assertEquals(2, tweets.size)
        assertEquals("Android Developer", tweets[0].username)
        assertEquals("@androiddev@mastodon.social", tweets[0].handle)
        assertTrue(tweets[0].content.isNotEmpty())
        assertTrue(tweets[0].iconUrl.isNotEmpty())
    }

    @Test
    fun `fetchTweets includes city tag in request URL`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        fetchTweets(mockAddress(locality = "London"), service)

        val path = mockWebServer.takeRequest().path!!
        assertTrue("URL should contain city tag", path.contains("london"))
    }

    // ---------- fallback ----------

    @Test
    fun `fetchTweets falls back to unfiltered when city returns empty list`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))    // city = no results
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200)) // fallback

        val tweets = fetchTweets(mockAddress(locality = "TinyTown"), service)

        assertEquals(2, tweets.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `fetchTweets makes single unfiltered call when no location name is available`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        fetchTweets(mockAddress(), service)  // all fields null

        assertEquals(1, mockWebServer.requestCount)
        val path = mockWebServer.takeRequest().path!!
        assertTrue("Should not include all[] param", !path.contains("all"))
    }

    @Test
    fun `fetchTweets uses adminArea when locality is null`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        fetchTweets(mockAddress(locality = null, adminArea = "Bavaria"), service)

        val path = mockWebServer.takeRequest().path!!
        assertTrue("Should use adminArea as city tag", path.contains("bavaria"))
    }

    @Test
    fun `fetchTweets uses subAdminArea before adminArea`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        fetchTweets(mockAddress(subAdminArea = "Shoreditch", adminArea = "London"), service)

        val path = mockWebServer.takeRequest().path!!
        assertTrue("subAdminArea should take precedence", path.contains("shoreditch"))
    }

    // ---------- error ----------

    @Test
    fun `fetchTweets propagates exception on HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = runCatching { fetchTweets(mockAddress(), service) }

        assertTrue("Should propagate HTTP exception", result.isFailure)
    }
}
