package com.boom.harmix.extractor

import com.chaquo.python.PyException
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpRepository @Inject constructor() {

    suspend fun getAudioStreamUrl(videoIdOrUrl: String): String = withContext(Dispatchers.IO) {
        val python = Python.getInstance()
        val extractorModule = python.getModule("extractor")

        try {
            val result = extractorModule.callAttr("get_audio_url", videoIdOrUrl)
            result.toString().ifBlank {
                throw NoSuchElementException("extractor.get_audio_url returned an empty string for $videoIdOrUrl")
            }
        } catch (e: PyException) {
            throw RuntimeException("yt-dlp extraction failed: ${e.message}", e)
        }
    }
}
