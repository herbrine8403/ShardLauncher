package com.lanrhyme.shardlauncher.game.modloader.forgelike.forge

import com.lanrhyme.shardlauncher.game.addons.mirror.mapMirrorableUrls
import com.lanrhyme.shardlauncher.game.modloader.forgelike.ForgeVersion
import com.lanrhyme.shardlauncher.game.modloader.forgelike.NeoForgeVersion
import com.lanrhyme.shardlauncher.game.modloader.forgelike.forge.ForgeVersionToken.ForgeFile
import com.lanrhyme.shardlauncher.settings.AllSettings
import com.lanrhyme.shardlauncher.settings.enums.MirrorSourceType
import com.lanrhyme.shardlauncher.utils.logging.Logger
import com.lanrhyme.shardlauncher.utils.network.downloadAndParseJson
import com.lanrhyme.shardlauncher.utils.network.fetchStringFromUrls
import com.lanrhyme.shardlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Forge版本管理器
 * 支持从官方源和BMCLAPI双源获取版本列表
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/main/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModDownload.vb#L626-L697)
 */
object ForgeVersions {
    private const val TAG = "ForgeVersions"
    private const val FORGE_MAVEN_URL = "https://files.minecraftforge.net/maven/net/minecraftforge/forge"

    /**
     * 获取Forge版本列表（支持双源）
     * @param mcVersion Minecraft版本（如"1.12.2", "1.14"）
     * @return 排序后的Forge版本列表，null表示获取失败
     */
    suspend fun fetchForgeList(mcVersion: String): List<ForgeVersion>? = withContext(Dispatchers.Default) {
        try {
            // 根据设置选择数据源优先级
            val versions = when (AllSettings.fetchModLoaderSource.state) {
                MirrorSourceType.MIRROR_FIRST -> {
                    // 优先使用BMCLAPI，失败则fallback到官方源
                    try {
                        fetchFromBMCLAPI(mcVersion)
                    } catch (e: Exception) {
                        Logger.lWarning("BMCLAPI获取失败，尝试官方源: ${e.message}")
                        fetchFromOfficial(mcVersion)
                    }
                }
                else -> {
                    // 优先使用官方源，失败则fallback到BMCLAPI
                    try {
                        fetchFromOfficial(mcVersion)
                    } catch (e: Exception) {
                        Logger.lWarning("官方源获取失败，尝试BMCLAPI: ${e.message}")
                        fetchFromBMCLAPI(mcVersion)
                    }
                }
            }

            // 按构建版本号降序排序（最新的在前）
            versions?.sortedWith { v1, v2 ->
                v2.forgeBuildVersion.compareTo(v1.forgeBuildVersion)
            }
        } catch (e: Exception) {
            Logger.lError("获取Forge列表失败: ${e.message}", e)
            null
        }
    }

    /**
     * 从BMCLAPI获取Forge版本列表
     * 支持所有版本，包括1.12.2、1.14等
     */
    private suspend fun fetchFromBMCLAPI(mcVersion: String): List<ForgeVersion>? = withContext(Dispatchers.IO) {
        Logger.lInfo("从BMCLAPI获取Forge列表: $mcVersion")
        
        val url = "https://bmclapi2.bangbang93.com/forge/minecraft/${mcVersion.replace("-", "_")}"
        
        val tokens: List<ForgeVersionToken> = withRetry("$TAG-BMCLAPI", maxRetries = 2) {
            downloadAndParseJson(
                url = url,
                classOfT = Array<ForgeVersionToken>::class.java
            )?.toList() ?: throw IOException("Failed to parse response")
        }

        tokens.map { token ->
            val (hash, category) = selectPreferredFile(token.files)
            val formattedDate = formatDate(token.modified)

            ForgeVersion(
                versionName = token.version,
                branch = token.branch,
                inherit = mcVersion,
                releaseTime = formattedDate,
                hash = hash,
                isRecommended = false,
                category = category,
                fileVersion = token.branch?.let { "${token.version}-$it" } ?: token.version
            )
        }
    }

    /**
     * 从官方源获取Forge版本列表
     * 通过HTML解析，支持旧版本如1.12.2、1.14等
     */
    private suspend fun fetchFromOfficial(mcVersion: String): List<ForgeVersion>? = withContext(Dispatchers.IO) {
        Logger.lInfo("从官方源获取Forge列表: $mcVersion")
        
        val url = "$FORGE_MAVEN_URL/index_${mcVersion.replace("-", "_")}.html"
        
        val html = withRetry("$TAG-Official", maxRetries = 2) {
            fetchStringFromUrls(listOf(url))
        }

        if (html.length < 100) {
            throw IOException("响应内容过短")
        }

        // 解析HTML表格
        html.split("<td class=\"download-version\"")
            .drop(1) // 跳过第一个空元素
            .mapNotNull { section -> parseVersionFromHtml(section, mcVersion) }
    }

    /**
     * 从HTML片段解析Forge版本信息
     */
    private fun parseVersionFromHtml(html: String, mcVersion: String): ForgeVersion? {
        return try {
            // 提取版本号（如"14.23.5.2860"）
            val versionMatch = Regex("""(?<=\D)\d+(\.\d+)+""").find(html) ?: return null
            val versionName = versionMatch.value
            
            // 检查是否为推荐版本
            val isRecommended = html.contains("promo-latest") || html.contains("promo-recommended")
            
            // 提取分支信息
            val branchMatch = Regex("""(?<=-$versionName-)[^-\"]+(?=-[a-z]+\.[a-z]{3})""").find(html)
            val branch = branchMatch?.value
            
            // 提取发布时间
            val timeMatch = Regex("""(?<=\"download-time\" title=\")[^\"]+""").find(html) ?: return null
            val timeStr = timeMatch.value
            
            val dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
            
            // 确定文件类型和哈希
            val (category, hash) = when {
                html.contains("classifier-installer") -> parseInstallerInfo(html)
                html.contains("classifier-universal") -> parseUniversalInfo(html)
                html.contains("client.zip") -> parseClientInfo(html)
                else -> return null
            } ?: return null

            ForgeVersion(
                versionName = versionName,
                branch = branch,
                inherit = mcVersion,
                releaseTime = dateTime,
                hash = hash,
                isRecommended = isRecommended,
                category = category,
                fileVersion = "$versionName${branch?.let { "-$it" } ?: ""}"
            )
        } catch (e: Exception) {
            Logger.lWarning("解析Forge版本失败: ${e.message}")
            null
        }
    }

    /**
     * 解析installer类型文件信息
     * 支持范围: 1.5.2-1.6.1
     */
    private fun parseInstallerInfo(html: String): Pair<String, String>? {
        val section = html.substringAfter("installer.jar")
        val hash = Regex("""(?<=MD5:</strong> )[^<]+""").find(section)?.value?.trim()
        return hash?.let { "installer" to it }
    }

    /**
     * 解析universal类型文件信息
     * 支持范围: 1.3.2-1.6.1
     */
    private fun parseUniversalInfo(html: String): Pair<String, String>? {
        val section = html.substringAfter("universal.zip")
        val hash = Regex("""(?<=MD5:</strong> )[^<]+""").find(section)?.value?.trim()
        return hash?.let { "universal" to it }
    }

    /**
     * 解析client类型文件信息
     * 支持范围: 1.3.2及更早版本
     */
    private fun parseClientInfo(html: String): Pair<String, String>? {
        val section = html.substringAfter("client.zip")
        val hash = Regex("""(?<=MD5:</strong> )[^<]+""").find(section)?.value?.trim()
        return hash?.let { "client" to it }
    }

    /**
     * 选择首选文件（按优先级）
     */
    private fun selectPreferredFile(files: List<ForgeFile>): Pair<String?, String> {
        var hash: String? = null
        var category = "unknown"
        var priority = -1

        fun updateSelection(file: ForgeFile, newCategory: String, newPriority: Int) {
            hash = file.hash
            category = newCategory
            priority = newPriority
        }

        files.forEach { file ->
            when {
                file.isInstallerJar() && priority <= 2 -> updateSelection(file, "installer", 2)
                file.isUniversalZip() && priority <= 1 -> updateSelection(file, "universal", 1)
                file.isClientZip() && priority <= 0 -> updateSelection(file, "client", 0)
            }
        }
        return hash to category
    }

    /**
     * 格式化日期字符串
     */
    private fun formatDate(modified: String): String {
        return try {
            ZonedDateTime.parse(modified, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
        } catch (e: Exception) {
            modified // 如果解析失败，返回原始字符串
        }
    }

    /**
     * 获取Forge下载URL
     */
    fun getDownloadUrl(version: ForgeVersion): String {
        val baseUrl = "$FORGE_MAVEN_URL/${version.inherit}-${version.fileVersion}"
        return "$baseUrl/forge-${version.inherit}-${version.fileVersion}-${version.category}.${version.fileExtension}"
            .mapMirrorableUrls()
            .first()
    }

    /**
     * 获取NeoForge下载URL
     */
    fun getNeoForgeDownloadUrl(neoForgeVersion: NeoForgeVersion): String {
        val baseUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge"
        return "$baseUrl/${neoForgeVersion.inherit}-${neoForgeVersion.versionName}/neoforge-${neoForgeVersion.inherit}-${neoForgeVersion.versionName}-installer.jar"
            .mapMirrorableUrls()
            .first()
    }
}
