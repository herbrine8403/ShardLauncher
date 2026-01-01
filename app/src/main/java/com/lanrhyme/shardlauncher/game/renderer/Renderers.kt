/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.renderer

import android.content.Context
import com.lanrhyme.shardlauncher.settings.AllSettings
import com.lanrhyme.shardlauncher.utils.device.Architecture

/**
 * Renderer management system
 */
object Renderers {
    private val renderers: MutableList<Renderer> = mutableListOf()
    private var compatibleRenderers: Pair<RenderersList, MutableList<Renderer>>? = null
    private var currentRenderer: Renderer = OpenGLES2Renderer()
    private var isInitialized: Boolean = false

    fun init(reset: Boolean = false) {
        if (isInitialized && !reset) return
        isInitialized = true

        if (reset) {
            renderers.clear()
            compatibleRenderers = null
            currentRenderer = OpenGLES2Renderer()
        }

        addRenderers(
            OpenGLES2Renderer(),
            OpenGLES3Renderer(),
            VulkanRenderer(),
            GL4ESRenderer(),
            ZinkRenderer()
        )
    }

    /**
     * Get compatible renderers for current device
     */
    fun getCompatibleRenderers(context: Context): Pair<RenderersList, List<Renderer>> = compatibleRenderers ?: run {
        // For now, assume all renderers are compatible
        // TODO: Add device compatibility checks
        val compatibleRenderers1: MutableList<Renderer> = mutableListOf()
        renderers.forEach { renderer ->
            compatibleRenderers1.add(renderer)
        }

        val rendererIdentifiers: MutableList<String> = mutableListOf()
        val rendererNames: MutableList<String> = mutableListOf()
        compatibleRenderers1.forEach { renderer ->
            rendererIdentifiers.add(renderer.getUniqueIdentifier())
            rendererNames.add(renderer.getRendererName())
        }

        val rendererPair = Pair(RenderersList(rendererIdentifiers, rendererNames), compatibleRenderers1)
        compatibleRenderers = rendererPair
        rendererPair
    }

    /**
     * Add multiple renderers
     */
    fun addRenderers(vararg renderers: Renderer) {
        renderers.forEach { renderer ->
            addRenderer(renderer)
        }
    }

    /**
     * Add single renderer
     */
    fun addRenderer(renderer: Renderer): Boolean {
        return if (renderers.any { it.getUniqueIdentifier() == renderer.getUniqueIdentifier() }) {
            false
        } else {
            renderers.add(renderer)
            true
        }
    }

    /**
     * Check if current renderer is valid
     */
    fun isCurrentRendererValid(): Boolean = isInitialized

    /**
     * Set current renderer
     */
    fun setCurrentRenderer(context: Context, uniqueIdentifier: String, retryToFirstOnFailure: Boolean = true) {
        if (!isInitialized) init()
        val compatibleRenderers = getCompatibleRenderers(context).second
        currentRenderer = compatibleRenderers.find { it.getUniqueIdentifier() == uniqueIdentifier } ?: run {
            if (retryToFirstOnFailure && compatibleRenderers.isNotEmpty()) {
                compatibleRenderers[0]
            } else OpenGLES2Renderer()
        }
    }

    /**
     * Get current renderer
     */
    fun getCurrentRenderer(): Renderer {
        if (!isInitialized) init()
        return currentRenderer
    }

    /**
     * Get all available renderers
     */
    fun getAllRenderers(): List<Renderer> {
        if (!isInitialized) init()
        return renderers.toList()
    }
}

/**
 * Base renderer interface
 */
interface Renderer {
    fun getRendererId(): String
    fun getRendererName(): String
    fun getRendererSummary(): String?
    fun getRendererLibrary(): String?
    fun getRendererEnv(): RendererEnv
    fun getRendererEGL(): String?
    fun getUniqueIdentifier(): String = getRendererId()
}

/**
 * Renderer environment variables
 */
data class RendererEnv(
    val value: Map<String, String> = emptyMap()
)

/**
 * OpenGL ES 2.0 Renderer
 */
class OpenGLES2Renderer : Renderer {
    override fun getRendererId(): String = "opengles2"
    override fun getRendererName(): String = "OpenGL ES 2.0"
    override fun getRendererSummary(): String? = "OpenGL ES 2.0 renderer"
    override fun getRendererLibrary(): String? = "libGL.so.1"
    override fun getRendererEnv(): RendererEnv = RendererEnv(mapOf(
        "LIBGL_ES" to "2"
    ))
    override fun getRendererEGL(): String? = "libEGL_mesa.so"
}

/**
 * OpenGL ES 3.0 Renderer
 */
class OpenGLES3Renderer : Renderer {
    override fun getRendererId(): String = "opengles3"
    override fun getRendererName(): String = "OpenGL ES 3.0"
    override fun getRendererSummary(): String? = "OpenGL ES 3.0 renderer"
    override fun getRendererLibrary(): String? = "libGL.so.1"
    override fun getRendererEnv(): RendererEnv = RendererEnv(mapOf(
        "LIBGL_ES" to "3"
    ))
    override fun getRendererEGL(): String? = "libEGL_mesa.so"
}

/**
 * Vulkan Renderer
 */
class VulkanRenderer : Renderer {
    override fun getRendererId(): String = "vulkan"
    override fun getRendererName(): String = "Vulkan"
    override fun getRendererSummary(): String? = "Vulkan renderer"
    override fun getRendererLibrary(): String? = "libvulkan.so"
    override fun getRendererEnv(): RendererEnv = RendererEnv(mapOf(
        "MESA_LOADER_DRIVER_OVERRIDE" to "zink"
    ))
    override fun getRendererEGL(): String? = null
}
/**
 * GL4ES Renderer
 */
class GL4ESRenderer : Renderer {
    override fun getRendererId(): String = "gl4es"
    override fun getRendererName(): String = "GL4ES"
    override fun getRendererSummary(): String? = "OpenGL ES to OpenGL translation layer"
    override fun getRendererLibrary(): String? = "libGL.so.1"
    override fun getRendererEnv(): RendererEnv = RendererEnv(mapOf(
        "LIBGL_GL" to "21"
    ))
    override fun getRendererEGL(): String? = "libEGL_mesa.so"
}

/**
 * Zink Renderer (Vulkan-based OpenGL)
 */
class ZinkRenderer : Renderer {
    override fun getRendererId(): String = "zink"
    override fun getRendererName(): String = "Zink (Vulkan)"
    override fun getRendererSummary(): String? = "OpenGL over Vulkan implementation"
    override fun getRendererLibrary(): String? = "libGL.so.1"
    override fun getRendererEnv(): RendererEnv = RendererEnv(mapOf(
        "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
        "GALLIUM_DRIVER" to "zink"
    ))
    override fun getRendererEGL(): String? = "libEGL_mesa.so"
}