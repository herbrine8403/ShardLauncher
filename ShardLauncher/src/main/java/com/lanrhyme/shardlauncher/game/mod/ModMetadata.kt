package com.lanrhyme.shardlauncher.game.mod

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lanrhyme.shardlauncher.game.mod.api.ModrinthApiService
import com.lanrhyme.shardlauncher.utils.file.HashUtils
import com.lanrhyme.shardlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.zip.ZipFile

/**
 * 本地模组信息
 */
data class LocalMod(
    val file: File,
    val fileSize: Long,
    val id: String,
    val loader: ModLoader,
    val name: String,
    val description: String? = null,
    val version: String? = null,
    val authors: List<String>,
    val icon: ByteArray? = null,
    val notMod: Boolean = false
) {
    enum class ModLoader(val displayName: String) {
        FABRIC("Fabric"),
        FORGE("Forge"), 
        QUILT("Quilt"),
        NEOFORGE("NeoForge"),
        UNKNOWN("Unknown")
    }
    
    fun isEnabled(): Boolean = !file.name.endsWith(".disabled", ignoreCase = true)
    fun isDisabled(): Boolean = !isEnabled()
    
    /**
     * 禁用模组
     */
    fun disable() {
        if (isDisabled()) return
        val newFile = File(file.absolutePath + ".disabled")
        if (file.renameTo(newFile)) {
            // 更新file引用需要在外部处理
        }
    }
    
    /**
     * 启用模组
     */
    fun enable() {
        if (isEnabled()) return
        val newPath = file.absolutePath.removeSuffix(".disabled")
        val newFile = File(newPath)
        if (file.renameTo(newFile)) {
            // 更新file引用需要在外部处理
        }
    }
}

/**
 * 模组项目信息（来自API）
 */
@Parcelize
data class ModProject(
    val id: String,
    val platform: Platform,
    val iconUrl: String? = null,
    val title: String,
    val slug: String
): Parcelable

/**
 * 模组文件信息（来自API）
 */
@Parcelize
data class ModFile(
    val id: String,
    val projectId: String,
    val platform: Platform,
    val loaders: Array<ModLoaderDisplayLabel>,
    val datePublished: String
): Parcelable

/**
 * 平台枚举
 */
enum class Platform {
    MODRINTH, CURSEFORGE
}

/**
 * 模组加载器显示标签
 */
@Parcelize
enum class ModLoaderDisplayLabel(private val displayName: String) : Parcelable {
    FABRIC("Fabric"),
    FORGE("Forge"),
    QUILT("Quilt"),
    NEOFORGE("NeoForge"),
    UNKNOWN("Unknown");
    
    fun getDisplayName(): String = displayName
}

/**
 * 远程模组（包含本地信息和远程API信息）
 */
class RemoteMod(
    val localMod: LocalMod
) {
    /**
     * 是否正在加载项目信息
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * 平台对应的文件
     */
    var remoteFile: ModFile? by mutableStateOf(null)
        private set

    /**
     * 项目信息
     */
    var projectInfo: ModProject? by mutableStateOf(null)
        private set

    /**
     * 是否已经加载过
     */
    var isLoaded: Boolean = false
        private set
        
    /**
     * 上次加载是否失败
     */
    var lastLoadFailed: Boolean = false
        private set

    /**
     * 加载远程信息（通过Modrinth API，支持缓存）
     */
    suspend fun load(loadFromCache: Boolean = true) {
        if (loadFromCache && isLoaded) return

        if (!loadFromCache) {
            remoteFile = null
            projectInfo = null
        }

        isLoaded = false
        isLoading = true
        lastLoadFailed = false

        try {
            withContext(Dispatchers.IO) {
                // 计算文件SHA1哈希
                val sha1 = HashUtils.calculateFileSha1(localMod.file)
                
                // 尝试从缓存加载
                if (loadFromCache) {
                    val cachedEntry = ModCache.getCachedModInfo(sha1)
                    if (cachedEntry != null && !ModCache.isCacheExpired(cachedEntry)) {
                        // 使用缓存数据
                        projectInfo = cachedEntry.projectInfo
                        remoteFile = cachedEntry.remoteFile
                        lastLoadFailed = !cachedEntry.loadSuccess
                        isLoaded = true
                        return@withContext
                    }
                }
                
                // 从API加载
                var loadSuccess = false
                try {
                    // 通过SHA1从Modrinth获取版本信息
                    val modrinthVersion = ModrinthApiService.getVersionByFileSha1(sha1)
                    
                    if (modrinthVersion != null) {
                        // 创建ModFile
                        val loaderLabels = modrinthVersion.loaders.mapNotNull { loaderName ->
                            when (loaderName.lowercase()) {
                                "fabric" -> ModLoaderDisplayLabel.FABRIC
                                "forge" -> ModLoaderDisplayLabel.FORGE
                                "quilt" -> ModLoaderDisplayLabel.QUILT
                                "neoforge" -> ModLoaderDisplayLabel.NEOFORGE
                                else -> null
                            }
                        }.toTypedArray()
                        
                        remoteFile = ModFile(
                            id = modrinthVersion.id,
                            projectId = modrinthVersion.projectId,
                            platform = Platform.MODRINTH,
                            loaders = loaderLabels,
                            datePublished = modrinthVersion.datePublished
                        )
                        
                        // 获取项目信息
                        val modrinthProject = ModrinthApiService.getProjectById(modrinthVersion.projectId)
                        if (modrinthProject != null) {
                            projectInfo = ModProject(
                                id = modrinthProject.id,
                                platform = Platform.MODRINTH,
                                iconUrl = modrinthProject.iconUrl,
                                title = modrinthProject.title,
                                slug = modrinthProject.slug
                            )
                        }
                        loadSuccess = true
                    }
                } catch (e: Exception) {
                    Logger.e("RemoteMod", "Failed to load from API for mod: ${localMod.file.name}", e)
                    lastLoadFailed = true
                }
                
                // 缓存结果（无论成功还是失败）
                ModCache.cacheModInfo(sha1, projectInfo, remoteFile, loadSuccess)
                
                isLoaded = true
            }
        } catch (e: Exception) {
            Logger.e("RemoteMod", "Failed to load project info for mod: ${localMod.file.name}", e)
            lastLoadFailed = true
        } finally {
            isLoading = false
        }
    }
}

/**
 * 模组元数据解析器
 */
object ModMetadataParser {
    
    suspend fun parseModFile(modFile: File): LocalMod? = withContext(Dispatchers.IO) {
        if (!modFile.exists() || !modFile.isFile) return@withContext null
        
        try {
            // 尝试各种解析器
            val readers = when (modFile.extension.lowercase()) {
                "jar", "zip" -> listOf(
                    ::parseFabricMod,
                    ::parseQuiltMod,
                    ::parseForgeMod,
                    ::parseNeoForgeMod
                )
                else -> emptyList()
            }
            
            for (reader in readers) {
                val result = reader(modFile)
                if (result != null) return@withContext result
            }
            
            // 如果所有解析器都失败，创建fallback
            createFallbackMod(modFile)
        } catch (e: Exception) {
            Logger.e("ModMetadataParser", "Failed to parse mod file: ${modFile.name}", e)
            createFallbackMod(modFile)
        }
    }
    
    private fun parseFabricMod(modFile: File): LocalMod? {
        return try {
            ZipFile(modFile).use { zipFile ->
                val fabricModJson = zipFile.getEntry("fabric.mod.json") ?: return null
                
                val jsonContent = zipFile.getInputStream(fabricModJson).bufferedReader().use { it.readText() }
                val jsonObject = com.google.gson.JsonParser.parseString(jsonContent).asJsonObject
                
                val id = jsonObject.get("id")?.asString ?: modFile.nameWithoutExtension
                val name = jsonObject.get("name")?.asString ?: id
                val version = jsonObject.get("version")?.asString ?: "Unknown"
                val description = jsonObject.get("description")?.asString ?: ""
                
                val authors = mutableListOf<String>()
                jsonObject.get("authors")?.let { authorsElement ->
                    if (authorsElement.isJsonArray) {
                        authorsElement.asJsonArray.forEach { author ->
                            if (author.isJsonPrimitive) {
                                authors.add(author.asString)
                            } else if (author.isJsonObject) {
                                author.asJsonObject.get("name")?.asString?.let { authors.add(it) }
                            }
                        }
                    } else if (authorsElement.isJsonPrimitive) {
                        authors.add(authorsElement.asString)
                    }
                }
                
                val icon = parseIcon(zipFile, jsonObject.get("icon")?.asString)
                
                LocalMod(
                    file = modFile,
                    fileSize = FileUtils.sizeOf(modFile),
                    id = id,
                    name = name,
                    version = version,
                    description = description,
                    authors = authors,
                    icon = icon,
                    loader = LocalMod.ModLoader.FABRIC
                )
            }
        } catch (e: Exception) {
            Logger.e("ModMetadataParser", "Failed to parse Fabric mod: ${modFile.name}", e)
            null
        }
    }
    
    private fun parseQuiltMod(modFile: File): LocalMod? {
        return try {
            ZipFile(modFile).use { zipFile ->
                val quiltModJson = zipFile.getEntry("quilt.mod.json") ?: return null
                
                val jsonContent = zipFile.getInputStream(quiltModJson).bufferedReader().use { it.readText() }
                val jsonObject = com.google.gson.JsonParser.parseString(jsonContent).asJsonObject
                
                val quiltLoader = jsonObject.get("quilt_loader")?.asJsonObject
                val metadata = quiltLoader?.get("metadata")?.asJsonObject
                
                val id = quiltLoader?.get("id")?.asString ?: modFile.nameWithoutExtension
                val name = metadata?.get("name")?.asString ?: id
                val version = quiltLoader?.get("version")?.asString ?: "Unknown"
                val description = metadata?.get("description")?.asString ?: ""
                
                val authors = mutableListOf<String>()
                metadata?.get("contributors")?.asJsonObject?.entrySet()?.forEach { (author, _) ->
                    authors.add(author)
                }
                
                val icon = parseIcon(zipFile, metadata?.get("icon")?.asString)
                
                LocalMod(
                    file = modFile,
                    fileSize = FileUtils.sizeOf(modFile),
                    id = id,
                    name = name,
                    version = version,
                    description = description,
                    authors = authors,
                    icon = icon,
                    loader = LocalMod.ModLoader.QUILT
                )
            }
        } catch (e: Exception) {
            Logger.e("ModMetadataParser", "Failed to parse Quilt mod: ${modFile.name}", e)
            null
        }
    }
    
    private fun parseForgeMod(modFile: File): LocalMod? {
        return try {
            ZipFile(modFile).use { zipFile ->
                val modsToml = zipFile.getEntry("META-INF/mods.toml") ?: return null
                
                val tomlContent = zipFile.getInputStream(modsToml).bufferedReader().use { it.readText() }
                
                val id = extractTomlValue(tomlContent, "modId") ?: modFile.nameWithoutExtension
                val name = extractTomlValue(tomlContent, "displayName") ?: id
                val version = extractTomlValue(tomlContent, "version") ?: "Unknown"
                val description = extractTomlValue(tomlContent, "description") ?: ""
                val authors = extractTomlValue(tomlContent, "authors")?.split(",")?.map { it.trim() } ?: emptyList()
                
                val icon = parseIcon(zipFile, extractTomlValue(tomlContent, "logoFile"))
                
                LocalMod(
                    file = modFile,
                    fileSize = FileUtils.sizeOf(modFile),
                    id = id,
                    name = name,
                    version = version,
                    description = description,
                    authors = authors,
                    icon = icon,
                    loader = LocalMod.ModLoader.FORGE
                )
            }
        } catch (e: Exception) {
            Logger.e("ModMetadataParser", "Failed to parse Forge mod: ${modFile.name}", e)
            null
        }
    }
    
    private fun parseNeoForgeMod(modFile: File): LocalMod? {
        return try {
            ZipFile(modFile).use { zipFile ->
                val neoforgeToml = zipFile.getEntry("META-INF/neoforge.mods.toml") ?: return null
                
                val tomlContent = zipFile.getInputStream(neoforgeToml).bufferedReader().use { it.readText() }
                
                val id = extractTomlValue(tomlContent, "modId") ?: modFile.nameWithoutExtension
                val name = extractTomlValue(tomlContent, "displayName") ?: id
                val version = extractTomlValue(tomlContent, "version") ?: "Unknown"
                val description = extractTomlValue(tomlContent, "description") ?: ""
                val authors = extractTomlValue(tomlContent, "authors")?.split(",")?.map { it.trim() } ?: emptyList()
                
                val icon = parseIcon(zipFile, extractTomlValue(tomlContent, "logoFile"))
                
                LocalMod(
                    file = modFile,
                    fileSize = FileUtils.sizeOf(modFile),
                    id = id,
                    name = name,
                    version = version,
                    description = description,
                    authors = authors,
                    icon = icon,
                    loader = LocalMod.ModLoader.NEOFORGE
                )
            }
        } catch (e: Exception) {
            Logger.e("ModMetadataParser", "Failed to parse NeoForge mod: ${modFile.name}", e)
            null
        }
    }
    
    private fun parseIcon(zipFile: ZipFile, iconPath: String?): ByteArray? {
        if (iconPath.isNullOrEmpty()) return null
        
        return try {
            val iconEntry = zipFile.getEntry(iconPath) ?: zipFile.getEntry("assets/${iconPath}") ?: return null
            zipFile.getInputStream(iconEntry).use { it.readBytes() }
        } catch (e: Exception) {
            Logger.e("ModMetadataParser", "Failed to parse icon: $iconPath", e)
            null
        }
    }
    
    private fun extractTomlValue(tomlContent: String, key: String): String? {
        val regex = """$key\s*=\s*["']([^"']+)["']""".toRegex()
        return regex.find(tomlContent)?.groupValues?.get(1)
    }
    
    private fun createFallbackMod(modFile: File): LocalMod {
        val fileName = modFile.nameWithoutExtension.removeSuffix(".disabled")
        return LocalMod(
            file = modFile,
            fileSize = FileUtils.sizeOf(modFile),
            id = fileName,
            name = fileName,
            version = "Unknown",
            description = null,
            authors = emptyList(),
            icon = null,
            loader = LocalMod.ModLoader.UNKNOWN,
            notMod = true
        )
    }
}