package edu.gwu.androidtweets.api.dto

import edu.gwu.androidtweets.Tweet

fun StatusDto.toTweet(): Tweet = Tweet(
    username = account.displayName.ifEmpty { account.username },
    handle = "@${account.acct}",
    content = content.stripHtml(),
    iconUrl = account.avatar,
    id = id
)

private fun String.stripHtml(): String = replace(Regex("<[^>]+>"), "")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .trim()
