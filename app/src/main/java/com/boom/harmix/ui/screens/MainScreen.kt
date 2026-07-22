package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.boom.harmix.navigation.HarmixNavHost
import com.boom.harmix.navigation.bottomNavItems
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.ZenCyan

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column {
                MiniPlayerPlaceholder()
                HarmixBottomBar(navController)
            }
        }
    ) { innerPadding ->
        HarmixNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun MiniPlayerPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = "Nothing playing", color = CoolGray)
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
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
