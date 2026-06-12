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

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var cityTag: String? = null
    private var lastId: String? = null

    fun loadTweets(address: Address) {
        viewModelScope.launch {
            _tweets.value = runCatching {
                withContext(Dispatchers.IO) {
                    val result = fetchInitialTweets(address, apiService)
                    cityTag = result.cityTag
                    lastId = result.tweets.lastOrNull()?.id
                    result.tweets
                }
            }
        }
    }

    fun loadMoreTweets() {
        val currentLastId = lastId ?: return
        val currentList = _tweets.value?.getOrNull() ?: return
        if (_isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    apiService.tagTimeline("Android", cityTag, maxId = currentLastId).map { it.toTweet() }
                }
            }.onSuccess { newTweets ->
                if (newTweets.isNotEmpty()) {
                    lastId = newTweets.last().id
                    _tweets.value = Result.success(currentList + newTweets)
                }
            }
            _isLoadingMore.value = false
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

internal data class InitialFetchResult(
    val tweets: List<Tweet>,
    val cityTag: String?
)

/**
 * Pure business logic: city-filtered hashtag search with fallback to unfiltered #Android.
 * Returns both the tweets and the city tag that was actually used (null = unfiltered fallback).
 * Extracted from the ViewModel so it can be tested without lifecycle/dispatcher complexity.
 */
internal suspend fun fetchInitialTweets(address: Address, apiService: MastodonApiService): InitialFetchResult {
    val city = address.locality
        ?: address.subAdminArea
        ?: address.adminArea
        ?: address.countryName
        ?: ""
    val cityTag = city.toHashtag()
    return if (cityTag.isNotEmpty()) {
        val filtered = apiService.tagTimeline("Android", cityTag)
        if (filtered.isNotEmpty()) {
            InitialFetchResult(filtered.map { it.toTweet() }, cityTag)
        } else {
            InitialFetchResult(apiService.tagTimeline("Android").map { it.toTweet() }, null)
        }
    } else {
        InitialFetchResult(apiService.tagTimeline("Android").map { it.toTweet() }, null)
    }
}

internal fun String.toHashtag(): String = lowercase()
    .replace(Regex("[^a-z0-9]"), "")
    .trim()
