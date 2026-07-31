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
import com.boom.harmix.metadata.LyricsResult
import com.boom.harmix.metadata.MetadataRepository
import com.boom.harmix.playback.HarmixPlaybackService
import com.boom.harmix.playback.QueueItemUi
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

    @Inject
    lateinit var metadataRepository: MetadataRepository

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var currentSongTitle by mutableStateOf("Nothing playing")
    private var currentArtist by mutableStateOf("")
    private var currentArtworkUrl by mutableStateOf<String?>(null)
    private var currentTrackUrl by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var isBuffering by mutableStateOf(false)
    private var currentPositionMs by mutableLongStateOf(0L)
    private var durationMs by mutableLongStateOf(0L)
    private var canSkipNext by mutableStateOf(false)
    private var canSkipPrevious by mutableStateOf(false)
    private var playlists by mutableStateOf<List<PlaylistUi>>(emptyList())
    private var isGuest by mutableStateOf(true)
    private var queueItems by mutableStateOf<List<QueueItemUi>>(emptyList())
    private var playlistDialogTarget by mutableStateOf<StreamItem?>(null)
    
    private var lyricsResult by mutableStateOf<LyricsResult?>(null)
    private var isExtendingQueue = false

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
                        syncCurrentPlayerState() // Instant state sync on wake/reconnect
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
                        onPlayNext = ::playNext,
                        onAddToQueue = ::addToQueue,
                        currentSongTitle = currentSongTitle,
                        currentArtist = currentArtist,
                        currentArtworkUrl = currentArtworkUrl,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        canSkipNext = canSkipNext,
                        canSkipPrevious = canSkipPrevious,
                        queueItems = queueItems,
                        playlists = playlists,
                        isGuest = isGuest,
                        lyricsResult = lyricsResult,
                        onSignIn = { isGuest = false },
                        onSignOut = { isGuest = true },
                        onPlayPauseClick = ::togglePlayPause,
                        onSkipNext = { mediaController?.seekToNext() },
                        onSkipPrevious = { mediaController?.seekToPrevious() },
                        onSeekTo = { positionMs -> mediaController?.seekTo(positionMs) },
                        onQueueItemClick = { index -> mediaController?.seekTo(index, 0L) },
                        onQueueItemRemove = ::removeQueueItem,
                        onLyricsClick = { 
                            if (lyricsResult == null) fetchLyricsForCurrentTrack() 
                        },
                        playlistDialogTarget = playlistDialogTarget,
                        currentTrackForPlaylist = currentStreamItemOrNull(),
                        onAddToPlaylistRequest = { item -> playlistDialogTarget = item },
                        onDismissPlaylistDialog = { playlistDialogTarget = null },
                        onSelectPlaylistForTarget = ::addTargetToPlaylist,
                        onCreatePlaylistForTarget = ::createPlaylistAndAddTarget
                    )
                }
            }
        }
    }

    private fun syncCurrentPlayerState() {
        val controller = mediaController ?: return
        isPlaying = controller.isPlaying
        isBuffering = (controller.playbackState == Player.STATE_BUFFERING)
        durationMs = controller.duration.coerceAtLeast(0L)
        
        val activeItem = controller.currentMediaItem
        if (activeItem != null) {
            currentSongTitle = activeItem.mediaMetadata.title?.toString() ?: "Nothing playing"
            currentArtist = activeItem.mediaMetadata.artist?.toString() ?: ""
            currentArtworkUrl = activeItem.mediaMetadata.artworkUri?.toString()
            currentTrackUrl = activeItem.mediaId
        }
        refreshQueueState()
    }

    private fun fetchLyricsForCurrentTrack() {
        val title = currentSongTitle
        val artist = currentArtist
        val durationSec = (durationMs / 1000L).toInt()

        lifecycleScope.launch {
            lyricsResult = metadataRepository.getLyrics(title, artist, durationSec)
        }
    }

    private fun observePlaylists() {
        lifecycleScope.launch {
            libraryRepository.getPlaylists().collect { list -> playlists = list }
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
                if (playing) isBuffering = false
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
                currentArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
                currentArtworkUrl = mediaItem?.mediaMetadata?.artworkUri?.toString()
                currentTrackUrl = mediaItem?.mediaId
                durationMs = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                lyricsResult = null
                isExtendingQueue = false
                refreshQueueState()
                maybeExtendQueue()
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                refreshQueueState()
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
                mediaController?.let { controller ->
                    // 1. Force isPlaying state directly from the audio engine
                    isPlaying = controller.isPlaying
                    
                    // 2. If sound is moving, strictly block the buffering spinner
                    isBuffering = if (isPlaying) false else (controller.playbackState == androidx.media3.common.Player.STATE_BUFFERING)
                    
                    // 3. Constantly poll duration. (ExoPlayer returns a negative number if it hasn't parsed the stream length yet)
                    val dur = controller.duration
                    durationMs = if (dur < 0L) 0L else dur
                    
                    // 4. Update the slider position
                    currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun refreshQueueState() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex

        queueItems = (0 until controller.mediaItemCount).map { index ->
            val item = controller.getMediaItemAt(index)
            QueueItemUi(
                index = index,
                title = item.mediaMetadata.title?.toString() ?: "Unknown title",
                thumbnailUrl = item.mediaMetadata.artworkUri?.toString(),
                isCurrent = index == currentIndex
            )
        }
    }

    private fun maybeExtendQueue() {
        val controller = mediaController ?: return
        if (isExtendingQueue) return

        val remaining = controller.mediaItemCount - 1 - controller.currentMediaItemIndex
        if (remaining > 2) return

        val seedUrl = currentTrackUrl ?: return
        val videoId = extractVideoId(seedUrl) ?: return

        isExtendingQueue = true
        lifecycleScope.launch {
            try {
                val related = metadataRepository.getUpNext(videoId, limit = 10)
                val existingUrls = (0 until controller.mediaItemCount)
                    .map { controller.getMediaItemAt(it).mediaId }
                    .toSet()

                val newItems = related.filter { it.url !in existingUrls }
                newItems.forEach { item -> controller.addMediaItem(item.toMediaItem()) }
                refreshQueueState()
            } catch (e: Exception) {
                Log.e("Harmix", "Autoplay queue extension failed: ${e.message}", e)
            }
        }
    }

    private fun extractVideoId(url: String): String? =
        runCatching { Uri.parse(url).getQueryParameter("v") }.getOrNull()

    private fun currentStreamItemOrNull(): StreamItem? {
        val url = currentTrackUrl ?: return null
        return StreamItem(title = currentSongTitle, url = url, thumbnailUrl = currentArtworkUrl, uploader = currentArtist)
    }

    private fun addTargetToPlaylist(playlistId: Long) {
        val item = playlistDialogTarget ?: return
        lifecycleScope.launch {
            libraryRepository.addSongToPlaylist(playlistId, item)
            Toast.makeText(this@MainActivity, "Added to playlist", Toast.LENGTH_SHORT).show()
            playlistDialogTarget = null
        }
    }

    private fun createPlaylistAndAddTarget(name: String) {
        if (name.isBlank()) return
        val item = playlistDialogTarget ?: return
        lifecycleScope.launch {
            val newPlaylistId = libraryRepository.createPlaylist(name)
            libraryRepository.addSongToPlaylist(newPlaylistId, item)
            Toast.makeText(this@MainActivity, "Created \"$name\" and added track", Toast.LENGTH_SHORT).show()
            playlistDialogTarget = null
        }
    }

    private fun playQueue(items: List<StreamItem>, startIndex: Int) {
        val controller = mediaController ?: run {
            Log.e("Harmix", "playQueue called before MediaController was ready.")
            return
        }
        if (items.isEmpty()) return

        val mediaItems = items.map { it.toMediaItem() }
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

    private fun playNext(item: StreamItem) {
        val controller = mediaController ?: return
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
        controller.addMediaItem(insertIndex, item.toMediaItem())
        refreshQueueState()
        Toast.makeText(this, "Playing next", Toast.LENGTH_SHORT).show()
    }

    private fun addToQueue(item: StreamItem) {
        val controller = mediaController ?: return
        controller.addMediaItem(item.toMediaItem())
        refreshQueueState()
        Toast.makeText(this, "Added to queue", Toast.LENGTH_SHORT).show()
    }

    private fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        controller.removeMediaItem(index)
        refreshQueueState()
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
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(url)).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(uploader)
                .apply { thumbnailUrl?.let { setArtworkUri(Uri.parse(it)) } }
                .build()
        )
        .build()
