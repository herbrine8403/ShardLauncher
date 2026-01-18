/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.resource

import android.content.Context
import com.lanrhyme.shardlauncher.game.multirt.RuntimesManager
import com.lanrhyme.shardlauncher.game.multirt.RuntimeInstaller
import com.lanrhyme.shardlauncher.game.renderer.RendererInstaller
import com.lanrhyme.shardlauncher.game.renderer.Renderers
import com.lanrhyme.shardlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resource manager for checking and installing required resources
 */
object ResourceManager {
    
    /**
     * Resource check result
     */
    data class ResourceCheckResult(
        val hasJavaRuntime: Boolean,
        val hasRenderers: Boolean,
        val missingResources: List<String>,
        val recommendedActions: List<String>
    )

    /**
     * Check if all required resources are available
     */
    suspend fun checkResources(context: Context): ResourceCheckResult = withContext(Dispatchers.IO) {
        val missingResources = mutableListOf<String>()
        val recommendedActions = mutableListOf<String>()
        
        // Check Java runtimes
        val runtimes = RuntimesManager.getRuntimes()
        val hasJavaRuntime = runtimes.isNotEmpty()
        
        if (!hasJavaRuntime) {
            missingResources.add("Java 运行时")
            recommendedActions.add("安装内置 Java 运行时环境")
        }
        
        // Check renderers (they're always available since bundled in jniLibs)
        val bundledRenderers = RendererInstaller.getBundledRenderers()
        val hasRenderers = bundledRenderers.isNotEmpty()
        
        if (!hasRenderers) {
            missingResources.add("渲染器库")
            recommendedActions.add("渲染器库已内置，无需额外安装")
        }
        
        // Check if we have basic Java 8 runtime for compatibility
        val hasJava8 = runtimes.any { it.javaVersion == 8 }
        if (hasJavaRuntime && !hasJava8) {
            recommendedActions.add("建议安装 Java 8 运行时以获得最佳兼容性")
        }
        
        // Check if we have Java 17 for modern versions
        val hasJava17 = runtimes.any { it.javaVersion >= 17 }
        if (hasJavaRuntime && !hasJava17) {
            recommendedActions.add("建议安装 Java 17+ 运行时以支持现代 Minecraft 版本")
        }
        
        ResourceCheckResult(
            hasJavaRuntime = hasJavaRuntime,
            hasRenderers = hasRenderers,
            missingResources = missingResources,
            recommendedActions = recommendedActions
        )
    }

    /**
     * Install essential resources automatically
     */
    suspend fun installEssentialResources(
        context: Context,
        onProgress: (progress: Int, message: String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "检查内置资源...")
            
            val bundledRuntimes = RuntimeInstaller.getBundledRuntimes(context)
            val installedRenderers = RendererInstaller.getInstalledRenderers()
            
            if (bundledRuntimes.isEmpty()) {
                return@withContext Result.failure(Exception("未找到兼容的内置运行时"))
            }
            
            var currentProgress = 10
            var installedCount = 0
            
            // Install Java 8 runtime first (most compatible)
            val java8Runtime = bundledRuntimes.find { it.javaVersion == 8 }
                ?: bundledRuntimes.first()
            
            val existingRuntimes = RuntimesManager.getRuntimes()
            if (!existingRuntimes.any { it.name == java8Runtime.name }) {
                onProgress(currentProgress, "安装 ${java8Runtime.displayName}...")
                
                RuntimeInstaller.installBundledRuntime(
                    context = context,
                    runtime = java8Runtime,
                    onProgress = { progress, message ->
                        val adjustedProgress = currentProgress + (progress * 80 / 100)
                        onProgress(adjustedProgress, message)
                    }
                ).onFailure { error ->
                    Logger.e("ResourceManager", "Failed to install Java runtime", error)
                    return@withContext Result.failure(error)
                }
                
                installedCount++
            }
            
            onProgress(100, "基础资源安装完成 (安装了 $installedCount 个资源)")
            Logger.i("ResourceManager", "Successfully installed essential resources")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Logger.e("ResourceManager", "Failed to install essential resources", e)
            Result.failure(e)
        }
    }

    /**
     * Install recommended resources for better performance
     */
    suspend fun installRecommendedResources(
        context: Context,
        onProgress: (progress: Int, message: String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "安装推荐资源...")
            
            val bundledRuntimes = RuntimeInstaller.getBundledRuntimes(context)
            val existingRuntimes = RuntimesManager.getRuntimes()
            
            var currentProgress = 10
            var installedCount = 0
            val totalItems = 1 // Java 17
            val stepProgress = 80 / totalItems
            
            // Install Java 17 if not present
            val hasJava17 = existingRuntimes.any { it.javaVersion >= 17 }
            
            if (!hasJava17) {
                val java17Runtime = bundledRuntimes.find { it.javaVersion == 17 }
                if (java17Runtime != null) {
                    onProgress(currentProgress, "安装 ${java17Runtime.displayName}...")
                    
                    RuntimeInstaller.installBundledRuntime(
                        context = context,
                        runtime = java17Runtime,
                        onProgress = { progress, message ->
                            val adjustedProgress = currentProgress + (progress * stepProgress / 100)
                            onProgress(adjustedProgress, message)
                        }
                    ).onSuccess {
                        installedCount++
                    }
                }
            }
            
            onProgress(100, "推荐资源安装完成 (安装了 $installedCount 个资源)")
            Logger.i("ResourceManager", "Successfully installed $installedCount recommended resources")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Logger.e("ResourceManager", "Failed to install recommended resources", e)
            Result.failure(e)
        }
    }

    /**
     * Get resource status summary
     */
    fun getResourceStatusSummary(context: Context): String {
        val runtimes = RuntimesManager.getRuntimes()
        val bundledRenderers = RendererInstaller.getBundledRenderers()
        
        return buildString {
            appendLine("=== 资源状态摘要 ===")
            appendLine("Java 运行时: ${runtimes.size} 个")
            runtimes.forEach { runtime ->
                appendLine("  - ${runtime.name} (Java ${runtime.javaVersion})")
            }
            appendLine("渲染器库: ${bundledRenderers.size} 个 (已内置)")
            bundledRenderers.forEach { renderer ->
                appendLine("  - ${renderer.displayName}")
            }
            
            if (runtimes.isEmpty()) {
                appendLine()
                appendLine("⚠️ 缺少 Java 运行时，请安装内置运行时")
            } else {
                appendLine()
                appendLine("✅ 所有必要资源已就绪")
            }
        }
    }
}