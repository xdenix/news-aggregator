package ru.project.newsaggregator.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object FeedDiscovery {
    private val linkTag = Regex("""(?is)<link\b[^>]*>""")
    private val hrefAttr = Regex("""(?i)\bhref\s*=\s*["']([^"']+)["']""")
    private val rssType = Regex("""(?i)application/(?:rss|atom)\+xml""")

    fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.toHttpUrlOrNull()?.toString()
    }

    fun suggestedName(url: String): String {
        val host = url.toHttpUrlOrNull()?.host?.removePrefix("www.").orEmpty()
        val label = host.substringBefore('.').replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
        return label.ifBlank { "Источник" }
    }

    fun isFeed(payload: String): Boolean {
        val head = payload.trimStart().take(1500).lowercase()
        return "<rss" in head || "<feed" in head
    }

    fun alternateFeed(html: String, pageUrl: String): String? {
        val base = pageUrl.toHttpUrlOrNull() ?: return null
        for (tag in linkTag.findAll(html)) {
            val text = tag.value
            if (!rssType.containsMatchIn(text)) continue
            if (!text.contains("alternate", ignoreCase = true)) continue
            val href = hrefAttr.find(text)?.groupValues?.get(1)?.trim().orEmpty()
            if (href.isEmpty()) continue
            val resolved = base.resolve(href)?.toString()
            if (!resolved.isNullOrBlank()) return resolved
        }
        return null
    }

    fun sameUrl(left: String, right: String): Boolean {
        return left.trimEnd('/') == right.trimEnd('/')
    }
}
