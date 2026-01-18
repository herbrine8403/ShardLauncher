/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.launch

import android.content.Context
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.utils.copyAssetFile
import com.lanrhyme.shardlauncher.utils.logging.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object MCOptions {
    private val lock = Any()
    private val parameterMap = ConcurrentHashMap<String, String>()
    private lateinit var version: Version

    /**
     * Initialize Minecraft Options
     */
    fun setup(context: Context, version: Version) {
        this.version = version
        synchronized(lock) {
            parameterMap.clear()
            setupFileStructure(context)
            loadInternal()
        }
    }

    private fun setupFileStructure(context: Context) {
        val optionsFile = getOptionsFile()
        optionsFile.parentFile?.takeIf { !it.exists() }?.mkdirs()
        if (!optionsFile.exists()) {
            runCatching {
                // In a real app, this file would be in assets
                // For now, we just ensure the parent exists
                // context.copyAssetFile("game/options.txt", optionsFile, false)
            }.onFailure {
                Logger.lWarning("Failed to create default options.txt")
            }
        }
    }

    private fun loadInternal() {
        val optionsFile = getOptionsFile()
        if (!optionsFile.exists()) return
        
        runCatching {
            val lines = optionsFile.readLines()
            parameterMap.clear()
            lines.forEach { line ->
                val idx = line.indexOf(':')
                if (idx > 0) {
                    val key = line.substring(0, idx)
                    val value = line.substring(idx + 1)
                    parameterMap[key] = value
                }
            }
        }.onFailure {
            Logger.lWarning("Failed to load options!", it)
        }
    }

    fun set(key: String, value: String) {
        parameterMap[key] = value
    }

    fun get(key: String): String? = parameterMap[key]

    fun containsKey(key: String): Boolean = parameterMap.containsKey(key)

    fun save() {
        synchronized(lock) {
            val optionsFile = getOptionsFile()
            runCatching {
                val content = parameterMap.entries.joinToString("\n") { "${it.key}:${it.value}" }
                optionsFile.writeText(content)
            }.onFailure {
                Logger.lWarning("Failed to save options.txt!", it)
            }
        }
    }

    private fun getOptionsFile() = File(version.getGameDir(), "options.txt")
}
