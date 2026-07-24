package com.boom.harmix.metadata

import com.boom.harmix.extractor.StreamItem
import com.chaquo.python.PyException
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepository @Inject constructor() {

    suspend fun getUpNext(videoId: String, limit: Int = 10): List<StreamItem> =
        withContext(Dispatchers.IO) {
            val python = Python.getInstance()
            val metadataModule = python.getModule("metadata_engine")

            try {
                val jsonResult = metadataModule.callAttr("get_up_next", videoId, limit).toString()
                parseUpNextResults(jsonResult)
            } catch (e: PyException) {
                throw RuntimeException("ytmusicapi get_up_next failed: ${e.message}", e)
            }
        }

    private fun parseUpNextResults(jsonArrayString: String): List<StreamItem> {
        val array = JSONArray(jsonArrayString)
        val items = mutableListOf<StreamItem>()

        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val videoId = entry.optString("videoId")
            if (videoId.isBlank()) continue

            items.add(
                StreamItem(
                    title = entry.optString("title", "Unknown title"),
                    url = "https://www.youtube.com/watch?v=$videoId",
                    thumbnailUrl = entry.optString("thumbnailUrl").ifBlank { null },
                    uploader = entry.optString("artist", "")
                )
            )
        }
        return items
    }
}
