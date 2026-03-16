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

package com.lanrhyme.shardlauncher.game.launch

import androidx.collection.ArrayMap
import com.lanrhyme.shardlauncher.BuildConfig
import com.lanrhyme.shardlauncher.game.account.Account
import com.lanrhyme.shardlauncher.game.account.isAuthServerAccount
import com.lanrhyme.shardlauncher.game.account.isLocalAccount
import com.lanrhyme.shardlauncher.game.account.offline.OfflineYggdrasilServer
import com.lanrhyme.shardlauncher.game.multirt.Runtime
import com.lanrhyme.shardlauncher.game.path.getAssetsHome
import com.lanrhyme.shardlauncher.game.path.getLibrariesHome
import com.lanrhyme.shardlauncher.game.version.download.artifactToPath
import com.lanrhyme.shardlauncher.game.version.download.filterLibrary
import com.lanrhyme.shardlauncher.game.version.download.getLibraryReplacement
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.installed.getGameManifest
import com.lanrhyme.shardlauncher.game.version.remote.MinecraftVersionJson
import com.lanrhyme.shardlauncher.path.LibPath
import com.lanrhyme.shardlauncher.path.PathManager
import com.lanrhyme.shardlauncher.utils.file.child
import com.lanrhyme.shardlauncher.utils.logging.Logger.lDebug
import com.lanrhyme.shardlauncher.utils.logging.Logger.lInfo
import com.lanrhyme.shardlauncher.utils.logging.Logger.lWarning
import com.lanrhyme.shardlauncher.utils.network.ServerAddress
import com.lanrhyme.shardlauncher.utils.string.insertJSONValueList
import com.lanrhyme.shardlauncher.utils.string.isLowerTo
import com.lanrhyme.shardlauncher.utils.string.isNotEmptyOrBlank
import com.lanrhyme.shardlauncher.utils.string.toUnicodeEscaped
import java.io.File

class LaunchArgs(
    private val runtimeLibraryPath: String,
    private val account: Account,
    private val offlineServer: OfflineYggdrasilServer,
    private val gameDirPath: File,
    private val version: Version,
    private val gameManifest: MinecraftVersionJson,
    private val runtime: Runtime,
    private val readAssetsFile: (path: String) -> String,
    private val getCacioJavaArgs: (isJava8: Boolean) -> List<String>
) {
    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = gameManifest.mainClass.substring(0, gameManifest.mainClass.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add(gameManifest.mainClass)
        argsList.addAll(getMinecraftClientArgs())

        version.getVersionInfo()?.let { info ->
            val playSingle = version.quickPlaySingle?.takeIf { it.isNotEmptyOrBlank() }
            if (playSingle != null) { //快速启动单人游戏
                if (info.quickPlay.isQuickPlaySingleplayer) {
                    //将不受支持的字符转换为Unicode
                    val saveName = playSingle.toUnicodeEscaped()
                    argsList.apply {
                        add("--quickPlaySingleplayer")
                        add(saveName)
                    }
                } else {
                    lWarning("Quick Play for singleplayer is not supported and has been skipped.")
                }
            } else {
                version.getServerIp()?.let { address ->
                    val parsed = ServerAddress.parse(address)
                    argsList += if (info.quickPlay.isQuickPlayMultiplayer) {
                        listOf(
                            "--quickPlayMultiplayer",
                            if (parsed.port < 0) "$address:25565" else address
                        )
                    } else {
                        val port = parsed.port.takeIf { it >= 0 } ?: 25565
                        listOf("--server", parsed.host, "--port", port.toString())
                    }
                }
            }
        }

        return argsList
    }

    private fun getLWJGL3ClassPath(): String =
        File(PathManager.DIR_COMPONENTS, "lwjgl3")
            .listFiles { file -> file.name.endsWith(".jar") }
            ?.joinToString(":") { it.absolutePath }
            ?: ""

    private fun getJavaArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        if (account.isLocalAccount()) {
            if (account.hasSkinFile) {
                //该离线账号拥有本地皮肤，启用离线yggdrasil服务器
                offlineServer.start()
                offlineServer.addCharacter(account)
                val port = offlineServer.getPort()
                if (port != null) {
                    lInfo("Using offline Yggdrasil server on port $port")
                    argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=http://localhost:$port")
                    argsList.add("-Dauthlibinjector.side=client")
                } else {
                    //无法获取端口号，说明服务器未成功启动
                    lWarning("Failed to start offline Yggdrasil server!")
                    //本次启动将被忽略，为避免浪费性能，关停服务器
                    offlineServer.stop()
                }
            }
        } else if (account.isAuthServerAccount()) {
            val baseUrl = account.otherBaseUrl
            if (baseUrl != null && baseUrl.contains("auth.mc-user.com")) {
                argsList.add("-javaagent:${LibPath.NIDE_8_AUTH.absolutePath}=${baseUrl.replace("https://auth.mc-user.com:233/", "")}")
                argsList.add("-Dnide8auth.client=true")
            } else if (baseUrl != null) {
                argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=$baseUrl")
            }
        }

        argsList.addAll(getCacioJavaArgs(runtime.javaVersion == 8))

        val configFilePath = version.getVersionPath().child("log4j2.xml")
        if (!configFilePath.exists()) {
            val is7 = (version.getVersionInfo()?.minecraftVersion ?: "0.0").isLowerTo("1.12")
            runCatching {
                val content = if (is7) {
                    readAssetsFile("components/log4j-1.7.xml")
                } else {
                    readAssetsFile("components/log4j-1.12.xml")
                }
                configFilePath.writeText(content)
            }.onFailure {
                lWarning("Failed to write fallback Log4j configuration autonomously!", it)
            }
        }
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")
        argsList.add("-Dminecraft.client.jar=${version.getClientJar().absolutePath}")

        return argsList
    }

    private fun getMinecraftJVMArgs(): Array<String> {
        val gameManifest1 = getGameManifest(version, true)

//        // Parse Forge 1.17+ additional JVM Arguments
//        if (versionInfo.inheritsFrom == null || versionInfo.arguments == null || versionInfo.arguments.jvm == null) {
//            return emptyArray()
//        }

        val varArgMap: MutableMap<String, String> = android.util.ArrayMap()
        val launchClassPath = "${getLWJGL3ClassPath()}:${generateLaunchClassPath(gameManifest)}"
        var hasClasspath = false //是否已经在jvm参数中包含 ${classpath} 配置

        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = getLibrariesHome()
        varArgMap["version_name"] = gameManifest1.id
        varArgMap["natives_directory"] = runtimeLibraryPath
        setLauncherInfo(varArgMap)

        fun Any.processJvmArg(): String? = (this as? String)?.let {
            when {
                it.startsWith("-DignoreList=") -> {
                    "$it,${version.getVersionName()}.jar"
                }
                it.contains("-Dio.netty.native.workdir") ||
                it.contains("-Djna.tmpdir") ||
                it.contains("-Dorg.lwjgl.system.SharedLibraryExtractPath") -> {
                    //使用一个可读的目录
                    it.replace("\${natives_directory}", PathManager.DIR_CACHE.absolutePath)
                }
                it == "\${classpath}" -> {
                    hasClasspath = true
                    launchClassPath
                }
                else -> it
            }
        }

        val jvmArgs = gameManifest1.arguments?.jvm
            ?.mapNotNull { it.processJvmArg() }
            ?.toTypedArray()
            ?: emptyArray()

        val replacedArgs = insertJSONValueList(jvmArgs, varArgMap)
        return if (hasClasspath) {
            replacedArgs
        } else {
            //不包含 ${classpath} 配置，则需要手动添加
            replacedArgs + arrayOf("-cp", launchClassPath)
        }
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L572-L592)
     */
    private fun generateLaunchClassPath(gameManifest: MinecraftVersionJson): String {
        val classpathList = mutableListOf<String>()

        val classpath: Array<String> = generateLibClasspath(gameManifest)

        val clientClass = version.getClientJar()
        val clientClasspath: String = clientClass.absolutePath

        for (jarFile in classpath) {
            val jarFileObj = File(jarFile)
            if (!jarFileObj.exists()) {
                lDebug("Ignored non-exists file: $jarFile")
                continue
            }
            classpathList.add(jarFile)
        }
        if (clientClass.exists()) {
            classpathList.add(clientClasspath)
        }

        return classpathList.joinToString(":")
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L871-L882)
     */
    private fun generateLibClasspath(gameManifest: MinecraftVersionJson): Array<String> {
        val libDir: MutableList<String> = ArrayList()
        for (libItem in gameManifest.libraries) {
            if (!(MinecraftVersionJson.checkRules(libItem.rules) && !libItem.isNative())) continue
            val libArtifactPath: String = libItem.progressLibrary() ?: continue
            libDir.add(getLibrariesHome() + "/" + libArtifactPath)
        }
        return libDir.toTypedArray<String>()
    }

    /**
     * @return 库相对路径
     */
    private fun MinecraftVersionJson.Library.progressLibrary(): String? {
        if (filterLibrary()) return null

        var path = artifactToPath(this)

        val versionSegment = name.split(":").getOrNull(2) ?: return path
        val versionParts = versionSegment.split(".")

        getLibraryReplacement(name, versionParts)?.let { replacement ->
            lDebug("Library ${this.name} has been changed to version ${replacement.newName.split(":").last()}")
            path = replacement.newPath
        }

        return path
    }

    private fun getMinecraftClientArgs(): Array<String> {
        val varArgMap: MutableMap<String, String> = ArrayMap()
        varArgMap["auth_session"] = account.accessToken
        varArgMap["auth_access_token"] = account.accessToken
        varArgMap["auth_player_name"] = account.username
        varArgMap["auth_uuid"] = account.profileId.replace("-", "")
        varArgMap["auth_xuid"] = account.xUid ?: ""
        varArgMap["assets_root"] = getAssetsHome()
        varArgMap["assets_index_name"] = gameManifest.assetIndex.id
        varArgMap["game_assets"] = getAssetsHome()
        varArgMap["game_directory"] = gameDirPath.absolutePath
        varArgMap["user_properties"] = "{}"
        varArgMap["user_type"] = "msa"
        varArgMap["version_name"] = version.getVersionInfo()!!.minecraftVersion

        setLauncherInfo(varArgMap)

        val minecraftArgs: MutableList<String> = ArrayList()
        gameManifest.arguments?.game?.forEach { 
            if (it.isJsonPrimitive && it.asJsonPrimitive.isString) {
                minecraftArgs.add(it.asString)
            }
        }

        return insertJSONValueList(
            splitAndFilterEmpty(
                gameManifest.minecraftArguments ?:
                minecraftArgs.toTypedArray().joinToString(" ")
            ), varArgMap
        )
    }

    private fun setLauncherInfo(verArgMap: MutableMap<String, String>) {
        verArgMap["launcher_name"] = "ShardLauncher"
        verArgMap["launcher_version"] = BuildConfig.VERSION_NAME
        verArgMap["version_type"] = version.getCustomInfo()
            .takeIf { it.isNotEmptyOrBlank() }
            ?: (gameManifest.type ?: "release")
    }

    private fun splitAndFilterEmpty(arg: String): Array<String> {
        val list: MutableList<String> = ArrayList()
        arg.split(" ").forEach {
            if (it.isNotEmpty()) list.add(it)
        }
        return list.toTypedArray()
    }
}
