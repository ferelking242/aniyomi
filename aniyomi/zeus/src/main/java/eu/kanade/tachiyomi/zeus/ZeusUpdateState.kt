package eu.kanade.tachiyomi.zeus

sealed class ZeusUpdateState {
    object Idle : ZeusUpdateState()
    object Checking : ZeusUpdateState()
    data class UpdateAvailable(val version: String, val downloadUrl: String) : ZeusUpdateState()
    object UpToDate : ZeusUpdateState()
    data class Downloading(val percent: Int) : ZeusUpdateState()
    data class InstallSuccess(val version: String) : ZeusUpdateState()
    data class Error(val message: String) : ZeusUpdateState()
}
