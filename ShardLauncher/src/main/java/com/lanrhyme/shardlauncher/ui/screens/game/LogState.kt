/*
 * Shard Launcher
 * Log state for JVM screen
 */

package com.lanrhyme.shardlauncher.ui.screens.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 日志展示状态
 */
enum class LogState {
    CLOSE,      // 日志关闭
    SHOW,       // 显示日志
    TRANSPARENT // 半透明显示
}

/**
 * 获取下一个日志状态
 */
fun LogState.next(): LogState {
    return when (this) {
        LogState.CLOSE -> LogState.SHOW
        LogState.SHOW -> LogState.TRANSPARENT
        LogState.TRANSPARENT -> LogState.CLOSE
    }
}
