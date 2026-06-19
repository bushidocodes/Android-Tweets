package edu.gwu.androidtweets

import edu.gwu.androidtweets.api.dto.AccountDto
import edu.gwu.androidtweets.api.dto.StatusDto
import edu.gwu.androidtweets.api.dto.toTweet
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    private fun makeStatus(
        content: String = "<p>Hello #Android</p>",
        displayName: String = "Alice Dev",
        username: String = "alice",
        acct: String = "alice@mastodon.social",
        avatar: String = "https://example.com/alice.jpg"
    ) = StatusDto(
        id = "1",
        content = content,
        account = AccountDto(
            username = username,
            acct = acct,
            displayName = displayName,
            avatar = avatar
        )
    )

    @Test
    fun `toTweet maps display_name to username`() {
        val tweet = makeStatus(displayName = "Alice Dev").toTweet()
        assertEquals("Alice Dev", tweet.username)
    }

    @Test
    fun `toTweet falls back to username when display_name is empty`() {
        val tweet = makeStatus(displayName = "", username = "alice").toTweet()
        assertEquals("alice", tweet.username)
    }

    @Test
    fun `toTweet prefixes acct with at-sign for handle`() {
        val tweet = makeStatus(acct = "alice@mastodon.social").toTweet()
        assertEquals("@alice@mastodon.social", tweet.handle)
    }

    @Test
    fun `toTweet strips HTML tags from content`() {
        val tweet = makeStatus(content = "<p>Hello <strong>Android</strong></p>").toTweet()
        assertEquals("Hello Android", tweet.content)
    }

    @Test
    fun `toTweet decodes HTML entities`() {
        val tweet = makeStatus(content = "<p>A &amp; B &lt;3 &quot;quoted&quot;</p>").toTweet()
        assertEquals("""A & B <3 "quoted"""", tweet.content)
    }

    @Test
    fun `toTweet strips anchor tags leaving link text`() {
        val tweet = makeStatus(
            content = """<p>Check <a href="https://mastodon.social/tags/Android">#Android</a></p>"""
        ).toTweet()
        assertEquals("Check #Android", tweet.content)
    }

    @Test
    fun `toTweet maps avatar to iconUrl`() {
        val tweet = makeStatus(avatar = "https://cdn.example.com/avatar.jpg").toTweet()
        assertEquals("https://cdn.example.com/avatar.jpg", tweet.iconUrl)
    }

    @Test
    fun `toTweet trims leading and trailing whitespace from content`() {
        val tweet = makeStatus(content = "<p>  spaced  </p>").toTweet()
        assertEquals("spaced", tweet.content)
    }

    @Test
    fun `full round-trip from fixture JSON fields`() {
        val status = StatusDto(
            id = "111111111111111111",
            content = "<p>Just published my new Android app! <a href=\"https://mastodon.social/tags/Android\">#<span>Android</span></a></p>",
            account = AccountDto(
                username = "androiddev",
                acct = "androiddev@mastodon.social",
                displayName = "Android Developer",
                avatar = "https://files.mastodon.social/accounts/avatars/1001/original/avatar.png"
            )
        )

        val tweet = status.toTweet()

        assertEquals("Android Developer", tweet.username)
        assertEquals("@androiddev@mastodon.social", tweet.handle)
        assertEquals("Just published my new Android app! #Android", tweet.content)
        assertEquals("https://files.mastodon.social/accounts/avatars/1001/original/avatar.png", tweet.iconUrl)
        assertEquals("111111111111111111", tweet.id)
    }
}
