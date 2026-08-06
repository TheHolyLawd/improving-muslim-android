package com.improvingmuslim.android.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.model.Episode
import com.improvingmuslim.android.model.LectureSeries
import com.improvingmuslim.android.model.formatDuration
import com.improvingmuslim.android.model.formatPublished
import com.improvingmuslim.android.ui.components.RemoteArtwork
import com.improvingmuslim.android.ui.theme.Brand

/**
 * The episode list for a series: tap an available episode to watch it. Opening an episode
 * by its `episode:<series>:<episode>` key lets the Watch screen cycle through the series
 * (and only fall back to a different video once the series ends).
 */
@Composable
fun SeriesScreen(
    series: LectureSeries,
    categoryLabel: String,
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val available = series.episodes.count { it.isAvailable }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(brand.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = brand.ink,
                    )
                }
            }
        }

        item {
            RemoteArtwork(
                url = series.thumbnailURL,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = categoryLabel,
                    color = brand.rose,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = series.title,
                    color = brand.ink,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                )
                Text(
                    text = "${series.speaker} · $available of ${series.episodeCount} available",
                    color = brand.muted,
                    fontSize = 14.sp,
                )
                series.description?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = brand.ink, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }

        items(series.episodes, key = { it.id }) { episode ->
            EpisodeRow(
                episode = episode,
                fallbackThumb = series.thumbnailURL,
                onClick = { onOpenVideo("episode:${series.id}:${episode.id}") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    fallbackThumb: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val enabled = episode.isAvailable
    val meta = if (enabled) {
        listOfNotNull(episode.duration?.let { formatDuration(it) }, formatPublished(episode.published))
            .joinToString(" · ")
    } else {
        episode.statusNote ?: "Coming soon"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = episode.number.toString(),
            color = brand.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp),
        )
        Box(contentAlignment = Alignment.Center) {
            RemoteArtwork(
                url = episode.thumbnailURL ?: fallbackThumb,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = episode.title,
                color = if (enabled) brand.ink else brand.muted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = meta, color = brand.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
