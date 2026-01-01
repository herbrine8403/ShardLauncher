/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.multirt

import android.content.Context
import com.lanrhyme.shardlauncher.utils.device.Architecture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * Runtime installer for downloading and installing Java runtimes
 */
object RuntimeInstaller {
    
    /**
     * Available runtime downloads
     */
    data class RuntimeDownload(
        val name: String,
        val displayName: String,
        val description: String,
        val javaVersion: Int,
        val architecture: String,
        val downloadUrl: String,
        val size: Long = 0L
    )

    /**
     * Get available runtime downloads
     */
    fun getAvailableRuntimes(): List<RuntimeDownload> {
        return listOf(
            RuntimeDownload(
                name = "openjdk-8-arm64",
                displayName = "OpenJDK 8 (ARM64)",
                description = "适用于 ARM64 设备的 OpenJDK 8",
                javaVersion = 8,
                architecture = "arm64-v8a",
                downloadUrl = "https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u/releases/download/jdk8u372-b07-pojav/openjdk-8-aarch64.tar.xz",
                size = 45 * 1024 * 1024 // ~45MB
            ),
            RuntimeDownload(
                name = "openjdk-8-arm32",
                displayName = "OpenJDK 8 (ARM32)",
                description = "适用于 ARM32 设备的 OpenJDK 8",
                javaVersion = 8,
                architecture = "armeabi-v7a",
                downloadUrl = "https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u/releases/download/jdk8u372-b07-pojav/openjdk-8-arm.tar.xz",
                size = 42 * 1024 * 1024 // ~42MB
            ),
            RuntimeDownload(
                name = "openjdk-17-arm64",
                displayName = "OpenJDK 17 (ARM64)",
                description = "适用于 ARM64 设备的 OpenJDK 17",
                javaVersion = 17,
                architecture = "arm64-v8a",
                downloadUrl = "https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk17u/releases/download/jdk-17.0.7%2B7-pojav/openjdk-17-aarch64.tar.xz",
                size = 55 * 1024 * 1024 // ~55MB
            ),
            RuntimeDownload(
                name = "openjdk-17-arm32",
                displayName = "OpenJDK 17 (ARM32)",
                description = "适用于 ARM32 设备的 OpenJDK 17",
                javaVersion = 17,
                architecture = "armeabi-v7a",
                downloadUrl = "https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk17u/releases/download/jdk-17.0.7%2B7-pojav/openjdk-17-arm.tar.xz",
                size = 52 * 1024 * 1024 // ~52MB
            ),
            RuntimeDownload(
                name = "openjdk-21-arm64",
                displayName = "OpenJDK 21 (ARM64)",
                description = "适用于 ARM64 设备的 OpenJDK 21",
                javaVersion = 21,
                architecture = "arm64-v8a",
                downloadUrl = "https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk21u/releases/download/jdk-21.0.3%2B9-pojav/openjdk-21-aarch64.tar.xz",
                size = 60 * 1024 * 1024 // ~60MB
            )
        )
    }

    /**
     * Get compatible runtimes for current device
     */
    fun getCompatibleRuntimes(): List<RuntimeDownload> {
        val deviceArch = Architecture.getDeviceArchitecture()
        return getAvailableRuntimes().filter { runtime ->
            when (deviceArch) {
                Architecture.ARCH_ARM64 -> runtime.architecture == "arm64-v8a"
                Architecture.ARCH_ARM -> runtime.architecture == "armeabi-v7a"
                Architecture.ARCH_X86_64 -> runtime.architecture == "x86_64"
                Architecture.ARCH_X86 -> runtime.architecture == "x86"
                else -> false
            }
        }
    }

    /**
     * Download and install runtime
     */
    suspend fun downloadAndInstallRuntime(
        runtime: RuntimeDownload,
        onProgress: (progress: Int, message: String) -> Unit = { _, _ -> }
    ): Result<Runtime> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "开始下载 ${runtime.displayName}...")
            
            val url = URL(runtime.downloadUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val totalSize = connection.contentLength
            var downloadedSize = 0L
            
            connection.getInputStream().use { inputStream ->
                val bufferedInputStream = inputStream.buffered()
                
                // Create a progress tracking input stream
                val progressInputStream = object : InputStream() {
                    override fun read(): Int {
                        val byte = bufferedInputStream.read()
                        if (byte != -1) {
                            downloadedSize++
                            if (downloadedSize % 8192 == 0L) { // Update every 8KB
                                val progress = if (totalSize > 0) {
                                    (downloadedSize * 100 / totalSize).toInt()
                                } else 0
                                onProgress(progress, "下载中... ${formatSize(downloadedSize)}")
                            }
                        }
                        return byte
                    }
                    
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        val bytesRead = bufferedInputStream.read(b, off, len)
                        if (bytesRead > 0) {
                            downloadedSize += bytesRead
                            val progress = if (totalSize > 0) {
                                (downloadedSize * 100 / totalSize).toInt()
                            } else 0
                            onProgress(progress, "下载中... ${formatSize(downloadedSize)}")
                        }
                        return bytesRead
                    }
                }
                
                onProgress(50, "正在安装 ${runtime.displayName}...")
                
                val installedRuntime = RuntimesManager.installRuntime(
                    inputStream = progressInputStream,
                    name = runtime.name
                ) { progress: Int, args: Array<Any> ->
                    onProgress(50 + progress / 2, "安装中... ${args.getOrNull(0) ?: ""}")
                }
                
                onProgress(100, "安装完成")
                Result.success(installedRuntime)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Install runtime from local file
     */
    suspend fun installRuntimeFromFile(
        inputStream: InputStream,
        name: String,
        onProgress: (progress: Int, message: String) -> Unit = { _, _ -> }
    ): Result<Runtime> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "开始安装运行时...")
            
            val installedRuntime = RuntimesManager.installRuntime(
                inputStream = inputStream,
                name = name
            ) { progress: Int, args: Array<Any> ->
                onProgress(progress, "安装中... ${args.getOrNull(0) ?: ""}")
            }
            
            onProgress(100, "安装完成")
            Result.success(installedRuntime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Format file size for display
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}