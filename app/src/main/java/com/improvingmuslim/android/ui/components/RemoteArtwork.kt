package com.improvingmuslim.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.improvingmuslim.android.ui.theme.Brand

/** 16:9 remote image with a calm placeholder for loading and failure states. */
@Composable
fun RemoteArtwork(url: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(Brand.colors.surface),
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand.colors.accent)
            }
        },
        // Empty/failed states simply show the calm surface background.
        error = {},
        success = { SubcomposeAsyncImageContent() },
    )
}
