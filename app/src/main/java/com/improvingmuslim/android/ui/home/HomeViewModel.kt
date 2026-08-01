package com.improvingmuslim.android.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.model.HomeFeedItem
import com.improvingmuslim.android.model.Topic
import com.improvingmuslim.android.model.homeFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ContentType(val label: String) {
    ALL("All"),
    SERIES("Series"),
    VIDEOS("Videos"),
}

enum class SortOption(val label: String) {
    DEFAULT("Default"),
    FEATURED("Featured order"),
    MOST_VIEWED("Most viewed"),
    AZ("A–Z"),
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val topics: List<Topic>,
        val selectedTopicId: String?,
        val contentType: ContentType,
        val sort: SortOption,
        val seriesCount: Int,
        val videoCount: Int,
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

    private var baseFeed: List<HomeFeedItem> = emptyList()
    private var shuffledFeed: List<HomeFeedItem> = emptyList()
    private var topics: List<Topic> = emptyList()

    private var selectedTopicId: String? = null
    private var contentType: ContentType = ContentType.ALL
    private var sort: SortOption = SortOption.DEFAULT

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val loaded = repository.fetchCatalog()
                topics = loaded.topics
                baseFeed = loaded.homeFeed()
                // Shuffle once per load so "Default" feels fresh but stays stable while browsing.
                shuffledFeed = baseFeed.shuffled()
                selectedTopicId = null
                contentType = ContentType.ALL
                sort = SortOption.DEFAULT
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

    fun selectContentType(type: ContentType) {
        contentType = type
        emitReady()
    }

    fun selectSort(option: SortOption) {
        sort = option
        emitReady()
    }

    private fun emitReady() {
        if (baseFeed.isEmpty() && shuffledFeed.isEmpty()) return

        val ordered = when (sort) {
            SortOption.DEFAULT -> shuffledFeed
            SortOption.FEATURED -> baseFeed
            SortOption.MOST_VIEWED -> baseFeed.sortedByDescending { it.sortViews }
            SortOption.AZ -> baseFeed.sortedBy { it.title.lowercase() }
        }

        val topicFiltered = ordered.filter { item ->
            selectedTopicId == null || item.categories.contains(selectedTopicId)
        }
        val seriesCount = topicFiltered.count { it is HomeFeedItem.SeriesItem }
        val videoCount = topicFiltered.count { it is HomeFeedItem.LectureItem }

        val visible = when (contentType) {
            ContentType.ALL -> topicFiltered
            ContentType.SERIES -> topicFiltered.filterIsInstance<HomeFeedItem.SeriesItem>()
            ContentType.VIDEOS -> topicFiltered.filterIsInstance<HomeFeedItem.LectureItem>()
        }

        _uiState.value = HomeUiState.Ready(
            topics = topics,
            selectedTopicId = selectedTopicId,
            contentType = contentType,
            sort = sort,
            seriesCount = seriesCount,
            videoCount = videoCount,
            items = visible,
        )
    }
}
