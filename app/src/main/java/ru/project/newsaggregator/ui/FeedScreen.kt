package ru.project.newsaggregator.ui

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.project.newsaggregator.data.Article
import ru.project.newsaggregator.data.NewsSource
import ru.project.newsaggregator.data.formatRelative

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onOpen: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sources by viewModel.sources.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val status by viewModel.status.collectAsState()
    val visible = viewModel.visible(articles, bookmarks)
    val refreshing = status.loading && articles.isNotEmpty()
    val subtitle = feedSubtitle(viewModel)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Новости")
                        if (subtitle != null) {
                            Text(subtitle, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                    if (status.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).height(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SourceTabs(
                sources = sources.filter { it.enabled },
                sourceId = viewModel.sourceId,
                savedOnly = viewModel.savedOnly,
                onAll = {
                    viewModel.sourceId = null
                    viewModel.savedOnly = false
                },
                onSource = {
                    viewModel.sourceId = it
                    viewModel.savedOnly = false
                },
                onSaved = {
                    viewModel.savedOnly = true
                    viewModel.sourceId = null
                },
            )
            if (status.failedSources.isNotEmpty()) {
                Text(
                    text = "Не удалось загрузить: ${status.failedSources.joinToString()}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (status.loading && articles.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = emptyMessage(viewModel.query, viewModel.savedOnly, articles.isEmpty()),
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visible, key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                saved = article.id in bookmarks,
                                onOpen = { onOpen(article.id) },
                                onToggleSaved = { viewModel.toggleBookmark(article.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun feedSubtitle(viewModel: FeedViewModel): String? {
    val query = viewModel.query.trim()
    return if (query.isEmpty()) null else "Поиск: $query"
}

@Composable
private fun SourceTabs(
    sources: List<NewsSource>,
    sourceId: String?,
    savedOnly: Boolean,
    onAll: () -> Unit,
    onSource: (String) -> Unit,
    onSaved: () -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = sourceId == null && !savedOnly,
            onClick = onAll,
            label = { Text("Все") },
        )
        sources.forEach { source ->
            FilterChip(
                selected = sourceId == source.id && !savedOnly,
                onClick = { onSource(source.id) },
                label = { Text(source.name) },
            )
        }
        FilterChip(
            selected = savedOnly,
            onClick = onSaved,
            label = { Text("Сохранённые") },
        )
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    saved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = article.sourceName,
                            style = MaterialTheme.typography.labelMedium,
                            color = sourceColor(article.sourceId),
                        )
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onToggleSaved) {
                        Icon(
                            imageVector = if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (saved) "Убрать из сохранённых" else "Сохранить",
                            tint = if (saved) Color(0xFFC9891A) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (article.summary.isNotBlank()) {
                    Text(
                        text = article.summary,
                        modifier = Modifier.padding(end = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val whenText = formatRelative(article.publishedAt)
                if (whenText.isNotBlank()) {
                    Text(
                        text = whenText,
                        modifier = Modifier.padding(top = 6.dp, end = 10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun emptyMessage(query: String, savedOnly: Boolean, nothingLoaded: Boolean): String {
    return when {
        query.isNotBlank() -> "Ничего не нашлось"
        savedOnly -> "Сохранённых материалов пока нет"
        nothingLoaded -> "Нет новостей. Проверьте сеть и обновите ленту."
        else -> "В этой подборке пусто"
    }
}

private fun sourceColor(sourceId: String): Color {
    return when (sourceId) {
        "pravda" -> Color(0xFF9B1B30)
        "rbc" -> Color(0xFF1F4E79)
        "habr" -> Color(0xFF4D7C0F)
        "xakep" -> Color(0xFF166534)
        "timeweb" -> Color(0xFF1D4ED8)
        "tass" -> Color(0xFFB45309)
        "interfax" -> Color(0xFF0F766E)
        else -> Color(0xFF1F3A5F)
    }
}
