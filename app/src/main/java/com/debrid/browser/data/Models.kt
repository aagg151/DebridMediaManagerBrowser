package com.debrid.browser.data

/** A torrent entry in the Real-Debrid account (from GET /torrents). */
data class TorrentItem(
    val id: String,
    val filename: String,
    val bytes: Long,
    val host: String,
    val status: String,      // e.g. "downloaded", "downloading", "magnet_conversion", "error"
    val progress: Int,       // 0..100
    val added: String,
    val links: List<String>  // RD "restricted" links, present when status == downloaded
) {
    val isReady: Boolean get() = status.equals("downloaded", ignoreCase = true)
}

/** A single file inside a torrent (from GET /torrents/info/{id}). */
data class TorrentFile(
    val id: Int,
    val path: String,        // e.g. "/Some.Movie/Some.Movie.mkv"
    val bytes: Long,
    val selected: Boolean
) {
    val displayName: String get() = path.trimStart('/').substringAfterLast('/')
    val isVideo: Boolean
        get() = displayName.substringAfterLast('.', "").lowercase() in VIDEO_EXTS

    companion object {
        val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "m4v", "webm", "ts", "flv", "wmv", "mpg", "mpeg")
    }
}

/** Full torrent info including ordered files + matching links. */
data class TorrentInfo(
    val id: String,
    val filename: String,
    val status: String,
    val files: List<TorrentFile>,
    val links: List<String>
)

/** Result of unrestricting a link (POST /unrestrict/link) — a direct, streamable/downloadable URL. */
data class UnrestrictedLink(
    val id: String,
    val filename: String,
    val filesize: Long,
    val download: String,     // direct URL
    val mimeType: String,
    val streamable: Boolean
)

/** Real-Debrid account info (GET /user). */
data class RdUser(
    val username: String,
    val email: String,
    val premium: Boolean,
    val expiration: String
)
