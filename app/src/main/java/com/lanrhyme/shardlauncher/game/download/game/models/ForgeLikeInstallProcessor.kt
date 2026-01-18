package com.lanrhyme.shardlauncher.game.download.game.models

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * Forge安装处理器模型
 * 用于执行安装器中的processor
 */
data class ForgeLikeInstallProcessor(
    private val jar: String,
    private val classpath: List<String>,
    private val args: List<String>,
    private val outputs: Map<String, String>?,
    private val sides: List<String>?
) {
    fun getJar(): String = jar
    fun getClasspath(): List<String> = classpath
    fun getArgs(): List<String> = args
    fun getOutputs(): Map<String, String> = outputs ?: emptyMap()
    fun getSides(): List<String> = sides ?: emptyList()
    
    fun isSide(side: String): Boolean = sides.isNullOrEmpty() || sides.contains(side)
    
    companion object {
        fun fromJson(json: JsonObject): ForgeLikeInstallProcessor {
            return ForgeLikeInstallProcessor(
                jar = json.get("jar")?.asString ?: "",
                classpath = json.get("classpath")?.asJsonArray?.map { it.asString } ?: emptyList(),
                args = json.get("args")?.asJsonArray?.map { it.asString } ?: emptyList(),
                outputs = json.get("outputs")?.asJsonObject?.let { obj ->
                    obj.entrySet().associate { (key, value) ->
                        key to value.asString
                    }
                },
                sides = json.get("sides")?.asJsonArray?.map { it.asString }
            )
        }
    }
}

fun JsonArray.toProcessorList(): List<ForgeLikeInstallProcessor> {
    return this.map { element ->
        ForgeLikeInstallProcessor.fromJson(element.asJsonObject)
    }
}
