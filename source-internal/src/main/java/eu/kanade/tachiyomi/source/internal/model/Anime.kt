package eu.kanade.tachiyomi.source.internal.model

data class Anime(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val status: AnimeStatus = AnimeStatus.Unknown,
    val author: String? = null,
)

enum class AnimeStatus {
    Unknown,
    Ongoing,
    Completed,
}
