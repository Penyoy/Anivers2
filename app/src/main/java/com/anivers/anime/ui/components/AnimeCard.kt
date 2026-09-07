package com.anivers.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0F1E))
        ) {
            AsyncImage(
                model = anime.cover.ifEmpty { Constants.FALLBACK_COVER },
                contentDescription = anime.judul,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // badge lastch
            if (anime.lastch.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF5B5BD6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(anime.lastch, color = Color.White, fontSize = 10.sp)
                }
            }
            if (showBookmark && onBookmark != null) {
                IconButton(
                    onClick = onBookmark,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color(0x99000000), RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "bookmark",
                        tint = if (isBookmarked) Color(0xFF5B5BD6) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // play overlay gradient
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))
                ),
                contentAlignment = Alignment.Center
            ) {
                // subtle play icon on hover would be Compose hover, just static low alpha
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = anime.judul,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color(0xFFCCCCCC),
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (anime.genre.isNotEmpty()) {
            Row(modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (g in anime.genre.take(2)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x0DFFFFFF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) { Text(g, fontSize = 10.sp, color = Color(0xFF888888)) }
                }
            }
        }
    }
}

@Composable
fun AnimeGrid(
    animes: List<Anime>,
    onClick: (Anime) -> Unit,
    onBookmark: ((Anime) -> Unit)? = null,
    bookmarkIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    // used inside LazyVerticalGrid caller ; this is helper for Column
}

@Composable
fun SectionHeader(title: String, onMore: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color(0xFFF8FAFC), fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
            if (onMore != null) {
                Text(
                    "Show More",
                    color = Color(0xFFFFDB89),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onMore() }.padding(4.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFFDB89).copy(alpha = 0.85f))
        )
    }
}
