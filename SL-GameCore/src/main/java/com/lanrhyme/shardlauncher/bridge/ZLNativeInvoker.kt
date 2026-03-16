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

package com.lanrhyme.shardlauncher.bridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import androidx.annotation.Keep
import com.lanrhyme.shardlauncher.utils.logging.Logger.lInfo
import java.io.File

@Keep
object ZLNativeInvoker {
    @JvmStatic
    var staticLauncher: Any? = null

    private var globalContext: Activity? = null

    fun setGlobalContext(context: Activity) {
        globalContext = context
    }

    @Keep
    @JvmStatic
    fun openLink(link: String) {
        globalContext?.let { activity ->
            activity.runOnUiThread {
                var prefix = "file:"
                if (link.startsWith(prefix)) {
                    if (link.startsWith("file://")) prefix += "//"
                    val newLink = link.removePrefix(prefix)
                    lInfo("open link: $newLink")

                    val file = File(newLink)
                    // Share file via intent
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        val uri = if (android.os.Build.VERSION.SDK_INT >= 24) {
                            androidx.core.content.FileProvider.getUriForFile(
                                activity,
                                "${activity.packageName}.fileprovider",
                                file
                            )
                        } else {
                            android.net.Uri.fromFile(file)
                        }
                        intent.setDataAndType(uri, "*/*")
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        activity.startActivity(android.content.Intent.createChooser(intent, "Open file"))
                        lInfo("In-game Share File/Folder: ${file.absolutePath}")
                    } catch (e: Exception) {
                        lInfo("Failed to share file: ${e.message}")
                    }
                } else {
                    // Open URL in browser
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        lInfo("Failed to open link: ${e.message}")
                    }
                }
            }
        }
    }

    @Keep
    @JvmStatic
    fun querySystemClipboard() {
        globalContext?.let { activity ->
            activity.runOnUiThread {
                val clipData = (activity.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip ?: run {
                    ZLBridge.clipboardReceived(null, null)
                    return@runOnUiThread
                }
                val clipItemText = clipData.getItemAt(0).text ?: run {
                    ZLBridge.clipboardReceived(null, null)
                    return@runOnUiThread
                }
                ZLBridge.clipboardReceived(clipItemText.toString(), "plain")
            }
        }
    }

    @Keep
    @JvmStatic
    fun putClipboardData(data: String, mimeType: String) {
        globalContext?.let { activity ->
            activity.runOnUiThread {
                val clipData = when (mimeType) {
                    "text/plain" -> ClipData.newPlainText("ShardLauncher", data)
                    "text/html" -> ClipData.newHtmlText("ShardLauncher", data, data)
                    else -> null
                }
                clipData?.let {
                    (activity.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(it)
                }
            }
        }
    }

    @Keep
    @JvmStatic
    fun putFpsValue(fps: Int) {
        ZLBridgeStates.currentFPS = fps
    }

    @Keep
    @JvmStatic
    fun jvmExit(exitCode: Int, isSignal: Boolean) {
        staticLauncher?.let { launcher ->
            // Use reflection to call exit() and onExit
            runCatching {
                launcher.javaClass.getMethod("exit").invoke(launcher)
                launcher.javaClass.getDeclaredField("onExit").apply { isAccessible = true }
                    .get(launcher)?.let { callback ->
                        (callback as? (Int, Boolean) -> Unit)?.invoke(exitCode, isSignal)
                    }
            }.onFailure {
                lInfo("Failed to call launcher exit: ${it.message}")
            }
        }
        staticLauncher = null
        // Kill the process
        try {
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            lInfo("Failed to kill process: ${e.message}")
        }
    }
}