package com.boom.harmix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.IOException
import okhttp3.Request as OkRequest

@HiltAndroidApp
class HarmixApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Localization + ContentCountry are required for kiosk extractors
        // (Trending, etc.) to resolve!
        NewPipe.init(
            HarmixDownloader.getInstance(),
            Localization("en"),
            ContentCountry("IN")
        )
    }
}

class HarmixDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        @Volatile
        private var instance: HarmixDownloader? = null

        fun getInstance(): HarmixDownloader =
            instance ?: synchronized(this) {
                instance ?: HarmixDownloader(
                    OkHttpClient.Builder().build()
                ).also { instance = it }
            }
    }

    @Throws(IOException::class)
    override fun execute(request: NPRequest): NPResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val body = dataToSend?.toRequestBody("application/octet-stream".toMediaTypeOrNull())

        var builder = OkRequest.Builder()
            .url(url)
            .method(httpMethod, body)

        for ((key, values) in headers) {
            if (values.isEmpty()) continue
            builder = builder.removeHeader(key)
            for (value in values) {
                builder = builder.addHeader(key, value)
            }
        }

        if (headers["User-Agent"].isNullOrEmpty()) {
            builder = builder.header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) HarmixApp/0.1"
            )
        }

        client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            return NPResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString()
            )
        }
    }
}
