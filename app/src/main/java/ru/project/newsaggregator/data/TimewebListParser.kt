package ru.project.newsaggregator.data

import java.util.Calendar

object TimewebListParser {
    private const val origin = "https://timeweb.com"
    private val card = Regex(
        """(?s)<div class="js-pagination-element cm-article-main\b.*?(?=<div class="js-pagination-element cm-article-main\b|$)""",
    )
    private val titleLink = Regex(
        """(?s)<h2[^>]*>\s*<a[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
    )
    private val summaryBlock = Regex(
        """(?s)<div v-pre class="font-style-1[^"]*">(.*?)</div>""",
    )
    private val image = Regex(
        """(?s)<div class="img-wrapper[^"]*">.*?<img[^>]*src="([^"]+)"""",
    )
    private val published = Regex(
        """(?s)<time class="date[^"]*">(.*?)</time>""",
    )
    private val clock = Regex("""(\d{1,2}):(\d{2})""")
    private val dayNumber = Regex("""(\d{1,2})""")
    private val yearNumber = Regex("""(20\d{2})""")
    private val today = Regex("""[cс]егодня""")
    private val months = listOf(
        "январ" to Calendar.JANUARY,
        "феврал" to Calendar.FEBRUARY,
        "март" to Calendar.MARCH,
        "апрел" to Calendar.APRIL,
        "мая" to Calendar.MAY,
        "мае" to Calendar.MAY,
        "июн" to Calendar.JUNE,
        "июл" to Calendar.JULY,
        "август" to Calendar.AUGUST,
        "сентябр" to Calendar.SEPTEMBER,
        "октябр" to Calendar.OCTOBER,
        "ноябр" to Calendar.NOVEMBER,
        "декабр" to Calendar.DECEMBER,
    )

    fun parse(html: String, source: NewsSource, now: Long = System.currentTimeMillis()): List<Article> {
        return card.findAll(html).mapNotNull { readCard(it.value, source, now) }.distinctBy { it.id }.toList()
    }

    internal fun parseWhen(raw: String, now: Long = System.currentTimeMillis()): Long? {
        val text = raw.trim().lowercase().replace('\u00a0', ' ')
        if (text.isEmpty()) return null
        val time = clock.find(text)
        val hour = time?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minute = time?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        when {
            today.containsMatchIn(text) -> Unit
            "вчера" in text -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            else -> {
                val day = dayNumber.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return null
                val month = months.firstOrNull { it.first in text }?.second ?: return null
                val year = yearNumber.find(text)?.groupValues?.get(1)?.toIntOrNull()
                    ?: calendar.get(Calendar.YEAR)
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
            }
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun readCard(html: String, source: NewsSource, now: Long): Article? {
        val titleMatch = titleLink.find(html) ?: return null
        val link = absolute(titleMatch.groupValues[1])
        val title = HtmlText.toPlain(titleMatch.groupValues[2])
        if (title.isBlank() || !link.startsWith("https://")) return null
        if ("/topic/" in link || "#" in link) return null
        val summary = summaryBlock.find(html)?.let { HtmlText.toPlain(it.groupValues[1]) }.orEmpty()
        val imageUrl = image.find(html)?.let { absolute(it.groupValues[1]) }?.takeIf { it.startsWith("https://") }
        val whenRaw = published.find(html)?.let { HtmlText.toPlain(it.groupValues[1]) }.orEmpty()
        return Article(
            id = articleId(link),
            sourceId = source.id,
            sourceName = source.name,
            title = title,
            link = link,
            summary = summary,
            imageUrl = imageUrl,
            publishedAt = parseWhen(whenRaw, now),
        )
    }

    private fun absolute(url: String): String {
        val value = HtmlText.toPlain(url).trim()
        return when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.startsWith("/") -> origin + value
            else -> ""
        }
    }
}
