package com.anivers.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anivers.anime.ui.components.BottomBar
import com.anivers.anime.ui.navigation.AppNavGraph
import com.anivers.anime.ui.theme.AniVerseTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AniVerseTheme {
                val sys = rememberSystemUiController()
                SideEffect {
                    sys.setStatusBarColor(Color.Transparent, darkIcons = false)
                    sys.setNavigationBarColor(Color.Transparent, darkIcons = false)
                }
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val route = backStack?.destination?.route ?: ""
                val hideBottom = route in listOf("splash", "welcome", "auth") || route.startsWith("watch/")

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF080A14), Color(0xFF030303))
                            )
                        )
                ) {
                    Scaffold(
                        bottomBar = { if (!hideBottom) BottomBar(navController) },
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // don't double-pad for floating bottom bar; only statusBars top handled per-screen
                                .padding(bottom = if (hideBottom) 0.dp else 0.dp)
                        ) {
                            AppNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    // PIP handling: keep player running, don't restart activity
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onUserLeaveHint() {
        // let WatchScreen handle PIP via button; not auto-enter to avoid unexpected behavior
        super.onUserLeaveHint()
    }
}
