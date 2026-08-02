package com.improvingmuslim.android.ui.watch

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.improvingmuslim.android.model.PlayableVideo
import com.improvingmuslim.android.ui.theme.Brand

@Composable
fun WatchScreen(video: PlayableVideo, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val brand = Brand.colors

    Column(modifier = modifier.fillMaxSize().background(brand.background).systemBarsPadding()) {
        // Video sits on a black stage with a floating back button, like a normal player.
        Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
            VideoPlayer(
                video = video,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header(video)
            video.description?.takeIf { it.isNotBlank() }?.let { Description(it) }
            if (video.takeaways.isNotEmpty()) Takeaways(video.takeaways)
            video.recap?.takeIf { it.isNotBlank() }?.let { Recap(it) }
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
    val brand = Brand.colors
    Text(text = text, color = brand.ink, fontSize = 15.sp, lineHeight = 22.sp)
}

@Composable
private fun Takeaways(items: List<String>) {
    val brand = Brand.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Key Takeaways")
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
        SectionTitle("Recap")
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Brand.colors.ink,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
    )
}

/** The feed uses lightweight **bold** markers; drop them for plain native text. */
private fun stripEmphasis(text: String): String = text.replace("**", "").replace("__", "")

@Composable
private fun VideoPlayer(video: PlayableVideo, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val player = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            val builder = MediaItem.Builder().setUri(video.videoURL)
            video.captionsURL?.let { captions ->
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(captions))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage("en")
                            .build(),
                    ),
                )
            }
            setMediaItem(builder.build())
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
    )
}
