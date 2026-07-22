package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.viewmodel.HomeUiState
import com.boom.harmix.ui.viewmodel.HomeViewModel
import java.util.Calendar

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadRecommendations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
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

        when (uiState) {
            is HomeUiState.Loading -> Text(text = "Loading recommendations…", color = CoolGray)
            is HomeUiState.Error -> Text(text = "Error loading recommendations", color = CoolGray)
            is HomeUiState.Success -> {
                val items = (uiState as HomeUiState.Success).items
                LazyRow(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(items) { item ->
                        RecommendationCard(
                            title = item.title,
                            thumbnailUrl = item.thumbnailUrl,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(title: String, thumbnailUrl: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(model = thumbnailUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
        }
        Text(
            text = title,
            color = MistWhite,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp)
        )
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
