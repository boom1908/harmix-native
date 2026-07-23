package com.boom.harmix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.navigation.HarmixNavHost
import com.boom.harmix.navigation.bottomNavItems
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan

@Composable
fun MainScreen(
    playTrack: (StreamItem) -> Unit,
    currentSongTitle: String,
    currentArtist: String,
    currentArtworkUrl: String?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    onPlayPauseClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val navController = rememberNavController()
    var isFullPlayerExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                Column {
                    MiniPlayer(
                        songTitle = currentSongTitle,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onExpandClick = { isFullPlayerExpanded = true }
                    )
                    HarmixBottomBar(navController)
                }
            }
        ) { innerPadding ->
            HarmixNavHost(
                navController = navController,
                playTrack = playTrack,
                modifier = Modifier.padding(innerPadding)
            )
        }

        AnimatedVisibility(
            visible = isFullPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight })
        ) {
            FullScreenPlayerScreen(
                songTitle = currentSongTitle,
                artist = currentArtist,
                artworkUrl = currentArtworkUrl,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                canSkipNext = canSkipNext,
                canSkipPrevious = canSkipPrevious,
                onPlayPauseClick = onPlayPauseClick,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeekTo = onSeekTo,
                onCollapse = { isFullPlayerExpanded = false }
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    songTitle: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onExpandClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = songTitle,
            color = if (songTitle == "Nothing playing") CoolGray else MistWhite,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onPlayPauseClick) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = ZenCyan
            )
        }
    }
}

@Composable
private fun HarmixBottomBar(navController: androidx.navigation.NavHostController) {
    NavigationBar(
        containerColor = GlassFill,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ZenCyan,
                    selectedTextColor = ZenCyan,
                    unselectedIconColor = CoolGray,
                    unselectedTextColor = CoolGray,
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}
