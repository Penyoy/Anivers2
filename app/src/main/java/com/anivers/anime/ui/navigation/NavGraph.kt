package com.anivers.anime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anivers.anime.ui.screens.*

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore?type={type}"
    const val SEARCH = "search?q={q}"
    const val GENRE = "genre/{slug}"
    const val DETAIL = "anime/{slug}"
    const val WATCH = "watch/{seriesUrl}/{episode}"
    const val RECENT = "recent"
    const val BOOKMARK = "bookmark"
    const val PROFILE = "profile"
    const val JADWAL = "jadwal"

    fun explore(type: String? = null) = if (type != null) "explore?type=$type" else "explore"
    fun search(q: String) = "search?q=${java.net.URLEncoder.encode(q, "UTF-8")}"
    fun genre(slug: String) = "genre/$slug"
    fun detail(slug: String) = "anime/$slug"
    fun watch(seriesUrl: String, episode: String) = "watch/$seriesUrl/$episode"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateSearch = { navController.navigate(Routes.SEARCH.replace("{q}", "")) },
                onSearchQuery = { q -> navController.navigate(Routes.search(q)) },
                onAnimeClick = { slug -> navController.navigate(Routes.detail(slug)) },
                onMoreClick = { type -> navController.navigate(Routes.explore(type)) },
                onGenreClick = { slug -> navController.navigate(Routes.genre(slug)) }
            )
        }
        composable(
            route = Routes.EXPLORE,
            arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = ""; nullable = true })
        ) { backStack ->
            val type = backStack.arguments?.getString("type") ?: ""
            ExploreScreen(
                initialType = type,
                onAnimeClick = { slug -> navController.navigate(Routes.detail(slug)) },
                onGenreClick = { slug -> navController.navigate(Routes.genre(slug)) }
            )
        }
        composable(
            route = Routes.SEARCH,
            arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = ""; nullable = true })
        ) { backStack ->
            val q = backStack.arguments?.getString("q") ?: ""
            SearchScreen(
                initialQuery = q,
                onAnimeClick = { slug -> navController.navigate(Routes.detail(slug)) }
            )
        }
        composable(
            route = Routes.GENRE,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStack ->
            val slug = backStack.arguments?.getString("slug") ?: ""
            GenreScreen(slug = slug, onAnimeClick = { s -> navController.navigate(Routes.detail(s)) })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStack ->
            val slug = backStack.arguments?.getString("slug") ?: ""
            DetailScreen(
                slug = slug,
                onEpisodeClick = { seriesUrl, epUrl -> navController.navigate(Routes.watch(seriesUrl, epUrl)) },
                onGenreClick = { g -> navController.navigate(Routes.genre(g)) }
            )
        }
        composable(
            route = Routes.WATCH,
            arguments = listOf(
                navArgument("seriesUrl") { type = NavType.StringType },
                navArgument("episode") { type = NavType.StringType }
            )
        ) { backStack ->
            val seriesUrl = backStack.arguments?.getString("seriesUrl") ?: ""
            val episode = backStack.arguments?.getString("episode") ?: ""
            WatchScreen(seriesUrl = seriesUrl, episode = episode)
        }
        composable(Routes.RECENT) { RecentScreen(onWatchClick = { s, e -> navController.navigate(Routes.watch(s, e)) }) }
        composable(Routes.BOOKMARK) { BookmarkScreen(onAnimeClick = { slug -> navController.navigate(Routes.detail(slug)) }) }
        composable(Routes.PROFILE) { ProfileScreen() }
        composable(Routes.JADWAL) { JadwalScreen(onAnimeClick = { slug -> navController.navigate(Routes.detail(slug)) }) }
    }
}
