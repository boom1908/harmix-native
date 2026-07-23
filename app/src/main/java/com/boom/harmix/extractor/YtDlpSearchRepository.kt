package com.boom.harmix.extractor

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class StreamItem(
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val uploader: String
)

@Singleton
class YtDlpSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun ensureInitialized() {
        // These calls are synchronized. If the app is currently unzipping the engine 
        // in the background, this will safely pause the thread until it's finished.
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
    }

    suspend fun search(query: String): List<StreamItem> = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val escapedQuery = query.replace("\"", "")
        val request = YoutubeDLRequest("ytsearch10:\"$escapedQuery\"").apply {
            addOption("-J")
            addOption("--flat-playlist")
            addOption("--no-warnings")
        }

        val response = YoutubeDL.getInstance().execute(request)
        parseSearchResults(response.out)
    }

    suspend fun getTrending(): List<StreamItem> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            
            val request = YoutubeDLRequest("ytsearch10:latest trending music").apply {
                addOption("-J")
                addOption("--flat-playlist")
                addOption("--no-warnings")
            }
            val response = YoutubeDL.getInstance().execute(request)
            parseSearchResults(response.out)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseSearchResults(jsonOutput: String): List<StreamItem> {
        val root = JSONObject(jsonOutput)
        val entries = root.optJSONArray("entries") ?: return emptyList()

        val items = mutableListOf<StreamItem>()
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue

            val title = entry.optString("title", "Unknown title")
            val id = entry.optString("id", "")

            val url = entry.optString("url").ifBlank {
                entry.optString("webpage_url").ifBlank {
                    if (id.isNotBlank()) "https://www.youtube.com/watch?v=$id" else ""
                }
            }
            if (url.isBlank()) continue

            val uploader = entry.optString("uploader").ifBlank {
                entry.optString("channel", "")
            }

            items.add(
                StreamItem(
                    title = title,
                    url = url,
                    thumbnailUrl = extractBestThumbnail(entry),
                    uploader = uploader
                )
            )
        }
        return items
    }

    private fun extractBestThumbnail(entry: JSONObject): String? {
        entry.optString("thumbnail").takeIf { it.isNotBlank() }?.let { return it }

        val thumbs = entry.optJSONArray("thumbnails") ?: return null
        if (thumbs.length() == 0) return null
        return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
    }
}
