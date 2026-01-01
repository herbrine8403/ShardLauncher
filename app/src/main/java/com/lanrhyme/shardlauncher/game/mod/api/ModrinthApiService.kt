package com.lanrhyme.shardlauncher.game.mod.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lanrhyme.shardlauncher.utils.network.downloadAndParseJson
import com.lanrhyme.shardlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Modrinth API 服务
 */
object ModrinthApiService {
    private const val MODRINTH_API = "https://api.modrinth.com/v2"
    private val gson = Gson()

    /**
     * 通过文件SHA1获取版本信息
     */
    suspend fun getVersionByFileSha1(sha1: String): ModrinthVersion? = withContext(Dispatchers.IO) {
        withRetry("ModrinthApi:getVersionByFileSha1", maxRetries = 2) {
            try {
                val url = "$MODRINTH_API/version_file/$sha1?algorithm=sha1"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    gson.fromJson(response, ModrinthVersion::class.java)
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    null // 文件不存在于Modrinth
                } else {
                    throw IOException("HTTP $responseCode")
                }
            } catch (e: Exception) {
                if (e is IOException && e.message?.contains("404") == true) {
                    null
                } else {
                    throw e
                }
            }
        }
    }

    /**
     * 通过项目ID获取项目信息
     */
    suspend fun getProjectById(projectId: String): ModrinthProject? = withContext(Dispatchers.IO) {
        withRetry("ModrinthApi:getProjectById", maxRetries = 2) {
            try {
                val url = "$MODRINTH_API/project/$projectId"
                downloadAndParseJson<ModrinthProject>(url, classOfT = ModrinthProject::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Modrinth版本信息
 */
data class ModrinthVersion(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("project_id")
    val projectId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("version_number")
    val versionNumber: String,
    
    @SerializedName("loaders")
    val loaders: List<String>,
    
    @SerializedName("game_versions")
    val gameVersions: List<String>,
    
    @SerializedName("date_published")
    val datePublished: String,
    
    @SerializedName("files")
    val files: List<ModrinthFile>
)

/**
 * Modrinth文件信息
 */
data class ModrinthFile(
    @SerializedName("hashes")
    val hashes: ModrinthHashes,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("filename")
    val filename: String,
    
    @SerializedName("primary")
    val primary: Boolean,
    
    @SerializedName("size")
    val size: Long
)

/**
 * Modrinth文件哈希
 */
data class ModrinthHashes(
    @SerializedName("sha1")
    val sha1: String,
    
    @SerializedName("sha512")
    val sha512: String
)

/**
 * Modrinth项目信息
 */
data class ModrinthProject(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("slug")
    val slug: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("icon_url")
    val iconUrl: String?,
    
    @SerializedName("downloads")
    val downloads: Long,
    
    @SerializedName("categories")
    val categories: List<String>,
    
    @SerializedName("loaders")
    val loaders: List<String>,
    
    @SerializedName("game_versions")
    val gameVersions: List<String>
)