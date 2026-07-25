package com.debrid.browser.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Genre(val id: Int, val name: String)

data class DiscoverItem(
    val tmdbId: Int,
    val title: String,
    val posterPath: String?,
    val year: String,
    val isMovie: Boolean
) {
    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
}

class TmdbException(message: String) : Exception(message)

/**
 * Minimal TMDB v3 client for the native Discover screen.
 * Docs: https://developer.themoviedb.org/  (auth: api_key query param)
 */
class Tmdb(private val prefs: Prefs) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun key(): String {
        val k = prefs.tmdbKey
        if (k.isBlank()) throw TmdbException("No TMDB API key set. Add it in Settings.")
        return k
    }

    private fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val urlBuilder = "$BASE$path".toHttpUrl().newBuilder()
        urlBuilder.addQueryParameter("api_key", key())
        params.forEach { (k, v) -> if (v.isNotBlank()) urlBuilder.addQueryParameter(k, v) }
        val request = Request.Builder().url(urlBuilder.build()).get().build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(body).optString("status_message") }.getOrNull()
                throw TmdbException(msg?.takeIf { it.isNotBlank() } ?: "TMDB HTTP ${resp.code}")
            }
            return JSONObject(body)
        }
    }

    suspend fun genres(isMovie: Boolean): List<Genre> = withContext(Dispatchers.IO) {
        val path = if (isMovie) "/genre/movie/list" else "/genre/tv/list"
        val arr = get(path).optJSONArray("genres") ?: return@withContext emptyList()
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Genre(o.getInt("id"), o.optString("name"))
        }
    }

    /**
     * Discover by media type, optional genre id, and optional original language ("en", "hi").
     */
    suspend fun discover(
        isMovie: Boolean,
        genreId: Int?,
        language: String?,
        page: Int
    ): List<DiscoverItem> = withContext(Dispatchers.IO) {
        val path = if (isMovie) "/discover/movie" else "/discover/tv"
        val params = buildMap {
            put("sort_by", "popularity.desc")
            put("page", page.toString())
            put("include_adult", "false")
            genreId?.let { put("with_genres", it.toString()) }
            if (!language.isNullOrBlank()) put("with_original_language", language)
        }
        parseResults(get(path, params), isMovie)
    }

    suspend fun search(isMovie: Boolean, query: String, page: Int): List<DiscoverItem> =
        withContext(Dispatchers.IO) {
            val path = if (isMovie) "/search/movie" else "/search/tv"
            parseResults(get(path, mapOf("query" to query, "page" to page.toString())), isMovie)
        }

    /** Resolve the IMDb id (ttXXXXXXX) used by DMM's /movie/{imdb} and /show/{imdb} routes. */
    suspend fun imdbId(item: DiscoverItem): String? = withContext(Dispatchers.IO) {
        val path = if (item.isMovie) "/movie/${item.tmdbId}/external_ids"
        else "/tv/${item.tmdbId}/external_ids"
        get(path).optString("imdb_id").takeIf { it.startsWith("tt") }
    }

    private fun parseResults(json: JSONObject, isMovie: Boolean): List<DiscoverItem> {
        val arr = json.optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val title = if (isMovie) o.optString("title") else o.optString("name")
            if (title.isBlank()) return@mapNotNull null
            val date = if (isMovie) o.optString("release_date") else o.optString("first_air_date")
            DiscoverItem(
                tmdbId = o.optInt("id"),
                title = title,
                posterPath = o.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                year = date.take(4),
                isMovie = isMovie
            )
        }
    }

    companion object {
        private const val BASE = "https://api.themoviedb.org/3"
    }
}
