package com.lanrhyme.shardlauncher.game.modloader.forgelike.forge

import com.google.gson.annotations.SerializedName

/**
 * Forge版本令牌（从BMCLAPI获取）
 */
data class ForgeVersionToken(
    @SerializedName("branch")
    val branch: String? = null,
    @SerializedName("version")
    val version: String,
    @SerializedName("modified")
    val modified: String,
    @SerializedName("files")
    val files: List<ForgeFile>
) {
    data class ForgeFile(
        @SerializedName("category")
        val category: String,
        @SerializedName("format")
        val format: String,
        @SerializedName("hash")
        val hash: String
    )
}

fun ForgeVersionToken.ForgeFile.isInstallerJar() = category == "installer" && format == "jar"
fun ForgeVersionToken.ForgeFile.isUniversalZip() = category == "universal" && format == "zip"
fun ForgeVersionToken.ForgeFile.isClientZip() = category == "client" && format == "zip"
