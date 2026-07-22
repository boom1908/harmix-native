package com.boom.harmix.playback

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.boom.harmix.extractor.NewPipeRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import javax.inject.Inject

@AndroidEntryPoint
class HarmixPlaybackService : MediaSessionService() {

    @Inject
    lateinit var newPipeRepository: NewPipeRepository

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(HarmixSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = mediaSession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showErrorToast(message: String) {
        mainHandler.post {
            Toast.makeText(this@HarmixPlaybackService, "Extraction Error: $message", Toast.LENGTH_LONG).show()
        }
    }

    private inner class HarmixSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> =
            serviceScope.future {
                mediaItems.map { item -> resolvePlayableItem(item) }.toMutableList()
            }

        private suspend fun resolvePlayableItem(item: MediaItem): MediaItem {
            val sourceIdentifier = item.requestMetadata.mediaUri?.toString()
                ?: item.mediaId

            return try {
                val resolvedUrl = newPipeRepository.getAudioStreamUrl(sourceIdentifier)

                if (resolvedUrl != null) {
                    item.buildUpon()
                        .setUri(resolvedUrl)
                        .build()
                } else {
                    showErrorToast("No audio stream found for \"${item.mediaMetadata.title}\"")
                    item
                }
            } catch (e: Exception) {
                showErrorToast(e.message ?: e.toString())
                item
            }
        }
    }
}
