package com.improvingmuslim.android.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.model.Catalog
import com.improvingmuslim.android.model.HomeFeedItem
import com.improvingmuslim.android.model.Topic
import com.improvingmuslim.android.model.homeFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val topics: List<Topic>,
        val selectedTopicId: String?,
        val items: List<HomeFeedItem>,
    ) : HomeUiState {
        val selectedTopicName: String? = topics.firstOrNull { it.id == selectedTopicId }?.name
    }

    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: CatalogRepository = CatalogRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var catalog: Catalog? = null
    private var discoveryOrder: List<HomeFeedItem> = emptyList()
    private var selectedTopicId: String? = null

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val loaded = repository.fetchCatalog()
                catalog = loaded
                // Shuffle once per load so the feed feels fresh but stays stable while browsing.
                discoveryOrder = loaded.homeFeed().shuffled()
                selectedTopicId = null
                emitReady()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Catalog fetch failed", e)
                _uiState.value =
                    HomeUiState.Error("The lecture library could not be loaded. Please try again.")
            }
        }
    }

    fun selectTopic(topicId: String?) {
        selectedTopicId = topicId
        emitReady()
    }

    private fun emitReady() {
        val loaded = catalog ?: return
        val items = discoveryOrder.filter { item ->
            selectedTopicId == null || item.categories.contains(selectedTopicId)
        }
        _uiState.value = HomeUiState.Ready(
            topics = loaded.topics,
            selectedTopicId = selectedTopicId,
            items = items,
        )
    }
}
