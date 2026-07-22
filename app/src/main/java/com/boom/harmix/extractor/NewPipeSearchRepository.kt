package com.boom.harmix.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
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

    private val youtubeService = ServiceList.YouTube

    suspend fun search(query: String): List<StreamItem> = withContext(Dispatchers.IO) {
        val searchExtractor = youtubeService.getSearchExtractor(query)
        searchExtractor.fetchPage()
        searchExtractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toStreamItem() }
    }

    suspend fun getTrendingRecommendations(): List<StreamItem> = withContext(Dispatchers.IO) {
        val kioskList = youtubeService.kioskList
        val trendingExtractor = kioskList.defaultKioskExtractor
        trendingExtractor.fetchPage()
        trendingExtractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toStreamItem() }
    }

    private fun StreamInfoItem.toStreamItem(): StreamItem {
        val thumbnail = thumbnails.firstOrNull()?.url
        return StreamItem(
            title = name,
            url = url,
            thumbnailUrl = thumbnail,
            uploader = uploaderName ?: ""
        )
    }
}
