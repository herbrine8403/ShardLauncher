/*
 * Shard Launcher
 * Game Input View
 * Surface view for game input handling
 */

package com.lanrhyme.shardlauncher.game.input

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.lanrhyme.shardlauncher.utils.logging.Logger

@Composable
fun GameInputSurface(
    inputManager: InputManager,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onSurfaceCreated: (SurfaceHolder) -> Unit = {},
    onSurfaceChanged: (SurfaceHolder, Int, Int) -> Unit = { _, _, _ -> },
    onSurfaceDestroyed: (SurfaceHolder) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            GameInputSurfaceView(context, inputManager).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        Logger.lInfo("GameInputSurface - Surface created")
                        onSurfaceCreated(holder)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        Logger.lInfo("GameInputSurface - Surface changed: ${width}x${height}")
                        onSurfaceChanged(holder, width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Logger.lInfo("GameInputSurface - Surface destroyed")
                        onSurfaceDestroyed(holder)
                    }
                })
            }
        }
    )
}

class GameInputSurfaceView(
    context: Context,
    private val inputManager: InputManager
) : SurfaceView(context) {
    
    private val TAG = "GameInputSurfaceView"
    
    init {
        // Configure the surface
        isFocusable = true
        isFocusableInTouchMode = true
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Logger.lInfo("$TAG - Attached to window")
        requestFocus()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Logger.lInfo("$TAG - Detached from window")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Logger.lDebug("$TAG - Key down: $keyCode")
        val handled = inputManager.handleKeyEvent(event)
        if (handled) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Logger.lDebug("$TAG - Key up: $keyCode")
        val handled = inputManager.handleKeyEvent(event)
        if (handled) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent): Boolean {
        Logger.lDebug("$TAG - Key multiple: $keyCode (count: $count)")
        val handled = inputManager.handleKeyEvent(event)
        if (handled) {
            return true
        }
        return super.onKeyMultiple(keyCode, count, event)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = inputManager.handleMotionEvent(event)
        if (handled) {
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = inputManager.handleMotionEvent(event)
        if (handled) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }
    
    override fun onTrackballEvent(event: MotionEvent): Boolean {
        val handled = inputManager.handleMotionEvent(event)
        if (handled) {
            return true
        }
        return super.onTrackballEvent(event)
    }
}