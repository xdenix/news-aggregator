package ru.project.newsaggregator.data

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val link: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAt: Long?,
)
