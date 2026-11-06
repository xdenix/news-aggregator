package ru.project.newsaggregator.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.project.newsaggregator.NewsApplication
import ru.project.newsaggregator.data.Article
import ru.project.newsaggregator.data.NewsRepository
import ru.project.newsaggregator.data.filterArticles

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NewsRepository = (application as NewsApplication).repository
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var appTheme by mutableStateOf(readTheme())
    var query by mutableStateOf("")
    var sourceId by mutableStateOf<String?>(null)
    var savedOnly by mutableStateOf(false)

    val sources = repository.sources
    val articles = repository.articles
    val bookmarks = repository.bookmarks
    val status = repository.status

    init {
        viewModelScope.launch {
            repository.loadCache()
            repository.refresh()
        }
    }

    fun selectTheme(theme: AppTheme) {
        appTheme = theme
        prefs.edit().putString(THEME, theme.name).apply()
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun toggleBookmark(id: String) {
        repository.toggleBookmark(id)
    }

    suspend fun addSource(name: String, url: String): String? {
        return repository.addSource(name, url)
    }

    fun setSourceEnabled(id: String, enabled: Boolean) {
        if (!enabled && sourceId == id) sourceId = null
        viewModelScope.launch { repository.setSourceEnabled(id, enabled) }
    }

    fun removeSource(id: String) {
        if (sourceId == id) sourceId = null
        viewModelScope.launch { repository.removeSource(id) }
    }

    private fun readTheme(): AppTheme {
        val stored = prefs.getString(THEME, AppTheme.Light.name)
        return AppTheme.entries.firstOrNull { it.name == stored } ?: AppTheme.Light
    }

    fun visible(articles: List<Article>, bookmarks: Set<String>): List<Article> {
        val enabled = repository.sources.value.filter { it.enabled }.map { it.id }.toSet()
        return filterArticles(articles, bookmarks, sourceId, query, savedOnly)
            .filter { it.sourceId in enabled }
    }

    private companion object {
        const val PREFS = "news_aggregator"
        const val THEME = "theme"
    }
}
