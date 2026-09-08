package com.anivers.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anivers.anime.data.model.Anime
import com.anivers.anime.utils.Constants

@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    onBookmark: (() -> Unit)? = null,
    isBookmarked: Boolean = false,
    showBookmark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = androidx.compose.foundation.interaction.rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            AsyncImage(
                model = anime.cover.ifEmpty { Constants.FALLBACK_COVER },
                contentDescription = anime.judul,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC000000))
                        )
                    )
            )

            // Episode badge - top left, gold pill
            if (anime.lastch.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFDB89))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        anime.lastch,
                        color = Color(0xFF030303),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Score badge - top right, circle
            if (anime.score.isNotBlank() && anime.score != "0") {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC030303))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFDB89), modifier = Modifier.size(10.dp))
                        Text(anime.score, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bookmark button
            if (showBookmark && onBookmark != null) {
                IconButton(
                    onClick = onBookmark,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "bookmark",
                        tint = if (isBookmarked) Color(0xFFFFDB89) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = anime.judul,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color(0xFFE6E8EE),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        if (anime.genre.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (g in anime.genre.take(2)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x14FFDB89))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(g, fontSize = 9.sp, color = Color(0xFFFFDB89), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onMore: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = Color(0xFFF8FAFC),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (onMore != null) {
                Text(
                    "Show More",
                    color = Color(0xFFFFDB89),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onMore() }.padding(4.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFDB89))
        )
    }
}
