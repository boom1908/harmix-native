package com.boom.harmix

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.playback.HarmixPlaybackService
import com.boom.harmix.ui.screens.MainScreen
import com.boom.harmix.ui.theme.HarmixTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var currentSongTitle by mutableStateOf("Nothing playing")
    private var currentArtist by mutableStateOf("")
    private var currentArtworkUrl by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var currentPositionMs by mutableLongStateOf(0L)
    private var durationMs by mutableLongStateOf(0L)
    private var canSkipNext by mutableStateOf(false)
    private var canSkipPrevious by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionToken = SessionToken(
            this,
            ComponentName(this, HarmixPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync().also { future ->
            future.addListener(
                {
                    try {
                        mediaController = future.get()
                        attachPlayerListener()
                        startPositionTicker()
                    } catch (e: Exception) {
                        Log.e("Harmix", "Failed to connect MediaController", e)
                    }
                },
                MoreExecutors.directExecutor()
            )
        }

        setContent {
            HarmixTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        playTrack = ::playTrack,
                        currentSongTitle = currentSongTitle,
                        currentArtist = currentArtist,
                        currentArtworkUrl = currentArtworkUrl,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        canSkipNext = canSkipNext,
                        canSkipPrevious = canSkipPrevious,
                        onPlayPauseClick = ::togglePlayPause,
                        onSkipNext = { mediaController?.seekToNext() },
                        onSkipPrevious = { mediaController?.seekToPrevious() },
                        onSeekTo = { positionMs -> mediaController?.seekTo(positionMs) }
                    )
                }
            }
        }
    }

    private fun attachPlayerListener() {
        val controller = mediaController ?: return

        controller.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                Log.e("Harmix", "Player error [${error.errorCodeName}]: ${error.message}")
                currentSongTitle = "Playback error — see logs"
                Toast.makeText(
                    this@MainActivity,
                    "Player Error [${error.errorCodeName}]: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
                currentArtworkUrl = mediaItem?.mediaMetadata?.artworkUri?.toString()
                durationMs = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_AVAILABLE_COMMANDS_CHANGED
                    )
                ) {
                    durationMs = player.duration.coerceAtLeast(0L)
                    canSkipNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)
                    canSkipPrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)
                }
            }
        })
    }

    private fun startPositionTicker() {
        lifecycleScope.launch {
            while (isActive) {
                currentPositionMs = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                delay(500)
            }
        }
    }

    private fun playTrack(item: StreamItem) {
        val controller = mediaController ?: run {
            Log.e("Harmix", "playTrack called before MediaController was ready.")
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(item.url)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(item.url))
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.uploader)
                    .apply {
                        item.thumbnailUrl?.let { setArtworkUri(Uri.parse(it)) }
                    }
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        currentSongTitle = item.title
        currentArtist = item.uploader
        currentArtworkUrl = item.thumbnailUrl
    }

    private fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}
