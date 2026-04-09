package eu.kanade.tachiyomi.zeus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
    @SerialName("html_url") val htmlUrl: String = "",
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L,
)

enum class ZeusArch(val suffix: String, val abiList: List<String>) {
    ARM64("arm64-v8a", listOf("arm64-v8a")),
    ARM_V7("armeabi-v7a", listOf("armeabi-v7a", "armeabi")),
    X86_64("x86_64", listOf("x86_64")),
    X86("x86", listOf("x86"));

    companion object {
        fun current(): ZeusArch {
            val supported = android.os.Build.SUPPORTED_ABIS.toList()
            return entries.firstOrNull { arch ->
                arch.abiList.any { it in supported }
            } ?: ARM64
        }
    }
}
