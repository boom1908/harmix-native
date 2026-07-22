package com.boom.harmix.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.ui.screens.HomeScreen
import com.boom.harmix.ui.screens.SearchScreen

sealed class HarmixDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : HarmixDestination("home", "Home", Icons.Filled.Home)
    data object Search : HarmixDestination("search", "Search", Icons.Filled.Search)
    data object Library : HarmixDestination("library", "Library", Icons.Filled.List)
}

val bottomNavItems = listOf(
    HarmixDestination.Home,
    HarmixDestination.Search,
    HarmixDestination.Library
)

@Composable
fun HarmixNavHost(
    navController: NavHostController,
    playTrack: (StreamItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HarmixDestination.Home.route,
        modifier = modifier
    ) {
        composable(HarmixDestination.Home.route) {
            HomeScreen(onItemClick = playTrack)
        }
        composable(HarmixDestination.Search.route) {
            SearchScreen(onItemClick = playTrack)
        }
        composable(HarmixDestination.Library.route) {
            Text(
                text = "Library — coming soon",
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
