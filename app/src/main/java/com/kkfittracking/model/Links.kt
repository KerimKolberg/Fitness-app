package com.kkfittracking.model

import java.net.URLEncoder

/** A video or page showing how an exercise is done. [title] is optional. */
data class ExerciseLink(val url: String, val title: String = "") {
    private val host: String
        get() = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#').lowercase()

    /** What to show: the title, or else the address without "https://www.". */
    val label: String get() = title.ifBlank { url.substringAfter("://").removePrefix("www.").removePrefix("m.").trimEnd('/') }

    val isVideo: Boolean
        get() = host == "youtu.be" || host.endsWith("youtube.com") || host.endsWith("vimeo.com") || host.endsWith("tiktok.com")
}

/** Links are stored as text, one per line: "url" or "title | url". */
object ExerciseLinks {
    private const val SEPARATOR = " | "

    fun parse(text: String): List<ExerciseLink> = text.lines().mapNotNull { line ->
        val trimmed = line.trim()
        val cut = trimmed.lastIndexOf(SEPARATOR)
        val link = if (cut >= 0) {
            ExerciseLink(url = trimmed.substring(cut + SEPARATOR.length).trim(), title = trimmed.substring(0, cut).trim())
        } else {
            ExerciseLink(url = trimmed)
        }
        link.takeIf { it.url.isNotEmpty() }
    }

    fun format(links: List<ExerciseLink>): String = links.joinToString("\n") { link ->
        val title = link.title.replace(Regex("\\s+"), " ").trim()
        if (title.isEmpty()) link.url else "$title$SEPARATOR${link.url}"
    }

    /**
     * Cleans up a typed or pasted address, adding "https://" when it is missing. Null when it cannot
     * be a web address.
     */
    fun normalizeUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val url = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val scheme = url.substringBefore("://").lowercase()
        val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        if (scheme != "http" && scheme != "https") return null
        if (!host.contains('.') || host.startsWith('.') || host.endsWith('.')) return null
        return url
    }

    /** A YouTube search for videos showing how to do [exerciseName]. */
    fun youTubeSearchUrl(exerciseName: String): String =
        "https://www.youtube.com/results?search_query=" + URLEncoder.encode("$exerciseName exercise form", "UTF-8")
}
