package com.lanrhyme.shardlauncher.api

import com.lanrhyme.shardlauncher.model.mojang.MojangProfile
import com.lanrhyme.shardlauncher.model.version.VersionManifest
import retrofit2.http.GET
import retrofit2.http.Path

interface MojangApiService {
    @GET("users/profiles/minecraft/{username}")
    suspend fun getProfile(@Path("username") username: String): MojangProfile

    @GET("mc/game/version_manifest.json")
    suspend fun getVersionManifest(): VersionManifest
}
