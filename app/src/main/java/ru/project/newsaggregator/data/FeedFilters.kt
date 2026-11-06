package ru.project.newsaggregator.data

fun filterArticles(
    articles: List<Article>,
    bookmarks: Set<String>,
    sourceId: String?,
    query: String,
    savedOnly: Boolean,
): List<Article> {
    val needle = query.trim()
    return articles.filter { article ->
        val sourceMatches = sourceId == null || article.sourceId == sourceId
        val savedMatches = !savedOnly || article.id in bookmarks
        val queryMatches = needle.isEmpty() ||
            article.title.contains(needle, ignoreCase = true) ||
            article.summary.contains(needle, ignoreCase = true)
        sourceMatches && savedMatches && queryMatches
    }
}
