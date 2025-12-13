package com.lanrhyme.shardlauncher.game.version.mod

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lanrhyme.shardlauncher.game.addons.modloader.ModLoader
import com.lanrhyme.shardlauncher.utils.logging.Logger.lWarning
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 本地模组信息
 * @param modFile 本地模组对应的文件
 * @param fileSize 文件大小（字节）
 * @param id 模组ID
 * @param loader 模组所属的加载器
 * @param name 模组的显示名称
 * @param description 模组描述
 * @param version 模组版本
 * @param authors 模组的作者列表
 * @param icon 模组的图标（字节数组）
 * @param notMod 标记是否为非模组文件
 */
class LocalMod(
    modFile: File,
    val fileSize: Long,
    val id: String,
    val loader: ModLoader,
    val name: String,
    val description: String? = null,
    val version: String? = null,
    val authors: List<String> = emptyList(),
    val icon: ByteArray? = null,
    val notMod: Boolean = false
) {
    var file by mutableStateOf(modFile)
        private set

    /**
     * 禁用模组（重命名为 .disabled）
     */
    fun disable() {
        val currentPath = file.absolutePath
        if (file.isDisabled()) return

        val newFile = File("$currentPath.disabled")
        if (!file.renameToSafely(newFile)) return

        file = newFile
    }

    /**
     * 启用模组（移除 .disabled 后缀）
     */
    fun enable() {
        val currentPath = file.absolutePath
        if (file.isEnabled()) return

        val newPath = currentPath.dropLast(".disabled".length)
        val newFile = File(newPath)
        if (!file.renameToSafely(newFile)) return

        file = newFile
    }

    private fun File.renameToSafely(dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            Files.move(
                this.toPath(),
                dest.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (e: IOException) {
            lWarning("Failed to rename file {$this} to $dest!", e)
            false
        }
    }

    override fun toString(): String {
        return "LocalMod(name='$name', version=$version, file=${file.name}, enabled=${file.isEnabled()})"
    }
}

/**
 * 模组是否启用
 */
fun File.isEnabled(): Boolean = !absolutePath.endsWith(".disabled", ignoreCase = true)

/**
 * 模组是否禁用
 */
fun File.isDisabled(): Boolean = !this.isEnabled()

/**
 * 创建一个非模组文件的占位符
 */
fun createNotMod(file: File): LocalMod = LocalMod(
    modFile = file,
    fileSize = FileUtils.sizeOf(file),
    id = "",
    loader = ModLoader.UNKNOWN,
    name = file.name,
    description = null,
    version = null,
    authors = emptyList(),
    icon = null,
    notMod = true
)
