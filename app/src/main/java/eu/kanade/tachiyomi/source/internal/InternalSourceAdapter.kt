package eu.kanade.tachiyomi.source.internal

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.source.internal.model.Anime
import eu.kanade.tachiyomi.source.internal.model.Episode

class InternalSourceAdapter(
    private val delegate: InternalSource,
) : AnimeCatalogueSource {

    override val id: Long = delegate.id
    override val name: String = delegate.name
    override val lang: String = delegate.lang
    override val supportsLatest: Boolean = true

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        val animes = delegate.getPopular(page)
        return AnimesPage(animes.map { it.toSAnime() }, animes.isNotEmpty())
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage {
        val animes = delegate.getLatest(page)
        return AnimesPage(animes.map { it.toSAnime() }, animes.isNotEmpty())
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        val animes = delegate.search(query, page)
        return AnimesPage(animes.map { it.toSAnime() }, animes.isNotEmpty())
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        val stub = Anime(id = anime.url, title = anime.title, url = anime.url, thumbnailUrl = anime.thumbnail_url)
        val updated = delegate.getAnimeDetails(stub)
        return updated.toSAnime()
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        val stub = Anime(id = anime.url, title = anime.title, url = anime.url)
        return delegate.getEpisodes(stub).map { it.toSEpisode() }
    }

    override suspend fun getHosterList(episode: SEpisode): List<Hoster> {
        val stub = Episode(
            id = episode.url,
            animeId = "",
            title = episode.name,
            url = episode.url,
            number = episode.episode_number,
        )
        val videoSource = delegate.getVideo(stub)
        val hosterUrl = when (videoSource) {
            is VideoSource.Direct -> videoSource.url
            is VideoSource.Zeus -> videoSource.url
        }
        val hosterName = when (videoSource) {
            is VideoSource.Direct -> "Direct"
            is VideoSource.Zeus -> "ZeusDL"
        }
        return listOf(Hoster(hosterUrl = hosterUrl, hosterName = hosterName))
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> = emptyList()

    override suspend fun getSeasonList(anime: SAnime): List<SAnime> = emptyList()

    override fun getFilterList(): AnimeFilterList = AnimeFilterList()

    override fun toString(): String = "$name (${lang.uppercase()}) [internal]"

    private fun Anime.toSAnime(): SAnime = SAnime.create().apply {
        url = this@toSAnime.url
        title = this@toSAnime.title
        thumbnail_url = this@toSAnime.thumbnailUrl
        description = this@toSAnime.description
        genre = this@toSAnime.genres.joinToString()
        author = this@toSAnime.author
    }

    private fun Episode.toSEpisode(): SEpisode = SEpisode.create().apply {
        url = this@toSEpisode.url
        name = this@toSEpisode.title
        episode_number = this@toSEpisode.number
        date_upload = this@toSEpisode.uploadDate
        scanlator = this@toSEpisode.scanlator
    }
}
