package ru.project.newsaggregator.data

import kotlinx.serialization.Serializable

@Serializable
data class NewsSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
)

object NewsSources {
    val defaults: List<NewsSource> = listOf(
        NewsSource("pravda", "Правда", "https://www.pravda.ru/export.xml"),
        NewsSource("rbc", "РБК", "https://rssexport.rbc.ru/rbcnews/news/30/full.rss"),
        NewsSource("habr", "Хабр", "https://habr.com/ru/rss/articles/?fl=ru"),
        NewsSource("xakep", "Хакер", "https://xakep.ru/feed/"),
        NewsSource("timeweb", "Timeweb", "https://timeweb.com/ru/community/articles/"),
        NewsSource("tass", "ТАСС", "https://tass.ru/rss/v2.xml"),
        NewsSource("interfax", "Интерфакс", "https://www.interfax.ru/rss.asp"),
    )
}
