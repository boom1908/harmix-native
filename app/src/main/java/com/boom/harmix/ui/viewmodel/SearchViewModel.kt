package com.boom.harmix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.harmix.extractor.NewPipeSearchRepository
import com.boom.harmix.extractor.StreamItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<StreamItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: NewPipeSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = searchRepository.search(query)
                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
