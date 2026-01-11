/*
 * Shard Launcher
 * Input Bridge System
 * Handles keyboard and mouse input for game control
 */

package com.lanrhyme.shardlauncher.game.input

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.Keep
import com.lanrhyme.shardlauncher.bridge.ZLBridge
import com.lanrhyme.shardlauncher.utils.logging.Logger

@Keep
object InputBridge {
    
    private const val TAG = "InputBridge"
    
    // Input state
    private var isInputReady = false
    private var isGrabbing = false
    private var cursorX = 0f
    private var cursorY = 0f
    
    // Modifier keys state
    private var shiftPressed = false
    private var ctrlPressed = false
    private var altPressed = false
    private var metaPressed = false
    
    // Key code mapping (Android -> GLFW)
    private val keyCodeMap = mapOf(
        KeyEvent.KEYCODE_A to 65,    // GLFW_KEY_A
        KeyEvent.KEYCODE_B to 66,    // GLFW_KEY_B
        KeyEvent.KEYCODE_C to 67,    // GLFW_KEY_C
        KeyEvent.KEYCODE_D to 68,    // GLFW_KEY_D
        KeyEvent.KEYCODE_E to 69,    // GLFW_KEY_E
        KeyEvent.KEYCODE_F to 70,    // GLFW_KEY_F
        KeyEvent.KEYCODE_G to 71,    // GLFW_KEY_G
        KeyEvent.KEYCODE_H to 72,    // GLFW_KEY_H
        KeyEvent.KEYCODE_I to 73,    // GLFW_KEY_I
        KeyEvent.KEYCODE_J to 74,    // GLFW_KEY_J
        KeyEvent.KEYCODE_K to 75,    // GLFW_KEY_K
        KeyEvent.KEYCODE_L to 76,    // GLFW_KEY_L
        KeyEvent.KEYCODE_M to 77,    // GLFW_KEY_M
        KeyEvent.KEYCODE_N to 78,    // GLFW_KEY_N
        KeyEvent.KEYCODE_O to 79,    // GLFW_KEY_O
        KeyEvent.KEYCODE_P to 80,    // GLFW_KEY_P
        KeyEvent.KEYCODE_Q to 81,    // GLFW_KEY_Q
        KeyEvent.KEYCODE_R to 82,    // GLFW_KEY_R
        KeyEvent.KEYCODE_S to 83,    // GLFW_KEY_S
        KeyEvent.KEYCODE_T to 84,    // GLFW_KEY_T
        KeyEvent.KEYCODE_U to 85,    // GLFW_KEY_U
        KeyEvent.KEYCODE_V to 86,    // GLFW_KEY_V
        KeyEvent.KEYCODE_W to 87,    // GLFW_KEY_W
        KeyEvent.KEYCODE_X to 88,    // GLFW_KEY_X
        KeyEvent.KEYCODE_Y to 89,    // GLFW_KEY_Y
        KeyEvent.KEYCODE_Z to 90,    // GLFW_KEY_Z
        KeyEvent.KEYCODE_0 to 48,    // GLFW_KEY_0
        KeyEvent.KEYCODE_1 to 49,    // GLFW_KEY_1
        KeyEvent.KEYCODE_2 to 50,    // GLFW_KEY_2
        KeyEvent.KEYCODE_3 to 51,    // GLFW_KEY_3
        KeyEvent.KEYCODE_4 to 52,    // GLFW_KEY_4
        KeyEvent.KEYCODE_5 to 53,    // GLFW_KEY_5
        KeyEvent.KEYCODE_6 to 54,    // GLFW_KEY_6
        KeyEvent.KEYCODE_7 to 55,    // GLFW_KEY_7
        KeyEvent.KEYCODE_8 to 56,    // GLFW_KEY_8
        KeyEvent.KEYCODE_9 to 57,    // GLFW_KEY_9
        KeyEvent.KEYCODE_SPACE to 32,  // GLFW_KEY_SPACE
        KeyEvent.KEYCODE_ENTER to 257, // GLFW_KEY_ENTER
        KeyEvent.KEYCODE_DEL to 259,   // GLFW_KEY_BACKSPACE
        KeyEvent.KEYCODE_TAB to 258,   // GLFW_KEY_TAB
        KeyEvent.KEYCODE_ESCAPE to 256, // GLFW_KEY_ESCAPE
        KeyEvent.KEYCODE_SHIFT_LEFT to 340,  // GLFW_KEY_LEFT_SHIFT
        KeyEvent.KEYCODE_SHIFT_RIGHT to 344, // GLFW_KEY_RIGHT_SHIFT
        KeyEvent.KEYCODE_CTRL_LEFT to 341,   // GLFW_KEY_LEFT_CONTROL
        KeyEvent.KEYCODE_CTRL_RIGHT to 345,  // GLFW_KEY_RIGHT_CONTROL
        KeyEvent.KEYCODE_ALT_LEFT to 342,    // GLFW_KEY_LEFT_ALT
        KeyEvent.KEYCODE_ALT_RIGHT to 346,   // GLFW_KEY_RIGHT_ALT
        KeyEvent.KEYCODE_META_LEFT to 343,   // GLFW_KEY_LEFT_SUPER
        KeyEvent.KEYCODE_META_RIGHT to 347,  // GLFW_KEY_RIGHT_SUPER
        KeyEvent.KEYCODE_F1 to 290,   // GLFW_KEY_F1
        KeyEvent.KEYCODE_F2 to 291,   // GLFW_KEY_F2
        KeyEvent.KEYCODE_F3 to 292,   // GLFW_KEY_F3
        KeyEvent.KEYCODE_F4 to 293,   // GLFW_KEY_F4
        KeyEvent.KEYCODE_F5 to 294,   // GLFW_KEY_F5
        KeyEvent.KEYCODE_F6 to 295,   // GLFW_KEY_F6
        KeyEvent.KEYCODE_F7 to 296,   // GLFW_KEY_F7
        KeyEvent.KEYCODE_F8 to 297,   // GLFW_KEY_F8
        KeyEvent.KEYCODE_F9 to 298,   // GLFW_KEY_F9
        KeyEvent.KEYCODE_F10 to 299,  // GLFW_KEY_F10
        KeyEvent.KEYCODE_F11 to 300,  // GLFW_KEY_F11
        KeyEvent.KEYCODE_F12 to 301,  // GLFW_KEY_F12
        KeyEvent.KEYCODE_MINUS to 45,  // GLFW_KEY_MINUS
        KeyEvent.KEYCODE_EQUALS to 61, // GLFW_KEY_EQUAL
        KeyEvent.KEYCODE_LEFT_BRACKET to 91,  // GLFW_KEY_LEFT_BRACKET
        KeyEvent.KEYCODE_RIGHT_BRACKET to 93, // GLFW_KEY_RIGHT_BRACKET
        KeyEvent.KEYCODE_BACKSLASH to 92,     // GLFW_KEY_BACKSLASH
        KeyEvent.KEYCODE_SEMICOLON to 59,     // GLFW_KEY_SEMICOLON
        KeyEvent.KEYCODE_APOSTROPHE to 39,    // GLFW_KEY_APOSTROPHE
        KeyEvent.KEYCODE_GRAVE to 96,         // GLFW_KEY_GRAVE_ACCENT
        KeyEvent.KEYCODE_COMMA to 44,         // GLFW_KEY_COMMA
        KeyEvent.KEYCODE_PERIOD to 46,        // GLFW_KEY_PERIOD
        KeyEvent.KEYCODE_SLASH to 47,         // GLFW_KEY_SLASH
        KeyEvent.KEYCODE_CAPS_LOCK to 280,    // GLFW_KEY_CAPS_LOCK
        KeyEvent.KEYCODE_SCROLL_LOCK to 281,  // GLFW_KEY_SCROLL_LOCK
        KeyEvent.KEYCODE_NUM_LOCK to 282,     // GLFW_KEY_NUM_LOCK
        KeyEvent.KEYCODE_PAGE_UP to 266,      // GLFW_KEY_PAGE_UP
        KeyEvent.KEYCODE_PAGE_DOWN to 267,    // GLFW_KEY_PAGE_DOWN
        KeyEvent.KEYCODE_MOVE_HOME to 268,    // GLFW_KEY_HOME
        KeyEvent.KEYCODE_MOVE_END to 269,     // GLFW_KEY_END
        KeyEvent.KEYCODE_INSERT to 260,       // GLFW_KEY_INSERT
        KeyEvent.KEYCODE_FORWARD_DEL to 261,  // GLFW_KEY_DELETE
        KeyEvent.KEYCODE_DPAD_UP to 265,      // GLFW_KEY_UP
        KeyEvent.KEYCODE_DPAD_DOWN to 264,    // GLFW_KEY_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT to 263,    // GLFW_KEY_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT to 262,   // GLFW_KEY_RIGHT
    )
    
    // Mouse button mapping
    private const val MOUSE_BUTTON_LEFT = 0
    private const val MOUSE_BUTTON_RIGHT = 1
    private const val MOUSE_BUTTON_MIDDLE = 2
    private const val MOUSE_BUTTON_4 = 3
    private const val MOUSE_BUTTON_5 = 4
    
    /**
     * Initialize the input bridge
     */
    fun initialize() {
        Logger.lInfo(TAG, "Input bridge initialized")
        isInputReady = true
    }
    
    /**
     * Set input ready state
     */
    fun setInputReady(ready: Boolean) {
        isInputReady = ready
        Logger.lDebug(TAG, "Input ready: $ready")
    }
    
    /**
     * Set grabbing state (mouse capture)
     */
    fun setGrabbing(grabbing: Boolean) {
        isGrabbing = grabbing
        Logger.lDebug(TAG, "Grabbing: $grabbing")
    }
    
    /**
     * Handle key event
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!isInputReady) return false
        
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> 1
            KeyEvent.ACTION_UP -> 0
            else -> return false
        }
        
        val keyCode = event.keyCode
        val glfwKeyCode = keyCodeMap[keyCode] ?: return false
        
        // Update modifier key state
        updateModifierKeyState(keyCode, action == 1)
        
        // Calculate modifiers
        val modifiers = getModifiers()
        
        // Send key event
        ZLBridge.sendInputData(
            ZLBridge.EVENT_TYPE_KEY,
            glfwKeyCode,
            event.scanCode,
            action,
            modifiers
        )
        
        // Send character event for key press
        if (action == 1) {
            val unicodeChar = event.unicodeChar
            if (unicodeChar != 0) {
                ZLBridge.sendInputData(
                    ZLBridge.EVENT_TYPE_CHAR,
                    unicodeChar,
                    modifiers,
                    0,
                    0
                )
            }
        }
        
        return true
    }
    
    /**
     * Handle mouse motion event
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        if (!isInputReady) return false
        
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                
                if (isGrabbing) {
                    // In grab mode, send relative motion
                    val dx = x - cursorX
                    val dy = y - cursorY
                    ZLBridge.sendInputData(
                        ZLBridge.EVENT_TYPE_CURSOR_POS,
                        dx.toInt(),
                        dy.toInt(),
                        0,
                        0
                    )
                } else {
                    // In normal mode, send absolute position
                    ZLBridge.sendInputData(
                        ZLBridge.EVENT_TYPE_CURSOR_POS,
                        x.toInt(),
                        y.toInt(),
                        0,
                        0
                    )
                }
                
                cursorX = x
                cursorY = y
                return true
            }
        }
        
        return false
    }
    
    /**
     * Handle mouse button event
     */
    fun handleMouseButtonEvent(event: MotionEvent, button: Int): Boolean {
        if (!isInputReady) return false
        
        val action = when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> 1
            MotionEvent.ACTION_UP -> 0
            else -> return false
        }
        
        val modifiers = getModifiers()
        
        ZLBridge.sendInputData(
            ZLBridge.EVENT_TYPE_MOUSE_BUTTON,
            button,
            action,
            modifiers,
            0
        )
        
        return true
    }
    
    /**
     * Handle scroll event
     */
    fun handleScrollEvent(event: MotionEvent): Boolean {
        if (!isInputReady) return false
        
        // Get scroll values
        val scrollX = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
        val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        
        if (scrollX != 0f || scrollY != 0f) {
            val modifiers = getModifiers()
            
            ZLBridge.sendInputData(
                ZLBridge.EVENT_TYPE_CHAR, // Reuse for scroll
                scrollX.toInt(),
                scrollY.toInt(),
                modifiers,
                0
            )
            
            return true
        }
        
        return false
    }
    
    /**
     * Update modifier key state
     */
    private fun updateModifierKeyState(keyCode: Int, pressed: Boolean) {
        when (keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                shiftPressed = pressed
            }
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                ctrlPressed = pressed
            }
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                altPressed = pressed
            }
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> {
                metaPressed = pressed
            }
        }
    }
    
    /**
     * Get current modifier flags
     */
    private fun getModifiers(): Int {
        var modifiers = 0
        
        if (shiftPressed) modifiers = modifiers or 0x0001 // GLFW_MOD_SHIFT
        if (ctrlPressed) modifiers = modifiers or 0x0002  // GLFW_MOD_CONTROL
        if (altPressed) modifiers = modifiers or 0x0004   // GLFW_MOD_ALT
        if (metaPressed) modifiers = modifiers or 0x0008  // GLFW_MOD_SUPER
        
        return modifiers
    }
    
    /**
     * Get current cursor position
     */
    fun getCursorPosition(): Pair<Float, Float> {
        return Pair(cursorX, cursorY)
    }
    
    /**
     * Set cursor position
     */
    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        
        if (isInputReady) {
            ZLBridge.sendInputData(
                ZLBridge.EVENT_TYPE_CURSOR_POS,
                x.toInt(),
                y.toInt(),
                0,
                0
            )
        }
    }
    
    /**
     * Check if input is ready
     */
    fun isInputReady(): Boolean = isInputReady
    
    /**
     * Check if mouse is grabbed
     */
    fun isGrabbing(): Boolean = isGrabbing
}