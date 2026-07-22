package com.boom.harmix.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
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
            val service = ServiceList.YouTube
            val kioskInfo = KioskInfo.getInfo(service, "https://www.youtube.com/feed/trending")
            
            // Filter strictly for playable streams, ignoring channels/playlists
            kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>().mapNotNull { item ->
                try {
                    StreamItem(
                        title = item.name ?: "Unknown",
                        url = item.url,
                        thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
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
            val service = ServiceList.YouTube
            val searchLinkHandler = service.searchQHFactory.fromQuery(query)
            val searchInfo = SearchInfo.getInfo(service, searchLinkHandler)
            
            // Filter strictly for playable streams, ignoring channels/playlists
            searchInfo.relatedItems.filterIsInstance<StreamInfoItem>().mapNotNull { item ->
                try {
                    StreamItem(
                        title = item.name ?: "Unknown",
                        url = item.url,
                        thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
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
