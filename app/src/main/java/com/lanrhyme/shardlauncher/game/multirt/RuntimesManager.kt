/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.lanrhyme.shardlauncher.game.multirt

import com.lanrhyme.shardlauncher.path.PathManager
import java.io.File

object RuntimesManager {
    fun getRuntimeHome(runtimeName: String): File {
        return File(PathManager.DIR_MULTIRT, runtimeName)
    }

    fun isJDK8(runtimePath: String): Boolean {
        // Check if the runtime is JDK 8 by looking for jre subdirectory
        val jreDir = File(runtimePath, "jre")
        return jreDir.exists() && jreDir.isDirectory
    }

    /**
     * Get runtime by name
     */
    fun getRuntime(name: String): Runtime {
        val runtimeHome = getRuntimeHome(name)
        val isJDK8 = isJDK8(runtimeHome.absolutePath)
        
        return Runtime(
            name = name,
            versionString = null,
            arch = System.getProperty("os.arch"),
            javaVersion = if (isJDK8) 8 else 17,
            isJDK8 = isJDK8
        )
    }

    /**
     * Get default runtime for Java version
     */
    fun getDefaultRuntime(javaVersion: Int): Runtime? {
        val runtimesDir = PathManager.DIR_MULTIRT
        runtimesDir.listFiles()?.let { runtimes ->
            for (runtime in runtimes) {
                val isJDK8 = isJDK8(runtime.absolutePath)
                val runtimeJavaVersion = if (isJDK8) 8 else 17
                
                if (runtimeJavaVersion >= javaVersion) {
                    return Runtime(
                        name = runtime.name,
                        versionString = null,
                        arch = System.getProperty("os.arch"),
                        javaVersion = runtimeJavaVersion,
                        isJDK8 = isJDK8
                    )
                }
            }
        }
        return null
    }

    /**
     * Get all detected runtimes
     */
    fun getRuntimes(): List<Runtime> {
        val runtimes = mutableListOf<Runtime>()
        val runtimesDir = PathManager.DIR_MULTIRT
        
        if (!runtimesDir.exists()) {
            return emptyList()
        }
        
        runtimesDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                runtimes.add(Runtime(
                    name = file.name,
                    versionString = null,
                    arch = System.getProperty("os.arch"),
                    javaVersion = if (isJDK8(file.absolutePath)) 8 else 17,
                    isJDK8 = isJDK8(file.absolutePath)
                ))
            }
        }
        return runtimes.sortedBy { it.name }
    }

    /**
     * Get exact runtime by Java version
     */
    fun getExactJreName(majorVersion: Int): String? {
        return getRuntimes().firstOrNull { it.javaVersion == majorVersion }?.name
    }

    /**
     * Get nearest runtime by Java version (finds the closest higher version)
     */
    fun getNearestJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        return runtimes
            .filter { it.javaVersion >= majorVersion }
            .minByOrNull { it.javaVersion }?.name
    }

    /**
     * Load runtime with caching support
     */
    fun loadRuntime(name: String): Runtime {
        return if (name.isEmpty()) {
            Runtime("", null, null, 0, false)
        } else {
            getRuntime(name)
        }
    }

    /**
     * Install runtime from input stream
     */
    suspend fun installRuntime(
        inputStream: java.io.InputStream,
        name: String,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ): Runtime = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dest = File(PathManager.DIR_MULTIRT, name)
        try {
            if (dest.exists()) {
                deleteDirectory(dest)
            }
            dest.mkdirs()
            
            // Extract tar.xz archive
            uncompressTarXZ(inputStream, dest, updateProgress)
            
            // Post-process the runtime
            postPrepare(name)
            
            getRuntime(name)
        } catch (e: Exception) {
            if (dest.exists()) {
                deleteDirectory(dest)
            }
            throw e
        }
    }

    /**
     * Remove runtime by name
     */
    fun removeRuntime(name: String) {
        val dest = File(PathManager.DIR_MULTIRT, name)
        if (dest.exists()) {
            deleteDirectory(dest)
        }
    }

    /**
     * Delete directory recursively
     */
    private fun deleteDirectory(dir: File): Boolean {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                deleteDirectory(child)
            }
        }
        return dir.delete()
    }

    /**
     * Post-process runtime after installation
     */
    private suspend fun postPrepare(name: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dest = File(PathManager.DIR_MULTIRT, name)
        if (!dest.exists()) return@withContext
        
        val runtime = getRuntime(name)
        var libFolder = "lib"
        
        val arch = runtime.arch
        if (arch != null && File(dest, "$libFolder/$arch").exists()) {
            libFolder += "/$arch"
        }
        
        val isJDK8Runtime = isJDK8(dest.absolutePath)
        if (isJDK8Runtime) {
            libFolder = "jre/$libFolder"
        }
        
        // Handle freetype library
        val ftIn = File(dest, "$libFolder/libfreetype.so.6")
        val ftOut = File(dest, "$libFolder/libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            ftIn.renameTo(ftOut)
        }
    }

    /**
     * Uncompress tar.xz archive
     */
    private suspend fun uncompressTarXZ(
        inputStream: java.io.InputStream,
        destDir: File,
        updateProgress: (Int, Array<Any>) -> Unit
    ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val xzInputStream = org.apache.commons.compress.compressors.xz.XZCompressorInputStream(inputStream)
            val tarInputStream = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(xzInputStream)
            
            var entry = tarInputStream.nextTarEntry
            var processedEntries = 0
            
            while (entry != null) {
                val outputFile = File(destDir, entry.name)
                
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    outputFile.outputStream().use { output ->
                        tarInputStream.copyTo(output)
                    }
                    
                    // Set executable permissions if needed
                    if (entry.mode and 0x49 != 0) { // Check if executable
                        outputFile.setExecutable(true)
                    }
                }
                
                processedEntries++
                if (processedEntries % 10 == 0) {
                    updateProgress(processedEntries, arrayOf(entry.name))
                }
                
                entry = tarInputStream.nextTarEntry
            }
            
            tarInputStream.close()
        } catch (e: Exception) {
            throw RuntimeException("Failed to extract runtime archive", e)
        }
    }
}