package com.boom.harmix

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.boom.harmix.playback.HarmixPlaybackService
import com.boom.harmix.ui.theme.DeepMidnight
import com.boom.harmix.ui.theme.HarmixTheme
import com.boom.harmix.ui.theme.ZenCyan
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint

private const val TEST_SONG_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionToken = SessionToken(
            this,
            ComponentName(this, HarmixPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync().also { future ->
            future.addListener(
                { mediaController = future.get() },
                MoreExecutors.directExecutor()
            )
        }

        setContent {
            HarmixTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PlayTestScreen(onPlayClick = ::playTestSong)
                }
            }
        }
    }

    private fun playTestSong() {
        val testItem = MediaItem.Builder()
            .setMediaId(TEST_SONG_URL)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(TEST_SONG_URL))
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Harmix Test Track")
                    .setArtist("Extraction Pipeline Check")
                    .build()
            )
            .build()

        mediaController?.apply {
            setMediaItem(testItem)
            prepare()
            play()
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@Composable
private fun PlayTestScreen(onPlayClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onPlayClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ZenCyan,
                contentColor = DeepMidnight
            ),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Play Test Song",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
