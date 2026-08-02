package com.improvingmuslim.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.R
import com.improvingmuslim.android.ui.theme.Brand

/**
 * The sticky top bar. Buttons are visual placeholders for now — search, streak, and the
 * menu don't have working features yet.
 */
@Composable
fun TopHeader(
    modifier: Modifier = Modifier,
    streakCount: Int = 0,
    onSearch: () -> Unit = {},
    onStreak: () -> Unit = {},
    onMenu: () -> Unit = {},
) {
    val brand = Brand.colors
    Surface(color = brand.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Emblem()
            Text(
                text = "Improving Muslim",
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f, fill = true),
            )

            HeaderIcon(Icons.Filled.Search, "Search", onSearch)
            StreakButton(count = streakCount, onClick = onStreak)
            HeaderIcon(Icons.Filled.Menu, "More menu", onMenu)
        }
    }
}

@Composable
private fun Emblem() {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp)),
    )
}

@Composable
private fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    val brand = Brand.colors
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(imageVector = icon, contentDescription = description, tint = brand.muted)
    }
}

@Composable
private fun StreakButton(count: Int, onClick: () -> Unit) {
    val brand = Brand.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Whatshot,
            contentDescription = "Daily learning streak",
            tint = brand.gold,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = count.toString(),
            color = brand.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
