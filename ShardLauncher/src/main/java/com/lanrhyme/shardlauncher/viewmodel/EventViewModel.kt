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

package com.lanrhyme.shardlauncher.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanrhyme.shardlauncher.game.launch.handler.TextInputMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EventViewModel : ViewModel() {
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun sendEvent(event: Event) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    sealed interface Event {
        sealed interface Key : Event {
            data object StartKeyCapture : Key
            data object StopKeyCapture : Key
            data class OnKeyDown(val key: KeyEvent) : Key
        }
        sealed interface Game : Event {
            data class KeyHandle(val handle: Boolean): Game
            data class SwitchIme(val mode: TextInputMode?) : Game
            data object RefreshSize : Game
            data object OnBack : Game
            data object OnResume: Game
        }
        data object CheckUpdate : Event
        data class OpenLink(val url: String) : Event
        data object RefreshFullScreen : Event
    }
}
