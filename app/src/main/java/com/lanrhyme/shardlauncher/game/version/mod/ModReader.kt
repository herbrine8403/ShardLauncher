package com.lanrhyme.shardlauncher.game.version.mod

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lanrhyme.shardlauncher.game.addons.modloader.ModLoader
import com.lanrhyme.shardlauncher.utils.logging.Logger
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry

/**
 * 模组元数据读取器
 */
abstract class ModReader {
    /**
     * 从本地文件读取模组元数据
     */
    abstract fun fromLocal(file: File): LocalMod

    /**
     * 从 JAR 文件中读取指定路径的文本内容
     */
    protected fun readTextFromJar(file: File, entryPath: String): String? {
        return try {
            JarFile(file).use { jar ->
                val entry: ZipEntry = jar.getEntry(entryPath) ?: return null
                jar.getInputStream(entry).bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Logger.lWarning("Failed to read $entryPath from ${file.name}", e)
            null
        }
    }

    /**
     * 从 JAR 文件中读取图标
     */
    protected fun readIconFromJar(file: File, iconPath: String): ByteArray? {
        return try {
            JarFile(file).use { jar ->
                val entry: ZipEntry = jar.getEntry(iconPath) ?: return null
                jar.getInputStream(entry).readBytes()
            }
        } catch (e: Exception) {
            Logger.lWarning("Failed to read icon from ${file.name}", e)
            null
        }
    }
}

/**
 * Fabric 模组读取器
 */
class FabricModReader : ModReader() {
    override fun fromLocal(file: File): LocalMod {
        val jsonText = readTextFromJar(file, "fabric.mod.json")
            ?: throw IllegalArgumentException("Not a valid Fabric mod: ${file.name}")

        val json = JsonParser.parseString(jsonText).asJsonObject

        val id = json.get("id")?.asString ?: ""
        val name = json.get("name")?.asString ?: file.nameWithoutExtension
        val version = json.get("version")?.asString
        val description = json.get("description")?.asString
        val authors = json.getAsJsonArray("authors")?.map { it.asString } ?: emptyList()
        val iconPath = json.get("icon")?.asString
        val icon = iconPath?.let { readIconFromJar(file, it) }

        return LocalMod(
            modFile = file,
            fileSize = FileUtils.sizeOf(file),
            id = id,
            loader = ModLoader.FABRIC,
            name = name,
            description = description,
            version = version,
            authors = authors,
            icon = icon
        )
    }
}

/**
 * Forge 模组读取器（新版 mods.toml）
 */
class ForgeModReader : ModReader() {
    override fun fromLocal(file: File): LocalMod {
        // 尝试读取新版 Forge (1.13+) 的 mods.toml
        val tomlText = readTextFromJar(file, "META-INF/mods.toml")
        if (tomlText != null) {
            return parseModsToml(file, tomlText)
        }

        // 尝试读取旧版 Forge 的 mcmod.info
        val jsonText = readTextFromJar(file, "mcmod.info")
            ?: throw IllegalArgumentException("Not a valid Forge mod: ${file.name}")

        return parseModInfo(file, jsonText)
    }

    private fun parseModsToml(file: File, toml: String): LocalMod {
        // 简化的 TOML 解析（仅提取关键字段）
        val modId = toml.lines().find { it.trim().startsWith("modId") }
            ?.substringAfter("=")?.trim()?.removeSurrounding("\"") ?: ""
        val displayName = toml.lines().find { it.trim().startsWith("displayName") }
            ?.substringAfter("=")?.trim()?.removeSurrounding("\"") ?: file.nameWithoutExtension
        val version = toml.lines().find { it.trim().startsWith("version") }
            ?.substringAfter("=")?.trim()?.removeSurrounding("\"")
        val description = toml.lines().find { it.trim().startsWith("description") }
            ?.substringAfter("=")?.trim()?.removeSurrounding("\"")
        val authors = toml.lines().find { it.trim().startsWith("authors") }
            ?.substringAfter("=")?.trim()?.removeSurrounding("\"")
            ?.split(",")?.map { it.trim() } ?: emptyList()

        val iconPath = toml.lines().find { it.trim().startsWith("logoFile") }
            ?.substringAfter("=")?.trim()?.removeSurrounding("\"")
        val icon = iconPath?.let { readIconFromJar(file, it) }

        return LocalMod(
            modFile = file,
            fileSize = FileUtils.sizeOf(file),
            id = modId,
            loader = ModLoader.FORGE,
            name = displayName,
            description = description,
            version = version,
            authors = authors,
            icon = icon
        )
    }

    private fun parseModInfo(file: File, jsonText: String): LocalMod {
        val json = JsonParser.parseString(jsonText)
        val modList = if (json.isJsonArray) {
            json.asJsonArray
        } else {
            throw IllegalArgumentException("Invalid mcmod.info format")
        }

        val modInfo: JsonObject = modList.firstOrNull()?.asJsonObject
            ?: throw IllegalArgumentException("No mod info found in mcmod.info")

        val modId = modInfo.get("modid")?.asString ?: ""
        val name = modInfo.get("name")?.asString ?: file.nameWithoutExtension
        val version = modInfo.get("version")?.asString
        val description = modInfo.get("description")?.asString
        val authors = modInfo.getAsJsonArray("authorList")?.map { it.asString } ?: emptyList()
        val logoFile = modInfo.get("logoFile")?.asString
        val icon = logoFile?.let { readIconFromJar(file, it) }

        return LocalMod(
            modFile = file,
            fileSize = FileUtils.sizeOf(file),
            id = modId,
            loader = ModLoader.FORGE,
            name = name,
            description = description,
            version = version,
            authors = authors,
            icon = icon
        )
    }
}

/**
 * Quilt 模组读取器
 */
class QuiltModReader : ModReader() {
    override fun fromLocal(file: File): LocalMod {
        val jsonText = readTextFromJar(file, "quilt.mod.json")
            ?: throw IllegalArgumentException("Not a valid Quilt mod: ${file.name}")

        val json = JsonParser.parseString(jsonText).asJsonObject
        val quiltLoader = json.getAsJsonObject("quilt_loader")

        val id = quiltLoader?.get("id")?.asString ?: ""
        val metadata = quiltLoader?.getAsJsonObject("metadata")
        val name = metadata?.get("name")?.asString ?: file.nameWithoutExtension
        val version = quiltLoader?.get("version")?.asString
        val description = metadata?.get("description")?.asString
        val contributors = metadata?.getAsJsonObject("contributors")?.keySet()?.toList() ?: emptyList()
        val iconPath = metadata?.get("icon")?.asString
        val icon = iconPath?.let { readIconFromJar(file, it) }

        return LocalMod(
            modFile = file,
            fileSize = FileUtils.sizeOf(file),
            id = id,
            loader = ModLoader.QUILT,
            name = name,
            description = description,
            version = version,
            authors = contributors,
            icon = icon
        )
    }
}

/**
 * 所有支持的模组读取器映射
 * 扩展名 -> 读取器列表（按优先级）
 */
val MOD_READERS = mapOf(
    "jar" to listOf(FabricModReader(), ForgeModReader(), QuiltModReader())
)
