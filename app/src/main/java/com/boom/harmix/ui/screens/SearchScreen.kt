package com.boom.harmix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.boom.harmix.ui.theme.CoolGray
import com.boom.harmix.ui.theme.GlassBorder
import com.boom.harmix.ui.theme.GlassFill
import com.boom.harmix.ui.theme.MistWhite
import com.boom.harmix.ui.theme.ZenCyan
import com.boom.harmix.ui.viewmodel.SearchUiState
import com.boom.harmix.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs or artists…", color = CoolGray) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassFill,
                unfocusedContainerColor = GlassFill,
                focusedBorderColor = ZenCyan,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = MistWhite,
                unfocusedTextColor = MistWhite
            ),
            trailingIcon = {
                IconButton(onClick = { if (query.isNotBlank()) viewModel.search(query) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = ZenCyan)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) viewModel.search(query) }),
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            when (uiState) {
                is SearchUiState.Idle -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Type to search", color = CoolGray) }
                is SearchUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Searching…", color = CoolGray) }
                is SearchUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Search error", color = CoolGray) }
                is SearchUiState.Success -> {
                    val results = (uiState as SearchUiState.Success).results
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results) { item ->
                            SearchResultRow(title = item.title, uploader = item.uploader, thumbnailUrl = item.thumbnailUrl)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(title: String, uploader: String, thumbnailUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(model = thumbnailUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(text = title, color = MistWhite, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(text = uploader, color = CoolGray, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
