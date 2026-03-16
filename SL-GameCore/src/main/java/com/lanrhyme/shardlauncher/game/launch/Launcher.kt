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

package com.lanrhyme.shardlauncher.game.launch

/**
 * Launcher interface for game launch lifecycle management.
 * This interface is used by ZLNativeInvoker to handle JVM exit callbacks.
 * The actual implementation resides in the main app module.
 */
interface Launcher {
    /**
     * Callback invoked when the JVM exits
     * @param code Exit code
     * @param isSignal Whether the exit was triggered by a signal
     */
    val onExit: (code: Int, isSignal: Boolean) -> Unit

    /**
     * Called to perform cleanup when the JVM is exiting
     */
    fun exit()
}
