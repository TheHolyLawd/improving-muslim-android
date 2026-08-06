package com.improvingmuslim.android.ui.history

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.data.WatchProgressStore
import com.improvingmuslim.android.data.WatchProgressStore.Progress
import com.improvingmuslim.android.model.PlayableVideo
import com.improvingmuslim.android.model.allPlayable
import com.improvingmuslim.android.model.formatDuration
import com.improvingmuslim.android.ui.components.RemoteArtwork
import com.improvingmuslim.android.ui.theme.Brand

/**
 * The Watch History screen behind the home "View history" button: every started or finished
 * lecture, most recent first. Tapping one resumes it; each row can be removed, or all cleared.
 * Mirrors the website's history page. Reads from [WatchProgressStore] (local-only for now).
 */
@Composable
fun HistoryScreen(
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val context = LocalContext.current
    var items by remember { mutableStateOf(loadHistory(context)) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(brand.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brand.ink)
            }
            Text(
                text = "Watch History",
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (items.isNotEmpty()) {
                Text(
                    text = "Clear",
                    color = brand.rose,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showClearDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
        Text(
            text = "Episodes you've started or finished, saved on this device.",
            color = brand.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        if (items.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                items(items, key = { it.progress.key }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onOpen = { onOpenVideo(entry.progress.key) },
                        onRemove = {
                            WatchProgressStore.remove(context, entry.progress.key)
                            items = loadHistory(context)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear watch history?") },
            text = { Text("This removes your progress for every video on this device. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    WatchProgressStore.clear(context)
                    items = emptyList()
                    showClearDialog = false
                }) { Text("Clear", color = brand.rose) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private data class HistoryEntry(val progress: Progress, val video: PlayableVideo)

/** Resolves each stored progress record to its catalog video, dropping any no longer present. */
private fun loadHistory(context: Context): List<HistoryEntry> {
    val byId = (CatalogRepository.cached?.allPlayable() ?: emptyList()).associateBy { it.id }
    return WatchProgressStore.all(context).mapNotNull { p -> byId[p.key]?.let { HistoryEntry(p, it) } }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val (progress, video) = entry
    val status = if (progress.completed) {
        "Completed"
    } else {
        "Resume at ${formatDuration((progress.positionMs / 1000).toInt())}"
    }
    val meta = listOfNotNull(status, relativeTime(progress.updatedAt)).joinToString(" · ")

    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            RemoteArtwork(url = video.thumbnailURL, modifier = Modifier.fillMaxSize())
            // Progress bar hugging the bottom edge.
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
                text = video.title,
                color = brand.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            video.contextLabel?.let {
                Text(text = it, color = brand.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = meta,
                color = if (progress.completed) brand.accent else brand.muted,
                fontSize = 12.sp,
                fontWeight = if (progress.completed) FontWeight.SemiBold else FontWeight.Normal,
            )
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove ${video.title} from history",
                tint = brand.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val brand = Brand.colors
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No watch history yet",
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(
                text = "Videos you start will show up here so you can pick up where you left off.",
                color = brand.muted,
                fontSize = 14.sp,
            )
        }
    }
}

/** "Just now" / "5m ago" / "3h ago" / "Yesterday" / "4 days ago" / "12 Aug". */
private fun relativeTime(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = System.currentTimeMillis() - ts
    val mins = diff / 60_000
    if (mins < 2) return "Just now"
    if (mins < 60) return "${mins}m ago"
    val hours = diff / 3_600_000
    if (hours < 24) return "${hours}h ago"
    val days = diff / 86_400_000
    if (days == 1L) return "Yesterday"
    if (days < 7) return "$days days ago"
    return android.text.format.DateFormat.format("d MMM", ts).toString()
}
