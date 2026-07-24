package com.debrid.browser.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RdException(val code: Int, message: String) : Exception(message)

/**
 * Minimal Real-Debrid REST client.
 * Docs: https://api.real-debrid.com/  (base /rest/1.0)
 * Auth: personal API token as a Bearer header (https://real-debrid.com/apitoken).
 */
class RealDebridApi(private val prefs: Prefs) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun requireToken(): String {
        val t = prefs.apiToken
        if (t.isBlank()) throw RdException(401, "No Real-Debrid API token set. Add it in Settings.")
        return t
    }

    private fun newRequest(path: String): Request.Builder =
        Request.Builder()
            .url("$BASE$path")
            .header("Authorization", "Bearer ${requireToken()}")

    private fun execString(request: Request): String {
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(body).optString("error") }.getOrNull()
                throw RdException(resp.code, msg?.takeIf { it.isNotBlank() } ?: "HTTP ${resp.code}")
            }
            return body
        }
    }

    suspend fun getUser(): RdUser = withContext(Dispatchers.IO) {
        val json = JSONObject(execString(newRequest("/user").get().build()))
        RdUser(
            username = json.optString("username"),
            email = json.optString("email"),
            premium = json.optInt("premium", 0) == 1 || json.optString("type") == "premium",
            expiration = json.optString("expiration")
        )
    }

    suspend fun getTorrents(limit: Int = 100): List<TorrentItem> = withContext(Dispatchers.IO) {
        val arr = JSONArray(execString(newRequest("/torrents?limit=$limit").get().build()))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TorrentItem(
                id = o.optString("id"),
                filename = o.optString("filename"),
                bytes = o.optLong("bytes"),
                host = o.optString("host"),
                status = o.optString("status"),
                progress = o.optInt("progress"),
                added = o.optString("added"),
                links = o.optJSONArray("links").toStringList()
            )
        }
    }

    suspend fun getTorrentInfo(id: String): TorrentInfo = withContext(Dispatchers.IO) {
        val o = JSONObject(execString(newRequest("/torrents/info/$id").get().build()))
        val filesArr = o.optJSONArray("files") ?: JSONArray()
        val files = (0 until filesArr.length()).map { i ->
            val f = filesArr.getJSONObject(i)
            TorrentFile(
                id = f.optInt("id"),
                path = f.optString("path"),
                bytes = f.optLong("bytes"),
                selected = f.optInt("selected", 0) == 1
            )
        }
        TorrentInfo(
            id = o.optString("id"),
            filename = o.optString("filename"),
            status = o.optString("status"),
            files = files,
            links = o.optJSONArray("links").toStringList()
        )
    }

    /**
     * Real-Debrid returns one "link" per *selected* file, in the same order as the selected files.
     * Returns null if there is no matching restricted link (e.g. file not selected / torrent not ready).
     */
    fun restrictedLinkFor(info: TorrentInfo, file: TorrentFile): String? {
        val selected = info.files.filter { it.selected }
        val idx = selected.indexOfFirst { it.id == file.id }
        return if (idx in info.links.indices) info.links[idx] else null
    }

    suspend fun unrestrict(link: String): UnrestrictedLink = withContext(Dispatchers.IO) {
        val form = FormBody.Builder().add("link", link).build()
        val json = JSONObject(execString(newRequest("/unrestrict/link").post(form).build()))
        UnrestrictedLink(
            id = json.optString("id"),
            filename = json.optString("filename"),
            filesize = json.optLong("filesize"),
            download = json.optString("download"),
            mimeType = json.optString("mimeType"),
            streamable = json.optInt("streamable", 0) == 1
        )
    }

    suspend fun deleteTorrent(id: String) = withContext(Dispatchers.IO) {
        // DELETE returns 204 No Content on success.
        execString(newRequest("/torrents/delete/$id").delete().build())
        Unit
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }

    companion object {
        private const val BASE = "https://api.real-debrid.com/rest/1.0"
    }
}
