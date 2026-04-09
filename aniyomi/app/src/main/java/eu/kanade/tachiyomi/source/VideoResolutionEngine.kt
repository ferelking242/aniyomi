package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.zeus.ZeusDL
import eu.kanade.tachiyomi.zeus.ZeusError
import eu.kanade.tachiyomi.zeus.ZeusResult
import logcat.LogPriority
import logcat.logcat
import okhttp3.Headers

class VideoResolutionEngine(private val zeus: ZeusDL) {

    private val cache = HashMap<String, ResolvedVideo>()

    suspend fun resolve(source: VideoSource): ResolvedVideo {
        val cacheKey = source.cacheKey()
        cache[cacheKey]?.let { return it }

        val resolved = when (source) {
            is VideoSource.Direct -> ResolvedVideo(
                url = source.url,
                headers = source.headers,
                title = source.title,
                resolvedViaZeus = false,
            )

            is VideoSource.Zeus -> resolveWithZeus(source)
        }

        cache[cacheKey] = resolved
        return resolved
    }

    private suspend fun resolveWithZeus(source: VideoSource.Zeus): ResolvedVideo {
        return try {
            val result = zeus.extract(source.url)
            val bestFormat = result.bestFormat()
            logcat { "ZeusDL resolved ${source.url} → ${bestFormat ?: result.url}" }
            ResolvedVideo(
                url = bestFormat ?: result.url,
                headers = null,
                title = result.title.ifBlank { source.title },
                resolvedViaZeus = true,
                zeusResult = result,
            )
        } catch (e: ZeusError) {
            logcat(LogPriority.ERROR) { "ZeusDL extraction failed for ${source.url}: ${e.message}" }
            throw e
        }
    }

    fun invalidate(source: VideoSource) {
        cache.remove(source.cacheKey())
    }

    fun clearCache() {
        cache.clear()
    }

    private fun VideoSource.cacheKey(): String = when (this) {
        is VideoSource.Direct -> "direct:$url"
        is VideoSource.Zeus -> "zeus:$url"
    }

    private fun ZeusResult.bestFormat(): String? {
        return formats
            .filter { it.videoExt != "none" }
            .maxByOrNull { it.tbr ?: 0.0 }
            ?.url
    }
}

data class ResolvedVideo(
    val url: String,
    val headers: Headers?,
    val title: String,
    val resolvedViaZeus: Boolean,
    val zeusResult: ZeusResult? = null,
)
