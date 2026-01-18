package com.lanrhyme.shardlauncher.game.download.game.fabric

import com.lanrhyme.shardlauncher.coroutine.Task
import com.lanrhyme.shardlauncher.game.download.game.GameLibDownloader
import com.lanrhyme.shardlauncher.game.version.download.BaseMinecraftDownloader
import com.lanrhyme.shardlauncher.utils.file.ensureParentDirectory
import com.lanrhyme.shardlauncher.utils.network.fetchStringFromUrls
import kotlinx.coroutines.Dispatchers
import java.io.File

const val FABRIC_LIKE_DOWNLOAD_ID = "Download.FabricLike"
const val FABRIC_LIKE_COMPLETER_ID = "Completer.FabricLike"

/**
 * Fabric/Quilt安装任务
 * 相比Forge，Fabric/Quilt的安装非常简单：
 * 1. 下载版本JSON配置文件
 * 2. 根据JSON中的库列表下载所有依赖
 */

/**
 * 下载Fabric/Quilt版本JSON
 */
fun getFabricLikeDownloadTask(
    loaderJsonUrl: String,
    tempVersionJson: File
): Task {
    return Task.runTask(
        id = FABRIC_LIKE_DOWNLOAD_ID,
        dispatcher = Dispatchers.IO,
        task = {
            // 下载版本Json（包含所有库信息）
            val loaderJson = fetchStringFromUrls(listOf(loaderJsonUrl))
            tempVersionJson
                .ensureParentDirectory()
                .writeText(loaderJson)
        }
    )
}

/**
 * 完成Fabric/Quilt安装（下载所有库文件）
 */
fun getFabricLikeCompleterTask(
    downloader: BaseMinecraftDownloader,
    tempMinecraftDir: File,
    tempVersionJson: File
): Task {
    return Task.runTask(
        id = FABRIC_LIKE_COMPLETER_ID,
        task = { task ->
            // 创建库下载器
            val libDownloader = GameLibDownloader(
                downloader = downloader,
                gameManifest = com.lanrhyme.shardlauncher.utils.GSON.fromJson(
                    tempVersionJson.readText(),
                    com.lanrhyme.shardlauncher.game.versioninfo.models.GameManifest::class.java
                ),
                maxDownloadThreads = 32
            )
            
            // 提交下载计划
            libDownloader.schedule(
                task = task,
                targetDir = File(tempMinecraftDir, "libraries")
            )
            
            // 下载所有库文件
            libDownloader.download(task)
        }
    )
}
