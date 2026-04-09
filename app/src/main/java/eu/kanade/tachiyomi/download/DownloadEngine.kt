package eu.kanade.tachiyomi.download

import eu.kanade.tachiyomi.source.VideoSource
import kotlinx.coroutines.flow.StateFlow

interface DownloadEngine {

    fun start(videoSource: VideoSource, outputPath: String, taskId: String)

    fun pause(taskId: String)

    fun resume(taskId: String)

    fun cancel(taskId: String)

    fun getProgress(taskId: String): StateFlow<DownloadProgress?>

    fun isRunning(taskId: String): Boolean
}

data class DownloadProgress(
    val taskId: String,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val speedBytesPerSec: Double?,
    val etaSeconds: Double?,
    val status: Status,
) {
    enum class Status { QUEUED, DOWNLOADING, PAUSED, COMPLETED, ERROR }

    val progressPercent: Float?
        get() = totalBytes?.let { total ->
            if (total > 0) (downloadedBytes.toFloat() / total.toFloat()) * 100f else null
        }
}
