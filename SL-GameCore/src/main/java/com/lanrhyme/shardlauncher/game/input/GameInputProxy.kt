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

package com.lanrhyme.shardlauncher.game.input

import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import kotlin.math.min

/**
 * 这是一个代理方案，和其他启动器一样，通过一个输入框UI输入文本，对输入的字符串进行处理，最后发送字符给游戏，
 * 或者发送指定事件给游戏，以尽可能达到预期的输入效果
 */
class GameInputProxy(
    var sender: CharacterSenderStrategy
) {
    /**
     * 处理文本变化并发送到游戏
     */
    fun handleTextChange(
        oldText: String,
        newText: String,
        oldSelection: TextRange,
        newSelection: TextRange
    ) {
        when (
            val diff = calculateTextDifference(oldText, newText, oldSelection, newSelection)
        ) {
            is TextDifference.Insert -> handleInsert(diff)
            is TextDifference.Delete -> handleDelete(diff)
            is TextDifference.Replace -> handleReplace(diff)
            is TextDifference.MoveCursor -> handleCursorMove(diff)
        }
    }

    private fun handleInsert(diff: TextDifference.Insert) {
        diff.insertedText.forEach { char ->
            sender.sendChar(char)
        }
    }

    private fun handleDelete(diff: TextDifference.Delete) {
        repeat(diff.deletedText.length) {
            sender.sendBackspace()
        }
    }

    private fun handleReplace(diff: TextDifference.Replace) {
        repeat(diff.deletedText.length) {
            sender.sendBackspace()
        }
        diff.insertedText.forEach { char ->
            sender.sendChar(char)
        }
    }

    private fun handleCursorMove(diff: TextDifference.MoveCursor) {
        val cursorDiff = diff.newPosition - diff.oldPosition
        if (cursorDiff > 0) {
            repeat(cursorDiff) {
                sender.sendRight()
            }
        } else if (cursorDiff < 0) {
            repeat(-cursorDiff) {
                sender.sendLeft()
            }
        }
    }

    fun keyCanHandle(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.keyCode
        val isShift = keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
        val isCtrl = keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
        return !isShift && !isCtrl
    }

    fun handleSpecialKey(
        keyEvent: KeyEvent,
        onClearState: () -> Unit
    ) {
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                return
            }
            KeyEvent.KEYCODE_ENTER -> sender.sendEnter()
            KeyEvent.KEYCODE_TAB -> sender.sendTab()
            KeyEvent.KEYCODE_DPAD_UP -> sender.sendUp()
            KeyEvent.KEYCODE_DPAD_DOWN -> sender.sendDown()
            else -> sender.sendOther(keyEvent)
        }
        onClearState()
    }

    fun handleSpecialKey(keyEvent: KeyEvent, text: CharSequence): Boolean {
        if (text.isEmpty()) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_DEL -> sender.sendBackspace()
                KeyEvent.KEYCODE_DPAD_LEFT -> sender.sendLeft()
                KeyEvent.KEYCODE_DPAD_RIGHT -> sender.sendRight()
                else -> return false
            }
            return true
        }
        return false
    }
}

private sealed class TextDifference {
    data class Insert(val insertedText: String) : TextDifference()
    data class Delete(val deletedText: String) : TextDifference()
    data class MoveCursor(val oldPosition: Int, val newPosition: Int) : TextDifference()
    data class Replace(val deletedText: String, val insertedText: String) : TextDifference()
}

private fun calculateTextDifference(
    oldText: String,
    newText: String,
    oldSelection: TextRange,
    newSelection: TextRange
): TextDifference {
    val oldPosition = oldSelection.max
    val newPosition = newSelection.max
    val hasSelection = !oldSelection.collapsed

    if (oldText == newText) {
        return TextDifference.MoveCursor(oldPosition, newPosition)
    }

    val oldLength = oldText.length
    val newLength = newText.length

    if (hasSelection) {
        val selectionStart = oldSelection.min
        val selectionEnd = oldSelection.max
        val selectionLength = selectionEnd - selectionStart
        val selectedText = oldText.substring(selectionStart, selectionEnd)

        if (newLength == oldLength - selectionLength) {
            val expectedText = oldText.take(selectionStart) + oldText.substring(selectionEnd)
            if (newText == expectedText) {
                return TextDifference.Delete(selectedText)
            }
        }

        if (newText.startsWith(oldText.take(selectionStart)) &&
            newText.endsWith(oldText.substring(selectionEnd))) {
            val insertedText = newText.substring(
                selectionStart,
                newLength - (oldLength - selectionEnd)
            )
            return TextDifference.Replace(selectedText, insertedText)
        }
    }

    if (newLength > oldLength && !hasSelection) {
        val beforeCursor = oldText.take(oldPosition)
        val afterCursor = oldText.substring(oldPosition)
        if (newText.startsWith(beforeCursor) && newText.endsWith(afterCursor)) {
            val insertedText = newText.substring(
                beforeCursor.length,
                newLength - afterCursor.length
            )
            return TextDifference.Insert(insertedText)
        }
    }

    if (newLength < oldLength && !hasSelection) {
        if (newText == oldText.take(newLength)) {
            return TextDifference.Delete(oldText.substring(newLength))
        }

        if (oldPosition in 1..oldLength) {
            val potentialDeleted = oldText.take(oldPosition - 1) + oldText.substring(oldPosition)
            if (potentialDeleted == newText) {
                return TextDifference.Delete(oldText.substring(oldPosition - 1, oldPosition))
            }
        }
    }

    if (oldPosition == newPosition && !hasSelection) {
        val minLength = min(oldLength, newLength)
        var firstDiffIndex = -1
        for (i in 0 until minLength) {
            if (oldText[i] != newText[i]) {
                firstDiffIndex = i
                break
            }
        }
        if (firstDiffIndex != -1) {
            return TextDifference.Delete(oldText.substring(firstDiffIndex))
        }
    }

    return TextDifference.Insert(newText)
}