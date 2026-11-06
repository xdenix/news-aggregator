package ru.project.newsaggregator.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelative(epochMillis: Long?, now: Long = System.currentTimeMillis()): String {
    if (epochMillis == null) return ""
    val delta = now - epochMillis
    val minutes = delta / 60_000
    return when {
        delta < 0 -> formatAbsolute(epochMillis)
        minutes < 1 -> "только что"
        minutes < 60 -> "$minutes мин назад"
        minutes < 24 * 60 -> "${minutes / 60} ч назад"
        else -> formatAbsolute(epochMillis)
    }
}

private fun formatAbsolute(epochMillis: Long): String {
    val format = SimpleDateFormat("d MMMM, HH:mm", Locale("ru"))
    return format.format(Date(epochMillis))
}
