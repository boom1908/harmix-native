package com.boom.harmix.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeRepository @Inject constructor() {

    suspend fun getAudioStreamUrl(videoIdOrUrl: String): String? = withContext(Dispatchers.IO) {
        val fullUrl = normalizeToUrl(videoIdOrUrl)

        return@withContext try {
            val streamInfo = StreamInfo.getInfo(fullUrl)
            selectBestAudioStream(streamInfo.audioStreams)?.url
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeToUrl(input: String): String {
        return if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else {
            "https://www.youtube.com/watch?v=$input"
        }
    }

    private fun selectBestAudioStream(streams: List<AudioStream>): AudioStream? {
        if (streams.isEmpty()) return null

        val m4aStreams = streams.filter { it.format == MediaFormat.M4A }
        val candidates = m4aStreams.ifEmpty {
            streams.filter {
                it.format == MediaFormat.WEBMA || it.format == MediaFormat.WEBMA_OPUS
            }
        }.ifEmpty { streams }

        return candidates.maxByOrNull { it.averageBitrate }
    }
}
