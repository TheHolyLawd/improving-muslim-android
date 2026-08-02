package com.improvingmuslim.android.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.model.PlayableVideo
import com.improvingmuslim.android.model.WatchBundle
import com.improvingmuslim.android.ui.components.RemoteArtwork
import com.improvingmuslim.android.ui.theme.Brand

@Composable
fun WatchScreen(
    bundle: WatchBundle,
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Brand.colors
    val video = bundle.video

    // One player instance that moves between the fullscreen and normal layouts without
    // being recreated (which would restart playback and reset speed/captions).
    val playerSlot = remember(video.id) {
        movableContentOf<Boolean, Modifier> { fs, mod ->
            VideoPlayer(
                video = video,
                isFullscreen = fs,
                onToggleFullscreen = onToggleFullscreen,
                modifier = mod,
            )
        }
    }

    if (isFullscreen) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
            playerSlot(true, Modifier.fillMaxSize())
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth().background(brand.background)) {
        // Video sits on a black stage with a floating back button.
        Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
            playerSlot(false, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(4.dp).size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header(video)
            video.description?.takeIf { it.isNotBlank() }?.let { Description(it) }

            if (video.takeaways.isNotEmpty()) {
                CollapsibleSection(title = "Key Takeaways") { Takeaways(video.takeaways) }
            }
            video.recap?.takeIf { it.isNotBlank() }?.let { recap ->
                CollapsibleSection(title = "Recap") { Recap(recap) }
            }

            bundle.upNext?.let { UpNext(it, onOpenVideo) }
            if (bundle.related.isNotEmpty()) MoreLikeThis(bundle.related, onOpenVideo)
        }
    }
}

@Composable
private fun Header(video: PlayableVideo) {
    val brand = Brand.colors
    val meta = listOfNotNull(video.speaker, video.contextLabel, video.publishedLabel)
        .joinToString(" · ")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = video.title,
            color = brand.ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
        )
        Text(text = meta, color = brand.muted, fontSize = 14.sp)
    }
}

@Composable
private fun Description(text: String) {
    Text(text = text, color = Brand.colors.ink, fontSize = 15.sp, lineHeight = 22.sp)
}

/** A titled block that stays collapsed until the user taps it open. */
@Composable
private fun CollapsibleSection(title: String, content: @Composable () -> Unit) {
    val brand = Brand.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, brand.line, shape)
            .background(brand.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = brand.muted,
            )
        }
        if (expanded) {
            Box(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun Takeaways(items: List<String>) {
    val brand = Brand.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { raw ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "•", color = brand.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = stripEmphasis(raw),
                    color = brand.ink,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun Recap(text: String) {
    val brand = Brand.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        text.split("\n\n").filter { it.isNotBlank() }.forEach { paragraph ->
            Text(
                text = stripEmphasis(paragraph.trim()),
                color = brand.ink,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun UpNext(video: PlayableVideo, onOpenVideo: (String) -> Unit) {
    val brand = Brand.colors
    val shape = RoundedCornerShape(10.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "UP NEXT",
            color = brand.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { onOpenVideo(video.id) }
                .border(1.dp, brand.line, shape),
            color = brand.strongSurface,
            shape = shape,
        ) {
            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Thumb(video)
                VideoRowText(video)
            }
        }
    }
}

@Composable
private fun MoreLikeThis(items: List<PlayableVideo>, onOpenVideo: (String) -> Unit) {
    val brand = Brand.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "More like this",
            color = brand.ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
        )
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVideo(item.id) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Thumb(item)
                VideoRowText(item)
            }
        }
    }
}

@Composable
private fun Thumb(video: PlayableVideo) {
    RemoteArtwork(
        url = video.thumbnailURL,
        modifier = Modifier
            .width(132.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(6.dp)),
    )
}

@Composable
private fun VideoRowText(video: PlayableVideo) {
    val brand = Brand.colors
    val secondLine = listOfNotNull(video.speaker, video.durationLabel).joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = video.title,
            color = brand.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = secondLine, color = brand.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The feed uses lightweight **bold** markers; drop them for plain native text. */
private fun stripEmphasis(text: String): String = text.replace("**", "").replace("__", "")
