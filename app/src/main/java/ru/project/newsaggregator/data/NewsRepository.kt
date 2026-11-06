package ru.project.newsaggregator.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class LoadStatus(
    val loading: Boolean = false,
    val failedSources: List<String> = emptyList(),
)

class NewsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cache = ArticleCache(appContext)
    private val sourceStore = SourceStore(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _sources = MutableStateFlow(sourceStore.read())
    val sources: StateFlow<List<NewsSource>> = _sources.asStateFlow()

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _bookmarks = MutableStateFlow(
        prefs.getStringSet(BOOKMARKS, emptySet()).orEmpty().toSet(),
    )
    val bookmarks: StateFlow<Set<String>> = _bookmarks.asStateFlow()

    private val _status = MutableStateFlow(LoadStatus())
    val status: StateFlow<LoadStatus> = _status.asStateFlow()

    suspend fun loadCache() {
        val known = _sources.value.map { it.id }.toSet()
        val cached = withContext(Dispatchers.IO) { cache.read() }
            .filter { it.sourceId in known }
        if (cached.isNotEmpty() && _articles.value.isEmpty()) {
            _articles.value = cached
        }
    }

    suspend fun refresh() {
        if (!mutex.tryLock()) return
        try {
            _status.value = LoadStatus(loading = true)
            val results = coroutineScope {
                _sources.value.filter { it.enabled }.map { source ->
                    async(Dispatchers.IO) {
                        source to runCatching { loadSource(source) }
                    }
                }.awaitAll()
            }
            val failed = mutableListOf<String>()
            val fresh = mutableListOf<Article>()
            for ((source, result) in results) {
                result.onSuccess { items -> fresh += items }
                    .onFailure { failed += source.name }
            }
            if (fresh.isNotEmpty()) {
                val known = _sources.value.map { it.id }.toSet()
                val updatedSources = fresh.map { it.sourceId }.toSet()
                val kept = _articles.value.filter { it.sourceId in known && it.sourceId !in updatedSources }
                val merged = (fresh + kept)
                    .distinctBy { it.id }
                    .sortedByDescending { it.publishedAt ?: Long.MIN_VALUE }
                    .take(MAX_ARTICLES)
                _articles.value = merged
                withContext(Dispatchers.IO) { cache.write(merged) }
            }
            _status.value = LoadStatus(loading = false, failedSources = failed)
        } finally {
            _status.value = _status.value.copy(loading = false)
            mutex.unlock()
        }
    }

    suspend fun addSource(name: String, rawUrl: String): String? = withContext(Dispatchers.IO) {
        val url = FeedDiscovery.normalizeUrl(rawUrl) ?: return@withContext "Нужна ссылка http или https"
        if (_sources.value.any { FeedDiscovery.sameUrl(it.url, url) }) {
            return@withContext "Такой ресурс уже есть"
        }
        if (_sources.value.size >= MAX_SOURCES) {
            return@withContext "Можно сохранить не больше $MAX_SOURCES ресурсов"
        }
        val opened = runCatching { openFeed(url) }.getOrElse {
            return@withContext "Не удалось открыть ссылку"
        }
        if (opened.items.isEmpty()) return@withContext "По этой ссылке нет новостей"
        if (!mutex.tryLock()) return@withContext "Подождите, лента обновляется"
        try {
            if (_sources.value.any { FeedDiscovery.sameUrl(it.url, opened.source.url) }) {
                return@withContext "Такой ресурс уже есть"
            }
            val titled = name.trim().ifBlank { opened.source.name }.take(NAME_LIMIT)
            val source = opened.source.copy(name = titled)
            val items = opened.items.map { it.copy(sourceName = titled) }
            _sources.value = _sources.value + source
            sourceStore.write(_sources.value)
            val merged = (_articles.value + items)
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAt ?: Long.MIN_VALUE }
                .take(MAX_ARTICLES)
            _articles.value = merged
            cache.write(merged)
            null
        } finally {
            mutex.unlock()
        }
    }

    suspend fun setSourceEnabled(id: String, enabled: Boolean) {
        mutex.withLock {
            _sources.value = _sources.value.map { source ->
                if (source.id == id) source.copy(enabled = enabled) else source
            }
            withContext(Dispatchers.IO) { sourceStore.write(_sources.value) }
        }
        if (enabled) refresh()
    }

    suspend fun removeSource(id: String) {
        mutex.withLock {
            _sources.value = _sources.value.filter { it.id != id }
            _articles.value = _articles.value.filter { it.sourceId != id }
            withContext(Dispatchers.IO) {
                sourceStore.write(_sources.value)
                cache.write(_articles.value)
            }
        }
    }

    fun toggleBookmark(id: String) {
        val next = _bookmarks.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _bookmarks.value = next
        prefs.edit().putStringSet(BOOKMARKS, next).apply()
    }

    private fun loadSource(source: NewsSource): List<Article> {
        return parse(source, download(source.url)).take(PER_SOURCE)
    }

    private fun openFeed(url: String): OpenedFeed {
        val first = download(url)
        val feedUrl = when {
            FeedDiscovery.isFeed(first) -> url
            else -> FeedDiscovery.alternateFeed(first, url) ?: url
        }
        val payload = if (feedUrl == url) first else download(feedUrl)
        val source = NewsSource(
            id = articleId(feedUrl),
            name = FeedDiscovery.suggestedName(feedUrl),
            url = feedUrl,
        )
        return OpenedFeed(source, parse(source, payload).take(PER_SOURCE))
    }

    private fun download(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, text/html, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Пустой ответ")
            return XmlDecoding.decode(body.bytes(), body.contentType()?.charset())
        }
    }

    private fun parse(source: NewsSource, payload: String): List<Article> {
        if (FeedDiscovery.isFeed(payload)) return RssParser.parse(payload, source)
        if ("js-pagination-element cm-article-main" in payload) {
            return TimewebListParser.parse(payload, source)
        }
        return emptyList()
    }

    private class ArticleCache(context: Context) {
        private val file = File(context.filesDir, "articles.json")
        private val json = Json { ignoreUnknownKeys = true }

        fun read(): List<Article> = runCatching {
            if (!file.exists()) emptyList() else json.decodeFromString<List<Article>>(file.readText())
        }.getOrDefault(emptyList())

        fun write(items: List<Article>) {
            runCatching { file.writeText(json.encodeToString(items)) }
        }
    }

    private class SourceStore(context: Context) {
        private val file = File(context.filesDir, "sources.json")
        private val json = Json { ignoreUnknownKeys = true }

        fun read(): List<NewsSource> {
            if (!file.exists()) return NewsSources.defaults
            return runCatching {
                json.decodeFromString<List<NewsSource>>(file.readText())
            }.getOrElse { NewsSources.defaults }
        }

        fun write(items: List<NewsSource>) {
            file.writeText(json.encodeToString(items))
        }
    }

    private data class OpenedFeed(
        val source: NewsSource,
        val items: List<Article>,
    )

    private companion object {
        const val PREFS = "news_aggregator"
        const val BOOKMARKS = "bookmarks"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        const val PER_SOURCE = 30
        const val MAX_ARTICLES = 200
        const val MAX_SOURCES = 20
        const val NAME_LIMIT = 40
    }
}
