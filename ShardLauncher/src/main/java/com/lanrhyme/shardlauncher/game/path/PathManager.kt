package com.lanrhyme.shardlauncher.game.path

import android.content.Context
import java.io.File

object PathManager {
    lateinit var DIR_FILES_PRIVATE: File
    lateinit var DIR_FILES_EXTERNAL: File
    lateinit var DIR_CACHE: File
    lateinit var DIR_NATIVE_LIB: String
    
    lateinit var DIR_GAME: File
    lateinit var DIR_ACCOUNT_SKIN: File
    lateinit var DIR_LAUNCHER_LOGS: File
    lateinit var DIR_COMPONENTS: File
    
    lateinit var FILE_MINECRAFT_VERSIONS: File

    fun refreshPaths(context: Context) {
        DIR_FILES_PRIVATE = context.filesDir
        DIR_FILES_EXTERNAL = context.getExternalFilesDir(null) ?: context.filesDir
        DIR_CACHE = context.cacheDir
        DIR_NATIVE_LIB = context.applicationInfo.nativeLibraryDir

        DIR_GAME = File(DIR_FILES_PRIVATE, "games")
        DIR_ACCOUNT_SKIN = File(DIR_GAME, "account_skins")
        DIR_LAUNCHER_LOGS = File(DIR_FILES_EXTERNAL, "logs")
        DIR_COMPONENTS = File(DIR_FILES_PRIVATE, "components")
        
        FILE_MINECRAFT_VERSIONS = File(DIR_GAME, "minecraft_versions.json")

        createDirs()
    }

    private fun createDirs() {
        DIR_GAME.mkdirs()
        DIR_ACCOUNT_SKIN.mkdirs()
        DIR_LAUNCHER_LOGS.mkdirs()
        DIR_COMPONENTS.mkdirs()
    }
}
