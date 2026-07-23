package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.boom.harmix.data.local.PlaylistUi
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.DeepMidnight
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan
import com.boom.harmix.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onPlayQueue: (List<StreamItem>, Int) -> Unit
) {
    val savedSongs by viewModel.savedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Your Library",
            color = MistWhite,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Songs and playlists saved on this device",
            color = CoolGray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(28.dp)) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Songs",
                        color = MistWhite,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (savedSongs.isEmpty()) {
                item {
                    EmptyStateCard(text = "Songs you save will show up here. Tap the bookmark icon on the full-screen player to save one.")
                }
            } else {
                items(savedSongs) { song ->
                    SavedSongRow(
                        song = song,
                        onClick = {
                            val index = savedSongs.indexOf(song)
                            onPlayQueue(savedSongs, index.coerceAtLeast(0))
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Playlists",
                        color = MistWhite,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Create playlist", tint = ZenCyan)
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    EmptyStateCard(text = "No playlists yet. Tap + above to create one.")
                }
            } else {
                item {
                    LazyRow(contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(playlists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = {
                                    if (playlist.songs.isNotEmpty()) {
                                        onPlayQueue(playlist.songs, 0)
                                    }
                                },
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = text, color = CoolGray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SavedSongRow(song: StreamItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = song.title, color = MistWhite, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(text = song.uploader, color = CoolGray, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LibraryMusic,
            contentDescription = playlist.name,
            tint = ZenCyan,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = playlist.name,
            color = MistWhite,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = "${playlist.songs.size} songs",
            color = CoolGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepMidnight,
        title = { Text(text = "New Playlist", color = MistWhite) },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                placeholder = { Text("Playlist name", color = CoolGray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = MistWhite,
                    unfocusedTextColor = MistWhite,
                    cursorColor = ZenCyan
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(playlistName.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = ZenCyan, contentColor = DeepMidnight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GlassFill, contentColor = MistWhite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
