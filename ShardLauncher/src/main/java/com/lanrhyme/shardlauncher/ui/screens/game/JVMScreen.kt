/*
 * Shard Launcher
 * JVM Screen - 显示 JVM 运行时界面
 */

package com.lanrhyme.shardlauncher.ui.screens.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lanrhyme.shardlauncher.bridge.ZLBridge
import com.lanrhyme.shardlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun JVMScreen(
    logState: LogState,
    onLogStateChange: (LogState) -> Unit,
    eventViewModel: EventViewModel
) {
    var forceCloseState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        eventViewModel.events
            .filterIsInstance<EventViewModel.Event.Game.OnBack>()
            .collect {
                forceCloseState = true
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 简单的控制按钮布局
        Button(
            onClick = { onLogStateChange(logState.next()) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("日志")
        }

        Button(
            onClick = { forceCloseState = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("关闭")
        }

        // 显示提示信息
        Text(
            text = "JVM 正在运行...",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}