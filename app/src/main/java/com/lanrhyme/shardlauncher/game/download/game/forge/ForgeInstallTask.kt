package com.lanrhyme.shardlauncher.game.download.game.forge

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.coroutine.Task
import com.lanrhyme.shardlauncher.game.download.game.GameLibDownloader
import com.lanrhyme.shardlauncher.game.download.game.copyVanillaFiles
import com.lanrhyme.shardlauncher.game.download.game.getLibraryPath
import com.lanrhyme.shardlauncher.game.download.game.models.ForgeLikeInstallProcessor
import com.lanrhyme.shardlauncher.game.modloader.forgelike.ForgeLikeVersion
import com.lanrhyme.shardlauncher.game.modloader.forgelike.forge.ForgeVersion
import com.lanrhyme.shardlauncher.game.modloader.forgelike.forge.NeoForgeVersion
import com.lanrhyme.shardlauncher.game.version.download.BaseMinecraftDownloader
import com.lanrhyme.shardlauncher.game.versioninfo.MinecraftVersions
import com.lanrhyme.shardlauncher.game.versioninfo.models.GameManifest
import com.lanrhyme.shardlauncher.utils.GSON
import com.lanrhyme.shardlauncher.utils.file.ensureDirectory
import com.lanrhyme.shardlauncher.utils.file.extractEntryToFile
import com.lanrhyme.shardlauncher.utils.file.extractDirectory
import com.lanrhyme.shardlauncher.utils.file.readText
import com.lanrhyme.shardlauncher.utils.json.merge
import com.lanrhyme.shardlauncher.utils.json.parseToJson
import com.lanrhyme.shardlauncher.utils.logging.Logger
import com.lanrhyme.shardlauncher.utils.network.fetchStringFromUrls
import com.lanrhyme.shardlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jackhuang.hmcl.util.DigestUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.io.path.name

/**
 * 完整的Forge/NeoForge安装任务
 * 支持新版和旧版安装方式
 */

const val FORGE_LIKE_DOWNLOAD_ID = "Download.ForgeLike"
const val FORGE_LIKE_ANALYSE_ID = "Analyse.ForgeLike"
const val FORGE_LIKE_INSTALL_ID = "Install.ForgeLike"

/**
 * 下载Forge安装器任务
 */
fun getForgeLikeDownloadTask(
    targetTempInstaller: File,
    forgeLikeVersion: ForgeLikeVersion
): Task {
    return Task.runTask(
        id = FORGE_LIKE_DOWNLOAD_ID,
        task = {
            //获取安装器下载链接
            val url = when (forgeLikeVersion) {
                is NeoForgeVersion -> {
                    com.lanrhyme.shardlauncher.game.modloader.forgelike.forge.ForgeVersions.getNeoForgeDownloadUrl(forgeLikeVersion)
                }
                is ForgeVersion -> {
                    com.lanrhyme.shardlauncher.game.modloader.forgelike.forge.ForgeVersions.getDownloadUrl(forgeLikeVersion)
                }
                else -> throw IllegalArgumentException("Unsupported ForgeLike version type")
            }

            com.lanrhyme.shardlauncher.utils.network.downloadFromMirrorListSuspend(
                urls = listOf(url),
                targetFile = targetTempInstaller
            )
        }
    )
}

/**
 * 分析Forge安装器任务
 * 解压并解析install_profile.json
 */
fun getForgeLikeAnalyseTask(
    downloader: BaseMinecraftDownloader,
    targetTempInstaller: File,
    forgeLikeVersion: ForgeLikeVersion,
    tempMinecraftFolder: File,
    sourceInherit: String,
    processedInherit: String,
    loaderVersion: String
): Task {
    return Task.runTask(
        id = FORGE_LIKE_ANALYSE_ID,
        dispatcher = Dispatchers.IO,
        task = { task ->
            if (sourceInherit != processedInherit) {
                //准备安装环境
                //复制原版文件
                copyVanillaFiles(
                    sourceGameFolder = tempMinecraftFolder,
                    sourceVersion = sourceInherit,
                    destinationGameFolder = tempMinecraftFolder,
                    targetVersion = processedInherit
                )
            }

            analyseNewForge(
                task = task,
                downloader = downloader,
                forgeLikeVersion = forgeLikeVersion,
                installer = targetTempInstaller,
                tempMinecraftFolder = tempMinecraftFolder,
                inherit = processedInherit,
                loaderVersion = loaderVersion
            )
        }
    )
}

/**
 * 分析新版Forge/NeoForge
 * 解压安装器，提取库文件，解析processors
 */
private suspend fun analyseNewForge(
    task: Task,
    downloader: BaseMinecraftDownloader,
    forgeLikeVersion: ForgeLikeVersion,
    installer: File,
    tempMinecraftFolder: File,
    inherit: String,
    loaderVersion: String
) {
    task.updateProgress(-1f)

    //解析 NeoForge 的支持库列表，并统一进行下载
    val (installProfile, installProfileString, versionString) = withContext(Dispatchers.IO) {
        ZipFile(installer).use { zip ->
            task.updateProgress(0.2f)

            val installProfileString = zip.readText("install_profile.json")
            val versionString = zip.readText("version.json")

            val installProfile = installProfileString.parseToJson()
            installProfile["libraries"]?.takeIf { it.isJsonArray }?.let { libraries ->
                val libraryList: List<GameManifest.Library> =
                    GSON.fromJson(libraries, object : TypeToken<List<GameManifest.Library>>() {}.type) ?: return@let

                for (library in libraryList) {
                    val path = com.lanrhyme.shardlauncher.game.version.download.artifactToPath(library) ?: continue
                    zip.getEntry("maven/$path")?.let { entry ->
                        val dest = File(tempMinecraftFolder, "libraries/$path")
                        zip.extractEntryToFile(entry, dest)
                    }
                }
            }

            installProfile["path"]?.takeIf { it.isJsonPrimitive }?.let { path ->
                val libraryPath = getLibraryPath(path.asString)
                zip.getEntry("maven/$libraryPath")?.let { entry ->
                    val dest = File(tempMinecraftFolder, "libraries/$libraryPath")
                    zip.extractEntryToFile(entry, dest)
                }
            }

            Triple(installProfile, installProfileString, versionString)
        }
    }

    //合并为一个Json
    installProfile.merge(versionString.parseToJson())

    //计划下载 install_profile.json 内的所有支持库
    val libDownloader = GameLibDownloader(
        downloader = downloader,
        gameManifest = GSON.fromJson(installProfileString, GameManifest::class.java)
    )
    libDownloader.schedule(task, File(tempMinecraftFolder, "libraries").ensureDirectory(), false)

    //添加 Mojang Mappings 下载信息
    task.updateProgress(0.4f)
    scheduleMojangMappings(
        mergedJson = installProfile,
        tempMinecraftDir = tempMinecraftFolder,
        tempVanillaJar = File(tempMinecraftFolder, "versions/$inherit/$inherit.jar"),
        tempInstaller = installer
    ) { urls, sha1, targetFile, size ->
        libDownloader.scheduleDownload(
            urls = urls,
            sha1 = sha1,
            targetFile = targetFile,
            size = size
        )
    }

    task.updateProgress(0.8f)

    libDownloader.apply {
        val neoforgeVersionString = "${forgeLikeVersion.loaderName.lowercase()}-$inherit-$loaderVersion"
        //去除其中的原始 ForgeLike
        removeDownload { lib ->
            (lib.targetFile.name.endsWith("$neoforgeVersionString.jar") ||
             lib.targetFile.name.endsWith("$neoforgeVersionString-client.jar")).also {
                if (it) {
                    Logger.lInfo(
                        "The download task has been removed from the scheduled downloads: \n" +
                        "url: \n${lib.urls.joinToString("\n")}\n" +
                        "target path: ${lib.targetFile.absolutePath}"
                    )
                }
            }
        }
    }

    //开始下载 NeoForge 支持库
    libDownloader.download(task)

    task.updateProgress(1f)
}

/**
 * 解析并提交下载Mojang映射
 */
private suspend fun scheduleMojangMappings(
    mergedJson: JsonObject,
    tempMinecraftDir: File,
    tempVanillaJar: File,
    tempInstaller: File,
    schedule: (urls: List<String>, sha1: String?, targetFile: File, size: Long) -> Unit
) = withContext(Dispatchers.IO) {
    val tempDir = File(tempMinecraftDir, ".temp/forge_installer_cache").ensureDirectory()
    val vars = mutableMapOf<String, String>()

    ZipFile(tempInstaller).use { zip ->
        zip.readText("install_profile.json").parseToJson()["data"].asJsonObject?.let { data ->
            for ((key, value) in data.entrySet()) {
                if (value.isJsonObject) {
                    val client = value.asJsonObject["client"]
                    if (client != null && client.isJsonPrimitive) {
                        parseLiteral(
                            baseDir = tempMinecraftDir,
                            literal = client.asString,
                            plainConverter = { str ->
                                val dest: Path = Files.createTempFile(tempDir.toPath(), null, null)
                                val item = str
                                    .removePrefix("\\")
                                    .removePrefix("/")
                                    .replace("\\", "/")
                                zip.extractEntryToFile(item, dest.toFile())
                                dest.toString()
                            }
                        )?.let {
                            vars[key] = it
                        }
                    }
                }
            }
        }
    }

    vars += mapOf(
        "SIDE" to "client",
        "MINECRAFT_JAR" to tempVanillaJar.absolutePath,
        "MINECRAFT_VERSION" to tempVanillaJar.absolutePath,
        "ROOT" to tempMinecraftDir.absolutePath,
        "INSTALLER" to tempInstaller.absolutePath,
        "LIBRARY_DIR" to File(tempMinecraftDir, "libraries").absolutePath
    )

    parseProcessors(
        baseDir = tempMinecraftDir,
        jsonObject = mergedJson,
        vars = vars,
        schedule = schedule
    )
}

/**
 * 解析Processors并安排Mojang映射下载
 */
private suspend fun parseProcessors(
    baseDir: File,
    jsonObject: JsonObject,
    vars: Map<String, String>,
    schedule: (urls: List<String>, sha1: String?, targetFile: File, size: Long) -> Unit
) = withContext(Dispatchers.IO) {
    val processors: List<ForgeLikeInstallProcessor> = jsonObject["processors"]?.asJsonArray?.let { processors ->
        val type = object : TypeToken<List<ForgeLikeInstallProcessor>>() {}.type
        GSON.fromJson(processors, type)
    } ?: return@withContext

    processors.map { processor ->
        parseOptions(baseDir, processor.getArgs(), vars)
    }.forEach { options ->
        if (options["task"] != "DOWNLOAD_MOJMAPS" || options["side"] != "client") return@forEach
        val version = options["version"] ?: return@forEach
        val output = options["output"] ?: return@forEach
        Logger.lInfo("Patching DOWNLOAD_MOJMAPS task")

        val versionManifest = MinecraftVersions.getVersionManifest()
        versionManifest.versions.find { it.id == version }?.let { vanilla ->
            val manifest = withRetry(FORGE_LIKE_ANALYSE_ID, maxRetries = 1) {
                fetchStringFromUrls(
                    vanilla.url.mapMirrorableUrls()
                ).parseTo(GameManifest::class.java)
            }
            manifest.downloads?.clientMappings?.let { mappings ->
                schedule(mappings.url.mapMirrorableUrls(), mappings.sha1, File(output), mappings.size)
                Logger.lInfo("Mappings: ${mappings.url} (SHA1: ${mappings.sha1})")
            } ?: throw Exception("client_mappings download info not found")
        }
    }
}

/**
 * 安装Forge任务（主入口）
 */
fun getForgeLikeInstallTask(
    isNew: Boolean,
    downloader: BaseMinecraftDownloader,
    forgeLikeVersion: ForgeLikeVersion,
    tempFolderName: String,
    tempInstaller: File,
    tempGameFolder: File,
    tempMinecraftDir: File,
    inherit: String
): Task {
    return Task.runTask(
        id = FORGE_LIKE_INSTALL_ID,
        task = { task ->
            val tempVanillaJar = File(tempMinecraftDir, "versions/$inherit/$inherit.jar")
            val tempVersionJson = File(tempMinecraftDir, "versions/$tempFolderName/$tempFolderName.json")
            
            if (isNew) { //新版 Forge、NeoForge
                //以 HMCL 的方式手动安装
                installNewForgeHMCLWay(
                    task = task,
                    forgeLikeVersion = forgeLikeVersion,
                    tempInstaller = tempInstaller,
                    tempGameFolder = tempGameFolder,
                    tempMinecraftDir = tempMinecraftDir,
                    tempVersionJson = tempVersionJson,
                    tempVanillaJar = tempVanillaJar
                )
            } else { //旧版 Forge
                installOldForge(
                    task = task,
                    downloader = downloader,
                    tempInstaller = tempInstaller,
                    tempMinecraftDir = tempMinecraftDir,
                    tempFolderName = tempFolderName,
                    tempVersionJson = tempVersionJson,
                    inherit = inherit
                )
            }

            // 修复 bootstraplauncher 0.1.17+ 的启动问题
            progressIgnoreList(tempVersionJson)
        }
    )
}

/**
 * 用 HMCL 的方式安装新版 Forge、NeoForge
 */
private suspend fun installNewForgeHMCLWay(
    task: Task,
    forgeLikeVersion: ForgeLikeVersion,
    tempInstaller: File,
    tempGameFolder: File,
    tempMinecraftDir: File,
    tempVersionJson: File,
    tempVanillaJar: File
) = withContext(Dispatchers.IO) {
    task.updateProgress(-1f)

    val tempDir = File(tempMinecraftDir, ".temp/forge_installer_cache").ensureDirectory()
    val vars = mutableMapOf<String, String>()

    val installProfile = ZipFile(tempInstaller).use { zip ->
        val installProfile = zip.readText("install_profile.json").parseToJson()
        //解压版本Json
        zip.extractEntryToFile("version.json", tempVersionJson)

        task.updateProgress(0.2f, R.string.download_game_install_forgelike_preparing_mapping_file, forgeLikeVersion.loaderName)
        installProfile["data"].asJsonObject?.let { data ->
            for ((key, value) in data.entrySet()) {
                if (value.isJsonObject) {
                    val client = value.asJsonObject["client"]
                    if (client != null && client.isJsonPrimitive) {
                        Logger.lInfo("Attempting to recognize mapping: ${client.asString}")
                        parseLiteral(
                            baseDir = tempMinecraftDir,
                            literal = client.asString,
                            plainConverter = { str ->
                                val dest: Path = Files.createTempFile(tempDir.toPath(), null, null)
                                val item = str
                                    .removePrefix("\\")
                                    .removePrefix("/")
                                    .replace("\\", "/")
                                zip.extractEntryToFile(item, dest.toFile())
                                Logger.lInfo("Extracting item $item to directory $dest")
                                dest.toString()
                            }
                        )?.let {
                            vars[key] = it
                            Logger.lInfo("Recognized as mapping $key - $it")
                        }
                    }
                }
            }
        }

        installProfile
    }

    task.updateProgress(1f, R.string.download_game_install_forgelike_preparing_mapping_file, forgeLikeVersion.loaderName)

    vars["SIDE"] = "client"
    vars["MINECRAFT_JAR"] = tempVanillaJar.absolutePath
    vars["MINECRAFT_VERSION"] = tempVanillaJar.absolutePath
    vars["ROOT"] = tempMinecraftDir.absolutePath
    vars["INSTALLER"] = tempInstaller.absolutePath
    vars["LIBRARY_DIR"] = File(tempMinecraftDir, "libraries").absolutePath

    val processors: List<ForgeLikeInstallProcessor> = installProfile["processors"]
        ?.asJsonArray
        ?.let { GSON.fromJson(it, object : TypeToken<List<ForgeLikeInstallProcessor>>() {}.type) }
        ?: return@withContext

    runProcessors(
        task = task,
        tempMinecraftDir = tempMinecraftDir,
        tempGameDir = tempGameFolder,
        processors = processors,
        vars = vars
    )
}

/**
 * 安装旧版 Forge
 */
private suspend fun installOldForge(
    task: Task,
    downloader: BaseMinecraftDownloader,
    tempInstaller: File,
    tempMinecraftDir: File,
    tempFolderName: String,
    tempVersionJson: File,
    inherit: String
) = withContext(Dispatchers.IO) {
    val librariesFolder = File(tempMinecraftDir, "libraries")

    val versionInfo: JsonObject? = ZipFile(tempInstaller).use { zip ->
        task.updateProgress(0.2f)
        val installProfile = zip.readText("install_profile.json").parseToJson()

        task.updateProgress(0.4f)
        File(tempMinecraftDir, "versions/$tempFolderName").ensureDirectory()
        task.updateProgress(0.5f)

        if (!installProfile.has("install")) {
            Logger.lInfo("Starting the Forge installation, Legacy method A")

            //建立 Json 文件
            val jsonVersion = zip.readText(installProfile["json"].asString.trimStart('/')).parseToJson()
            jsonVersion.addProperty("id", tempFolderName)
            tempVersionJson.writeText(GSON.toJson(jsonVersion))
            task.updateProgress(0.6f)

            //解压支持库文件
            zip.extractDirectory("maven", librariesFolder)

            null
        } else {
            Logger.lInfo("Starting the Forge installation, Legacy method B")
            val artifact = installProfile["install"].asJsonObject["path"].asString
            val jarPath = getLibraryPath(artifact, baseFolder = tempMinecraftDir.absolutePath)

            val jarFile = File(jarPath)
            if (jarFile.exists()) jarFile.delete()

            //解压 Jar 文件
            zip.extractEntryToFile(installProfile["install"].asJsonObject["filePath"].asString, jarFile)
            task.updateProgress(0.9f)

            //建立 Json 文件
            val versionInfo = installProfile["versionInfo"].asJsonObject
            if (!versionInfo.has("inheritsFrom")) {
                versionInfo.addProperty("inheritsFrom", inherit)
            }
            tempVersionJson.writeText(GSON.toJson(versionInfo))

            versionInfo.apply {
                get("libraries").asJsonArray.removeAll {
                    val lib = it.asJsonObject
                    if (lib.has("name")) {
                        val name = lib["name"].asString
                        //过滤掉 path 对应的 library (这个是需要解压出去的，没法下载)
                        //比如 net.minecraftforge:forge:1.7.10-10.13.4.1614-1.7.10
                        name == artifact
                    } else false //保留
                }
            }
        }
    }
    task.updateProgress(1f)

    //判断是否需要补全 Forge 支持库
    versionInfo?.let { info ->
        val libDownloader = GameLibDownloader(
            downloader = downloader,
            gameManifest = GSON.fromJson(GSON.toJson(info), GameManifest::class.java)
        )
        libDownloader.schedule(
            task = task,
            targetDir = librariesFolder
        )
        //开始补全 Forge 支持库
        libDownloader.download(task)
    }
}

/**
 * 执行Processors
 */
private suspend fun runProcessors(
    task: Task,
    tempMinecraftDir: File,
    tempGameDir: File,
    processors: List<ForgeLikeInstallProcessor>,
    vars: Map<String, String>
): Unit = withContext(Dispatchers.IO) {
    //优先构建所有需要执行的命令，以便于更好的计算进度
    val commandList = processors.mapNotNull { processor ->
        val options = parseOptions(tempMinecraftDir, processor.getArgs(), vars)
        if (options["task"] == "DOWNLOAD_MOJMAPS" || !processor.isSide("client")) return@mapNotNull null

        val outputs: Map<String, String> = processor.getOutputs().mapKeys { (k, _) ->
            parseLiteral(tempMinecraftDir, k, vars) ?: run {
                throw IllegalArgumentException("Invalid forge installation configuration")
            }
        }.mapValues { (_, v) ->
            parseLiteral(tempMinecraftDir, v, vars) ?: run {
                throw IllegalArgumentException("Invalid forge installation configuration")
            }
        }.also {
            Logger.lInfo("Parsed output mappings for ${processor.javaClass.simpleName}: ${it.entries.joinToString("\n") { entry -> "${entry.key} = ${entry.value}" }}")
        }

        val anyMissing = outputs.any { (key, expectedHash) ->
            val artifact = tempMinecraftDir.toPath().resolve(key)
            if (!Files.exists(artifact)) return@any true

            val actualHash = Files.newInputStream(artifact).use { stream ->
                DigestUtils.digestToString("SHA-1", stream)
            }
            if (actualHash != expectedHash) {
                Files.delete(artifact)
                Logger.lInfo("Invalid artifact removed: $artifact")
                true
            } else false
        }

        if (outputs.isNotEmpty() && !anyMissing) return@mapNotNull null

        val jarPath = tempMinecraftDir.toPath().resolve("libraries").resolve(processor.getJar().toPath())
        require(Files.isRegularFile(jarPath)) { "Game processor file not found: $jarPath" }

        val mainClass = JarFile(jarPath.toFile()).use {
            it.manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
        }.takeIf(String::isNotBlank)
            ?: throw Exception("Game processor jar missing Main-Class: $jarPath")

        val classpath = processor.getClasspath().map { lib ->
            tempMinecraftDir.toPath().resolve("libraries").resolve(lib.toPath()).also { path ->
                require(Files.isRegularFile(path)) { "Missing dependency: $path" }
            }.toString()
        } + jarPath.toString()

        //构建 JvmArgs
        val jvmArgs = buildList {
            add("-cp")
            add(classpath.joinToString(File.pathSeparator))
            add(mainClass)
            addAll(
                processor.getArgs().map { arg ->
                    parseLiteral(tempMinecraftDir, arg, vars)
                        ?: throw IOException("Invalid forge installation argument: $arg")
                }
            )
        }.joinToString(" ")

        Triple(processor, jvmArgs, outputs.map { (key, value) -> Paths.get(key) to value })
    }

    //正式开始执行命令
    commandList.forEachIndexed { index, (processor, jvmArgs, outputs) ->
        // 这里需要执行JVM命令，暂时用日志代替
        Logger.lInfo("Would execute: $jvmArgs")
        
        val progress = index.toFloat() / commandList.size
        task.updateProgress(progress, R.string.download_game_install_base_installing,
            outputs.joinToString(", ") { (artifact, _) -> artifact.name }
        )

        Logger.lInfo("Start to run ${processor.getJar()} with args: $jvmArgs")

        // TODO: 实际执行JVM命令需要集成JRE运行环境
        // runJvmRetryRuntimes(...) 

        for ((artifact, value) in outputs) {
            if (!Files.isRegularFile(artifact)) throw FileNotFoundException("File missing: $artifact")

            val code: String = Files.newInputStream(artifact).use { stream ->
                DigestUtils.digestToString("SHA-1", stream)
            }
            if (code != value) {
                Files.delete(artifact)
                throw IOException("Checksum mismatch: expected $value, got $code for $artifact")
            }
        }
    }
}

/**
 * 修复 bootstraplauncher 0.1.17+ 的启动问题
 */
private suspend fun progressIgnoreList(
    tempVersionJson: File
) = withContext(Dispatchers.IO) {
    val jsonObject = tempVersionJson.readText().parseToJson()

    val libraries = jsonObject["libraries"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return@withContext

    val hasNewBootstrapLauncher = libraries
        .mapNotNull { je ->
            je.takeIf { je1 -> je1.isJsonObject }
                ?.asJsonObject
                ?.get("name")
                ?.takeIf { !it.isJsonNull }
                ?.asString
        }
        .map { parseLibraryComponents(it) }
        .any { it.groupId == "cpw.mods" && it.artifactId == "bootstraplauncher" && it.version >= "0.1.17" }

    if (!hasNewBootstrapLauncher) return@withContext

    val jvmArgs = jsonObject["arguments"]?.takeIf { it.isJsonObject }
        ?.asJsonObject?.get("jvm")?.takeIf { it.isJsonArray }?.asJsonArray ?: return@withContext

    val ignoreListIndex = jvmArgs.indexOfLast {
        it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.startsWith("-DignoreList=")
    }.takeIf { it != -1 } ?: return@withContext

    //追加 ${primary_jar_name}
    val originalArg = jvmArgs[ignoreListIndex].asString
    jvmArgs[ignoreListIndex] = GSON.toJsonTree("$originalArg,\${primary_jar_name}")

    tempVersionJson.writeText(
        GSON.toJson(jsonObject)
    )
}

/**
 * 解析库组件
 */
data class LibraryComponents(
    val groupId: String,
    val artifactId: String,
    val version: String
)

fun parseLibraryComponents(name: String): LibraryComponents {
    val parts = name.split(":")
    return LibraryComponents(
        groupId = parts.getOrNull(0) ?: "",
        artifactId = parts.getOrNull(1) ?: "",
        version = parts.getOrNull(2) ?: ""
    )
}

/**
 * 版本比较扩展
 */
operator fun String.compareTo(other: String): Int {
    val v1 = this.split(".").map { it.toIntOrNull() ?: 0 }
    val v2 = other.split(".").map { it.toIntOrNull() ?: 0 }
    
    for (i in 0 until maxOf(v1.size, v2.size)) {
        val part1 = v1.getOrNull(i) ?: 0
        val part2 = v2.getOrNull(i) ?: 0
        if (part1 != part2) return part1 - part2
    }
    return 0
}

operator fun String.compareTo(other: Int): Int = this.compareTo(other.toString())
