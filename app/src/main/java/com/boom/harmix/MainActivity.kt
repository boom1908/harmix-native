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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.boom.harmix.data.local.LibraryRepository
import com.boom.harmix.data.local.PlaylistUi
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var libraryRepository: LibraryRepository

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var currentSongTitle by mutableStateOf("Nothing playing")
    private var currentArtist by mutableStateOf("")
    private var currentArtworkUrl by mutableStateOf<String?>(null)
    private var currentTrackUrl by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var currentPositionMs by mutableLongStateOf(0L)
    private var durationMs by mutableLongStateOf(0L)
    private var canSkipNext by mutableStateOf(false)
    private var canSkipPrevious by mutableStateOf(false)

    private var playlists by mutableStateOf<List<PlaylistUi>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observePlaylists()

        val sessionToken = SessionToken(this, ComponentName(this, HarmixPlaybackService::class.java))

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
                        playTrack = { item -> playQueue(listOf(item), 0) },
                        onPlayQueue = ::playQueue,
                        currentSongTitle = currentSongTitle,
                        currentArtist = currentArtist,
                        currentArtworkUrl = currentArtworkUrl,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        canSkipNext = canSkipNext,
                        canSkipPrevious = canSkipPrevious,
                        playlists = playlists,
                        onPlayPauseClick = ::togglePlayPause,
                        onSkipNext = { mediaController?.seekToNext() },
                        onSkipPrevious = { mediaController?.seekToPrevious() },
                        onSeekTo = { positionMs -> mediaController?.seekTo(positionMs) },
                        onAddToPlaylist = ::addCurrentTrackToPlaylist,
                        onCreatePlaylistAndAdd = ::createPlaylistAndAddCurrentTrack
                    )
                }
            }
        }
    }

    private fun observePlaylists() {
        lifecycleScope.launch {
            libraryRepository.getPlaylists().collect { list ->
                playlists = list
            }
        }
    }

    private fun attachPlayerListener() {
        val controller = mediaController ?: return

        controller.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                Log.e("Harmix", "Player error [${error.errorCodeName}]: ${error.message}")
                currentSongTitle = "Playback error — see logs"
                Toast.makeText(this@MainActivity, "Player Error [${error.errorCodeName}]: ${error.message}", Toast.LENGTH_LONG).show()
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
                currentArtworkUrl = mediaItem?.mediaMetadata?.artworkUri?.toString()
                currentTrackUrl = mediaItem?.mediaId
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

    private fun currentStreamItemOrNull(): StreamItem? {
        val url = currentTrackUrl ?: return null
        return StreamItem(
            title = currentSongTitle,
            url = url,
            thumbnailUrl = currentArtworkUrl,
            uploader = currentArtist
        )
    }

    private fun addCurrentTrackToPlaylist(playlistId: Long) {
        val item = currentStreamItemOrNull() ?: return
        lifecycleScope.launch {
            libraryRepository.addSongToPlaylist(playlistId, item)
            Toast.makeText(this@MainActivity, "Added to playlist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPlaylistAndAddCurrentTrack(name: String) {
        if (name.isBlank()) return
        val item = currentStreamItemOrNull() ?: return
        lifecycleScope.launch {
            val newPlaylistId = libraryRepository.createPlaylist(name)
            libraryRepository.addSongToPlaylist(newPlaylistId, item)
            Toast.makeText(this@MainActivity, "Created \"$name\" and added track", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playQueue(items: List<StreamItem>, startIndex: Int) {
        val controller = mediaController ?: run {
            Log.e("Harmix", "playQueue called before MediaController was ready.")
            return
        }
        if (items.isEmpty()) return

        val mediaItems = items.map { item -> item.toMediaItem() }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)

        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()

        val startItem = items[safeIndex]
        currentSongTitle = startItem.title
        currentArtist = startItem.uploader
        currentArtworkUrl = startItem.thumbnailUrl
        currentTrackUrl = startItem.url
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

private fun StreamItem.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(url)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(url)).build()
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(uploader)
                .apply { thumbnailUrl?.let { setArtworkUri(Uri.parse(it)) } }
                .build()
        )
        .build()
