package com.lanrhyme.shardlauncher.game.renderer

import android.content.Context
import com.lanrhyme.shardlauncher.game.renderer.renderers.*
import com.lanrhyme.shardlauncher.utils.device.Architecture
import com.lanrhyme.shardlauncher.utils.device.checkVulkanSupport
import com.lanrhyme.shardlauncher.utils.logging.Logger

object Renderers {
    private val renderers: MutableList<RendererInterface> = mutableListOf()
    private var isInitialized: Boolean = false

    fun init(reset: Boolean = false) {
        if (isInitialized && !reset) return
        isInitialized = true

        if (reset) {
            renderers.clear()
        }

        addRenderers(
            NGGL4ESRenderer,
            GL4ESRenderer,
            VulkanZinkRenderer,
            VirGLRenderer,
            FreedrenoRenderer,
            PanfrostRenderer
        )
    }

    fun getAllRenderers(): List<RendererInterface> {
        if (!isInitialized) init()
        return renderers.toList()
    }

    @JvmStatic
    fun addRenderers(vararg renderers: RendererInterface) {
        renderers.forEach { renderer ->
            addRenderer(renderer)
        }
    }

    @JvmStatic
    fun addRenderer(renderer: RendererInterface): Boolean {
        return if (renderers.any { it.getUniqueIdentifier() == renderer.getUniqueIdentifier() }) {
            Logger.w("Renderers", "Renderer conflict: ${renderer.getRendererName()}")
            false
        } else {
            renderers.add(renderer)
            Logger.i("Renderers", "Renderer loaded: ${renderer.getRendererName()}")
            true
        }
    }

    fun findRendererByIdentifier(uniqueIdentifier: String): RendererInterface? {
        if (!isInitialized) init()
        return renderers.find { it.getUniqueIdentifier() == uniqueIdentifier }
    }

    fun getCompatibleRenderers(context: Context): List<RendererInterface> {
        if (!isInitialized) init()
        
        val deviceHasVulkan = checkVulkanSupport(context.packageManager)
        val deviceHasZinkBinary = !(Architecture.is32BitsDevice && Architecture.isx86Device())

        return renderers.filter { renderer ->
            when {
                renderer.getRendererId().contains("vulkan") && !deviceHasVulkan -> false
                renderer.getRendererId().contains("zink") && !deviceHasZinkBinary -> false
                else -> true
            }
        }
    }

    private var currentRenderer: RendererInterface? = null

    /**
     * 设置当前的渲染器
     */
    fun setCurrentRenderer(context: Context, uniqueIdentifier: String, retryToFirstOnFailure: Boolean = true) {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        val compatibleRenderers = getCompatibleRenderers(context)
        currentRenderer = compatibleRenderers.find { it.getUniqueIdentifier() == uniqueIdentifier } ?: run {
            if (retryToFirstOnFailure && compatibleRenderers.isNotEmpty()) {
                val renderer = compatibleRenderers[0]
                Logger.w("Renderers", "Incompatible renderer $uniqueIdentifier will be replaced with ${renderer.getUniqueIdentifier()} (${renderer.getRendererName()})")
                renderer
            } else null
        }
    }

    /**
     * 获取当前的渲染器
     */
    fun getCurrentRenderer(): RendererInterface {
        if (!isInitialized) throw IllegalStateException("Uninitialized renderer!")
        return currentRenderer ?: throw IllegalStateException("Current renderer not set")
    }

    /**
     * 当前是否设置了渲染器
     */
    fun isCurrentRendererValid(): Boolean = isInitialized && currentRenderer != null
}