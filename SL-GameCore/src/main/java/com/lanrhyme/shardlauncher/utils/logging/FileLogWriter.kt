/*
 * Shard Launcher
 * Asynchronous file log writer with buffer and rotation
 */

package com.lanrhyme.shardlauncher.utils.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 异步日志文件写入器
 * 特性：
 * - 内存缓冲队列，减少IO操作，保护硬盘
 * - 定时批量写入（默认每秒或缓冲区达到阈值）
 * - 文件轮转，限制单个日志文件大小
 * - 自动清理旧日志文件
 */
object FileLogWriter {
    private const val TAG = "FileLogWriter"
    
    // 日志缓冲队列
    private val logQueue = ConcurrentLinkedQueue<String>()
    
    // 写入线程池
    private var scheduler: ScheduledExecutorService? = null
    
    // 日志文件
    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    
    // 当前日志文件大小
    private var currentFileSize: Long = 0
    
    // 配置参数
    private const val MAX_FILE_SIZE = 2 * 1024 * 1024L // 单个日志文件最大2MB
    private const val MAX_LOG_FILES = 5 // 最多保留5个日志文件
    private const val FLUSH_INTERVAL_MS = 1000L // 定时刷新间隔
    private const val BUFFER_THRESHOLD = 10 // 缓冲区达到10条立即写入
    
    // 状态标志
    private val isInitialized = AtomicBoolean(false)
    private val isWriting = AtomicBoolean(false)
    
    // 日期格式化
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    // 日志目录
    private var logDir: File? = null
    
    /**
     * 初始化日志写入器
     * @param directory 日志目录
     */
    @Synchronized
    fun init(directory: File) {
        if (isInitialized.get()) {
            return
        }
        
        logDir = directory
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        // 创建当前日志文件
        createNewLogFile()
        
        // 启动定时刷新任务
        scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "FileLogWriter-Thread").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }
        }
        
        scheduler?.scheduleWithFixedDelay(
            { flush() },
            FLUSH_INTERVAL_MS,
            FLUSH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
        
        isInitialized.set(true)
        Logger.i(TAG, "FileLogWriter initialized, logDir: ${directory.absolutePath}")
    }
    
    /**
     * 写入日志
     * @param entry 日志条目
     */
    fun write(entry: LogCollector.LogEntry) {
        if (!isInitialized.get()) return
        
        val timeStr = dateFormat.format(Date(entry.timestamp))
        val levelStr = when (entry.level) {
            LogCollector.LogLevel.DEBUG -> "D"
            LogCollector.LogLevel.INFO -> "I"
            LogCollector.LogLevel.WARNING -> "W"
            LogCollector.LogLevel.ERROR -> "E"
        }
        
        val logLine = buildString {
            append("[$timeStr] ")
            append("$levelStr/${entry.tag}: ")
            append(entry.message)
            entry.throwable?.let { throwable ->
                append("\n")
                append(throwable.stackTraceToString())
            }
        }
        
        logQueue.offer(logLine)
        
        // 缓冲区达到阈值时立即写入
        if (logQueue.size >= BUFFER_THRESHOLD) {
            scheduler?.execute { flush() }
        }
    }
    
    /**
     * 写入崩溃日志
     * @param crashLog 崩溃日志内容
     */
    fun writeCrashLog(crashLog: String) {
        if (!isInitialized.get()) return
        
        val timeStr = dateFormat.format(Date())
        val logLine = buildString {
            append("\n")
            append("=" .repeat(60))
            append("\n")
            append("[$timeStr] CRASH REPORT:\n")
            append(crashLog)
            append("\n")
            append("=" .repeat(60))
            append("\n")
        }
        
        logQueue.offer(logLine)
        flush() // 崩溃日志立即刷新
    }
    
    /**
     * 刷新缓冲区到文件
     */
    @Synchronized
    fun flush() {
        if (!isInitialized.get() || isWriting.get()) return
        if (logQueue.isEmpty()) return
        
        isWriting.set(true)
        try {
            var writer = fileWriter ?: return
            
            // 批量写入
            var count = 0
            while (logQueue.isNotEmpty()) {
                val logLine = logQueue.poll() ?: break
                writer.write(logLine)
                writer.write("\n")
                currentFileSize += logLine.length + 1
                count++
                
                // 检查文件大小，需要轮转
                if (currentFileSize >= MAX_FILE_SIZE) {
                    writer.flush()
                    writer.close()
                    rotateLogFile()
                    writer = fileWriter ?: break
                }
            }
            
            writer.flush()
            
            // 不输出调试日志，避免循环写入
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to flush logs: ${e.message}", e)
        } finally {
            isWriting.set(false)
        }
    }
    
    /**
     * 创建新的日志文件
     */
    private fun createNewLogFile() {
        try {
            val dir = logDir ?: return
            val timestamp = fileDateFormat.format(Date())
            val fileName = "launcher_$timestamp.log"
            logFile = File(dir, fileName)
            
            fileWriter = FileWriter(logFile, true)
            currentFileSize = if (logFile!!.exists()) logFile!!.length() else 0
            
            // 写入文件头
            fileWriter?.apply {
                write("ShardLauncher Log - Started at ${dateFormat.format(Date())}\n")
                write("=" .repeat(60))
                write("\n")
                flush()
            }
            
            // 清理旧日志
            cleanupOldLogs()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create log file: ${e.message}", e)
        }
    }
    
    /**
     * 轮转日志文件
     */
    private fun rotateLogFile() {
        try {
            fileWriter?.close()
            fileWriter = null
            
            createNewLogFile()
            Logger.i(TAG, "Log file rotated")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to rotate log file: ${e.message}", e)
        }
    }
    
    /**
     * 清理旧日志文件
     */
    private fun cleanupOldLogs() {
        val dir = logDir ?: return
        try {
            val logFiles = dir.listFiles { file ->
                file.name.startsWith("launcher_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }
            
            if (logFiles != null && logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { oldFile ->
                    oldFile.delete()
                    Logger.d(TAG, "Deleted old log file: ${oldFile.name}")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to cleanup old logs: ${e.message}", e)
        }
    }
    
    /**
     * 关闭日志写入器
     */
    @Synchronized
    fun close() {
        if (!isInitialized.get()) return
        
        try {
            // 刷新剩余日志
            flush()
            
            // 关闭调度器
            scheduler?.shutdown()
            scheduler?.awaitTermination(5, TimeUnit.SECONDS)
            scheduler = null
            
            // 关闭文件写入器
            fileWriter?.close()
            fileWriter = null
            
            isInitialized.set(false)
            Logger.i(TAG, "FileLogWriter closed")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to close FileLogWriter: ${e.message}", e)
        }
    }
    
    /**
     * 获取当前日志文件路径
     */
    fun getCurrentLogFile(): File? = logFile
    
    /**
     * 获取日志目录
     */
    fun getLogDir(): File? = logDir
}
