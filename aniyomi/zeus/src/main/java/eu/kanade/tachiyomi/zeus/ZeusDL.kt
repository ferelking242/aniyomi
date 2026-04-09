package eu.kanade.tachiyomi.zeus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat
import java.io.File

class ZeusDL(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val binaryFile: File by lazy {
        File(context.filesDir, "zeus/zeusdl")
    }

    suspend fun ensureInstalled() = withContext(Dispatchers.IO) {
        val dir = binaryFile.parentFile ?: return@withContext
        if (!dir.exists()) dir.mkdirs()

        if (!binaryFile.exists() || !binaryFile.canExecute()) {
            copyBinaryFromAssets()
        }
    }

    private fun copyBinaryFromAssets() {
        val assetName = "zeus/zeusdl"
        context.assets.open(assetName).use { input ->
            binaryFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        binaryFile.setExecutable(true, false)
        logcat { "ZeusDL binary installed at ${binaryFile.absolutePath}" }
    }

    suspend fun extract(url: String): ZeusResult = withContext(Dispatchers.IO) {
        ensureReady()

        val process = ProcessBuilder(
            binaryFile.absolutePath,
            "--dump-json",
            "--no-playlist",
            "--no-warnings",
            "--no-interactive",
            url,
        )
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            logcat(LogPriority.ERROR) { "ZeusDL extract failed (exit $exitCode): $stderr" }
            throw ZeusError.ProcessFailed(exitCode, stderr)
        }

        try {
            json.decodeFromString<ZeusResult>(stdout)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "ZeusDL parse error: ${e.message}" }
            throw ZeusError.ParseError(e.message ?: "Unknown parse error")
        }
    }

    fun download(
        url: String,
        outputPath: String,
        taskId: String,
        onProgress: ((ZeusProgress) -> Unit)? = null,
    ): Flow<ZeusProgress> = flow {
        ensureReady()

        val process = ProcessBuilder(
            binaryFile.absolutePath,
            "--no-playlist",
            "--no-warnings",
            "--no-interactive",
            "--newline",
            "--progress-template",
            "%(progress)j",
            "-o", outputPath,
            url,
        )
            .redirectErrorStream(false)
            .start()

        process.inputStream.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("{")) {
                    try {
                        val progress = json.decodeFromString<ZeusProgress>(trimmed)
                        emit(progress)
                        onProgress?.invoke(progress)
                    } catch (_: Exception) {
                    }
                }
                line = reader.readLine()
            }
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val stderr = process.errorStream.bufferedReader().readText()
            logcat(LogPriority.ERROR) { "ZeusDL download failed (exit $exitCode) taskId=$taskId: $stderr" }
            throw ZeusError.ProcessFailed(exitCode, stderr)
        }
    }.flowOn(Dispatchers.IO)

    private fun ensureReady() {
        if (!binaryFile.exists() || !binaryFile.canExecute()) {
            throw ZeusError.BinaryNotFound()
        }
    }
}
