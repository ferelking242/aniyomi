package eu.kanade.tachiyomi.zeus

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

class ZeusUpdateManager(private val context: Context) {

    val prefs = ZeusPreferences(context)
    private val updater = ZeusUpdater(context, prefs)
    private val notifier = ZeusUpdateNotifier(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<ZeusUpdateState>(ZeusUpdateState.Idle)
    val state: StateFlow<ZeusUpdateState> = _state.asStateFlow()

    fun checkForUpdate(forceCheck: Boolean = false, silent: Boolean = true) {
        scope.launch {
            _state.value = ZeusUpdateState.Checking
            when (val result = updater.checkForUpdate(forceCheck)) {
                is ZeusUpdater.UpdateResult.NewVersion -> {
                    _state.value = ZeusUpdateState.UpdateAvailable(result.version, result.downloadUrl)
                    if (!silent) notifier.notifyUpdateAvailable(result.version)
                }
                is ZeusUpdater.UpdateResult.UpToDate -> {
                    _state.value = ZeusUpdateState.UpToDate
                }
                is ZeusUpdater.UpdateResult.Error -> {
                    logcat(LogPriority.WARN) { "ZeusUpdateManager: ${result.message}" }
                    _state.value = ZeusUpdateState.Error(result.message)
                }
            }
        }
    }

    fun downloadAndInstall(version: String, downloadUrl: String) {
        scope.launch {
            _state.value = ZeusUpdateState.Downloading(0)
            val result = updater.downloadAndInstall(version, downloadUrl) { percent ->
                _state.value = ZeusUpdateState.Downloading(percent)
                notifier.notifyDownloadProgress(percent)
            }
            notifier.cancelDownloadProgress()
            result.fold(
                onSuccess = {
                    _state.value = ZeusUpdateState.InstallSuccess(version)
                    notifier.notifyInstallSuccess(version)
                    logcat { "ZeusUpdateManager: installed $version" }
                },
                onFailure = { e ->
                    val msg = e.message ?: "Unknown error"
                    _state.value = ZeusUpdateState.Error(msg)
                    notifier.notifyError(msg)
                },
            )
        }
    }

    fun currentVersion(): String = prefs.installedVersion.ifEmpty { "Not installed" }

    fun currentArch(): String = prefs.binaryArch.ifEmpty { ZeusArch.current().suffix }
}
