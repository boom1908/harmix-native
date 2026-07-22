package com.boom.harmix.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import javax.inject.Inject
import javax.inject.Singleton

data class StreamItem(
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val uploader: String
)

@Singleton
class NewPipeSearchRepository @Inject constructor() {

    suspend fun getTrendingRecommendations(): List<StreamItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            val kioskInfo = KioskInfo.getInfo("https://www.youtube.com/feed/trending")
            kioskInfo.relatedItems.mapNotNull { item ->
                try {
                    StreamItem(
                        title = item.name ?: "Unknown",
                        url = item.url,
                        thumbnailUrl = item.thumbnail,
                        uploader = item.uploaderName ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun search(query: String): List<StreamItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            val searchInfo = SearchInfo.getInfo(query)
            searchInfo.relatedItems.mapNotNull { item ->
                try {
                    StreamItem(
                        title = item.name ?: "Unknown",
                        url = item.url,
                        thumbnailUrl = item.thumbnail,
                        uploader = item.uploaderName ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
