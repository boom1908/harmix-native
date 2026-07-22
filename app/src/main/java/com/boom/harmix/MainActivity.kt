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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var currentSongTitle by mutableStateOf("Nothing playing")
    private var isPlaying by mutableStateOf(false)

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
                        isPlaying = isPlaying,
                        onPlayPauseClick = ::togglePlayPause
                    )
                }
            }
        }
    }

    private fun attachPlayerListener() {
        mediaController?.addListener(object : Player.Listener {

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
            }
        })
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
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()

        currentSongTitle = item.title
    }

    private fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}
