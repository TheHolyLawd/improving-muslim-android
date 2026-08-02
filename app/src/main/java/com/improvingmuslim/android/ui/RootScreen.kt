package com.improvingmuslim.android.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.improvingmuslim.android.data.CatalogRepository
import com.improvingmuslim.android.model.buildWatchBundle
import com.improvingmuslim.android.ui.components.TopHeader
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
    var watchKey by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val closeWatch = { watchKey = null; fullscreen = false }

    // The Watch screen is a full-screen surface (with the shared header) over the tabs.
    val bundle = watchKey?.let { key -> CatalogRepository.cached?.buildWatchBundle(key) }
    if (bundle != null) {
        val activity = LocalContext.current as? Activity

        BackHandler { if (fullscreen) fullscreen = false else closeWatch() }

        // Landscape + immersive while fullscreen; restore otherwise and on leaving Watch.
        LaunchedEffect(fullscreen) {
            val a = activity ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(a.window, a.window.decorView)
            if (fullscreen) {
                a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                val a = activity ?: return@onDispose
                a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(a.window, a.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }

        // One WatchScreen call site so the player survives the fullscreen toggle; only the
        // header is added/removed around it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (fullscreen) Color.Black else brand.background),
        ) {
            if (!fullscreen) HeaderBar()
            WatchScreen(
                bundle = bundle,
                onOpenVideo = { watchKey = it },
                onBack = closeWatch,
                isFullscreen = fullscreen,
                onToggleFullscreen = { fullscreen = !fullscreen },
                modifier = if (fullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.weight(1f).navigationBarsPadding()
                },
            )
        }
        return
    }

    Scaffold(
        containerColor = brand.background,
        topBar = { HeaderBar() },
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
                onOpenVideo = { watchKey = it },
            )
            else -> ComingSoon(
                title = selectedTab.label,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** The shared top header, padded below the status bar and coloured as one surface. */
@Composable
private fun HeaderBar() {
    val brand = Brand.colors
    Column(
        modifier = Modifier.fillMaxWidth().background(brand.surface).statusBarsPadding(),
    ) {
        TopHeader()
        HorizontalDivider(color = brand.line)
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
