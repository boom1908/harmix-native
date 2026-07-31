package com.boom.harmix.extractor

import com.chaquo.python.PyException
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AudioStreamResult(
    val url: String,
    val durationSeconds: Int?
)

@Singleton
class YtDlpRepository @Inject constructor() {

    suspend fun getAudioStreamUrl(videoIdOrUrl: String): AudioStreamResult = withContext(Dispatchers.IO) {
        val python = Python.getInstance()
        val extractorModule = python.getModule("extractor")

        try {
            val jsonResult = extractorModule.callAttr("get_audio_url", videoIdOrUrl).toString()
            val json = JSONObject(jsonResult)

            val url = json.optString("url").ifBlank {
                throw NoSuchElementException("extractor.get_audio_url returned an empty URL for $videoIdOrUrl")
            }
            val durationSeconds = if (json.isNull("durationSeconds")) null else json.optInt("durationSeconds")

            AudioStreamResult(url = url, durationSeconds = durationSeconds)
        } catch (e: PyException) {
            throw RuntimeException("yt-dlp extraction failed: ${e.message}", e)
        }
    }
}
