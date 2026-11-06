package ru.project.newsaggregator.data

import java.nio.charset.Charset

object XmlDecoding {
    fun decode(bytes: ByteArray, httpCharset: Charset?): String {
        val headLength = minOf(bytes.size, 240)
        val head = String(bytes, 0, headLength, Charsets.ISO_8859_1).lowercase()
        val charset = when {
            "windows-1251" in head || "cp1251" in head -> charset("windows-1251")
            "koi8-r" in head -> charset("KOI8-R")
            "utf-8" in head || "utf8" in head -> Charsets.UTF_8
            else -> httpCharset ?: Charsets.UTF_8
        }
        return String(bytes, charset).removePrefix("\uFEFF")
    }
}
