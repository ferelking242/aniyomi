package eu.kanade.tachiyomi.zeus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import java.util.concurrent.ConcurrentHashMap

class ZeusDownloadEngine(private val zeus: ZeusDL) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val progressMap = ConcurrentHashMap<String, MutableStateFlow<ZeusProgress?>>()

    fun start(url: String, outputPath: String, taskId: String) {
        if (activeJobs.containsKey(taskId)) {
            logcat(LogPriority.WARN) { "Task $taskId already running" }
            return
        }

        val progressFlow = MutableStateFlow<ZeusProgress?>(null)
        progressMap[taskId] = progressFlow

        val job = scope.launch {
            try {
                zeus.download(url, outputPath, taskId).collect { progress ->
                    progressFlow.value = progress
                }
            } catch (e: ZeusError) {
                logcat(LogPriority.ERROR) { "Download error taskId=$taskId: ${e.message}" }
            } finally {
                activeJobs.remove(taskId)
            }
        }

        activeJobs[taskId] = job
    }

    fun cancel(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        progressMap.remove(taskId)
        logcat { "Cancelled task $taskId" }
    }

    fun getProgress(taskId: String): StateFlow<ZeusProgress?> {
        return progressMap.getOrPut(taskId) { MutableStateFlow(null) }.asStateFlow()
    }

    fun isRunning(taskId: String): Boolean {
        return activeJobs[taskId]?.isActive == true
    }

    fun cancelAll() {
        activeJobs.keys.toList().forEach { cancel(it) }
        scope.cancel()
    }
}
