package com.boom.harmix.extractor

import com.chaquo.python.PyException
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

// THIS IS WHAT I FORGOT! The blueprint for your songs.
data class StreamItem(
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val uploader: String
)

@Singleton
class YtDlpSearchRepository @Inject constructor() {

    suspend fun search(query: String): List<StreamItem> = withContext(Dispatchers.IO) {
        val python = Python.getInstance()
        val extractorModule = python.getModule("extractor")

        try {
            val jsonResult = extractorModule.callAttr("search", query).toString()
            parseStreamItems(jsonResult)
        } catch (e: PyException) {
            throw RuntimeException("yt-dlp search failed: ${e.message}", e)
        }
    }

    suspend fun getTrending(): List<StreamItem> = withContext(Dispatchers.IO) {
        try {
            val python = Python.getInstance()
            val extractorModule = python.getModule("extractor")
            val jsonResult = extractorModule.callAttr("search", "latest trending music").toString()
            parseStreamItems(jsonResult)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseStreamItems(jsonArrayString: String): List<StreamItem> {
        val array = JSONArray(jsonArrayString)
        val items = mutableListOf<StreamItem>()

        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val url = entry.optString("url")
            if (url.isBlank()) continue

            items.add(
                StreamItem(
                    title = entry.optString("title", "Unknown title"),
                    url = url,
                    thumbnailUrl = entry.optString("thumbnailUrl").ifBlank { null },
                    uploader = entry.optString("uploader", "")
                )
            )
        }
        return items
    }
}
