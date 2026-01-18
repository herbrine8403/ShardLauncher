package com.lanrhyme.shardlauncher.utils.json

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type

/**
 * JSON解析工具扩展
 */

internal val GSON = Gson()

/**
 * 将字符串解析为JsonObject
 */
fun String.parseToJson(): JsonObject = JsonParser.parseString(this).asJsonObject

/**
 * 将字符串解析为指定类型对象
 */
fun <T> String.parseTo(type: Type): T = GSON.fromJson(this, type)

inline fun <reified T> String.parseTo(): T = GSON.fromJson(this, object : TypeToken<T>() {}.type)

/**
 * 将文件内容解析为JsonObject
 */
fun File.readAsJsonObject(): JsonObject = this.readText().parseToJson()

/**
 * 将JsonObject合并到当前对象
 */
fun JsonObject.merge(other: JsonObject) {
    other.entrySet().forEach { (key, value) ->
        this.add(key, value)
    }
}

/**
 * 安全获取JsonObject属性
 */
fun JsonObject.getString(key: String): String? = this.get(key)?.takeIf { !it.isJsonNull }?.asString

fun JsonObject.getInt(key: String): Int? = this.get(key)?.takeIf { !it.isJsonNull }?.asInt

fun JsonObject.getBoolean(key: String): Boolean? = this.get(key)?.takeIf { !it.isJsonNull }?.asBoolean

fun JsonObject.getJsonObject(key: String): JsonObject? = this.get(key)?.takeIf { !it.isJsonNull }?.asJsonObject

fun JsonObject.getJsonArray(key: String) = this.get(key)?.takeIf { !it.isJsonNull }?.asJsonArray
