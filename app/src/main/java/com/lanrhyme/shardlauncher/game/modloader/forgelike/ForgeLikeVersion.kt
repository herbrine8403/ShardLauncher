package com.lanrhyme.shardlauncher.game.modloader.forgelike

import com.lanrhyme.shardlauncher.game.addons.modloader.ModLoader

/**
 * ForgeLike版本基类（Forge和NeoForge的父类）
 */
abstract class ForgeLikeVersion(
    open val loaderName: String,
    open val forgeBuildVersion: ForgeBuildVersion,
    open val versionName: String,
    open val inherit: String,
    open val fileExtension: String,
    open val isLegacy: Boolean = false
) {
    val isNeoForge: Boolean
        get() = loaderName == ModLoader.NEOFORGE.displayName
}

/**
 * Forge版本实现
 */
data class ForgeVersion(
    override val versionName: String,
    val branch: String?,
    override val inherit: String,
    /** 发布时间，格式为"yyyy/MM/dd HH:mm" */
    val releaseTime: String,
    /** 文件的 MD5 或 SHA1 */
    val hash: String?,
    /** 是否为推荐版本 */
    val isRecommended: Boolean,
    /** 安装类型: installer、client、universal */
    val category: String,
    /** 用于下载的文件版本名 */
    val fileVersion: String,
    override val isLegacy: Boolean = false
) : ForgeLikeVersion(
    loaderName = ModLoader.FORGE.displayName,
    forgeBuildVersion = parseForgeVersion(versionName, branch, inherit),
    versionName = versionName,
    inherit = inherit,
    fileExtension = if (category == "installer") "jar" else "zip",
    isLegacy = isLegacy
)

/**
 * NeoForge版本实现
 */
data class NeoForgeVersion(
    override val versionName: String,
    override val inherit: String,
    override val isLegacy: Boolean = false
) : ForgeLikeVersion(
    loaderName = ModLoader.NEOFORGE.displayName,
    forgeBuildVersion = ForgeBuildVersion.parse(versionName),
    versionName = versionName,
    inherit = inherit,
    fileExtension = "jar",
    isLegacy = isLegacy
)

/**
 * 解析Forge版本
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/main/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModDownload.vb#L588-L598)
 */
private fun parseForgeVersion(version: String, branch: String?, inherit: String): ForgeBuildVersion {
    val specialVersions = listOf("11.15.1.2318", "11.15.1.1902", "11.15.1.1890")
    val modifiedBranch = when {
        version in specialVersions -> "1.8.9"
        branch == null && inherit == "1.7.10" && version.split(".")[3].toInt() >= 1300 -> "1.7.10"
        else -> branch
    }
    val fullVersion = version + (modifiedBranch?.let { "-$it" } ?: "")
    return ForgeBuildVersion.parse(fullVersion)
}
