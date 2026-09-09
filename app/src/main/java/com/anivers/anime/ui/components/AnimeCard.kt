package com.anivers.anime.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anivers.anime.data.model.Anime
import com.anivers.anime.ui.theme.GlassBorder
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
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "cardScale")

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.2f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14141A))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = anime.cover.ifEmpty { Constants.FALLBACK_COVER },
                contentDescription = anime.judul,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // top scrim for badges
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent)))
            )
            // bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
            )

            // episode badge - glass pill
            if (anime.lastch.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFDB89))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(anime.lastch, color = Color(0xFF030303), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // score badge - glass dark pill
            if (anime.score.isNotBlank() && anime.score != "0") {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC0A0A0A))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .align(Alignment.TopEnd),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFDB89), modifier = Modifier.size(11.dp))
                        Text(anime.score, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // bookmark - glass circle
            if (showBookmark && onBookmark != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onBookmark() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "bookmark",
                        tint = if (isBookmarked) Color(0xFFFFDB89) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // type/status subtle bottom left
            if (anime.type.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text(anime.type, color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp) }
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = anime.judul,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color(0xFFF1F1F3),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        if (anime.genre.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (g in anime.genre.take(2)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x14FFDB89))
                            .border(1.dp, Color(0x1AFFDB89), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text(g, fontSize = 9.sp, color = Color(0xFFFFDB89), fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onMore: (() -> Unit)? = null) {
    // kept for backward compat - delegate to glass variant
    SectionHeaderGlass(title = title, onMore = onMore)
}

@Composable
fun AnimeCardHorizontal(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = anime.cover.ifEmpty { Constants.FALLBACK_COVER },
            contentDescription = anime.judul,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp, 86.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(anime.judul, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp)
            if (anime.lastch.isNotEmpty()) Text(anime.lastch, color = Color(0xFFFFDB89), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (anime.genre.isNotEmpty()) Text(anime.genre.take(2).joinToString(" • "), color = Color(0xFF8A8FA3), fontSize = 11.sp, maxLines = 1)
        }
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x14FFDB89)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFDB89), modifier = Modifier.size(16.dp))
        }
    }
}
