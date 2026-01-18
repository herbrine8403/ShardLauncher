package com.lanrhyme.shardlauncher.game.download.game

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import com.google.gson.JsonObject
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.coroutine.Task
import com.lanrhyme.shardlauncher.coroutine.TaskFlowExecutor
import com.lanrhyme.shardlauncher.coroutine.TitledTask
import com.lanrhyme.shardlauncher.coroutine.addTask
import com.lanrhyme.shardlauncher.coroutine.buildPhase
import com.lanrhyme.shardlauncher.game.addons.mirror.mapMirrorableUrls
import com.lanrhyme.shardlauncher.game.download.game.forge.getForgeLikeAnalyseTask
import com.lanrhyme.shardlauncher.game.download.game.forge.getForgeLikeDownloadTask
import com.lanrhyme.shardlauncher.game.download.game.forge.getForgeLikeInstallTask
import com.lanrhyme.shardlauncher.game.download.game.fabric.getFabricLikeCompleterTask
import com.lanrhyme.shardlauncher.game.download.game.fabric.getFabricLikeDownloadTask
import com.lanrhyme.shardlauncher.game.modloader.forgelike.ForgeVersion
import com.lanrhyme.shardlauncher.game.modloader.forgelike.NeoForgeVersion
import com.lanrhyme.shardlauncher.game.download.game.ForgeVersion as DownloadForgeVersion
import com.lanrhyme.shardlauncher.game.download.game.NeoForgeVersion as DownloadNeoForgeVersion
import com.lanrhyme.shardlauncher.game.path.getGameHome
import com.lanrhyme.shardlauncher.game.version.download.BaseMinecraftDownloader
import com.lanrhyme.shardlauncher.game.version.download.GameLibDownloader
import com.lanrhyme.shardlauncher.game.version.download.MinecraftDownloader
import com.lanrhyme.shardlauncher.game.version.installed.VersionsManager
import com.lanrhyme.shardlauncher.game.versioninfo.models.GameManifest
import com.lanrhyme.shardlauncher.path.PathManager
import com.lanrhyme.shardlauncher.settings.AllSettings
import com.lanrhyme.shardlauncher.utils.GSON
import com.lanrhyme.shardlauncher.utils.logging.Logger
import com.lanrhyme.shardlauncher.utils.network.downloadFromMirrorListSuspend
import com.lanrhyme.shardlauncher.utils.network.fetchStringFromUrls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * 在安装游戏前发现存在冲突的已安装版本，抛出这个异常
 */
private class GameAlreadyInstalledException : RuntimeException()

/**
 * 游戏安装器
 * @param context 用于获取任务描述信息
 * @param info 安装游戏所需要的信息，包括 Minecraft id、自定义版本名称、Addon 列表
 * @param scope 在有生命周期管理的scope中执行安装任务
 */
class GameInstaller(
    private val context: Context,
    private val info: GameDownloadInfo,
    private val scope: CoroutineScope
) {
    private val taskExecutor = TaskFlowExecutor(scope)
    val tasksFlow: StateFlow<List<TitledTask>> = taskExecutor.tasksFlow

    /**
     * 基础下载器
     */
    private val downloader = BaseMinecraftDownloader(verifyIntegrity = true)

    /**
     * 目标游戏客户端目录（缓存）
     * versions/<client-name>/...
     */
    private var targetClientDir: File? = null

    /**
     * 目标游戏目录
     */
    private val targetGameFolder: File = File(getGameHome())

    /**
     * 安装 Minecraft 游戏
     * @param isRunning 正在运行中，阻止此次安装时
     * @param onInstalled 游戏已完成安装
     * @param onError 游戏安装失败
     * @param onGameAlreadyInstalled 在安装游戏前发现存在冲突的已安装版本
     */
    fun installGame(
        isRunning: () -> Unit = {},
        onInstalled: (version: String) -> Unit,
        onError: (th: Throwable) -> Unit,
        onGameAlreadyInstalled: () -> Unit
    ) {
        if (taskExecutor.isRunning()) {
            //正在安装中，阻止这次安装请求
            isRunning()
            return
        }

        taskExecutor.executePhasesAsync(
            onStart = {
                val tasks = getTaskPhase()
                taskExecutor.addPhases(tasks)
            },
            onComplete = {
                onInstalled(info.customVersionName)
            },
            onError = { th ->
                if (th is GameAlreadyInstalledException) {
                    onGameAlreadyInstalled()
                } else {
                    onError(th)
                }
            }
        )
    }

    /**
     * 安装过程中所需的所有文件路径配置
     */
    private class InstallationPathConfig(
        val targetClientDir: File,
        val tempGameDir: File,
        val tempMinecraftDir: File,
        val tempGameVersionsDir: File,
        val tempClientDir: File,
        val fabricDir: File?
    )

    /**
     * 构建安装过程中使用的所有路径配置
     */
    private fun createPathConfig(checkTargetVersion: Boolean): InstallationPathConfig {
        //目标版本目录
        val targetClientDir1 = VersionsManager.getVersionPath(info.customVersionName)
        targetClientDir = targetClientDir1
        val targetVersionJson = File(targetClientDir1, "${info.customVersionName}.json")

        //目标版本已经安装的情况
        if (checkTargetVersion && targetVersionJson.exists()) {
            Logger.lDebug("The game has already been installed!")
            throw GameAlreadyInstalledException()
        }

        val tempGameDir = PathManager.DIR_CACHE_GAME_DOWNLOADER
        val tempMinecraftDir = File(tempGameDir, ".minecraft")
        val tempGameVersionsDir = File(tempMinecraftDir, "versions")
        val tempClientDir = File(tempGameVersionsDir, info.gameVersion)

        //ModLoader临时目录
        val fabricDir = info.fabric?.let { File(tempGameVersionsDir, "fabric-loader-${it.version}-${info.gameVersion}") }

        return InstallationPathConfig(
            targetClientDir = targetClientDir1,
            tempGameDir = tempGameDir,
            tempMinecraftDir = tempMinecraftDir,
            tempGameVersionsDir = tempGameVersionsDir,
            tempClientDir = tempClientDir,
            fabricDir = fabricDir
        )
    }

    /**
     * 获取安装 Minecraft 游戏的任务流阶段
     */
    private fun getTaskPhase(): List<TaskFlowExecutor.TaskPhase> {
        val pathConfig = createPathConfig(checkTargetVersion = true)

        return listOf(
            buildPhase {
                //开始之前，应该先清理一次临时游戏目录，否则可能会影响安装结果
                addTask(
                    id = "Download.Game.ClearTemp",
                    title = context.getString(R.string.download_install_clear_temp),
                    icon = Icons.Outlined.CleaningServices,
                    task = Task.runTask(id = "Download.Game.ClearTemp", task = { task ->
                        clearTempGameDir()
                        //清理完成缓存目录后，创建新的缓存目录
                        pathConfig.tempClientDir.createDirAndLog()
                        pathConfig.fabricDir?.createDirAndLog()
                    })
                )

                //下载安装原版
                addTask(
                    title = context.getString(R.string.download_game_install_vanilla, info.gameVersion),
                    task = createMinecraftDownloadTask(info.gameVersion, pathConfig.tempGameVersionsDir)
                )

                //下载加载器/模组
                addLoaderTasks(
                    tempGameDir = pathConfig.tempGameDir,
                    tempMinecraftDir = pathConfig.tempMinecraftDir,
                    fabricDir = pathConfig.fabricDir
                )

                //最终游戏安装任务
                addTask(
                    title = context.getString(R.string.download_game_install_game_files_progress),
                    icon = Icons.Outlined.Build,
                    //如果有非原版以外的任务，则需要进行处理安装（合并版本Json、迁移文件等）
                    task = if (pathConfig.fabricDir != null) {
                        createGameInstalledTask(
                            tempMinecraftDir = pathConfig.tempMinecraftDir,
                            targetMinecraftDir = targetGameFolder,
                            targetClientDir = pathConfig.targetClientDir,
                            tempClientDir = pathConfig.tempClientDir,
                            fabricFolder = pathConfig.fabricDir
                        )
                    } else {
                        //仅仅下载了原版，只复制版本client文件
                        createVanillaFilesCopyTask(
                            tempMinecraftDir = pathConfig.tempMinecraftDir
                        )
                    }
                )
            }
        )
    }

    private fun MutableList<TitledTask>.addLoaderTasks(
        tempGameDir: File,
        tempMinecraftDir: File,
        fabricDir: File?
    ) {
        // Fabric 安装
        info.fabric?.let {
            createFabricLikeTask(
                loaderName = "Fabric",
                loaderVersion = it,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = fabricDir!!.name
            )
        }
        
        // Quilt 安装
        info.quilt?.let {
            createFabricLikeTask(
                loaderName = "Quilt",
                loaderVersion = it,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = fabricDir!!.name
            )
        }
        
        // Forge 安装
        info.forge?.let {
            createForgeLikeTask(
                loaderName = "Forge",
                loaderVersion = it,
                tempGameDir = tempGameDir,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = "forge-${it.version}-${info.gameVersion}"
            )
        }
        
        // NeoForge 安装
        info.neoForge?.let {
            createForgeLikeTask(
                loaderName = "NeoForge",
                loaderVersion = it,
                tempGameDir = tempGameDir,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = "neoforge-${it.version}-${info.gameVersion}"
            )
        }
    }

    fun cancelInstall() {
        taskExecutor.cancel()
        clearTargetClient()
    }

    /**
     * 清除临时游戏目录
     */
    private fun clearTempGameDir() {
        PathManager.DIR_CACHE_GAME_DOWNLOADER.takeIf { it.exists() }?.let { folder ->
            FileUtils.deleteQuietly(folder)
            Logger.lInfo("Temporary game directory cleared.")
        }
    }

    /**
     * 安装失败、取消安装时，都应该清除目标客户端版本文件夹
     */
    private fun clearTargetClient() {
        val dirToDelete = targetClientDir //临时变量
        targetClientDir = null

        CoroutineScope(Dispatchers.IO).launch {
            dirToDelete?.let {
                //直接清除上一次安装的目标目录
                FileUtils.deleteQuietly(it)
                Logger.lInfo("Successfully deleted version directory: ${it.name} at path: ${it.absolutePath}")
            }
        }
    }

    /**
     * 获取下载原版 Task
     */
    private fun createMinecraftDownloadTask(
        tempClientName: String,
        tempVersionsDir: File
    ): Task {
        val mcDownloader = MinecraftDownloader(
            context = context,
            version = info.gameVersion,
            customName = info.customVersionName,
            verifyIntegrity = true
        )

        return mcDownloader.getDownloadTask(tempClientName, tempVersionsDir)
    }

    private fun MutableList<TitledTask>.createFabricLikeTask(
        loaderName: String,
        loaderVersion: ModLoaderVersion,
        tempMinecraftDir: File,
        tempFolderName: String
    ) {
        val tempVersionJson = File(tempMinecraftDir, "versions/$tempFolderName/$tempFolderName.json")

        // 构造版本JSON下载URL
        val loaderJsonUrl = when (loaderName) {
            "Fabric" -> {
                val downloadSource = AllSettings.fetchModLoaderSource.state
                // 支持镜像源
                if (downloadSource == com.lanrhyme.shardlauncher.settings.enums.MirrorSourceType.MIRROR_FIRST) {
                    "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/loader/${info.gameVersion}/${loaderVersion.version}/profile/json"
                } else {
                    "https://meta.fabricmc.net/v2/versions/loader/${info.gameVersion}/${loaderVersion.version}/profile/json"
                }
            }
            "Quilt" -> {
                val downloadSource = AllSettings.fetchModLoaderSource.state
                // 支持镜像源
                if (downloadSource == com.lanrhyme.shardlauncher.settings.enums.MirrorSourceType.MIRROR_FIRST) {
                    "https://bmclapi2.bangbang93.com/quilt-meta/v3/versions/loader/${info.gameVersion}/${loaderVersion.version}/profile/json"
                } else {
                    "https://meta.quiltmc.org/v3/versions/loader/${info.gameVersion}/${loaderVersion.version}/profile/json"
                }
            }
            else -> throw IllegalArgumentException("不支持的加载器: $loaderName")
        }

        // 1. 下载版本JSON配置文件
        addTask(
            title = context.getString(
                R.string.download_game_install_fabric,
                loaderVersion.version
            ),
            task = com.lanrhyme.shardlauncher.game.download.game.fabric.getFabricLikeDownloadTask(
                loaderJsonUrl = loaderJsonUrl,
                tempVersionJson = tempVersionJson
            )
        )
        
        // 2. 下载所有库文件（完成安装）
        addTask(
            title = context.getString(R.string.download_game_install_game_files_progress),
            task = com.lanrhyme.shardlauncher.game.download.game.fabric.getFabricLikeCompleterTask(
                downloader = this@GameInstaller.downloader,
                tempMinecraftDir = tempMinecraftDir,
                tempVersionJson = tempVersionJson
            )
        )
    }
    
    private fun MutableList<TitledTask>.createForgeLikeTask(
        loaderName: String,
        loaderVersion: ModLoaderVersion,
        tempGameDir: File,
        tempMinecraftDir: File,
        tempFolderName: String
    ) {
        val tempVersionJson = File(tempMinecraftDir, "versions/$tempFolderName/$tempFolderName.json")
        val tempInstallerJar = File(tempGameDir, "$tempFolderName-installer.jar")

        // 创建ForgeLikeVersion对象
        val forgeLikeVersion = when (loaderName) {
            "Forge" -> {
                val forgeVersion = loaderVersion as DownloadForgeVersion
                ForgeVersion(
                    versionName = forgeVersion.version,
                    branch = forgeVersion.branch,
                    inherit = info.gameVersion,
                    releaseTime = "",
                    hash = null,
                    isRecommended = false, // DownloadForgeVersion 没有 isRecommended 字段，使用默认值
                    category = "installer",
                    fileVersion = forgeVersion.fileVersion ?: "${info.gameVersion}-${forgeVersion.version}",
                    isLegacy = false // 可以根据版本号判断
                )
            }
            "NeoForge" -> {
                val neoForgeVersion = loaderVersion as DownloadNeoForgeVersion
                NeoForgeVersion(
                    versionName = neoForgeVersion.version,
                    inherit = info.gameVersion,
                    isLegacy = false
                )
            }
            else -> throw IllegalArgumentException("不支持的加载器: $loaderName")
        }

        // 1. 下载安装器
        addTask(
            title = "下载 $loaderName 安装器",
            task = com.lanrhyme.shardlauncher.game.download.game.forge.getForgeLikeDownloadTask(
                targetTempInstaller = tempInstallerJar,
                forgeLikeVersion = forgeLikeVersion
            )
        )
        
        // 2. 分析安装器（解压、解析配置）
        addTask(
            title = "分析 $loaderName 安装器",
            task = com.lanrhyme.shardlauncher.game.download.game.forge.getForgeLikeAnalyseTask(
                downloader = this@GameInstaller.downloader,
                targetTempInstaller = tempInstallerJar,
                forgeLikeVersion = forgeLikeVersion,
                tempMinecraftFolder = tempMinecraftDir,
                sourceInherit = info.gameVersion,
                processedInherit = info.gameVersion,
                loaderVersion = loaderVersion.version
            )
        )
        
        // 3. 执行安装（运行processors）
        addTask(
            title = "安装 $loaderName",
            task = com.lanrhyme.shardlauncher.game.download.game.forge.getForgeLikeInstallTask(
                isNew = true, // TODO: 根据版本号判断是否为新版
                downloader = this@GameInstaller.downloader,
                forgeLikeVersion = forgeLikeVersion,
                tempFolderName = tempFolderName,
                tempInstaller = tempInstallerJar,
                tempGameFolder = tempGameDir,
                tempMinecraftDir = tempMinecraftDir,
                inherit = info.gameVersion
            )
        )
    }

    /**
     * 游戏带附加内容安装完成，合并版本Json、迁移游戏文件
     */
    private fun createGameInstalledTask(
        tempMinecraftDir: File,
        targetMinecraftDir: File,
        targetClientDir: File,
        tempClientDir: File,
        fabricFolder: File? = null
    ) = Task.runTask(
        id = GAME_JSON_MERGER_ID,
        dispatcher = Dispatchers.IO,
        task = { task ->
            //合并版本 Json
            task.updateProgress(0.1f)
            mergeGameJson(
                info = info,
                outputFolder = targetClientDir,
                clientFolder = tempClientDir,
                fabricFolder = fabricFolder
            )

            //迁移游戏文件
            val sourceLibraries = File(tempMinecraftDir, "libraries")
            val targetLibraries = File(targetMinecraftDir, "libraries")
            if (sourceLibraries.exists()) {
                FileUtils.copyDirectory(sourceLibraries, targetLibraries)
            }

            //复制客户端文件
            copyVanillaFiles(
                sourceGameFolder = tempMinecraftDir,
                sourceVersion = info.gameVersion,
                destinationGameFolder = targetGameFolder,
                targetVersion = info.customVersionName
            )

            //清除临时游戏目录
            task.updateProgress(-1f, R.string.download_install_clear_temp)
            clearTempGameDir()
        }
    )

    /**
     * 仅原本客户端文件复制任务 json、jar
     */
    private fun createVanillaFilesCopyTask(
        tempMinecraftDir: File
    ): Task {
        return Task.runTask(
            id = "VanillaFilesCopy",
            task = { task ->
                //复制客户端文件
                copyVanillaFiles(
                    sourceGameFolder = tempMinecraftDir,
                    sourceVersion = info.gameVersion,
                    destinationGameFolder = targetGameFolder,
                    targetVersion = info.customVersionName
                )

                //清除临时游戏目录
                task.updateProgress(-1f, R.string.download_install_clear_temp)
                clearTempGameDir()
            }
        )
    }

    private fun File.createDirAndLog(): File {
        this.mkdirs()
        Logger.lDebug("Created directory: $this")
        return this
    }

    companion object {
        private const val GAME_JSON_MERGER_ID = "GameJsonMerger"
        
        // Forge 下载 URL 模板
        private const val FORGE_MAVEN_URL = "https://files.minecraftforge.net/maven/net/minecraftforge/forge"
        
        // NeoForge 下载 URL 模板
        private const val NEOFORGE_MAVEN_URL = "https://maven.neoforged.net/releases/net/neoforged"
        
        /**
         * 获取 Forge 安装器下载 URL
         */
        fun getForgeDownloadUrl(mcVersion: String, forgeVersion: String): String {
            // Forge 版本格式：1.19.3-41.2.8
            // URL 格式：https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.19.3-41.2.8/forge-1.19.3-41.2.8-installer.jar
            return "$FORGE_MAVEN_URL/$mcVersion-$forgeVersion/forge-$mcVersion-$forgeVersion-installer.jar"
        }
        
        /**
         * 获取 Forge 版本 JSON URL
         * 使用 BMCLAPI 提供的 Forge 版本 JSON
         */
        fun getForgeVersionJsonUrl(mcVersion: String, forgeVersion: String): String? {
            // Forge 版本格式：1.19.3-41.2.8
            // BMCLAPI 提供版本 JSON：https://bmclapi2.bangbang93.com/maven/net/minecraftforge/forge/{version}/forge-{version}.json
            val fullVersion = "$mcVersion-$forgeVersion"
            return "$FORGE_MAVEN_URL/$fullVersion/forge-$fullVersion.json"
        }
        
        /**
         * 获取 NeoForge 安装器下载 URL
         */
        fun getNeoForgeDownloadUrl(mcVersion: String, neoForgeVersion: String): String {
            // NeoForge 版本格式：1.20.1-47.1.76
            // URL 格式：https://maven.neoforged.net/releases/net/neoforged/neoforge/1.20.1-47.1.76/neoforge-1.20.1-47.1.76-installer.jar
            return "$NEOFORGE_MAVEN_URL/neoforge/$mcVersion-$neoForgeVersion/neoforge-$mcVersion-$neoForgeVersion-installer.jar"
        }
        
        /**
         * 获取 NeoForge 版本 JSON URL
         * 使用 BMCLAPI 提供的 NeoForge 版本 JSON
         */
        fun getNeoForgeVersionJsonUrl(mcVersion: String, neoForgeVersion: String): String? {
            // NeoForge 版本格式：1.20.1-47.1.76
            // BMCLAPI 提供版本 JSON：https://bmclapi2.bangbang93.com/maven/net/neoforged/neoforge/{version}/neoforge-{version}.json
            val fullVersion = "$mcVersion-$neoForgeVersion"
            return "$NEOFORGE_MAVEN_URL/neoforge/$fullVersion/neoforge-$fullVersion.json"
        }
    }
}

/**
 * 合并游戏版本Json
 */
private fun mergeGameJson(
    info: GameDownloadInfo,
    outputFolder: File,
    clientFolder: File,
    fabricFolder: File?
) {
    //合并版本 Json
    val vanillaJson = File(clientFolder, "${info.gameVersion}.json")
    val fabricJson = fabricFolder?.let { File(it, "${it.name}.json") }
    val outputJson = File(outputFolder, "${info.customVersionName}.json")
    
    // 如果有 Fabric，使用 Fabric 配置，否则使用原版配置
    val finalJson = fabricJson?.takeIf { it.exists() } ?: vanillaJson
    
    // 复制并保存最终配置
    finalJson.copyTo(outputJson, overwrite = true)
    Logger.lInfo("Merged game JSON files")
}

/**
 * 复制原版游戏文件
 */
internal fun copyVanillaFiles(
    sourceGameFolder: File,
    sourceVersion: String,
    destinationGameFolder: File,
    targetVersion: String
) {
    // 复制客户端文件
    val sourceClientFolder = File(sourceGameFolder, "versions/$sourceVersion")
    val destClientFolder = File(destinationGameFolder, "versions/$targetVersion")
    
    // 创建目标目录
    destClientFolder.mkdirs()
    
    // 复制JSON文件
    val sourceJson = File(sourceClientFolder, "$sourceVersion.json")
    val destJson = File(destClientFolder, "$targetVersion.json")
    sourceJson.copyTo(destJson, overwrite = true)
    
    // 复制JAR文件
    val sourceJar = File(sourceClientFolder, "$sourceVersion.jar")
    val destJar = File(destClientFolder, "$targetVersion.jar")
    sourceJar.copyTo(destJar, overwrite = true)
    
    Logger.lInfo("Copied vanilla files from $sourceClientFolder to $destClientFolder")
}

/**
 * 根据 Maven 坐标获取库文件路径
 * 例如: "com.google.guava:guava:28.2" -> "com/google/guava/guava/28.2/guava-28.2.jar"
 */
fun getLibraryPath(path: String): String {
    val parts = path.split(":")
    return when (parts.size) {
        3 -> {
            val (group, artifact, version) = parts
            val groupPath = group.replace(".", "/")
            "$groupPath/$artifact/$version/$artifact-$version.jar"
        }
        4 -> {
            val (group, artifact, version, classifier) = parts
            val groupPath = group.replace(".", "/")
            "$groupPath/$artifact/$version/$artifact-$version-$classifier.jar"
        }
        else -> throw IllegalArgumentException("Invalid library path format: $path")
    }
}

/**
 * 根据 artifact 信息获取库文件路径
 */
fun getLibraryPath(artifact: JsonObject, baseFolder: String): String {
    val path = artifact.get("path")?.asString
    if (path != null) return path
    
    val name = artifact.get("name")?.asString
        ?: throw IllegalArgumentException("Artifact must have 'name' or 'path' property")
    
    return getLibraryPath(name)
}