package ru.project.newsaggregator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.project.newsaggregator.ui.AppTheme
import ru.project.newsaggregator.ui.ArticleScreen
import ru.project.newsaggregator.ui.FeedScreen
import ru.project.newsaggregator.ui.FeedViewModel
import ru.project.newsaggregator.ui.NewsTheme
import ru.project.newsaggregator.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FeedViewModel = viewModel()
            val lightBars = viewModel.appTheme == AppTheme.Light
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = lightBars
                controller.isAppearanceLightNavigationBars = lightBars
            }
            NewsTheme(viewModel.appTheme) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "feed") {
                    composable("feed") {
                        FeedScreen(
                            viewModel = viewModel,
                            onOpen = { id -> navController.navigate("article/$id") },
                            onOpenSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "article/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        val articles by viewModel.articles.collectAsState()
                        val bookmarks by viewModel.bookmarks.collectAsState()
                        ArticleScreen(
                            article = articles.firstOrNull { it.id == id },
                            saved = id in bookmarks,
                            onBack = { navController.popBackStack() },
                            onToggleSaved = { viewModel.toggleBookmark(id) },
                        )
                    }
                }
            }
        }
    }
}
