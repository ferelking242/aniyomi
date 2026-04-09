package eu.kanade.tachiyomi.source.internal.model

data class Episode(
    val id: String,
    val animeId: String,
    val title: String,
    val url: String,
    val number: Float = -1f,
    val uploadDate: Long = 0L,
    val scanlator: String? = null,
)
