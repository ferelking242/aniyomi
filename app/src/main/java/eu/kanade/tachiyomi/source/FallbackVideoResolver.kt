package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.zeus.ZeusError
import logcat.LogPriority
import logcat.logcat

class FallbackVideoResolver(
    private val engine: VideoResolutionEngine,
) {

    suspend fun resolveWithFallback(
        primary: VideoSource,
        fallback: VideoSource? = null,
    ): ResolvedVideo {
        return try {
            engine.resolve(primary)
        } catch (e: ZeusError) {
            logcat(LogPriority.WARN) { "Primary resolution failed (${e.message}), trying fallback" }
            if (fallback != null) {
                try {
                    engine.resolve(fallback)
                } catch (fe: Exception) {
                    logcat(LogPriority.ERROR) { "Fallback resolution also failed: ${fe.message}" }
                    throw fe
                }
            } else {
                throw e
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Unexpected resolution error: ${e.message}" }
            throw e
        }
    }
}
