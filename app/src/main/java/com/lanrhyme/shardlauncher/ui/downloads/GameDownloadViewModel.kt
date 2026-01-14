package com.lanrhyme.shardlauncher.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lanrhyme.shardlauncher.api.ApiClient
import com.lanrhyme.shardlauncher.model.BmclapiManifest
import com.lanrhyme.shardlauncher.model.version.VersionManifest
import com.lanrhyme.shardlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class GameDownloadViewModel : ViewModel() {

    private val _versions = MutableStateFlow<List<BmclapiManifest.Version>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedVersionTypes = MutableStateFlow(setOf(VersionType.Release))
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Cache file
    private val cacheFile = File(com.lanrhyme.shardlauncher.path.PathManager.DIR_CACHE, "version_manifest.json")
    private val cacheValidityDuration = TimeUnit.HOURS.toMillis(24) // 24 hours

    // Cache
    private var cachedVersions: List<BmclapiManifest.Version>? = null
    private var lastSourceWasBmclapi: Boolean? = null

    val searchQuery = _searchQuery.asStateFlow()
    val selectedVersionTypes = _selectedVersionTypes.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val errorMessage = _errorMessage.asStateFlow()

    val filteredVersions: StateFlow<List<BmclapiManifest.Version>> = combine(
        _versions,
        _searchQuery,
        _selectedVersionTypes
    ) { versions, query, types ->
        versions.filter { version ->
            val typeMatches = when (version.type) {
                "release" -> VersionType.Release in types
                "snapshot" -> VersionType.Snapshot in types
                "old_alpha", "old_beta" -> VersionType.Ancient in types
                else -> {
                    // Check if it's April Fools version (usually starts with specific patterns)
                    val isAprilFools = version.id.startsWith("2.0") ||
                                       version.id.startsWith("15w14a") ||
                                       version.id.startsWith("1.RV-Pre1")
                    if (isAprilFools) {
                        VersionType.AprilFools in types
                    } else {
                        false
                    }
                }
            }
            val queryMatches = version.id.contains(query, ignoreCase = true)
            typeMatches && queryMatches
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleVersionType(type: VersionType) {
        val currentTypes = _selectedVersionTypes.value.toMutableSet()
        if (type in currentTypes) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }
        _selectedVersionTypes.value = currentTypes
    }

    fun loadVersions(forceRefresh: Boolean = false) {
        val useBmclapi = com.lanrhyme.shardlauncher.settings.AllSettings.fileDownloadSource.state == com.lanrhyme.shardlauncher.settings.enums.MirrorSourceType.MIRROR_FIRST

        // Check if we can use cache
        if (!forceRefresh && cachedVersions != null && lastSourceWasBmclapi == useBmclapi) {
            _versions.value = cachedVersions!!
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Try to load from cache file first
            if (!forceRefresh && cacheFile.exists() && cacheFile.isFile) {
                val isCacheValid = cacheFile.lastModified() + cacheValidityDuration > System.currentTimeMillis()
                if (isCacheValid) {
                    try {
                        val cachedManifest = withContext(Dispatchers.IO) {
                            cacheFile.readText()
                        }
                        val manifest = com.google.gson.Gson().fromJson(cachedManifest, VersionManifest::class.java)
                        val versions = manifest.versions.map { version ->
                            BmclapiManifest.Version(
                                id = version.id,
                                type = version.type,
                                url = version.url,
                                time = version.time,
                                releaseTime = version.releaseTime
                            )
                        }
                        _versions.value = versions
                        cachedVersions = versions
                        lastSourceWasBmclapi = useBmclapi
                        _isLoading.value = false
                        return@launch
                    } catch (e: Exception) {
                        Logger.e("GameDownloadViewModel", "Failed to load version manifest from cache", e)
                    }
                }
            }

            try {
                val versionsFromApi = if (useBmclapi) {
                    loadFromBmclapi()
                } else {
                    loadFromMojang()
                }
                _versions.value = versionsFromApi
                // Update cache
                cachedVersions = versionsFromApi
                lastSourceWasBmclapi = useBmclapi
            } catch (e: Exception) {
                Logger.e("GameDownloadViewModel", "Failed to load versions", e)
                _errorMessage.value = e.message ?: "加载版本列表失败"

                // Try to use cache as fallback
                if (cacheFile.exists()) {
                    try {
                        val cachedManifest = withContext(Dispatchers.IO) {
                            cacheFile.readText()
                        }
                        val manifest = com.google.gson.Gson().fromJson(cachedManifest, VersionManifest::class.java)
                        val versions = manifest.versions.map { version ->
                            BmclapiManifest.Version(
                                id = version.id,
                                type = version.type,
                                url = version.url,
                                time = version.time,
                                releaseTime = version.releaseTime
                            )
                        }
                        _versions.value = versions
                        cachedVersions = versions
                        lastSourceWasBmclapi = useBmclapi
                        _errorMessage.value = "使用缓存数据"
                    } catch (cacheError: Exception) {
                        Logger.e("GameDownloadViewModel", "Failed to load version manifest from cache as fallback", cacheError)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadFromBmclapi(): List<BmclapiManifest.Version> {
        return try {
            val manifest = ApiClient.bmclapiService.getGameVersionManifest()
            saveToCache(manifest)
            manifest.versions
        } catch (e: Exception) {
            Logger.e("GameDownloadViewModel", "Failed to load versions from BMCLAPI, trying Mojang API", e)
            // Fallback to Mojang API
            loadFromMojang()
        }
    }

    private suspend fun loadFromMojang(): List<BmclapiManifest.Version> {
        val manifest = ApiClient.mojangApiService.getVersionManifest()
        saveToCache(manifest)
        return manifest.versions.map { version ->
            BmclapiManifest.Version(
                id = version.id,
                type = version.type,
                url = version.url,
                time = version.time,
                releaseTime = version.releaseTime
            )
        }
    }

    private suspend fun saveToCache(manifest: VersionManifest) {
        withContext(Dispatchers.IO) {
            try {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(com.google.gson.Gson().toJson(manifest))
            } catch (e: Exception) {
                Logger.e("GameDownloadViewModel", "Failed to save version manifest to cache", e)
            }
        }
    }

    private suspend fun saveToCache(manifest: BmclapiManifest) {
        withContext(Dispatchers.IO) {
            try {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(com.google.gson.Gson().toJson(manifest))
            } catch (e: Exception) {
                Logger.e("GameDownloadViewModel", "Failed to save version manifest to cache", e)
            }
        }
    }
}
