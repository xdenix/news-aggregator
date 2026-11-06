package ru.project.newsaggregator.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object RssParser {
    private const val summaryLimit = 1500

    fun parse(xml: String, source: NewsSource): List<Article> {
        return parseWith(xml, source, Xml.newPullParser())
    }

    internal fun parseWith(xml: String, source: NewsSource, parser: XmlPullParser): List<Article> {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        val items = mutableListOf<Article>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && isEntry(localName(parser))) {
                readEntry(parser, localName(parser), source)?.let(items::add)
            }
            event = parser.next()
        }
        return items
    }

    private fun readEntry(parser: XmlPullParser, tag: String, source: NewsSource): Article? {
        var title = ""
        var link = ""
        var summary = ""
        var dateRaw = ""
        var image: String? = null
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.END_TAG && localName(parser) == tag) break
            if (event != XmlPullParser.START_TAG) continue
            when (localName(parser)) {
                "title" -> if (title.isBlank()) title = readText(parser)
                "link" -> link = preferLink(link, parser)
                "guid" -> if (link.isBlank()) link = readText(parser).trim()
                "description", "summary" -> summary = preferLonger(summary, readText(parser))
                "content" -> {
                    val url = attribute(parser, "url")
                    val type = attribute(parser, "type").orEmpty()
                    if (!url.isNullOrBlank()) {
                        if (image == null && isImage(type, url)) image = url
                    } else {
                        summary = preferLonger(summary, readText(parser))
                    }
                }
                "enclosure", "thumbnail" -> {
                    val url = attribute(parser, "url")
                    val type = attribute(parser, "type").orEmpty()
                    if (!url.isNullOrBlank() && image == null && isImage(type, url)) {
                        image = url
                    }
                }
                "pubdate", "published", "updated" -> {
                    if (dateRaw.isBlank()) dateRaw = readText(parser)
                }
            }
        }
        val plainTitle = HtmlText.toPlain(title)
        if (image == null) image = HtmlText.firstImage(summary)?.let(::absoluteHttp)
        val plainSummary = HtmlText.toPlain(summary).take(summaryLimit)
        val url = link.trim()
        if (plainTitle.isBlank() || url.isBlank()) return null
        return Article(
            id = articleId(url),
            sourceId = source.id,
            sourceName = source.name,
            title = plainTitle,
            link = url,
            summary = plainSummary,
            imageUrl = absoluteHttp(image),
            publishedAt = DateFormats.parse(dateRaw),
        )
    }

    private fun preferLink(current: String, parser: XmlPullParser): String {
        val href = attribute(parser, "href")?.trim().orEmpty()
        val rel = attribute(parser, "rel")?.trim().orEmpty()
        val text = readText(parser).trim()
        val candidate = href.ifBlank { text }
        if (candidate.isBlank()) return current
        val preferred = rel.isEmpty() || rel.equals("alternate", ignoreCase = true)
        return if (current.isBlank() || preferred) candidate else current
    }

    private fun readText(parser: XmlPullParser): String {
        val builder = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (depth == 1) builder.append(parser.text)
            }
        }
        return builder.toString()
    }

    private fun attribute(parser: XmlPullParser, name: String): String? {
        for (index in 0 until parser.attributeCount) {
            val raw = parser.getAttributeName(index).orEmpty().substringAfter(':')
            if (raw.equals(name, ignoreCase = true)) return parser.getAttributeValue(index)
        }
        return null
    }

    private fun localName(parser: XmlPullParser): String {
        return parser.name.orEmpty().substringAfter(':').lowercase()
    }

    private fun isEntry(name: String): Boolean = name == "item" || name == "entry"

    private fun isImage(type: String, url: String): Boolean {
        if (type.startsWith("image")) return true
        if (type.isNotBlank()) return false
        val path = url.substringBefore('?').lowercase()
        return path.endsWith(".jpg") || path.endsWith(".jpeg") ||
            path.endsWith(".png") || path.endsWith(".webp") || path.endsWith(".gif")
    }

    private fun preferLonger(current: String, next: String): String {
        return if (next.trim().length > current.trim().length) next else current
    }

    private fun absoluteHttp(url: String?): String? {
        val value = url?.trim().orEmpty()
        return when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.startsWith("//") -> "https:$value"
            else -> null
        }
    }
}
