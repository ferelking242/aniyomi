package eu.kanade.tachiyomi.download

import eu.kanade.tachiyomi.source.VideoSource
import eu.kanade.tachiyomi.zeus.ZeusDL
import eu.kanade.tachiyomi.zeus.ZeusDownloadEngine
import eu.kanade.tachiyomi.zeus.ZeusProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

class ZeusDownloadEngineImpl(zeus: ZeusDL) : DownloadEngine {

    private val engine = ZeusDownloadEngine(zeus)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val progressCache = HashMap<String, MutableStateFlow<DownloadProgress?>>()
    private val pausedUrls = HashMap<String, Pair<String, String>>()

    override fun start(videoSource: VideoSource, outputPath: String, taskId: String) {
        val url = when (videoSource) {
            is VideoSource.Direct -> videoSource.url
            is VideoSource.Zeus -> videoSource.url
        }

        val progressFlow = MutableStateFlow<DownloadProgress?>(
            DownloadProgress(
                taskId = taskId,
                downloadedBytes = 0,
                totalBytes = null,
                speedBytesPerSec = null,
                etaSeconds = null,
                status = DownloadProgress.Status.QUEUED,
            ),
        )
        progressCache[taskId] = progressFlow

        scope.launch {
            engine.getProgress(taskId).collect { zeusProgress ->
                zeusProgress?.let {
                    progressFlow.value = it.toDownloadProgress(taskId)
                }
            }
        }

        engine.start(url, outputPath, taskId)
        logcat { "Started ZeusDL download: taskId=$taskId url=$url" }
    }

    override fun pause(taskId: String) {
        logcat(LogPriority.WARN) { "ZeusDL pause is not supported natively; cancelling task $taskId" }
        engine.cancel(taskId)
    }

    override fun resume(taskId: String) {
        val paused = pausedUrls[taskId] ?: return
        engine.start(paused.first, paused.second, taskId)
        pausedUrls.remove(taskId)
    }

    override fun cancel(taskId: String) {
        engine.cancel(taskId)
        progressCache.remove(taskId)
        pausedUrls.remove(taskId)
        logcat { "Cancelled ZeusDL download: taskId=$taskId" }
    }

    override fun getProgress(taskId: String): StateFlow<DownloadProgress?> {
        return progressCache.getOrPut(taskId) { MutableStateFlow(null) }.asStateFlow()
    }

    override fun isRunning(taskId: String): Boolean = engine.isRunning(taskId)

    private fun ZeusProgress.toDownloadProgress(taskId: String): DownloadProgress {
        val mappedStatus = when (status) {
            "downloading" -> DownloadProgress.Status.DOWNLOADING
            "finished" -> DownloadProgress.Status.COMPLETED
            "error" -> DownloadProgress.Status.ERROR
            else -> DownloadProgress.Status.QUEUED
        }
        return DownloadProgress(
            taskId = taskId,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speedBytesPerSec = speed,
            etaSeconds = eta,
            status = mappedStatus,
        )
    }
}
