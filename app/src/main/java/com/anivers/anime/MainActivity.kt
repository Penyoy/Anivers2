package com.anivers.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.anivers.anime.ui.components.BottomBar
import com.anivers.anime.ui.components.TopBar
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
                Scaffold(
                    topBar = {
                        // TopBar per-screen
                    },
                    bottomBar = { BottomBar(navController) },
                    containerColor = Color(0xFF030303),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF030303))
                            .padding(bottom = padding.calculateBottomPadding())
                    ) {
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
