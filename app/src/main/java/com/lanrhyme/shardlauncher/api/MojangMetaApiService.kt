package com.lanrhyme.shardlauncher.api

import com.lanrhyme.shardlauncher.model.version.VersionManifest
import retrofit2.http.GET

interface MojangMetaApiService {
    @GET("mc/game/version_manifest.json")
    suspend fun getVersionManifest(): VersionManifest
}