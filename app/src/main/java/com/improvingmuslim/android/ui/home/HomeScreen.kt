package com.improvingmuslim.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.data.WatchProgressStore
import com.improvingmuslim.android.model.HomeFeedItem
import com.improvingmuslim.android.model.PlayableVideo
import com.improvingmuslim.android.model.Topic
import com.improvingmuslim.android.model.allPlayable
import com.improvingmuslim.android.model.formatDuration
import com.improvingmuslim.android.model.toPlayableVideo
import com.improvingmuslim.android.ui.components.FeedCard
import com.improvingmuslim.android.ui.components.RemoteArtwork
import com.improvingmuslim.android.ui.components.SortDropdown
import com.improvingmuslim.android.ui.components.TopicPill
import com.improvingmuslim.android.ui.theme.Brand
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onOpenVideo: (String) -> Unit = {},
    onOpenSeries: (String) -> Unit = {},
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
                onOpenSeries = onOpenSeries,
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
    onOpenSeries: (String) -> Unit,
) {
    // Resolved once per Home mount — and Home re-mounts whenever the Watch screen closes,
    // so returning from a video refreshes the "Continue learning" card.
    val context = LocalContext.current
    val resume = remember { WatchProgressStore.mostRecentResumable(context) }
    val resumeVideo = remember(resume) {
        resume?.let { p -> CatalogRepository.cached?.allPlayable()?.firstOrNull { it.id == p.key } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (resume != null && resumeVideo != null) {
            item {
                ContinueLearning(
                    video = resumeVideo,
                    progress = resume,
                    onResume = { onOpenVideo(resumeVideo.id) },
                    onViewHistory = {}, // placeholder — history screen not built yet
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

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
                onClick = {
                    when (item) {
                        is HomeFeedItem.SeriesItem -> onOpenSeries(item.series.id)
                        is HomeFeedItem.LectureItem ->
                            item.lecture.toPlayableVideo()?.id?.let(onOpenVideo)
                    }
                },
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

/**
 * The "Continue learning" card at the top of Home: the most recently watched, unfinished
 * lecture with its resume point, mirroring the website. Tapping it reopens the video, which
 * seeks back to where the viewer left off. "View history" is a placeholder for now.
 */
@Composable
private fun ContinueLearning(
    video: PlayableVideo,
    progress: WatchProgressStore.Progress,
    onResume: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val percent = (progress.percent * 100).roundToInt()
    val minutesLeft = ((progress.durationMs - progress.positionMs) / 60000.0).roundToInt().coerceAtLeast(1)
    val resumeTime = formatDuration((progress.positionMs / 1000).toInt())
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Continue learning",
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onViewHistory)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    text = "View history",
                    color = brand.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = brand.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(onClick = onResume)
                .border(1.dp, brand.line, shape),
            color = brand.strongSurface,
            shape = shape,
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(132.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    RemoteArtwork(url = video.thumbnailURL, modifier = Modifier.fillMaxSize())
                    Text(
                        text = resumeTime,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                    // Progress bar hugging the bottom edge of the thumbnail.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.35f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.percent)
                                .fillMaxHeight()
                                .background(brand.accent),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = video.speaker,
                        color = brand.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = video.title,
                        color = brand.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$percent% watched · $minutesLeft min left",
                        color = brand.muted,
                        fontSize = 12.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = brand.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Resume",
                            color = brand.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
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
