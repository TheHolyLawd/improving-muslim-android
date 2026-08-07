package com.improvingmuslim.android.ui.search

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.LaunchedEffect
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.model.HomeFeedItem
import com.improvingmuslim.android.model.SearchOutcome
import com.improvingmuslim.android.model.search
import com.improvingmuslim.android.model.toPlayableVideo
import com.improvingmuslim.android.ui.components.RemoteArtwork
import com.improvingmuslim.android.ui.theme.Brand
import kotlinx.coroutines.delay

/**
 * Search screen behind the header's search icon. As-you-type ranked search over the in-memory
 * catalog ([com.improvingmuslim.android.model.search]), split into "results" (headline hits)
 * and "Related" (buried-text hits). Tapping a result opens the video or the series list.
 */
@Composable
fun SearchScreen(
    onOpenVideo: (String) -> Unit,
    onOpenSeries: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val catalog = CatalogRepository.cached
    var query by remember { mutableStateOf("") }
    // Debounce so we don't re-score the catalog on every keystroke.
    var debounced by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(150)
        debounced = query
    }
    val outcome = remember(debounced) {
        catalog?.search(debounced) ?: SearchOutcome(emptyList(), emptyList())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val open: (HomeFeedItem) -> Unit = { item ->
        when (item) {
            is HomeFeedItem.SeriesItem -> onOpenSeries(item.series.id)
            is HomeFeedItem.LectureItem -> item.lecture.toPlayableVideo()?.id?.let(onOpenVideo)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(brand.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brand.ink)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text("Search lectures, speakers, topics", color = brand.muted) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = brand.muted) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = brand.muted)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = brand.ink,
                    unfocusedTextColor = brand.ink,
                    focusedBorderColor = brand.accent,
                    unfocusedBorderColor = brand.line,
                    cursorColor = brand.accent,
                ),
            )
        }

        when {
            debounced.isBlank() ->
                PopularSearches(onPick = { query = it })
            outcome.isEmpty ->
                Hint("No results for “$debounced”. Try a different word or spelling.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
            ) {
                items(outcome.results, key = { "r:" + it.id }) { item ->
                    ResultRow(item = item, query = debounced, onClick = { open(item) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                if (outcome.related.isNotEmpty()) {
                    item {
                        Text(
                            text = "Related",
                            color = brand.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(outcome.related, key = { "x:" + it.id }) { item ->
                        ResultRow(item = item, query = debounced, onClick = { open(item) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(item: HomeFeedItem, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = Brand.colors
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))) {
            RemoteArtwork(url = item.thumbnailURL, modifier = Modifier.fillMaxSize())
            item.badge?.let { badge ->
                Text(
                    text = badge,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.categoryLabel, color = brand.rose, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(highlight(item.title, query, brand.accent), color = brand.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(highlight(item.speaker, query, brand.accent), color = brand.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Bolds the query's words wherever they literally appear in [text] (case-insensitive). A
 *  best-effort visual cue — synonym/fuzzy matches have no literal substring, so they stay plain. */
private fun highlight(text: String, query: String, color: Color): AnnotatedString {
    val words = query.lowercase().split(Regex("[^a-z0-9']+")).filter { it.length >= 2 }
    if (words.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val mark = BooleanArray(text.length)
    for (w in words) {
        var from = lower.indexOf(w)
        while (from >= 0) {
            for (k in from until from + w.length) mark[k] = true
            from = lower.indexOf(w, from + w.length)
        }
    }
    if (mark.none { it }) return AnnotatedString(text)
    return buildAnnotatedString {
        var k = 0
        while (k < text.length) {
            val start = k
            val on = mark[k]
            while (k < text.length && mark[k] == on) k++
            val chunk = text.substring(start, k)
            if (on) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(chunk) }
            else append(chunk)
        }
    }
}

private val POPULAR = listOf("Salah", "Ramadan", "Seerah", "Dua", "Patience", "Quran", "Hajj", "Marriage")

/** Empty-state before the user types: a few tap-to-run popular searches. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularSearches(onPick: (String) -> Unit) {
    val brand = Brand.colors
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text("Popular searches", color = brand.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            POPULAR.forEach { term ->
                Text(
                    text = term,
                    color = brand.ink,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, brand.line, RoundedCornerShape(20.dp))
                        .clickable { onPick(term) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    val brand = Brand.colors
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = brand.muted, fontSize = 15.sp, fontFamily = FontFamily.Default)
    }
}
