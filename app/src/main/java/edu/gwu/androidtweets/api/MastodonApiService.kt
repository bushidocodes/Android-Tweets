package edu.gwu.androidtweets.api

import edu.gwu.androidtweets.api.dto.StatusDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MastodonApiService {

    /**
     * Returns public posts tagged with [hashtag].
     * Pass [cityTag] to require posts also carry that city hashtag (e.g. "london").
     * No authentication required. Docs: https://docs.joinmastodon.org/methods/timelines/#tag
     */
    @GET("v1/timelines/tag/{hashtag}")
    suspend fun tagTimeline(
        @Path("hashtag") hashtag: String,
        @Query("all[]") cityTag: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("max_id") maxId: String? = null
    ): List<StatusDto>
}
