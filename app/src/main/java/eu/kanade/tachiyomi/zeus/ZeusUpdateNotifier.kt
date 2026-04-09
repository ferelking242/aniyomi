package eu.kanade.tachiyomi.zeus

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify

class ZeusUpdateNotifier(private val context: Context) {

    private val builder = context.notificationBuilder(Notifications.CHANNEL_ZEUS_UPDATE)

    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_ZEUS_UPDATE) {
        context.notify(id, build())
    }

    fun notifyUpdateAvailable(version: String) {
        with(builder) {
            setContentTitle("ZeusDL update available")
            setContentText("Version $version is ready to install")
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setAutoCancel(true)
        }
        builder.show()
    }

    fun notifyDownloadProgress(percent: Int) {
        with(builder) {
            setContentTitle("Downloading ZeusDL…")
            setContentText("$percent%")
            setSmallIcon(android.R.drawable.stat_sys_download)
            setProgress(100, percent, percent == 0)
            setOngoing(true)
            setAutoCancel(false)
        }
        builder.show(Notifications.ID_ZEUS_DOWNLOAD_PROGRESS)
    }

    fun notifyInstallSuccess(version: String) {
        context.notify(
            Notifications.ID_ZEUS_DOWNLOAD_PROGRESS,
            context.notificationBuilder(Notifications.CHANNEL_ZEUS_UPDATE) {
                setContentTitle("ZeusDL updated")
                setContentText("Version $version installed successfully")
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                setAutoCancel(true)
            }.build(),
        )
    }

    fun notifyError(message: String) {
        context.notify(
            Notifications.ID_ZEUS_UPDATE_ERROR,
            context.notificationBuilder(Notifications.CHANNEL_ZEUS_UPDATE) {
                setContentTitle("ZeusDL update failed")
                setContentText(message)
                setSmallIcon(android.R.drawable.stat_notify_error)
                setAutoCancel(true)
            }.build(),
        )
    }

    fun cancelDownloadProgress() {
        context.getSystemService(android.app.NotificationManager::class.java)
            ?.cancel(Notifications.ID_ZEUS_DOWNLOAD_PROGRESS)
    }
}
