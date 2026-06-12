package edu.gwu.androidtweets

import android.location.Address
import edu.gwu.androidtweets.api.MastodonApi
import edu.gwu.androidtweets.api.MastodonApiService
import edu.gwu.androidtweets.viewmodel.fetchInitialTweets
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [fetchInitialTweets] — the pure business logic extracted from TweetsViewModel.
 * Tests the city-filtered search, fallback behaviour, and city-tag tracking without
 * lifecycle/dispatcher complexity.
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
    fun `fetchInitialTweets returns mapped tweets on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        val result = fetchInitialTweets(mockAddress(locality = "Berlin"), service)

        assertEquals(2, result.tweets.size)
        assertEquals("Android Developer", result.tweets[0].username)
        assertEquals("@androiddev@mastodon.social", result.tweets[0].handle)
        assertTrue(result.tweets[0].content.isNotEmpty())
        assertTrue(result.tweets[0].iconUrl.isNotEmpty())
    }

    @Test
    fun `fetchInitialTweets includes city tag in request URL`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        fetchInitialTweets(mockAddress(locality = "London"), service)

        val path = mockWebServer.takeRequest().path!!
        assertTrue("URL should contain city tag", path.contains("london"))
    }

    @Test
    fun `fetchInitialTweets returns cityTag when city results are found`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        val result = fetchInitialTweets(mockAddress(locality = "Berlin"), service)

        assertEquals("berlin", result.cityTag)
    }

    // ---------- fallback ----------

    @Test
    fun `fetchInitialTweets falls back to unfiltered when city returns empty list`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))    // city = no results
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200)) // fallback

        val result = fetchInitialTweets(mockAddress(locality = "TinyTown"), service)

        assertEquals(2, result.tweets.size)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `fetchInitialTweets returns null cityTag when fallback to unfiltered is used`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        val result = fetchInitialTweets(mockAddress(locality = "TinyTown"), service)

        assertNull("cityTag should be null when unfiltered fallback is used", result.cityTag)
    }

    @Test
    fun `fetchInitialTweets makes single unfiltered call when no location name is available`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        fetchInitialTweets(mockAddress(), service)  // all fields null

        assertEquals(1, mockWebServer.requestCount)
        val path = mockWebServer.takeRequest().path!!
        assertTrue("Should not include all[] param", !path.contains("all"))
    }

    @Test
    fun `fetchInitialTweets uses adminArea when locality is null`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        fetchInitialTweets(mockAddress(locality = null, adminArea = "Bavaria"), service)

        val path = mockWebServer.takeRequest().path!!
        assertTrue("Should use adminArea as city tag", path.contains("bavaria"))
    }

    @Test
    fun `fetchInitialTweets uses subAdminArea before adminArea`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        fetchInitialTweets(mockAddress(subAdminArea = "Shoreditch", adminArea = "London"), service)

        val path = mockWebServer.takeRequest().path!!
        assertTrue("subAdminArea should take precedence", path.contains("shoreditch"))
    }

    // ---------- pagination ----------

    @Test
    fun `fetchInitialTweets tweets carry id for pagination cursor`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(timelineJson).setResponseCode(200))

        val result = fetchInitialTweets(mockAddress(locality = "Berlin"), service)

        assertEquals("111111111111111111", result.tweets[0].id)
        assertEquals("111111111111111112", result.tweets[1].id)
    }

    // ---------- error ----------

    @Test
    fun `fetchInitialTweets propagates exception on HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = runCatching { fetchInitialTweets(mockAddress(), service) }

        assertTrue("Should propagate HTTP exception", result.isFailure)
    }
}
