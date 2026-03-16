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

package com.lanrhyme.shardlauncher.utils.file

import java.io.File

/**
 * 获取子文件
 */
fun File.child(vararg names: String): File {
    var result = this
    for (name in names) {
        result = File(result, name)
    }
    return result
}

/**
 * 确保目录存在
 */
fun File.ensureDirectory(): File {
    if (!exists()) {
        mkdirs()
    }
    return this
}

/**
 * 确保目录存在（静默模式）
 */
fun File.ensureDirectorySilently(): Boolean {
    return try {
        if (!exists()) {
            mkdirs()
        }
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * 确保父目录存在
 */
fun File.ensureParentDirectory(): File {
    parentFile?.let {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
    return this
}
