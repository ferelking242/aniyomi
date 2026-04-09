package eu.kanade.tachiyomi.source

import okhttp3.Headers

sealed class VideoSource {
    data class Direct(
        val url: String,
        val headers: Headers? = null,
        val title: String = "",
    ) : VideoSource()

    data class Zeus(
        val url: String,
        val title: String = "",
    ) : VideoSource()
}
