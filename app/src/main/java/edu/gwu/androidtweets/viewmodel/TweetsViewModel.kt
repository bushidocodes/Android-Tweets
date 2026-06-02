package edu.gwu.androidtweets.viewmodel

import android.app.Application
import android.location.Address
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import edu.gwu.androidtweets.Tweet
import edu.gwu.androidtweets.api.MastodonApi
import edu.gwu.androidtweets.api.MastodonApiService
import edu.gwu.androidtweets.api.dto.toTweet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TweetsViewModel(
    application: Application,
    private val apiService: MastodonApiService
) : AndroidViewModel(application) {

    private val _tweets = MutableStateFlow<Result<List<Tweet>>?>(null)
    val tweets: StateFlow<Result<List<Tweet>>?> = _tweets.asStateFlow()

    fun loadTweets(address: Address) {
        viewModelScope.launch {
            _tweets.value = runCatching {
                withContext(Dispatchers.IO) { fetchTweets(address, apiService) }
            }
        }
    }

    companion object {
        /** Default factory — used by [edu.gwu.androidtweets.TweetsFragment] in production. */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T = TweetsViewModel(extras[APPLICATION_KEY]!!, MastodonApi.service) as T
        }
    }
}

/**
 * Pure business logic: city-filtered hashtag search with fallback to unfiltered #Android.
 * Extracted from the ViewModel so it can be tested without lifecycle/dispatcher complexity.
 */
internal suspend fun fetchTweets(address: Address, apiService: MastodonApiService): List<Tweet> {
    val city = address.locality
        ?: address.subAdminArea
        ?: address.adminArea
        ?: address.countryName
        ?: ""
    val cityTag = city.toHashtag()
    val filtered = if (cityTag.isNotEmpty()) {
        apiService.tagTimeline("Android", cityTag)
    } else emptyList()
    return filtered.ifEmpty { apiService.tagTimeline("Android") }.map { it.toTweet() }
}

internal fun String.toHashtag(): String = lowercase()
    .replace(Regex("[^a-z0-9]"), "")
    .trim()
