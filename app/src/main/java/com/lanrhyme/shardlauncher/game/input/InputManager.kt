/*
 * Shard Launcher
 * Input Manager
 * Manages input devices and event handling
 */

package com.lanrhyme.shardlauncher.game.input

import android.content.Context
import android.hardware.input.InputManager as AndroidInputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.Keep
import com.lanrhyme.shardlauncher.utils.logging.Logger

@Keep
class InputManager(private val context: Context) {
    
    private val TAG = "InputManager"
    
    private val androidInputManager = context.getSystemService(Context.INPUT_SERVICE) as AndroidInputManager
    private val inputDevices = mutableMapOf<Int, InputDeviceInfo>()
    private var activeKeyboard: InputDeviceInfo? = null
    private var activeMouse: InputDeviceInfo? = null
    
    // Input listeners
    private val inputListeners = mutableListOf<InputListener>()
    
    init {
        initialize()
    }
    
    private fun initialize() {
        Logger.lInfo("$TAG - Initializing input manager")

        // Register input device listener
        androidInputManager.registerInputDeviceListener(inputDeviceListener, null)

        // Scan for existing input devices
        scanInputDevices()

        Logger.lInfo("$TAG - Input manager initialized")
    }

    private val inputDeviceListener = object : AndroidInputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            val device = androidInputManager.getInputDevice(deviceId)
            if (device != null) {
                Logger.lInfo("$TAG - Input device added: ${device.name} (ID: $deviceId)")
                addInputDevice(device)
            }
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            Logger.lInfo("$TAG - Input device removed (ID: $deviceId)")
            removeInputDevice(deviceId)
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            val device = androidInputManager.getInputDevice(deviceId)
            if (device != null) {
                Logger.lInfo("$TAG - Input device changed: ${device.name} (ID: $deviceId)")
                updateInputDevice(device)
            }
        }
    }

    private fun scanInputDevices() {
        val deviceIds = androidInputManager.inputDeviceIds

        Logger.lInfo("$TAG - Scanning for input devices... Found ${deviceIds.size} devices")

        deviceIds.forEach { deviceId ->
            val device = androidInputManager.getInputDevice(deviceId)
            if (device != null) {
                addInputDevice(device)
            }
        }
    }
    
    private fun addInputDevice(device: InputDevice) {
        val deviceInfo = InputDeviceInfo(
            id = device.id,
            name = device.name,
            descriptor = device.descriptor,
            sources = device.sources,
            isExternal = device.isExternal
        )
        
        inputDevices[device.id] = deviceInfo
        
        // Check if this is a keyboard
        if (device.supportsSource(InputDevice.SOURCE_KEYBOARD)) {
            if (activeKeyboard == null) {
                activeKeyboard = deviceInfo
                Logger.lInfo("$TAG - Active keyboard set: ${device.name}")
            }
        }

        // Check if this is a mouse
        if (device.supportsSource(InputDevice.SOURCE_MOUSE)) {
            if (activeMouse == null) {
                activeMouse = deviceInfo
                Logger.lInfo("$TAG - Active mouse set: ${device.name}")
            }
        }

        // Check if this is a gamepad
        if (device.supportsSource(InputDevice.SOURCE_GAMEPAD) ||
            device.supportsSource(InputDevice.SOURCE_JOYSTICK)) {
            Logger.lInfo("$TAG - Gamepad detected: ${device.name}")
        }
        
        notifyListeners { onInputDeviceAdded(deviceInfo) }
    }
    
    private fun removeInputDevice(deviceId: Int) {
        val deviceInfo = inputDevices.remove(deviceId)
        
        if (deviceInfo != null) {
            // Update active devices if needed
            if (activeKeyboard?.id == deviceId) {
                activeKeyboard = null
                // Find another keyboard
                inputDevices.values.firstOrNull { 
                    it.sources and InputDevice.SOURCE_KEYBOARD != 0 
                }?.let { activeKeyboard = it }
            }
            
            if (activeMouse?.id == deviceId) {
                activeMouse = null
                // Find another mouse
                inputDevices.values.firstOrNull { 
                    it.sources and InputDevice.SOURCE_MOUSE != 0 
                }?.let { activeMouse = it }
            }
            
            notifyListeners { onInputDeviceRemoved(deviceInfo) }
        }
    }
    
    private fun updateInputDevice(device: InputDevice) {
        val deviceInfo = inputDevices[device.id]
        if (deviceInfo != null) {
            notifyListeners { onInputDeviceChanged(deviceInfo) }
        }
    }
    
    /**
     * Handle key event
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        // Check if this is from an external keyboard
        val device = event.device
        if (device != null && device.isExternal) {
            Logger.lDebug("$TAG - External keyboard event: ${event.keyCode}")
            return InputBridge.handleKeyEvent(event)
        }

        // Also handle system keyboard events
        return InputBridge.handleKeyEvent(event)
    }
    
    /**
     * Handle motion event
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        val device = event.device
        
        // Check if this is from an external mouse
        if (device != null && device.isExternal && 
            (device.supportsSource(InputDevice.SOURCE_MOUSE) || 
             device.supportsSource(InputDevice.SOURCE_TRACKBALL))) {
            
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    val button = getMouseButton(event)
                    return InputBridge.handleMouseButtonEvent(event, button)
                }
                MotionEvent.ACTION_MOVE -> {
                    return InputBridge.handleMotionEvent(event)
                }
                MotionEvent.ACTION_SCROLL -> {
                    return InputBridge.handleScrollEvent(event)
                }
            }
        }
        
        // Handle touch events as mouse events for testing
        if (device != null && device.supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) {
            return handleTouchEvent(event)
        }
        
        return false
    }
    
    /**
     * Handle touch events (convert to mouse events)
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                return InputBridge.handleMouseButtonEvent(event, 0) // Left button
            }
            MotionEvent.ACTION_UP -> {
                return InputBridge.handleMouseButtonEvent(event, 0) // Left button
            }
            MotionEvent.ACTION_MOVE -> {
                return InputBridge.handleMotionEvent(event)
            }
        }
        return false
    }
    
    /**
     * Get mouse button from motion event
     */
    private fun getMouseButton(event: MotionEvent): Int {
        val buttonState = event.buttonState
        
        return when {
            buttonState and MotionEvent.BUTTON_PRIMARY != 0 -> 0 // Left
            buttonState and MotionEvent.BUTTON_SECONDARY != 0 -> 1 // Right
            buttonState and MotionEvent.BUTTON_TERTIARY != 0 -> 2 // Middle
            buttonState and MotionEvent.BUTTON_BACK != 0 -> 3 // Back
            buttonState and MotionEvent.BUTTON_FORWARD != 0 -> 4 // Forward
            else -> 0
        }
    }
    
    /**
     * Add input listener
     */
    fun addInputListener(listener: InputListener) {
        inputListeners.add(listener)
    }
    
    /**
     * Remove input listener
     */
    fun removeInputListener(listener: InputListener) {
        inputListeners.remove(listener)
    }
    
    /**
     * Notify all listeners
     */
    private fun notifyListeners(action: (InputListener) -> Unit) {
        inputListeners.toList().forEach { listener ->
            try {
                action(listener)
            } catch (e: Exception) {
                Logger.lError("$TAG - Error notifying input listener: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get all input devices
     */
    fun getInputDevices(): List<InputDeviceInfo> {
        return inputDevices.values.toList()
    }
    
    /**
     * Get active keyboard
     */
    fun getActiveKeyboard(): InputDeviceInfo? = activeKeyboard
    
    /**
     * Get active mouse
     */
    fun getActiveMouse(): InputDeviceInfo? = activeMouse
    
    /**
     * Check if external keyboard is connected
     */
    fun hasExternalKeyboard(): Boolean = activeKeyboard != null
    
    /**
     * Check if external mouse is connected
     */
    fun hasExternalMouse(): Boolean = activeMouse != null
    
    /**
     * Cleanup
     */
    fun cleanup() {
        androidInputManager.unregisterInputDeviceListener(inputDeviceListener)
        inputDevices.clear()
        inputListeners.clear()
        Logger.lInfo("$TAG - Input manager cleaned up")
    }
    
    /**
     * Input device information
     */
    data class InputDeviceInfo(
        val id: Int,
        val name: String,
        val descriptor: String,
        val sources: Int,
        val isExternal: Boolean
    ) {
        fun isKeyboard(): Boolean = sources and InputDevice.SOURCE_KEYBOARD != 0
        fun isMouse(): Boolean = sources and InputDevice.SOURCE_MOUSE != 0
        fun isTouchscreen(): Boolean = sources and InputDevice.SOURCE_TOUCHSCREEN != 0
        fun isGamepad(): Boolean = 
            sources and InputDevice.SOURCE_GAMEPAD != 0 || 
            sources and InputDevice.SOURCE_JOYSTICK != 0
    }
    
    /**
     * Input listener interface
     */
    interface InputListener {
        fun onInputDeviceAdded(device: InputDeviceInfo) {}
        fun onInputDeviceRemoved(device: InputDeviceInfo) {}
        fun onInputDeviceChanged(device: InputDeviceInfo) {}
        fun onKeyEvent(event: KeyEvent): Boolean = false
        fun onMotionEvent(event: MotionEvent): Boolean = false
    }
}