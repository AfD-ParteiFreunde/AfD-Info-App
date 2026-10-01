package de.afd.parteiapp.data

data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val color: String,
    val enabledByDefault: Boolean,
)

data class Article(
    val title: String,
    val link: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val sourceId: String,
    val sourceName: String,
    val sourceColor: String,
)

data class ReaderContent(
    val title: String,
    val imageUrl: String?,
    val paragraphs: List<String>,
    val siteName: String,
)
