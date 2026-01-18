/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.account.wardrobe

/**
 * Skin model enumeration
 */
enum class SkinModelType(val string: String, val targetParity: Int, val modelType: String) {
    /** Undefined */
    NONE("none", -1, ""),
    /** Wide arms (Steve) */
    STEVE("wide", 0, "classic"),
    /** Slim arms (Alex) */
    ALEX("slim", 1, "slim")
}
