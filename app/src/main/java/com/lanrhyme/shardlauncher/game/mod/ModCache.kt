package com.lanrhyme.shardlauncher.game.mod

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lanrhyme.shardlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 模组信息缓存管理器
 */
object ModCache {
    private const val CACHE_FILE_NAME = "mod_cache.json"
    private const val CACHE_VERSION = 1
    private val gson = Gson()
    
    data class CacheEntry(
        val sha1: String,
        val projectInfo: ModProject?,
        val remoteFile: ModFile?,
        val timestamp: Long,
        val loadSuccess: Boolean
    )
    
    data class CacheData(
        val version: Int,
        val entries: MutableMap<String, CacheEntry>
    )
    
    private var cacheData: CacheData? = null
    private lateinit var cacheFile: File
    
    /**
     * 初始化缓存
     */
    fun init(context: Context) {
        cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        loadCache()
    }
    
    /**
     * 从文件加载缓存
     */
    private fun loadCache() {
        try {
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val type = object : TypeToken<CacheData>() {}.type
                val loadedCache = gson.fromJson<CacheData>(json, type)
                
                // 检查缓存版本
                if (loadedCache.version == CACHE_VERSION) {
                    cacheData = loadedCache
                } else {
                    // 版本不匹配，重新创建缓存
                    cacheData = CacheData(CACHE_VERSION, mutableMapOf())
                }
            } else {
                cacheData = CacheData(CACHE_VERSION, mutableMapOf())
            }
        } catch (e: Exception) {
            Logger.e("ModCache", "Failed to load cache", e)
            cacheData = CacheData(CACHE_VERSION, mutableMapOf())
        }
    }
    
    /**
     * 保存缓存到文件
     */
    private suspend fun saveCache() = withContext(Dispatchers.IO) {
        try {
            cacheData?.let { cache ->
                val json = gson.toJson(cache)
                cacheFile.writeText(json)
            }
        } catch (e: Exception) {
            Logger.e("ModCache", "Failed to save cache", e)
        }
    }
    
    /**
     * 获取缓存的模组信息
     */
    fun getCachedModInfo(sha1: String): CacheEntry? {
        return cacheData?.entries?.get(sha1)
    }
    
    /**
     * 缓存模组信息
     */
    suspend fun cacheModInfo(
        sha1: String,
        projectInfo: ModProject?,
        remoteFile: ModFile?,
        loadSuccess: Boolean
    ) {
        val entry = CacheEntry(
            sha1 = sha1,
            projectInfo = projectInfo,
            remoteFile = remoteFile,
            timestamp = System.currentTimeMillis(),
            loadSuccess = loadSuccess
        )
        
        cacheData?.entries?.put(sha1, entry)
        saveCache()
    }
    
    /**
     * 检查缓存是否过期（24小时）
     */
    fun isCacheExpired(entry: CacheEntry): Boolean {
        val now = System.currentTimeMillis()
        val cacheAge = now - entry.timestamp
        return cacheAge > 24 * 60 * 60 * 1000 // 24小时
    }
    
    /**
     * 清理过期缓存
     */
    suspend fun cleanExpiredCache() {
        cacheData?.entries?.let { entries ->
            val iterator = entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (isCacheExpired(entry.value)) {
                    iterator.remove()
                }
            }
            saveCache()
        }
    }
    
    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache() {
        cacheData?.entries?.clear()
        saveCache()
    }
}