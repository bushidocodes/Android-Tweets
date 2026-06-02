package edu.gwu.androidtweets.viewmodel

import android.app.Application
import android.location.Address
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.gwu.androidtweets.Tweet
import edu.gwu.androidtweets.api.MastodonApi
import edu.gwu.androidtweets.api.dto.toTweet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TweetsViewModel(application: Application) : AndroidViewModel(application) {

    private val _tweets = MutableStateFlow<Result<List<Tweet>>?>(null)
    val tweets: StateFlow<Result<List<Tweet>>?> = _tweets.asStateFlow()

    fun loadTweets(address: Address) {
        viewModelScope.launch {
            _tweets.value = runCatching {
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.countryName
                    ?: ""
                val cityTag = city.toHashtag()
                withContext(Dispatchers.IO) {
                    val filtered = if (cityTag.isNotEmpty()) {
                        MastodonApi.service.tagTimeline("Android", cityTag)
                    } else emptyList()
                    filtered.ifEmpty { MastodonApi.service.tagTimeline("Android") }
                }.map { it.toTweet() }
            }
        }
    }
}

private fun String.toHashtag(): String = lowercase()
    .replace(Regex("[^a-z0-9]"), "")
    .trim()
