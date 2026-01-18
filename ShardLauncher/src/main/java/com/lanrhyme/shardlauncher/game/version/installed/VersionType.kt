/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.version.installed

/**
 * Version type enumeration
 */
enum class VersionType {
    RELEASE,
    SNAPSHOT,
    BETA,
    ALPHA,
    VANILLA,
    MODLOADERS,
    UNKNOWN;
    
    companion object {
        fun fromString(type: String?): VersionType {
            return when (type?.lowercase()) {
                "release" -> RELEASE
                "snapshot" -> SNAPSHOT
                "beta" -> BETA
                "alpha" -> ALPHA
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Extension function to get version type from VersionInfo
 */
fun VersionInfo?.getVersionType(): VersionType {
    // Determine if it's vanilla or has mod loaders
    return if (this?.loaderInfo != null) {
        VersionType.MODLOADERS
    } else {
        VersionType.VANILLA
    }
}