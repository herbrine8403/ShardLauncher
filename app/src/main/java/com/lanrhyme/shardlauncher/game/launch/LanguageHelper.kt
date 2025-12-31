/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.launch

import com.lanrhyme.shardlauncher.utils.getSystemLanguage
import com.lanrhyme.shardlauncher.utils.version.isLowerOrEqualVer

private fun getOlderLanguage(lang: String): String {
    val underscoreIndex = lang.indexOf('_')
    return if (underscoreIndex != -1) {
        val builder = StringBuilder(lang.substring(0, underscoreIndex + 1))
        builder.append(lang.substring(underscoreIndex + 1).uppercase())
        builder.toString()
    } else lang
}

private fun getLanguage(versionId: String): String {
    val lang = getSystemLanguage()
    return if (versionId.isLowerOrEqualVer("1.10.2", "16w32a")) {
        getOlderLanguage(lang)
    } else {
        lang
    }
}

/**
 * Load system language into MCOptions
 */
fun MCOptions.loadLanguage(versionId: String) {
    if (!containsKey("lang")) {
        val lang = getLanguage(versionId)
        set("lang", lang)
    }
}
