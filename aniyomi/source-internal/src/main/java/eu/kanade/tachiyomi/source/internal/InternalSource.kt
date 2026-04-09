package eu.kanade.tachiyomi.source.internal

import eu.kanade.tachiyomi.source.internal.model.Anime
import eu.kanade.tachiyomi.source.internal.model.Episode

interface InternalSource {

    val id: Long

    val name: String

    val lang: String

    suspend fun getPopular(page: Int): List<Anime>

    suspend fun getLatest(page: Int): List<Anime>

    suspend fun search(query: String, page: Int): List<Anime>

    suspend fun getAnimeDetails(anime: Anime): Anime

    suspend fun getEpisodes(anime: Anime): List<Episode>

    suspend fun getVideo(episode: Episode): VideoSource
}

sealed class VideoSource {
    data class Direct(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val title: String = "",
    ) : VideoSource()

    data class Zeus(
        val url: String,
        val title: String = "",
    ) : VideoSource()
}
