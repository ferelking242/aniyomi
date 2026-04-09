package eu.kanade.tachiyomi.zeus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ZeusUpdater(
    private val context: Context,
    private val prefs: ZeusPreferences,
) {

    private val json = Json { ignoreUnknownKeys = true }

    sealed class UpdateResult {
        data class NewVersion(val version: String, val downloadUrl: String) : UpdateResult()
        object UpToDate : UpdateResult()
        data class Error(val message: String, val cause: Throwable? = null) : UpdateResult()
    }

    suspend fun checkForUpdate(forceCheck: Boolean = false): UpdateResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceCheck && now - prefs.lastCheckTimestamp < ZeusPreferences.CHECK_INTERVAL_MS) {
            logcat { "ZeusUpdater: skipping check, within interval" }
            return@withContext UpdateResult.UpToDate
        }

        try {
            val release = fetchLatestRelease()
            prefs.lastCheckTimestamp = now

            val latestVersion = release.tagName.removePrefix("v")
            val currentVersion = prefs.installedVersion

            if (currentVersion.isNotEmpty() && currentVersion == latestVersion) {
                logcat { "ZeusUpdater: already on latest version $latestVersion" }
                return@withContext UpdateResult.UpToDate
            }

            val arch = ZeusArch.current()
            val asset = release.assets.firstOrNull { asset ->
                asset.name.contains(arch.suffix, ignoreCase = true) &&
                    !asset.name.endsWith(".sha256")
            }

            if (asset == null) {
                logcat(LogPriority.WARN) { "ZeusUpdater: no asset found for arch ${arch.suffix}" }
                return@withContext UpdateResult.Error("No binary found for architecture ${arch.suffix}")
            }

            logcat { "ZeusUpdater: new version available $latestVersion (current: $currentVersion)" }
            UpdateResult.NewVersion(latestVersion, asset.downloadUrl)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "ZeusUpdater: check failed — ${e.message}" }
            UpdateResult.Error(e.message ?: "Unknown error", e)
        }
    }

    suspend fun downloadAndInstall(
        version: String,
        downloadUrl: String,
        onProgress: ((Int) -> Unit)? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val arch = ZeusArch.current()
            val release = fetchLatestRelease()

            val checksumAsset = release.assets.firstOrNull { asset ->
                asset.name.contains(arch.suffix, ignoreCase = true) &&
                    asset.name.endsWith(".sha256")
            }

            val tempFile = File(context.cacheDir, "zeusdl_update_$version")
            downloadFile(downloadUrl, tempFile, onProgress)

            if (checksumAsset != null) {
                val expectedChecksum = fetchText(checksumAsset.downloadUrl).trim()
                    .split("\\s+".toRegex()).first()
                val actualChecksum = tempFile.sha256()
                if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                    tempFile.delete()
                    logcat(LogPriority.ERROR) { "ZeusUpdater: SHA256 mismatch. Expected $expectedChecksum, got $actualChecksum" }
                    return@withContext Result.failure(
                        SecurityException("Binary checksum mismatch — possible corruption or tampering"),
                    )
                }
                logcat { "ZeusUpdater: SHA256 verified ✓" }
            } else {
                logcat(LogPriority.WARN) { "ZeusUpdater: no checksum file found, skipping verification" }
            }

            val targetDir = File(context.filesDir, "zeus")
            if (!targetDir.exists()) targetDir.mkdirs()

            val targetFile = File(targetDir, "zeusdl")
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
            targetFile.setExecutable(true, false)

            prefs.installedVersion = version
            prefs.binaryArch = arch.suffix

            logcat { "ZeusUpdater: installed ZeusDL $version for ${arch.suffix}" }
            Result.success(Unit)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "ZeusUpdater: install failed — ${e.message}" }
            Result.failure(e)
        }
    }

    private fun fetchLatestRelease(): GithubRelease {
        val repo = prefs.githubRepo
        val url = "https://api.github.com/repos/$repo/releases/latest"
        val text = fetchText(url)
        return json.decodeFromString(text)
    }

    private fun fetchText(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        val pat = prefs.githubPat
        if (pat.isNotEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $pat")
        }
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(urlString: String, dest: File, onProgress: ((Int) -> Unit)?) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        val pat = prefs.githubPat
        if (pat.isNotEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $pat")
        }
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000

        try {
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            onProgress?.invoke(((downloaded * 100) / total).toInt())
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
