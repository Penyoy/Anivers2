package com.anivers.anime.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anivers.anime.data.local.AppDatabase
import com.anivers.anime.ui.theme.GlassBg
import com.anivers.anime.ui.theme.GlassBorder
import com.anivers.anime.ui.theme.GoldPrimary

data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomBar(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val bookmarks by db.bookmarkDao().getAllFlow().collectAsState(initial = emptyList())

    val items = remember(bookmarks.size) {
        listOf(
            BottomItem("home", "Home", Icons.Filled.Home),
            BottomItem("jadwal", "Jadwal", Icons.Filled.CalendarMonth),
            BottomItem("history", "Recent", Icons.Filled.History),
            BottomItem("bookmark", "Save", Icons.Filled.Bookmark),
            BottomItem("profile", "Profile", Icons.Filled.Person)
        )
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: "home"

    // Floating glass container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xE6121214))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in items) {
                val isSelected = when (item.route) {
                    "home" -> currentRoute.startsWith("home") || currentRoute.startsWith("anime/") || currentRoute.startsWith("watch/") || currentRoute.startsWith("genre/") || currentRoute.startsWith("explore") || currentRoute.startsWith("search")
                    "jadwal" -> currentRoute.startsWith("jadwal")
                    "history" -> currentRoute == "history"
                    "bookmark" -> currentRoute == "bookmark"
                    "profile" -> currentRoute == "profile" || currentRoute == "auth"
                    else -> currentRoute.startsWith(item.route)
                }
                BottomGlassItem(
                    item = item,
                    selected = isSelected,
                    badgeCount = if (item.route == "bookmark" && bookmarks.isNotEmpty()) bookmarks.size else null,
                    onClick = {
                        // Don't re-navigate if already selected and at root
                        if (currentRoute == item.route) return@BottomGlassItem
                        navController.navigate(item.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomGlassItem(
    item: BottomItem,
    selected: Boolean,
    badgeCount: Int?,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.06f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "scale")
    val bg by animateColorAsState(targetValue = if (selected) Color(0xFFFFDB89) else Color.Transparent, label = "bg")
    val iconColor by animateColorAsState(targetValue = if (selected) Color(0xFF030303) else Color(0xFF9AA0B6), label = "icon")
    val textColor by animateColorAsState(targetValue = if (selected) Color(0xFFFFDB89) else Color(0xFF9AA0B6), label = "text")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .then(if (!selected) Modifier.border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(14.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount != null && badgeCount > 0) {
                        Badge(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                        ) { Text(if (badgeCount > 9) "9+" else badgeCount.toString(), fontSize = 9.sp) }
                    }
                }
            ) {
                Icon(item.icon, contentDescription = item.label, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
        // indicator dot
        if (selected) {
            Spacer(Modifier.height(2.dp))
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(GoldPrimary))
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}
