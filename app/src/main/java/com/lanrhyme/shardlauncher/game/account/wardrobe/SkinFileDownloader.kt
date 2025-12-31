/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.account.wardrobe

import android.util.Base64
import com.google.gson.JsonObject
import com.lanrhyme.shardlauncher.utils.GSON
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class SkinFileDownloader {
    private val mClient = OkHttpClient()

    /**
     * Decode Base64 string to UTF-8 string
     */
    private fun decodeBase64(rawValue: String): String {
        val decodedBytes = Base64.decode(rawValue, Base64.DEFAULT)
        return String(decodedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Attempt to download Yggdrasil skin
     */
    @Throws(Exception::class)
    suspend fun yggdrasil(
        url: String,
        skinFile: File,
        uuid: String,
        changeSkinModel: (SkinModelType) -> Unit
    ) {
        val profileUrl = "${url.removeSuffix("/")}/session/minecraft/profile/$uuid"
        val request = Request.Builder().url(profileUrl).build()
        
        val profileJson = mClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty body")
        }

        val profileObject = GSON.fromJson(profileJson, JsonObject::class.java)
        val properties = profileObject.get("properties").asJsonArray
        val rawValue = properties.get(0).asJsonObject.get("value").asString

        val value = decodeBase64(rawValue)

        val valueObject = GSON.fromJson(value, JsonObject::class.java)
        val skinObject = valueObject.get("textures").asJsonObject.get("SKIN").asJsonObject
        val skinUrl = skinObject.get("url").asString

        val skinModelType = runCatching {
            skinObject.takeIf {
                it.has("metadata")
            }?.get("metadata")?.let {
                // If metadata exists, it's usually Alex (slim)
                SkinModelType.ALEX
            } ?: SkinModelType.STEVE
        }.getOrElse {
            SkinModelType.NONE
        }

        downloadSkin(skinUrl, skinFile)
        changeSkinModel(skinModelType)
    }

    private fun downloadSkin(url: String, skinFile: File) {
        skinFile.parentFile?.apply {
            if (!exists()) mkdirs()
        }

        val request = Request.Builder()
            .url(url)
            .build()

        mClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Unexpected code $response")
            }

            try {
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(skinFile).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
