package com.anivers.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anivers.anime.data.local.AppDatabase

data class BottomItem(val route: String, val label: String, val icon: ImageVector, val badge: Int? = null)

@Composable
fun BottomBar(navController: NavController) {
    val context = LocalContext.current
    // observe bookmark count
    val db = AppDatabase.get(context)
    val bookmarks by db.bookmarkDao().getAllFlow().collectAsState(initial = emptyList())

    val items = listOf(
        BottomItem("home", "Home", Icons.Filled.Home),
        BottomItem("jadwal", "Jadwal", Icons.Filled.CalendarToday),
        BottomItem("recent", "Recent", Icons.Filled.History),
        BottomItem("bookmark", "Bookmark", Icons.Filled.Bookmark, if (bookmarks.isNotEmpty()) bookmarks.size else null),
        BottomItem("profile", "Profil", Icons.Filled.Person)
    )
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: "home"

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xE6050714)) // glass strong
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in items) {
                val selected = when {
                    item.route == "home" && (currentRoute.startsWith("home") || currentRoute.startsWith("anime") || currentRoute.startsWith("watch") || currentRoute.startsWith("genre") || currentRoute.startsWith("explore") || currentRoute.startsWith("search")) -> true
                    currentRoute.startsWith(item.route) -> true
                    else -> false
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (item.badge != null) Badge(containerColor = Color(0xFFEF4444)) { Text(if (item.badge > 9) "9+" else item.badge.toString(), fontSize = 10.sp) }
                        }) {
                            Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(20.dp))
                        }
                    },
                    label = { Text(item.label, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF3730A3),
                        unselectedIconColor = Color(0xFF7A7F9A),
                        unselectedTextColor = Color(0xFF7A7F9A)
                    )
                )
            }
        }
    }
}
