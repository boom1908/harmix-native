package com.boom.harmix.extractor

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpRepository @Inject constructor() {

    suspend fun getAudioStreamUrl(videoIdOrUrl: String): String = withContext(Dispatchers.IO) {
        val fullUrl = normalizeToUrl(videoIdOrUrl)

        val request = YoutubeDLRequest(fullUrl).apply {
            addOption("-f", "bestaudio")
            addOption("-g")
            addOption("--no-playlist")
            addOption("--no-warnings")
        }

        val response = YoutubeDL.getInstance().execute(request)

        val streamUrl = response.out
            .lineSequence()
            .map { it.trim() }
            .lastOrNull { it.isNotEmpty() }

        streamUrl ?: throw NoSuchElementException(
            "yt-dlp returned no stream URL for $fullUrl. stderr: ${response.err}"
        )
    }

    private fun normalizeToUrl(input: String): String {
        return if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else {
            "https://www.youtube.com/watch?v=$input"
        }
    }
}
