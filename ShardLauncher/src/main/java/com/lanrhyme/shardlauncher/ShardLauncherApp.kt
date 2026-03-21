package com.lanrhyme.shardlauncher

import android.app.Application
import android.content.Intent
import com.lanrhyme.shardlauncher.path.PathManager
import com.lanrhyme.shardlauncher.ui.crash.CrashActivity
import com.lanrhyme.shardlauncher.utils.logging.LogCollector
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class ShardLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化路径管理
        PathManager.refreshPaths(this)
        
        // 初始化日志收集器，启动实时日志文件写入
        LogCollector.init(PathManager.DIR_LAUNCHER_LOGS)
        
        // 设置崩溃处理器
        setCrashHandler()
    }

    private fun setCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val errorLog = stringWriter.toString()

            // 保存崩溃日志到文件
            LogCollector.saveCrashLog(errorLog)

            val intent = Intent(this, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashActivity.EXTRA_LOG, errorLog)
            }
            startActivity(intent)

            // Exit the app
            defaultHandler?.uncaughtException(thread, throwable)
            exitProcess(1)
        }
    }
}