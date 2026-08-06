package com.improvingmuslim.android.ui.watch

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.improvingmuslim.android.data.WatchProgressStore
import com.improvingmuslim.android.model.PlayableVideo
import com.improvingmuslim.android.model.formatDuration
import kotlinx.coroutines.delay

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)

/**
 * The video surface with our own overlaid controls: play/pause, ±10s, a scrubber, and
 * standalone captions / speed / fullscreen buttons. No ExoPlayer settings gear.
 */
@Composable
fun VideoPlayer(
    video: PlayableVideo,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Where the viewer left off (0 if new or already finished).
    val resumeAtMs = remember(video.id) {
        WatchProgressStore.get(context, video.id)?.takeIf { it.isResumable }?.positionMs ?: 0L
    }

    val player = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            val builder = MediaItem.Builder().setUri(video.videoURL)
            video.captionsURL?.let { captions ->
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(captions))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage("en")
                            .build(),
                    ),
                )
            }
            setMediaItem(builder.build())
            prepare()
            if (resumeAtMs > 0) seekTo(resumeAtMs)
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    // Seed the scrubber at the resume point so it doesn't read 0:00 before playback starts.
    var position by remember(video.id) { mutableLongStateOf(resumeAtMs) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var speedIndex by remember { mutableStateOf(0) }
    var captionsOn by remember { mutableStateOf(false) }
    val hasCaptions = video.captionsURL != null

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = player.duration
                    if (d != C.TIME_UNSET) duration = d
                    // Reflect the resumed position once the media is ready.
                    if (!scrubbing) position = player.currentPosition
                }
                if (state == Player.STATE_ENDED) {
                    WatchProgressStore.save(context, video.id, player.duration, player.duration, ended = true)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            // Persist the final spot so Home can resume it.
            WatchProgressStore.save(context, video.id, player.currentPosition, player.duration, ended = false)
            player.release()
        }
    }

    // Track the playhead while playing, and persist progress every ~5s (tick 0 records the
    // video as started right away, even for a short watch).
    LaunchedEffect(isPlaying) {
        var tick = 0
        while (isPlaying) {
            if (!scrubbing) position = player.currentPosition
            if (tick % 10 == 0) {
                WatchProgressStore.save(context, video.id, player.currentPosition, player.duration, ended = false)
            }
            tick++
            delay(500)
        }
    }

    // Auto-hide the controls a few seconds after they appear during playback.
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
        )

        // Tap anywhere to toggle the controls.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { controlsVisible = !controlsVisible },
        )

        if (controlsVisible) {
            Controls(
                isPlaying = isPlaying,
                position = if (scrubbing) scrubValue.toLong() else position,
                duration = duration,
                sliderValue = if (scrubbing) scrubValue else position.toFloat(),
                sliderMax = duration.coerceAtLeast(1L).toFloat(),
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    controlsVisible = true
                },
                onReplay10 = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
                onForward10 = { player.seekTo(player.currentPosition + 10_000) },
                onScrub = { scrubbing = true; scrubValue = it },
                onScrubFinished = {
                    player.seekTo(scrubValue.toLong())
                    position = scrubValue.toLong()
                    scrubbing = false
                },
                hasCaptions = hasCaptions,
                captionsOn = captionsOn,
                onToggleCaptions = {
                    captionsOn = !captionsOn
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsOn)
                        .setPreferredTextLanguage(if (captionsOn) "en" else null)
                        .build()
                },
                speedLabel = speedLabel(SPEEDS[speedIndex]),
                onCycleSpeed = {
                    speedIndex = (speedIndex + 1) % SPEEDS.size
                    player.setPlaybackSpeed(SPEEDS[speedIndex])
                },
                isFullscreen = isFullscreen,
                onToggleFullscreen = onToggleFullscreen,
            )
        }
    }
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    sliderValue: Float,
    sliderMax: Float,
    onPlayPause: () -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    hasCaptions: Boolean,
    captionsOn: Boolean,
    onToggleCaptions: () -> Unit,
    speedLabel: String,
    onCycleSpeed: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        // Center transport.
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlIcon(Icons.Filled.Replay10, "Rewind 10 seconds", onReplay10, size = 34.dp)
            ControlIcon(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (isPlaying) "Pause" else "Play",
                onClick = onPlayPause,
                size = 52.dp,
            )
            ControlIcon(Icons.Filled.Forward10, "Forward 10 seconds", onForward10, size = 34.dp)
        }

        // Bottom bar: scrubber + standalone buttons.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Slider(
                value = sliderValue.coerceIn(0f, sliderMax),
                onValueChange = onScrub,
                onValueChangeFinished = onScrubFinished,
                valueRange = 0f..sliderMax,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatMs(position)} · ${formatMs(duration)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(modifier = Modifier.weight(1f))
                if (hasCaptions) {
                    ControlIcon(
                        icon = if (captionsOn) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff,
                        description = if (captionsOn) "Turn captions off" else "Turn captions on",
                        onClick = onToggleCaptions,
                        size = 24.dp,
                    )
                }
                SpeedButton(label = speedLabel, onClick = onCycleSpeed)
                ControlIcon(
                    icon = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    description = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                    onClick = onToggleFullscreen,
                    size = 24.dp,
                )
            }
        }
    }
}

@Composable
private fun ControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(size))
    }
}

@Composable
private fun SpeedButton(label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun speedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${text}×"
}

private fun formatMs(ms: Long): String = formatDuration((ms / 1000).coerceAtLeast(0).toInt())
