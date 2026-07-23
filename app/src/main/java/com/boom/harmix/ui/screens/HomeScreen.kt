package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.viewmodel.HomeUiState
import com.boom.harmix.ui.viewmodel.HomeViewModel
import java.util.Calendar

private val ErrorRed = Color(0xFFFF6B6B)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onItemClick: (StreamItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadRecommendations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Removed bottom padding here so the list can scroll fully
            .padding(top = 20.dp, start = 20.dp, end = 20.dp)
    ) {
        Text(
            text = greetingForCurrentTime(),
            color = MistWhite,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Here's what's trending right now",
            color = CoolGray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Text(text = "Loading recommendations…", color = CoolGray)
            }
            is HomeUiState.Error -> {
                HomeStatusBanner(
                    title = "Trending feed failed to load",
                    detail = state.message
                )
            }
            is HomeUiState.Success -> {
                if (state.items.isEmpty()) {
                    HomeStatusBanner(
                        title = "Trending feed returned no tracks",
                        detail = "yt-dlp is either still initializing, or YouTube temporarily blocked the request."
                    )
                } else {
                    LazyColumn(
                        // 100dp bottom padding ensures the last item isn't hidden behind the mini-player
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items) { item ->
                            RecommendationCard(
                                item = item,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatusBanner(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ErrorRed.copy(alpha = 0.12f))
            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = ErrorRed,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = detail,
            color = MistWhite,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun RecommendationCard(
    item: StreamItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MistWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            if (item.uploader.isNotEmpty()) {
                Text(
                    text = item.uploader,
                    color = CoolGray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun greetingForCurrentTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
