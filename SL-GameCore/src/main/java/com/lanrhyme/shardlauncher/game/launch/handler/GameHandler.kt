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

package com.lanrhyme.shardlauncher.game.launch.handler

import android.app.Activity
import android.view.KeyEvent
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.lanrhyme.shardlauncher.bridge.ZLBridge
import com.lanrhyme.shardlauncher.game.input.LWJGLCharSender
import com.lanrhyme.shardlauncher.game.keycodes.LwjglGlfwKeycode
import com.lanrhyme.shardlauncher.game.launch.GameLauncher
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.viewmodel.ErrorViewModel
import com.lanrhyme.shardlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lwjgl.glfw.CallbackBridge

class GameHandler(
    val activity: Activity,
    private val version: Version,
    private val eventViewModel: EventViewModel,
    getWindowSize: () -> IntSize,
    private val gameLauncher: GameLauncher,
    onExit: (code: Int) -> Unit
) : AbstractHandler(
    type = HandlerType.GAME,
    getWindowSize = getWindowSize,
    sender = LWJGLCharSender,
    launcher = gameLauncher,
    onExit = onExit
) {
    private val _inputArea = MutableStateFlow<IntRect?>(null)
    override val inputArea = _inputArea.asStateFlow()

    private var isGameRendering by mutableStateOf(false)

    override suspend fun execute(surface: Surface?, scope: CoroutineScope) {
        ZLBridge.setupBridgeWindow(surface)
        super.execute(surface, scope)
    }

    override fun onPause() {
    }

    override fun onResume() {
        eventViewModel.sendEvent(EventViewModel.Event.Game.OnResume)
    }

    override fun onDestroy() {
    }

    override fun onGraphicOutput() {
        if (!isGameRendering) {
            isGameRendering = true
        }
    }

    override fun shouldIgnoreKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && (event.flags and KeyEvent.FLAG_CANCELED) != 0) return false

        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            eventViewModel.sendEvent(EventViewModel.Event.Game.OnBack)
            return false
        }

        if ((event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                LWJGLCharSender.sendEnter()
                return false
            }
        }

        return when (event.keyCode) {
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.ACTION_MULTIPLE,
            KeyEvent.ACTION_UP -> false

            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP -> true

            else -> (event.flags and KeyEvent.FLAG_FALLBACK) != KeyEvent.FLAG_FALLBACK
        }
    }

    override fun sendMouseRight(isPressed: Boolean) {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), isPressed)
    }

    @Composable
    override fun ComposableLayout(
        textInputMode: TextInputMode
    ) {
        // TODO: Implement GameScreen
        // For now, just show a placeholder
        // GameScreen will be implemented in next phase
    }
}
