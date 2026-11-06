package ru.project.newsaggregator.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme(val title: String) {
    Light("Светлая"),
    Dark("Тёмная"),
    Blue("Синяя"),
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    onPrimary = Color.White,
    secondary = Color(0xFF8C4A2F),
    background = Color(0xFFF4F1EA),
    surface = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFE7E1D6),
    onBackground = Color(0xFF1C1B19),
    onSurface = Color(0xFF1C1B19),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    secondary = Color(0xFFE8B298),
    background = Color(0xFF141311),
    surface = Color(0xFF1E1C19),
    surfaceVariant = Color(0xFF2C2924),
    onBackground = Color(0xFFF3EFE7),
    onSurface = Color(0xFFF3EFE7),
    onSurfaceVariant = Color(0xFFCBC4B8),
)

private val BlueColors = darkColorScheme(
    primary = Color(0xFF8ECAFF),
    onPrimary = Color(0xFF003258),
    secondary = Color(0xFF7DD3FC),
    background = Color(0xFF0B1E33),
    surface = Color(0xFF13293F),
    surfaceVariant = Color(0xFF1C3A56),
    onBackground = Color(0xFFE7F0FA),
    onSurface = Color(0xFFE7F0FA),
    onSurfaceVariant = Color(0xFFB7C9DC),
)

@Composable
fun NewsTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = when (theme) {
        AppTheme.Light -> LightColors
        AppTheme.Dark -> DarkColors
        AppTheme.Blue -> BlueColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
