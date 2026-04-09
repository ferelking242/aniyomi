package eu.kanade.tachiyomi.zeus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZeusResult(
    val url: String,
    val title: String = "",
    val ext: String = "",
    val formats: List<ZeusFormat> = emptyList(),
    val thumbnail: String? = null,
    val duration: Double? = null,
)

@Serializable
data class ZeusFormat(
    @SerialName("format_id") val formatId: String,
    val url: String,
    val ext: String = "",
    val resolution: String? = null,
    val fps: Double? = null,
    val tbr: Double? = null,
    @SerialName("audio_ext") val audioExt: String = "none",
    @SerialName("video_ext") val videoExt: String = "none",
)

@Serializable
data class ZeusProgress(
    val status: String,
    @SerialName("downloaded_bytes") val downloadedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long? = null,
    val speed: Double? = null,
    val eta: Double? = null,
    val filename: String = "",
)

sealed class ZeusError(message: String) : Exception(message) {
    class ExtractionFailed(message: String) : ZeusError(message)
    class ProcessFailed(val exitCode: Int, val stderr: String) : ZeusError("Process exited with code $exitCode: $stderr")
    class BinaryNotFound : ZeusError("ZeusDL binary not found or not executable")
    class ParseError(message: String) : ZeusError("Failed to parse ZeusDL output: $message")
}
