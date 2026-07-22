package com.boom.harmix

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.boom.harmix.playback.HarmixPlaybackService
import com.boom.harmix.ui.screens.MainScreen
import com.boom.harmix.ui.theme.DeepMidnight
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.HarmixTheme
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TEST_SONG_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val logMessages = mutableStateListOf<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private fun addLog(message: String) {
        logMessages.add("[${timeFormat.format(Date())}] $message")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addLog("MainActivity created. Building MediaController...")

        val sessionToken = SessionToken(
            this,
            ComponentName(this, HarmixPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync().also { future ->
            future.addListener(
                {
                    try {
                        mediaController = future.get()
                        addLog("MediaController connected.")
                        attachPlayerListener()
                    } catch (e: Exception) {
                        addLog("ERROR: Controller failed — ${e.message}")
                    }
                },
                MoreExecutors.directExecutor()
            )
        }

        setContent {
            HarmixTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Launching MainScreen shell with our navigation tabs
                    MainScreen()
                }
            }
        }
    }

    private fun attachPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                addLog("PLAYER ERROR [${error.errorCodeName}]: ${error.message}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                addLog("State -> $stateName")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                addLog("isPlaying -> $isPlaying")
            }
        })
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}
