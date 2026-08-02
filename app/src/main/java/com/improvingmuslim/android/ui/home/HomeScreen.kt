package com.improvingmuslim.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.improvingmuslim.android.model.Topic
import com.improvingmuslim.android.model.toPlayableVideo
import com.improvingmuslim.android.ui.components.FeedCard
import com.improvingmuslim.android.ui.components.SortDropdown
import com.improvingmuslim.android.ui.components.TopicPill
import com.improvingmuslim.android.ui.theme.Brand

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onOpenVideo: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val brand = Brand.colors

    Box(modifier = modifier.fillMaxSize().background(brand.background)) {
        when (val state = uiState) {
            is HomeUiState.Loading -> LoadingState()
            is HomeUiState.Error -> ErrorState(state.message, onRetry = viewModel::loadCatalog)
            is HomeUiState.Ready -> ReadyState(
                state = state,
                onSelectTopic = viewModel::selectTopic,
                onSelectSort = viewModel::selectSort,
                onOpenVideo = onOpenVideo,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Brand.colors.accent)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val brand = Brand.colors
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(message, color = brand.ink, textAlign = TextAlign.Center)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand.accent,
                    contentColor = brand.background,
                ),
            ) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun ReadyState(
    state: HomeUiState.Ready,
    onSelectTopic: (String?) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Hero(modifier = Modifier.padding(horizontal = 16.dp)) }

        item {
            TopicStrip(
                topics = state.topics,
                selectedTopicId = state.selectedTopicId,
                onSelectTopic = onSelectTopic,
            )
        }

        item {
            FilterSortBar(
                state = state,
                onSelectSort = onSelectSort,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        items(state.items, key = { it.id }) { item ->
            FeedCard(
                item = item,
                onClick = { item.toPlayableVideo()?.id?.let(onOpenVideo) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun FilterSortBar(
    state: HomeUiState.Ready,
    onSelectSort: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${state.seriesCount} series · ${state.videoCount} videos",
            color = brand.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        SortDropdown(selected = state.sort, onSelect = onSelectSort)
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    val brand = Brand.colors
    Column(
        modifier = modifier.padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = "LEARN WITH PURPOSE",
            color = brand.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Learn Islam.\nLive it better.",
            color = brand.ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 44.sp,
        )
        Text(
            text = "Thoughtful lectures, structured series, and a calmer path back to what matters.",
            color = brand.muted,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun TopicStrip(
    topics: List<Topic>,
    selectedTopicId: String?,
    onSelectTopic: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TopicPill(
                title = "All",
                selected = selectedTopicId == null,
                onClick = { onSelectTopic(null) },
            )
        }
        items(topics, key = { it.id }) { topic ->
            TopicPill(
                title = topic.name,
                selected = selectedTopicId == topic.id,
                onClick = {
                    onSelectTopic(if (selectedTopicId == topic.id) null else topic.id)
                },
            )
        }
    }
}
