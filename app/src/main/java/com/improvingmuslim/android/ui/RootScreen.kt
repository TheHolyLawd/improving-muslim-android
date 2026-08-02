package com.improvingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.improvingmuslim.android.model.PlayableVideo
import com.improvingmuslim.android.ui.home.HomeScreen
import com.improvingmuslim.android.ui.theme.Brand
import com.improvingmuslim.android.ui.watch.WatchScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    EXPLORE("Explore", Icons.Outlined.Explore),
    PATHWAYS("Pathways", Icons.Outlined.Route),
    SPEAKERS("Speakers", Icons.Outlined.Groups),
    PROFILE("Profile", Icons.Outlined.Person),
}

@Composable
fun RootScreen() {
    val brand = Brand.colors
    var selectedTab by remember { mutableStateOf(Tab.HOME) }
    var watchVideo by remember { mutableStateOf<PlayableVideo?>(null) }

    // The Watch screen is a full-screen surface over the tabs; back returns to them.
    watchVideo?.let { video ->
        BackHandler { watchVideo = null }
        WatchScreen(video = video, onBack = { watchVideo = null })
        return
    }

    Scaffold(
        containerColor = brand.background,
        bottomBar = {
            NavigationBar(containerColor = brand.surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = tab.label.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brand.accent,
                            selectedTextColor = brand.accent,
                            unselectedIconColor = brand.muted,
                            unselectedTextColor = brand.muted,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            Tab.HOME -> HomeScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenVideo = { watchVideo = it },
            )
            else -> ComingSoon(
                title = selectedTab.label,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ComingSoon(title: String, modifier: Modifier = Modifier) {
    val brand = Brand.colors
    Box(
        modifier = modifier.fillMaxSize().background(brand.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = brand.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            Text(
                text = "Coming soon",
                color = brand.muted,
                fontSize = 15.sp,
            )
        }
    }
}
