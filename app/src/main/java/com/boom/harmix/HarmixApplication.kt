package com.boom.harmix

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class HarmixApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            try {
                YoutubeDL.getInstance().init(this@HarmixApplication)
                FFmpeg.getInstance().init(this@HarmixApplication)
                isReady = true
                Log.i("Harmix", "yt-dlp + ffmpeg initialized successfully.")
            } catch (e: YoutubeDLException) {
                isReady = false
                Log.e("Harmix", "yt-dlp initialization failed: ${e.message}", e)
            }
        }
    }

    companion object {
        @Volatile
        var isReady: Boolean = false
            private set
    }
}
