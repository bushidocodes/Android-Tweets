package edu.gwu.androidtweets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TweetTest {

    @Test
    fun `primary constructor sets all fields`() {
        val tweet = Tweet(
            username = "Android Central",
            handle = "@androidcentral",
            content = "Hello Android!",
            iconUrl = "https://example.com/icon.jpg"
        )

        assertEquals("Android Central", tweet.username)
        assertEquals("@androidcentral", tweet.handle)
        assertEquals("Hello Android!", tweet.content)
        assertEquals("https://example.com/icon.jpg", tweet.iconUrl)
    }

    @Test
    fun `no-arg constructor required by Firebase sets empty strings`() {
        val tweet = Tweet()

        assertEquals("", tweet.username)
        assertEquals("", tweet.handle)
        assertEquals("", tweet.content)
        assertEquals("", tweet.iconUrl)
    }

    @Test
    fun `data class equals compares by value`() {
        val a = Tweet("User", "@user", "content", "https://icon")
        val b = Tweet("User", "@user", "content", "https://icon")
        val c = Tweet("Other", "@other", "content", "https://icon")

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `data class copy overrides specified fields`() {
        val original = Tweet("User", "@user", "original content", "https://icon")
        val updated = original.copy(content = "updated content")

        assertEquals("updated content", updated.content)
        assertEquals(original.username, updated.username)
        assertEquals(original.handle, updated.handle)
        assertEquals(original.iconUrl, updated.iconUrl)
    }

    @Test
    fun `data class toString contains field values`() {
        val tweet = Tweet("User", "@user", "content", "https://icon")
        val str = tweet.toString()

        assertTrue("toString should contain username", str.contains("User"))
        assertTrue("toString should contain handle", str.contains("@user"))
        assertTrue("toString should contain content", str.contains("content"))
    }

}
