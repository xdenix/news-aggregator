package ru.project.newsaggregator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.project.newsaggregator.data.Article
import ru.project.newsaggregator.data.DateFormats
import ru.project.newsaggregator.data.HtmlText
import ru.project.newsaggregator.data.NewsSource
import ru.project.newsaggregator.data.RssParser
import ru.project.newsaggregator.data.XmlDecoding
import ru.project.newsaggregator.data.FeedDiscovery
import ru.project.newsaggregator.data.TimewebListParser
import ru.project.newsaggregator.data.filterArticles
import org.kxml2.io.KXmlParser
import java.util.Calendar
import java.util.TimeZone

class AggregatorTest {
    private val source = NewsSource("demo", "Демо", "https://example.com/rss")

    @Test
    fun parsesRssItemAndIgnoresChannelImage() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Канал</title>
                <image><url>https://example.com/logo.png</url><title>Канал</title></image>
                <item>
                  <title>Заголовок &amp; новость</title>
                  <link>https://example.com/a</link>
                  <description><![CDATA[Текст <b>жирный</b><br/>ещё]]></description>
                  <pubDate>Wed, 05 Nov 2026 09:00:00 +0300</pubDate>
                  <enclosure url="https://example.com/a.jpg" type="image/jpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val items = RssParser.parseWith(xml, source, KXmlParser())

        assertEquals(1, items.size)
        assertEquals("Заголовок & новость", items[0].title)
        assertEquals("https://example.com/a", items[0].link)
        assertEquals("https://example.com/a.jpg", items[0].imageUrl)
        assertTrue(items[0].summary.contains("жирный"))
        assertNotNull(items[0].publishedAt)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
        calendar.timeInMillis = items[0].publishedAt!!
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(11, calendar.get(Calendar.MONTH) + 1)
        assertEquals(5, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, calendar.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun takesImageFromHtmlDescription() {
        val xml = """
            <rss><channel><item>
              <title>Хабр</title>
              <link>https://habr.com/ru/articles/1/</link>
              <description><![CDATA[<img src="https://cdn.example/pic.png" /><p>Анонс</p>]]></description>
            </item></channel></rss>
        """.trimIndent()

        val items = RssParser.parseWith(xml, source, KXmlParser())

        assertEquals("https://cdn.example/pic.png", items.single().imageUrl)
        assertEquals("Анонс", items.single().summary)
    }

    @Test
    fun stripsHtmlAndEntities() {
        assertEquals("Текст жирный\nещё", HtmlText.toPlain("Текст <b>жирный</b><br/>ещё"))
        assertEquals("A & B", HtmlText.toPlain("A &amp; B"))
    }

    @Test
    fun decodesWindows1251WhenPrologSaysSo() {
        val prolog = """<?xml version="1.0" encoding="windows-1251"?>""".toByteArray(Charsets.US_ASCII)
        val word = byteArrayOf(0xCF.toByte(), 0xF0.toByte(), 0xE8.toByte(), 0xE2.toByte(), 0xE5.toByte(), 0xF2.toByte())
        val xml = XmlDecoding.decode(prolog + word, Charsets.UTF_8)
        assertTrue(xml.contains("Привет"))
    }

    @Test
    fun parsesRfc822Date() {
        assertNotNull(DateFormats.parse("Thu, 05 Nov 2026 12:15:50 +0300"))
        assertNull(DateFormats.parse("не дата"))
    }

    @Test
    fun filtersBySourceQueryAndBookmarks() {
        val articles = listOf(
            article("1", "lenta", "Кот в городе"),
            article("2", "habr", "Kotlin digest"),
        )
        assertEquals(listOf("1"), filterArticles(articles, emptySet(), "lenta", "", false).map { it.id })
        assertEquals(listOf("2"), filterArticles(articles, emptySet(), null, "kotlin", false).map { it.id })
        assertEquals(listOf("2"), filterArticles(articles, setOf("2"), null, "", true).map { it.id })
    }

    @Test
    fun findsRssLinkOnHtmlPage() {
        val html = """
            <html><head>
            <link rel="alternate" type="application/rss+xml" title="Лента" href="/feed/" />
            </head></html>
        """.trimIndent()
        assertEquals("https://xakep.ru/feed/", FeedDiscovery.alternateFeed(html, "https://xakep.ru/"))
        assertEquals("https://xakep.ru/feed/", FeedDiscovery.normalizeUrl("xakep.ru/feed/"))
        assertTrue(FeedDiscovery.isFeed("<?xml version=\"1.0\"?><rss version=\"2.0\"></rss>"))
        assertTrue(FeedDiscovery.sameUrl("https://xakep.ru/feed", "https://xakep.ru/feed/"))
    }

    @Test
    fun parsesTimewebArticleCard() {
        val html = """
            <div class="js-pagination-element cm-article-main">
              <time class="date txt-secondary mr-8">Сегодня в 13:44</time>
              <h2><a href="/ru/community/articles/gde-chitat-aktualnye-it-novosti">Где читать IT-новости</a></h2>
              <div class="img-wrapper"><img src="/ru/community/article/pic.png" alt=""></div>
              <div v-pre class="font-style-1 mb-24">Подборка площадок</div>
            </div>
        """.trimIndent()
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val item = TimewebListParser.parse(html, source, now).single()
        assertEquals("Где читать IT-новости", item.title)
        assertEquals(
            "https://timeweb.com/ru/community/articles/gde-chitat-aktualnye-it-novosti",
            item.link,
        )
        assertEquals("https://timeweb.com/ru/community/article/pic.png", item.imageUrl)
        assertEquals("Подборка площадок", item.summary)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = item.publishedAt!!
        assertEquals(5, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(13, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(44, calendar.get(Calendar.MINUTE))
    }

    private fun article(id: String, sourceId: String, title: String): Article {
        return Article(id, sourceId, sourceId, title, "https://example.com/$id", "", null, 0L)
    }
}
