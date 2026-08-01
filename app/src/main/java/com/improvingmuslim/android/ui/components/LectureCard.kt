package com.improvingmuslim.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.model.LectureItem
import com.improvingmuslim.android.ui.theme.Brand

@Composable
fun LectureCard(item: LectureItem, modifier: Modifier = Modifier) {
    val brand = Brand.colors
    val cardShape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(1.dp, brand.line, cardShape)
            .clearAndSetSemantics {
                contentDescription =
                    "${item.title}, ${item.context}, by ${item.speaker}"
            },
        color = brand.strongSurface,
        shape = cardShape,
        shadowElevation = 3.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.BottomEnd,
            ) {
                RemoteArtwork(
                    url = item.thumbnailURL,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                item.duration?.let { duration ->
                    Text(
                        text = durationLabel(duration),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
            ) {
                Text(
                    text = item.context.uppercase(),
                    color = brand.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.title,
                    color = brand.ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    text = item.speaker,
                    color = brand.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

private fun durationLabel(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remaining)
    } else {
        "%d:%02d".format(minutes, remaining)
    }
}
