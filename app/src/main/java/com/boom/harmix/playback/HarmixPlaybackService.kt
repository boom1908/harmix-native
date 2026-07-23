package com.boom.harmix.playback

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.boom.harmix.extractor.YtDlpRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import javax.inject.Inject

@AndroidEntryPoint
class HarmixPlaybackService : MediaLibraryService() {

    @Inject
    lateinit var ytDlpRepository: YtDlpRepository

    private lateinit var player: ExoPlayer
    private var librarySession: MediaLibrarySession? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        librarySession = MediaLibrarySession.Builder(this, player, HarmixLibrarySessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return librarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = librarySession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        librarySession?.run {
            player.release()
            release()
            librarySession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showErrorToast(message: String) {
        mainHandler.post {
            Toast.makeText(this@HarmixPlaybackService, "Extraction Error: $message", Toast.LENGTH_LONG).show()
        }
    }

    private inner class HarmixLibrarySessionCallback : MediaLibrarySession.Callback {

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
                val resolvedUrl = ytDlpRepository.getAudioStreamUrl(sourceIdentifier)
                item.buildUpon().setUri(resolvedUrl).build()
            } catch (e: Exception) {
                showErrorToast(e.message ?: e.toString())
                item
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<androidx.media3.session.LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("harmix_root")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Harmix")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.LibraryResult.ofItem(rootItem, params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<androidx.media3.session.LibraryResult<androidx.media3.common.util.ImmutableList<MediaItem>>> {
            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.LibraryResult.ofItemList(
                    androidx.media3.common.util.ImmutableList.of(),
                    params
                )
            )
        }
    }
}
