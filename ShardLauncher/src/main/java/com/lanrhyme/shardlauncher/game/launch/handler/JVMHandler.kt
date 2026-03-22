/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 * JVM Handler - 用于执行 JAR 文件
 */

package com.lanrhyme.shardlauncher.game.launch.handler

import android.view.KeyEvent
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.lanrhyme.shardlauncher.bridge.ZLBridge
import com.lanrhyme.shardlauncher.game.input.CharacterSenderStrategy
import com.lanrhyme.shardlauncher.game.input.LWJGLCharSender
import com.lanrhyme.shardlauncher.game.launch.JvmLauncher
import com.lanrhyme.shardlauncher.ui.control.input.TextInputMode
import com.lanrhyme.shardlauncher.ui.screens.game.JVMScreen
import com.lanrhyme.shardlauncher.ui.screens.game.LogState
import com.lanrhyme.shardlauncher.viewmodel.EventViewModel

class JVMHandler(
    jvmLauncher: JvmLauncher,
    private val eventViewModel: EventViewModel,
    onExit: (code: Int) -> Unit
) : AbstractHandler(
    type = HandlerType.JVM,
    getWindowSize = { IntSize(1280, 720) },
    sender = LWJGLCharSender,
    launcher = jvmLauncher,
    onExit = onExit
) {
    /**
     * 日志展示状态
     */
    private var logState by mutableStateOf(LogState.CLOSE)

    override fun onPause() {
    }

    override fun onResume() {
    }

    override fun onDestroy() {
    }

    override fun onGraphicOutput() {
    }

    override fun shouldIgnoreKeyEvent(event: KeyEvent): Boolean {
        return true
    }

    override fun sendMouseRight(isPressed: Boolean) {
        ZLBridge.sendMousePress(0x4, isPressed) // Right mouse button
    }

    @Composable
    override fun ComposableLayout(
        textInputMode: TextInputMode
    ) {
        JVMScreen(
            logState = logState,
            onLogStateChange = { logState = it },
            eventViewModel = eventViewModel
        )
    }
}
