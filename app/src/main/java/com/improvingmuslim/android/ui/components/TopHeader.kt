package com.improvingmuslim.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.ui.theme.Brand

/**
 * The sticky top bar. Buttons are visual placeholders for now — streak, sign-in,
 * settings, and the menu don't have working features yet.
 */
@Composable
fun TopHeader(
    modifier: Modifier = Modifier,
    onStreak: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSettings: () -> Unit = {},
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

            HeaderIcon(Icons.Filled.Whatshot, "Start your daily learning streak", onStreak)
            SignInPill(onSignIn)
            HeaderIcon(Icons.Filled.Settings, "Settings", onSettings)
            HeaderIcon(Icons.Filled.Menu, "More menu", onMenu)
        }
    }
}

@Composable
private fun Emblem() {
    val brand = Brand.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(brand.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "IM",
            color = brand.background,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    val brand = Brand.colors
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(imageVector = icon, contentDescription = description, tint = brand.muted)
    }
}

@Composable
private fun SignInPill(onClick: () -> Unit) {
    val brand = Brand.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = brand.surface,
        border = BorderStroke(1.dp, brand.line),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = brand.ink,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Sign in",
                color = brand.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
