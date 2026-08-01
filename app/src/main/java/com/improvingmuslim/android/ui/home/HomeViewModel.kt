package com.improvingmuslim.android.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.model.LectureItem
import com.improvingmuslim.android.model.playableItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val items: List<LectureItem>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: CatalogRepository = CatalogRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val catalog = repository.fetchCatalog()
                HomeUiState.Ready(catalog.playableItems())
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Catalog fetch failed", e)
                HomeUiState.Error("The lecture library could not be loaded. Please try again.")
            }
        }
    }
}
