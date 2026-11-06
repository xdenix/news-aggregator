package ru.project.newsaggregator.data

object HtmlText {
    private val lineBreak = Regex("(?i)<br\\s*/?>")
    private val blockEnd = Regex("(?i)</p>|</div>|</h[1-6]>|</li>")
    private val tags = Regex("<[^>]+>")
    private val spaces = Regex("[ \\t\\x0C]+")
    private val blankLines = Regex("\\n{3,}")
    private val image = Regex("(?i)<img\\b[^>]*\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"]")
    private val decimalEntity = Regex("&#(\\d+);")
    private val hexEntity = Regex("&#x([0-9a-fA-F]+);")

    fun firstImage(html: String): String? = image.find(html)?.groupValues?.get(1)?.trim()

    fun toPlain(html: String): String {
        val withBreaks = lineBreak.replace(html, "\n")
        val withBlocks = blockEnd.replace(withBreaks, "\n")
        val stripped = tags.replace(withBlocks, "")
        return unescape(stripped)
            .replace(spaces, " ")
            .replace(blankLines, "\n\n")
            .trim()
    }

    private fun unescape(text: String): String {
        val decimal = decimalEntity.replace(text) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }
        val hex = hexEntity.replace(decimal) { match ->
            match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
        }
        return hex
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }
}
