/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.launch

import android.app.Activity
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.os.Build
import androidx.compose.ui.unit.IntSize
import com.lanrhyme.shardlauncher.BuildConfig
import com.lanrhyme.shardlauncher.bridge.LoggerBridge
import com.lanrhyme.shardlauncher.bridge.ZLBridge
import com.lanrhyme.shardlauncher.bridge.ZLNativeInvoker
import com.lanrhyme.shardlauncher.game.account.Account
import com.lanrhyme.shardlauncher.game.account.AccountType
import com.lanrhyme.shardlauncher.game.account.AccountsManager
import com.lanrhyme.shardlauncher.game.account.offline.OfflineYggdrasilServer
import com.lanrhyme.shardlauncher.game.multirt.Runtime
import com.lanrhyme.shardlauncher.game.multirt.RuntimesManager
import com.lanrhyme.shardlauncher.game.plugin.driver.DriverPluginManager
import com.lanrhyme.shardlauncher.game.plugin.renderer.RendererPluginManager
import com.lanrhyme.shardlauncher.game.renderer.Renderers
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.installed.getGameManifest
import com.lanrhyme.shardlauncher.game.version.remote.MinecraftVersionJson
import com.lanrhyme.shardlauncher.path.PathManager
import com.lanrhyme.shardlauncher.settings.AllSettings
import com.lanrhyme.shardlauncher.utils.device.Architecture
import com.lanrhyme.shardlauncher.game.account.isLocalAccount
import com.lanrhyme.shardlauncher.utils.logging.Logger
import kotlinx.coroutines.runBlocking
import java.io.File

class GameLauncher(
    private val activity: Activity,
    private val version: Version,
    private val getWindowSize: () -> IntSize,
    onExit: (code: Int, isSignal: Boolean) -> Unit
) : Launcher(onExit) {
    
    private lateinit var gameManifest: MinecraftVersionJson
    private var offlinePort: Int = 0

    override suspend fun launch(): Int {
        // Initialize renderer if needed
        if (!Renderers.isCurrentRendererValid()) {
            val rendererIdentifier = version.getRenderer()
            if (rendererIdentifier.isNotEmpty()) {
                Renderers.setCurrentRenderer(activity, rendererIdentifier)
            } else {
                // Auto-select first compatible renderer if none specified
                val compatibleRenderers = Renderers.getCompatibleRenderers(activity)
                if (compatibleRenderers.isNotEmpty()) {
                    Renderers.setCurrentRenderer(activity, compatibleRenderers[0].getUniqueIdentifier())
                    Logger.lInfo("Auto-selected renderer: ${compatibleRenderers[0].getRendererName()}")
                } else {
                    throw IllegalStateException("No compatible renderers available")
                }
            }
        }

        // Get game manifest
        gameManifest = getGameManifest(version)
        
        // Skip input stack queue setup for now to avoid UI thread issues
        // TODO: Set input stack queue usage after game is fully launched
        // org.lwjgl.glfw.CallbackBridge.nativeSetUseInputStackQueue(gameManifest.arguments != null)

        // Get current account
        val currentAccount = AccountsManager.currentAccountFlow.value!!
        val account = if (version.offlineAccountLogin) {
            currentAccount.copy(accountType = AccountType.LOCAL.toString())
        } else {
            currentAccount
        }

        val customArgs = version.getJvmArgs().takeIf { it.isNotBlank() } ?: AllSettings.jvmArgs.getValue()
        val javaRuntimeName = getRuntimeName()
        val selectedRuntime = RuntimesManager.getRuntime(javaRuntimeName)
        this.runtime = selectedRuntime

        // Initialize LoggerBridge (temporarily disabled due to JNI issues)
        // TODO: Fix LoggerBridge JNI method resolution
        try {
            // Skip native logging for now to avoid UnsatisfiedLinkError
            // val logFile = File(PathManager.DIR_NATIVE_LOGS, "${getLogName()}.log")
            // LoggerBridge.start(logFile.absolutePath)
            Logger.lInfo("Native logging skipped - using Java logging only")
        } catch (e: Exception) {
            Logger.lWarning("Failed to initialize native logging", e)
        }

        // Initialize MCOptions and set language (skip for now to avoid potential issues)
        // TODO: Re-enable after fixing stability issues
        try {
            MCOptions.setup(activity, version)
            MCOptions.loadLanguage(version.getVersionName())
            MCOptions.save()
            Logger.lInfo("MCOptions initialized successfully")
        } catch (e: Exception) {
            Logger.lWarning("Failed to initialize MCOptions, continuing without it", e)
        }

        // Start offline Yggdrasil if needed (skip for now to avoid potential issues)
        // TODO: Re-enable after fixing stability issues
        try {
            if (account.isLocalAccount() && account.hasSkinFile) {
                offlinePort = OfflineYggdrasilServer.start()
                OfflineYggdrasilServer.addCharacter(account.username, account.profileId)
                Logger.lInfo("Offline Yggdrasil server started on port $offlinePort")
            }
        } catch (e: Exception) {
            Logger.lWarning("Failed to start offline Yggdrasil server, continuing without it", e)
        }

        printLauncherInfo(
            javaArguments = customArgs.takeIf { it.isNotEmpty() } ?: "NONE",
            javaRuntime = javaRuntimeName,
            account = account
        )

        return launchGame(account, javaRuntimeName, customArgs)
    }

    private suspend fun launchGame(
        account: Account,
        javaRuntime: String,
        customArgs: String
    ): Int {
        val runtime = RuntimesManager.forceReload(javaRuntime)
        this.runtime = runtime

        val gameDirPath = version.getGameDir()
        disableSplash(gameDirPath)

        val runtimeLibraryPath = getRuntimeLibraryPath()

        val launchArgs = LaunchArgs(
            runtimeLibraryPath = runtimeLibraryPath,
            account = account,
            gameDirPath = gameDirPath,
            version = version,
            gameManifest = gameManifest,
            runtime = runtime,
            getCacioJavaArgs = { isJava8 ->
                val windowSize = getWindowSize()
                getCacioJavaArgs(windowSize.width, windowSize.height, isJava8)
            },
            offlineServerPort = offlinePort
        ).getAllArgs()

        return launchJvm(
            context = activity,
            jvmArgs = launchArgs,
            userArgs = customArgs,
            getWindowSize = getWindowSize
        )
    }

    override fun chdir(): String {
        return version.getGameDir().absolutePath
    }

    override fun getLogName(): String = "game_${version.getVersionName()}_${System.currentTimeMillis()}"

    override fun exit() {
        if (offlinePort != 0) {
            OfflineYggdrasilServer.stop()
        }
    }

    override fun MutableMap<String, String>.putJavaArgs() {
        val versionInfo = version.getVersionInfo()
        
        // Fix Forge 1.7.2
        val is172 = (versionInfo?.minecraftVersion ?: "0.0") == "1.7.2"
        if (is172 && (versionInfo?.loaderInfo?.loader?.name == "forge")) {
            Logger.lDebug("Is Forge 1.7.2, use the patched sorting method.")
            put("sort.patch", "true")
        }

        // JNA library path
        gameManifest.libraries?.find { library ->
            library.name.startsWith("net.java.dev.jna:jna:")
        }?.let { library ->
            val components = library.name.split(":")
            if (components.size >= 3) {
                val jnaVersion = components[2]
                val jnaDir = File(PathManager.DIR_COMPONENTS, "jna/$jnaVersion")
                if (jnaDir.exists()) {
                    val dirPath = jnaDir.absolutePath
                    put("java.library.path", "$dirPath:${PathManager.DIR_NATIVE_LIB}")
                    put("jna.boot.library.path", dirPath)
                }
            }
        }
    }

    override fun initEnv(): MutableMap<String, String> {
        val envMap = super.initEnv()

        // Set driver
        DriverPluginManager.setDriverById(version.getDriver())
        envMap["DRIVER_PATH"] = DriverPluginManager.getDriver().path

        // Set loader environment
        version.getVersionInfo()?.loaderInfo?.getLoaderEnvKey()?.let { loaderKey ->
            envMap[loaderKey] = "1"
        }

        // Set renderer environment
        if (Renderers.isCurrentRendererValid()) {
            setRendererEnv(envMap)
        }

        envMap["SHARD_VERSION_CODE"] = BuildConfig.VERSION_CODE.toString()
        
        return envMap
    }

    override fun dlopenEngine() {
        super.dlopenEngine()
        
        Logger.lInfo("==================== DLOPEN Renderer ====================")
        
        // Load renderer plugin libraries
        loadRendererPluginLibraries()
        
        // Load main graphics library
        loadMainGraphicsLibrary()
    }
    
    private fun loadRendererPluginLibraries() {
        RendererPluginManager.selectedRendererPlugin?.let { renderer ->
            Logger.lInfo("Loading renderer plugin: ${renderer.name}")
            
            renderer.dlopen.forEach { lib ->
                try {
                    val libPath = "${renderer.path}/$lib"
                    val success = ZLBridge.dlopen(libPath)
                    
                    if (success) {
                        Logger.lInfo("Loaded renderer plugin library: $lib")
                    } else {
                        Logger.lWarning("Failed to load renderer plugin library: $lib")
                    }
                } catch (e: UnsatisfiedLinkError) {
                    Logger.lError("JNI error loading renderer plugin library $lib: ${e.message}")
                } catch (e: Exception) {
                    Logger.lError("Unexpected error loading renderer plugin library $lib: ${e.message}")
                }
            }
        } ?: run {
            Logger.lInfo("No renderer plugin selected")
        }
    }
    
    private fun loadMainGraphicsLibrary() {
        val rendererLib = loadGraphicsLibrary()
        if (rendererLib != null) {
            Logger.lInfo("Loading main graphics library: $rendererLib")
            
            var success = false
            var loadedPath = ""
            
            try {
                // Try direct loading first
                success = ZLBridge.dlopen(rendererLib)
                loadedPath = rendererLib
                
                // If direct loading fails, try to find in LD_LIBRARY_PATH
                if (!success) {
                    val pathLib = findInLdLibPath(rendererLib)
                    if (pathLib != null) {
                        success = ZLBridge.dlopen(pathLib)
                        loadedPath = pathLib
                    }
                }
                
                if (success) {
                    Logger.lInfo("Successfully loaded graphics library: $loadedPath")
                } else {
                    Logger.lError("Failed to load graphics library: $rendererLib")
                }
            } catch (e: UnsatisfiedLinkError) {
                Logger.lError("JNI error loading graphics library $rendererLib: ${e.message}")
            } catch (e: Exception) {
                Logger.lError("Unexpected error loading graphics library $rendererLib: ${e.message}")
            }
        } else {
            Logger.lWarning("No graphics library specified for current renderer")
        }
    }
    
    override fun loadGraphicsLibraries() {
        // Load renderer-specific graphics libraries
        val renderer = Renderers.getCurrentRenderer()
        val rendererLibs = renderer.getRequiredLibraries()
        
        rendererLibs.forEach { libName ->
            try {
                val libPath = File(PathManager.DIR_NATIVE_LIB, libName)
                if (libPath.exists()) {
                    ZLBridge.dlopen(libPath.absolutePath)
                    Logger.lDebug("Loaded renderer library: $libName")
                }
            } catch (e: Exception) {
                Logger.lDebug("Skipped renderer library: $libName")
            }
        }
    }

    override fun progressFinalUserArgs(args: MutableList<String>, ramAllocation: Int) {
        super.progressFinalUserArgs(args, ramAllocation)
        if (Renderers.isCurrentRendererValid()) {
            args.add("-Dorg.lwjgl.opengl.libname=${loadGraphicsLibrary()}")
        }
    }

    private fun getCacioJavaArgs(windowWidth: Int, windowHeight: Int, isJava8: Boolean): List<String> {
        val args = mutableListOf<String>()
        val scaleFactor = AllSettings.resolutionRatio.getValue() / 100f
        val scaledWidth = getDisplayFriendlyRes(windowWidth, scaleFactor)
        val scaledHeight = getDisplayFriendlyRes(windowHeight, scaleFactor)
        
        args.add("-Djava.awt.headless=false")
        args.add("-Dawt.toolkit=net.java.openjdk.cacio.ctk.CToolkit")
        args.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctk.CGraphicsEnvironment")
        args.add("-Dglfwstub.windowWidth=$scaledWidth")
        args.add("-Dglfwstub.windowHeight=$scaledHeight")
        args.add("-Dglfwstub.initEgl=false")
        
        if (!isJava8) {
            args.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
            args.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
            args.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
            args.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
            
            if (runtime?.javaVersion ?: 8 >= 17) {
                args.add("-javaagent:${PathManager.DIR_COMPONENTS}/cacio-17/cacio-agent.jar")
            }
        }
        
        val cacioJarDir = if (runtime?.javaVersion ?: 8 >= 17) {
            File(PathManager.DIR_COMPONENTS, "cacio-17")
        } else {
            File(PathManager.DIR_COMPONENTS, "cacio-8")
        }
        args.add("-Xbootclasspath/a:${File(cacioJarDir, "cacio-ttc.jar").absolutePath}")
        
        return args
    }

    private fun findInLdLibPath(libName: String): String? {
        val ldLibraryPath = getRuntimeLibraryPath()
        return ldLibraryPath.split(":").firstNotNullOfOrNull { dir ->
            val file = File(dir, libName)
            if (file.exists()) file.absolutePath else null
        }
    }

    private fun printLauncherInfo(
        javaArguments: String,
        javaRuntime: String,
        account: Account
    ) {
        val renderer = Renderers.getCurrentRenderer()
        
        // Use regular Logger instead of LoggerBridge to avoid native library issues
        Logger.lInfo("==================== Launch Minecraft ====================")
        Logger.lInfo("Info: Launcher version: ${BuildConfig.VERSION_NAME}")
        Logger.lInfo("Info: Architecture: ${Architecture.archAsString(Architecture.getDeviceArchitecture())}")
        Logger.lInfo("Info: Renderer: ${renderer.getRendererName()}")
        Logger.lInfo("Info: Selected Minecraft version: ${version.getVersionName()}")
        Logger.lInfo("Info: Game Path: ${version.getGameDir().absolutePath} (Isolation: ${version.isIsolation()})")
        Logger.lInfo("Info: Custom Java arguments: $javaArguments")
        Logger.lInfo("Info: Java Runtime: $javaRuntime")
        Logger.lInfo("Info: Account: ${account.username} (${account.accountType})")
    }

    private fun getRuntimeName(): String {
        val versionRuntime = version.getJavaRuntime().takeIf { it.isNotEmpty() } ?: ""
        if (versionRuntime.isNotEmpty()) return versionRuntime

        val defaultRuntime = AllSettings.javaRuntime.getValue()
        if (AllSettings.autoPickJavaRuntime.getValue()) {
            val targetJavaVersion = gameManifest.javaVersion?.majorVersion ?: 8
            val runtime0 = RuntimesManager.getDefaultRuntime(targetJavaVersion)
            if (runtime0 != null) return runtime0.name
        }
        return defaultRuntime
    }

    private fun disableSplash(dir: File) {
        val configDir = File(dir, "config")
        if (configDir.exists() || configDir.mkdirs()) {
            val forgeSplashFile = File(configDir, "splash.properties")
            runCatching {
                if (forgeSplashFile.exists()) {
                    val content = forgeSplashFile.readText()
                    if (content.contains("enabled=true")) {
                        forgeSplashFile.writeText(content.replace("enabled=true", "enabled=false"))
                    }
                } else {
                    forgeSplashFile.writeText("enabled=false")
                }
            }
        }
    }

    private fun setRendererEnv(envMap: MutableMap<String, String>) {
        val renderer = Renderers.getCurrentRenderer()
        val rendererId = renderer.getRendererId()

        // Set OpenGL ES version
        if (rendererId.startsWith("opengles")) {
            val glEsVersion = getDetectedVersion()
            envMap["LIBGL_ES"] = glEsVersion.toString()
            envMap["LIBGL_GL"] = (glEsVersion + 10).toString()
        }

        // Apply renderer-specific environment variables
        envMap.putAll(renderer.getRendererEnv().value)
        
        // Set renderer identifier
        envMap["POJAV_RENDERER"] = rendererId
        
        // Configure Mesa-based renderers
        if (!rendererId.startsWith("opengles")) {
            envMap["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"
            envMap["MESA_GLSL_CACHE_DIR"] = PathManager.DIR_CACHE.absolutePath
            envMap["MESA_GL_VERSION_OVERRIDE"] = "4.6"
            envMap["MESA_GLSL_VERSION_OVERRIDE"] = "460"
            envMap["force_glsl_extensions_warn"] = "true"
            envMap["allow_higher_compat_version"] = "true"
            envMap["allow_glsl_extension_directive_midshader"] = "true"
        }

        // Configure GL4ES-based renderers
        if (rendererId.contains("gl4es")) {
            envMap["LIBGL_MIPMAP"] = "3"
            envMap["LIBGL_NORMALIZE"] = "1"
            envMap["LIBGL_NOINTOVLHACK"] = "1"
            envMap["LIBGL_NOERROR"] = "1"
            
            if (rendererId.contains("ng")) {
                envMap["LIBGL_USE_MC_COLOR"] = "1"
                envMap["DLOPEN"] = "libspirv-cross-c-shared.so"
            }
        }

        // Apply user settings
        applyRendererSettings(envMap)
    }
    
    private fun applyRendererSettings(envMap: MutableMap<String, String>) {
        // Shader dumping
        if (AllSettings.dumpShaders.state) {
            envMap["LIBGL_VGPU_DUMP"] = "1"
        }
        
        // Zink driver preferences
        if (AllSettings.zinkPreferSystemDriver.state) {
            envMap["POJAV_ZINK_PREFER_SYSTEM_DRIVER"] = "1"
        }
        
        // VSync control
        if (AllSettings.vsyncInZink.state) {
            envMap["POJAV_VSYNC_IN_ZINK"] = "1"
        }
        
        // Performance settings
        if (AllSettings.bigCoreAffinity.state) {
            envMap["POJAV_BIG_CORE_AFFINITY"] = "1"
        }
        
        if (AllSettings.sustainedPerformance.state) {
            envMap["POJAV_SUSTAINED_PERFORMANCE"] = "1"
        }
        
        // Memory settings
        val ramAllocation = AllSettings.ramAllocation.getValue()
        envMap["POJAV_MAX_RAM"] = "${ramAllocation}M"
    }

    private fun getDetectedVersion(): Int {
        return runCatching {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(display, version, 0, version, 1)
            
            val attribList = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfig, 0)
            
            if (numConfig[0] > 0) 3 else 2
        }.getOrElse { 2 }
    }

    private fun loadGraphicsLibrary(): String? {
        val rendererPlugin = RendererPluginManager.selectedRendererPlugin
        return if (rendererPlugin != null) {
            "${rendererPlugin.path}/${rendererPlugin.glName}"
        } else {
            Renderers.getCurrentRenderer().getRendererLibrary()
        }
    }

    /**
     * Calculate display-friendly resolution
     */
    private fun getDisplayFriendlyRes(pixels: Int, scaleFactor: Float): Int {
        return (pixels * scaleFactor).toInt().coerceAtLeast(1)
    }
}