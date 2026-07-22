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

        NewPipe.init(
            HarmixDownloader.getInstance(),
            Localization("en"),
            ContentCountry("IN")
        )
    }
}

class HarmixDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        private const val DEFAULT_ACCEPT_LANGUAGE = "en-US,en;q=0.9"

        private const val CONSENT_COOKIE =
            "CONSENT=YES+cb.20230214-04-p0.en+FX; SOCS=CAI;"

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
            builder = builder.header("User-Agent", DEFAULT_USER_AGENT)
        }

        if (headers["Accept-Language"].isNullOrEmpty()) {
            builder = builder.header("Accept-Language", DEFAULT_ACCEPT_LANGUAGE)
        }

        if (headers["Cookie"].isNullOrEmpty()) {
            builder = builder.header("Cookie", CONSENT_COOKIE)
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
