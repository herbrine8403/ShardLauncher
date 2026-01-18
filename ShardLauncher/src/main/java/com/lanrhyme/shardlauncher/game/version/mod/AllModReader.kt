package com.lanrhyme.shardlauncher.game.version.mod

import com.lanrhyme.shardlauncher.utils.logging.Logger.lWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val READER_PARALLELISM = 8

/**
 * 模组目录扫描器
 * 并发读取所有模组文件的元数据
 */
class AllModReader(val modsDir: File) {
    private val tasks = mutableListOf<ReadTask>()

    private fun scanFiles() {
        tasks.clear()
        val files = modsDir.listFiles()?.filter { !it.isDirectory } ?: return
        files.forEach { file ->
            tasks.add(ReadTask(file))
        }
    }

    /**
     * 异步读取所有模组文件
     * @return 模组列表，按文件名排序
     */
    suspend fun readAllMods(): List<LocalMod> = withContext(Dispatchers.IO) {
        // 扫描文件，封装任务
        scanFiles()

        val results = mutableListOf<LocalMod>()
        val taskChannel = Channel<ReadTask>(Channel.UNLIMITED)

        // 创建多个并发工作协程
        val workers = List(READER_PARALLELISM) {
            launch(Dispatchers.IO) {
                for (task in taskChannel) {
                    val mod = task.execute()
                    synchronized(results) {
                        results.add(mod)
                    }
                }
            }
        }

        // 将所有任务发送到通道
        tasks.forEach { taskChannel.send(it) }
        taskChannel.close()

        // 等待所有工作协程完成
        workers.joinAll()

        return@withContext results.sortedBy { it.file.name }
    }

    /**
     * 单个模组文件的读取任务
     */
    class ReadTask(private val file: File) {
        suspend fun execute(): LocalMod {
            try {
                currentCoroutineContext().ensureActive()

                // 获取真实扩展名（处理 .disabled 后缀）
                val extension = if (file.isDisabled()) {
                    File(file.nameWithoutExtension).extension
                } else {
                    file.extension
                }

                // 尝试所有匹配的读取器
                return MOD_READERS[extension]?.firstNotNullOfOrNull { reader ->
                    runCatching {
                        reader.fromLocal(file)
                    }.getOrNull()
                    // 返回 null 继续尝试下一个读取器
                } ?: throw IllegalArgumentException("No matching reader for extension: $extension")
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> throw e
                    else -> {
                        lWarning("Failed to read mod: $file"

, e)
                        return createNotMod(file)
                    }
                }
            }
        }
    }
}
