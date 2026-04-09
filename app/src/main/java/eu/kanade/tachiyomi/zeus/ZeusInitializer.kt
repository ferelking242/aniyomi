package eu.kanade.tachiyomi.zeus

import android.content.Context
import eu.kanade.tachiyomi.zeus.ZeusDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

object ZeusInitializer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context, zeus: ZeusDL) {
        scope.launch {
            try {
                zeus.ensureInstalled()
                logcat { "ZeusDL initialized successfully" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "ZeusDL initialization failed: ${e.message}" }
            }
        }
    }
}
