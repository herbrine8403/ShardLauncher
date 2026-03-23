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

package com.lanrhyme.shardlauncher.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.lanrhyme.shardlauncher.MainActivity
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.bridge.LoggerBridge
import com.lanrhyme.shardlauncher.bridge.ZLBridge
import com.lanrhyme.shardlauncher.bridge.ZLBridgeStates
import com.lanrhyme.shardlauncher.game.input.GameInputProxy
import com.lanrhyme.shardlauncher.game.input.LWJGLCharSender
import com.lanrhyme.shardlauncher.game.keycodes.LwjglGlfwKeycode
import com.lanrhyme.shardlauncher.game.launch.GameLauncher
import com.lanrhyme.shardlauncher.game.launch.JvmLaunchInfo
import com.lanrhyme.shardlauncher.game.launch.JvmLauncher
import com.lanrhyme.shardlauncher.game.launch.handler.AbstractHandler
import com.lanrhyme.shardlauncher.game.launch.handler.GameHandler
import com.lanrhyme.shardlauncher.game.launch.handler.HandlerType
import com.lanrhyme.shardlauncher.game.launch.handler.JVMHandler
import com.lanrhyme.shardlauncher.game.launch.handler.TextInputMode
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.path.PathManager
import com.lanrhyme.shardlauncher.settings.AllSettings
import com.lanrhyme.shardlauncher.ui.theme.ShardLauncherTheme
import com.lanrhyme.shardlauncher.utils.logging.Logger.lError
import com.lanrhyme.shardlauncher.utils.logging.Logger.lWarning
import com.lanrhyme.shardlauncher.viewmodel.ErrorViewModel
import com.lanrhyme.shardlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.io.IOException
import android.graphics.Color as NativeColor

private const val INTENT_RUN_GAME = "BUNDLE_RUN_GAME"
private const val INTENT_RUN_JAR = "INTENT_RUN_JAR"
private const val INTENT_JAR_INFO = "INTENT_JAR_INFO"
private const val INTENT_VERSION = "INTENT_VERSION"
private var isRunning = false

/**
 * VMViewModel - 游戏运行状态管理
 */
class VMViewModel : ViewModel() {
    lateinit var launcher: com.lanrhyme.shardlauncher.game.launch.Launcher
    lateinit var handler: AbstractHandler

    var textInputMode by mutableStateOf(TextInputMode.DISABLE)

    fun disableInputMode() {
        if (textInputMode == TextInputMode.ENABLE) textInputMode = TextInputMode.DISABLE
    }

    var keyHandle = true

    val inputProxy = GameInputProxy(LWJGLCharSender)
    val inputTextFieldState = TextFieldState()

    private val _enabledInputActionBar = MutableStateFlow(false)
    val enabledInputActionBar = _enabledInputActionBar.asStateFlow()

    fun updateInputActionBar(enabled: Boolean) {
        _enabledInputActionBar.update { enabled }
    }

    private var lastInputText = ""
    private var lastInputSelection = TextRange.Zero
    private var isInputCleaning = false
    private val inputMutex = Mutex()

    fun handleInputText(text: String, selection: TextRange) {
        viewModelScope.launch {
            inputMutex.withLock {
                if (!isInputCleaning && (text != lastInputText || selection != lastInputSelection)) {
                    withContext(Dispatchers.Main) {
                        inputProxy.handleTextChange(
                            oldText = lastInputText,
                            newText = text,
                            oldSelection = lastInputSelection,
                            newSelection = selection
                        )
                    }
                    lastInputText = text
                    lastInputSelection = selection
                }
            }
        }
    }

    fun clearInput() {
        viewModelScope.launch {
            inputMutex.withLock {
                isInputCleaning = true
                withContext(Dispatchers.Main) {
                    inputTextFieldState.edit {
                        replace(0, inputTextFieldState.text.length, "")
                        selection = TextRange.Zero
                    }
                }
                lastInputText = ""
                lastInputSelection = TextRange.Zero
                isInputCleaning = false
            }
        }
    }

    fun runIfHandlerInitialized(block: (AbstractHandler) -> Unit) {
        if (this::handler.isInitialized) block(this.handler)
    }

    fun isHandlerInitialized(): Boolean = this::handler.isInitialized
}

class VMActivity : androidx.activity.ComponentActivity(), SurfaceTextureListener {
    private val errorViewModel: ErrorViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()
    private val vmViewModel: VMViewModel by viewModels()

    var mTextureView: TextureView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras ?: throw IllegalStateException("Unknown VM launch state!")

        val exitListener = { exitCode: Int, isSignal: Boolean ->
            if (exitCode != 0) {
                showExitMessage(this, exitCode, isSignal)
            } else {
                startActivity(Intent(this@VMActivity, MainActivity::class.java))
            }
        }

        val getWindowSize = {
            val displayMetrics = getDisplayMetrics()
            IntSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }

        vmViewModel.launcher = when {
            bundle.getBoolean(INTENT_RUN_GAME, false) -> {
                val version: Version = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable(INTENT_VERSION, Version::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelable(INTENT_VERSION)
                } ?: throw IllegalStateException("No launch version has been set.")
                GameLauncher(
                    activity = this,
                    version = version,
                    getWindowSize = getWindowSize,
                    onExit = exitListener
                ).also { launcher ->
                    vmViewModel.handler = GameHandler(
                        activity = this,
                        version = version,
                        eventViewModel = eventViewModel,
                        getWindowSize = getWindowSize,
                        gameLauncher = launcher,
                        onExit = { code -> exitListener(code, false) }
                    )
                    vmViewModel.inputProxy.sender = LWJGLCharSender
                }
            }
            bundle.getBoolean(INTENT_RUN_JAR, false) -> {
                val jvmLaunchInfo: JvmLaunchInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable(INTENT_JAR_INFO, JvmLaunchInfo::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelable(INTENT_JAR_INFO)
                } ?: throw IllegalStateException("No launch jar info has been set.")
                
                val launcher = JvmLauncher(
                    context = this,
                    getWindowSize = getWindowSize,
                    jvmLaunchInfo = jvmLaunchInfo,
                    onExit = exitListener
                )
                
                vmViewModel.handler = JVMHandler(
                    jvmLauncher = launcher,
                    eventViewModel = eventViewModel,
                    onExit = { code -> exitListener(code, false) }
                )
                
                launcher
            }
            else -> throw IllegalStateException("Unknown VM launch mode!")
        }

        refreshWindowSize()

        window?.apply {
            setBackgroundDrawable(NativeColor.BLACK.toDrawable())
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val logFile = File(PathManager.DIR_FILES_EXTERNAL, "${vmViewModel.launcher.getLogName()}.log")
        if (!logFile.exists() && !logFile.createNewFile()) throw IOException("Failed to create a new log file")
        LoggerBridge.start(logFile.absolutePath)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorViewModel.errorEvents.collect { tm ->
                    errorViewModel.showErrorDialog(
                        context = this@VMActivity,
                        tm = tm
                    )
                }
            }
        }

        lifecycleScope.launch {
            eventViewModel.events.collect { event ->
                when (event) {
                    is EventViewModel.Event.Game.RefreshSize -> refreshSize()
                    is EventViewModel.Event.Game.SwitchIme -> {
                        vmViewModel.textInputMode = event.mode ?: vmViewModel.textInputMode.switch()
                    }
                    is EventViewModel.Event.Game.KeyHandle -> {
                        vmViewModel.keyHandle = event.handle
                    }
                    else -> { /* Ignore */ }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vmViewModel.textInputMode == TextInputMode.ENABLE) {
                    vmViewModel.disableInputMode()
                    return
                }
                if (!vmViewModel.keyHandle) return
                eventViewModel.sendEvent(EventViewModel.Event.Game.OnBack)
            }
        })

        setContent {
            ShardLauncherTheme(darkTheme = true) {
                Screen {
                    vmViewModel.handler.ComposableLayout(vmViewModel.textInputMode)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vmViewModel.runIfHandlerInitialized { it.onResume() }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1)
    }

    override fun onPause() {
        super.onPause()
        vmViewModel.runIfHandlerInitialized { it.onPause() }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0)
    }

    override fun onStart() {
        super.onStart()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1)
    }

    override fun onStop() {
        super.onStop()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshDisplayMetrics()
        refreshSize()
    }

    override fun onPostResume() {
        super.onPostResume()
        refreshDisplayMetrics()
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            refreshSize()
        }
    }

    override fun onDestroy() {
        vmViewModel.runIfHandlerInitialized { it.onDestroy() }
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!vmViewModel.keyHandle) return super.dispatchKeyEvent(event)

        val isPressed = event.action == KeyEvent.ACTION_DOWN

        if (vmViewModel.textInputMode == TextInputMode.ENABLE) {
            if (isPressed && !vmViewModel.inputProxy.keyCanHandle(event)) {
                return super.dispatchKeyEvent(event)
            }

            if (
                isPressed &&
                !vmViewModel.inputProxy.handleSpecialKey(event, vmViewModel.inputTextFieldState.text)
            ) {
                vmViewModel.inputProxy.handleSpecialKey(event) {
                    vmViewModel.clearInput()
                }
            }
            if (event.keyCode == KeyEvent.KEYCODE_TAB) {
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        event.device?.let {
            val source = event.source
            if (source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE ||
                source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    vmViewModel.runIfHandlerInitialized { it.sendMouseRight(isPressed) }
                    return false
                }
            }
        }

        if (vmViewModel.isHandlerInitialized() && vmViewModel.handler.shouldIgnoreKeyEvent(event)) {
            return super.dispatchKeyEvent(event)
        }
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (isRunning) {
            ZLBridge.setupBridgeWindow(Surface(surface))
            return
        }
        isRunning = true

        vmViewModel.runIfHandlerInitialized { it.mIsSurfaceDestroyed = false }
        val currentSize = refreshSize()
        vmViewModel.runIfHandlerInitialized { handler ->
            lifecycleScope.launch(Dispatchers.Default) {
                handler.execute(
                    surface = Surface(surface),
                    screenSize = currentSize,
                    scope = lifecycleScope
                )
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        refreshSize()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        vmViewModel.runIfHandlerInitialized { it.mIsSurfaceDestroyed = true }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        vmViewModel.runIfHandlerInitialized { it.onGraphicOutput() }
    }

    @Composable
    private fun Screen(content: @Composable () -> Unit = {}) {
        val imeInsets = WindowInsets.ime
        val inputArea by vmViewModel.handler.inputArea.collectAsStateWithLifecycle()

        Layout(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            content = {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .absoluteOffset {
                            val area = inputArea ?: return@absoluteOffset IntOffset.Zero
                            val imeHeight = imeInsets.getBottom(this@absoluteOffset)
                            val bottomDistance = CallbackBridge.windowHeight - area.bottom
                            val bottomPadding = (imeHeight - bottomDistance).coerceAtLeast(0)
                            IntOffset(0, -bottomPadding)
                        },
                    factory = { context ->
                        TextureView(context).apply {
                            isOpaque = true
                            alpha = 1.0f
                            surfaceTextureListener = this@VMActivity
                        }.also { view ->
                            mTextureView = view
                        }
                    }
                )
                content()
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { it.place(0, 0) }
            }
        }
    }

    private fun refreshDisplayMetrics() {
        val displayMetrics = getDisplayMetrics()
        CallbackBridge.physicalWidth = displayMetrics.widthPixels
        CallbackBridge.physicalHeight = displayMetrics.heightPixels
    }

    private fun refreshWindowSize(): IntSize {
        var resultSize = IntSize.Zero
        vmViewModel.runIfHandlerInitialized { handler ->
            val displayMetrics = getDisplayMetrics()
            fun getDisplayPixels(pixels: Int): Int {
                return when (handler.type) {
                    HandlerType.GAME -> getDisplayFriendlyRes(pixels, AllSettings.resolutionRatio.getValue() / 100f)
                    HandlerType.JVM -> getDisplayFriendlyRes(pixels, 0.8f)
                }
            }
            val width = getDisplayPixels(displayMetrics.widthPixels)
            val height = getDisplayPixels(displayMetrics.heightPixels)
            if (width < 1 || height < 1) {
                lError("Impossible resolution : $width x $height")
                return@runIfHandlerInitialized
            }
            CallbackBridge.windowWidth = width
            CallbackBridge.windowHeight = height
            ZLBridgeStates.onWindowChange()
            resultSize = IntSize(width, height)
        }
        return resultSize
    }

    private fun refreshSize() {
        refreshWindowSize()
        mTextureView?.surfaceTexture?.apply {
            setDefaultBufferSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight)
        } ?: run {
            lWarning("Attempt to refresh size on null surface")
            return
        }
        CallbackBridge.sendUpdateWindowSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight)
    }
}

/**
 * 获取显示指标
 */
fun androidx.activity.ComponentActivity.getDisplayMetrics(): android.util.DisplayMetrics {
    val displayMetrics = android.util.DisplayMetrics()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        displayMetrics.widthPixels = bounds.width()
        displayMetrics.heightPixels = bounds.height()
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }
    return displayMetrics
}

/**
 * 获取友好显示分辨率
 */
fun getDisplayFriendlyRes(pixels: Int, scaleFactor: Float): Int {
    return (pixels * scaleFactor).toInt().coerceAtLeast(1)
}

/**
 * 显示退出消息
 */
private fun showExitMessage(context: Context, exitCode: Int, isSignal: Boolean) {
    val message = if (isSignal) {
        "Game exited by signal: $exitCode"
    } else {
        "Game exited with code: $exitCode"
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

/**
 * 启动游戏
 */
fun runGame(context: Context, version: Version) {
    val intent = Intent(context, VMActivity::class.java).apply {
        putExtra(INTENT_RUN_GAME, true)
        putExtra(INTENT_VERSION, version)
    }
    context.startActivity(intent)
}

/**
 * 运行 JAR 文件
 * @param context 上下文
 * @param jarFile JAR 文件
 * @param jreName 指定使用的 Java 环境，null 则为自动选择
 * @param customArgs 指定 jvm 参数
 */
fun runJar(
    context: Context,
    jarFile: File,
    jreName: String? = null,
    customArgs: String? = null
) {
    val jvmArgsPrefix = customArgs?.let { "$it " } ?: ""
    val jvmArgs = "$jvmArgsPrefix-jar ${jarFile.absolutePath}"

    val jvmLaunchInfo = JvmLaunchInfo(
        jvmArgs = jvmArgs,
        jreName = jreName
    )

    val intent = Intent(context, VMActivity::class.java).apply {
        putExtra(INTENT_RUN_JAR, true)
        putExtra(INTENT_JAR_INFO, jvmLaunchInfo)
    }
    context.startActivity(intent)
}
