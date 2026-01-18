package com.lanrhyme.shardlauncher.utils.file

import java.io.File
import java.io.FileNotFoundException
import java.util.zip.ZipFile

/**
 * ZIP文件操作工具类
 */

/**
 * 从ZIP文件中提取条目到目标文件
 */
fun ZipFile.extractEntryToFile(entryName: String, destFile: File) {
    val entry = getEntry(entryName) ?: throw FileNotFoundException("Entry not found: $entryName")
    destFile.parentFile?.mkdirs()
    getInputStream(entry).use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

/**
 * 从ZIP文件中提取条目到目标文件（重载版本，接受 ZipEntry）
 */
fun ZipFile.extractEntryToFile(entry: java.util.zip.ZipEntry, destFile: File) {
    require(!entry.isDirectory) { "Cannot extract directory to file: ${entry.name}" }
    destFile.parentFile?.mkdirs()
    getInputStream(entry).use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

/**
 * 从ZIP文件中读取文本内容
 */
fun ZipFile.readText(entryName: String): String {
    val entry = getEntry(entryName) ?: throw FileNotFoundException("Entry not found: $entryName")
    return getInputStream(entry).bufferedReader().use { it.readText() }
}

/**
 * 从ZIP文件中提取整个目录
 */
fun ZipFile.extractDirectory(entryPrefix: String, destDir: File) {
    destDir.mkdirs()
    entries().asSequence()
        .filter { !it.isDirectory && it.name.startsWith(entryPrefix) }
        .forEach { entry ->
            val relativePath = entry.name.removePrefix(entryPrefix).trimStart('/')
            val destFile = File(destDir, relativePath)
            destFile.parentFile?.mkdirs()
            getInputStream(entry).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
}
