/*
 * Shard Launcher  
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.version.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Minecraft version JSON structure
 * Based on official Minecraft launcher format
 */
data class MinecraftVersionJson(
    val id: String,
    val type: String?,
    val mainClass: String,
    val minecraftArguments: String?, // Legacy format (pre-1.13)
    val arguments: Arguments?, // Modern format (1.13+)
    val libraries: List<Library>,
    val assetIndex: AssetIndex,
    val javaVersion: JavaVersion? = null
) {
    data class Arguments(
        val game: List<JsonElement>?,
        val jvm: List<JsonElement>?
    )
    
    data class Library(
        val name: String,
        val downloads: Downloads?,
        val rules: List<Rule>? = null,
        val natives: Map<String, String>? = null,
        val url: String? = null,
        val sha1: String? = null,
        val size: Long? = null
    ) {
        data class Downloads(
            val artifact: Artifact?,
            val classifiers: Map<String, Artifact>? = null
        )
        
        data class Artifact(
            val path: String,
            val url: String?,
            val sha1: String?,
            val size: Long?
        )
        
        data class Rule(
            val action: String,
            val os: Os? = null,
            val features: Features? = null
        )
        
        data class Os(
            val name: String?,
            val arch: String?
        )
        
        data class Features(
            @SerializedName("is_demo_user")
            val isDemoUser: Boolean? = null,
            @SerializedName("has_custom_resolution")
            val hasCustomResolution: Boolean? = null
        )
        
        /**
         * Check if this is a native library
         */
        fun isNative(): Boolean = natives != null && checkRules(rules)
    }
    
    data class AssetIndex(
        val id: String,
        val url: String?,
        val sha1: String?,
        val size: Long?,
        val totalSize: Long?
    )
    
    data class JavaVersion(
        val component: String?,
        val majorVersion: Int?
    )
    
    companion object {
        /**
         * Check if rules allow this library to be used
         * [Modified from PojavLauncher]
         */
        fun checkRules(rules: List<Library.Rule>?): Boolean {
            if (rules == null || rules.isEmpty()) return true
            
            for (rule in rules) {
                if (rule.action == "allow" && rule.os?.name == "osx") {
                    return false
                }
            }
            return true
        }
    }
}

/**
 * Extension to check rules for Library
 */
fun MinecraftVersionJson.Library.checkRules(): Boolean {
    return MinecraftVersionJson.checkRules(this.rules)
}
