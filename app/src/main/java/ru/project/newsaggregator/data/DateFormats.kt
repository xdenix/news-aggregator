package ru.project.newsaggregator.data

import java.text.SimpleDateFormat
import java.util.Locale

object DateFormats {
    private val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
    )

    fun parse(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        for (pattern in patterns) {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.isLenient = true
            val parsed = runCatching { format.parse(value) }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }
}
