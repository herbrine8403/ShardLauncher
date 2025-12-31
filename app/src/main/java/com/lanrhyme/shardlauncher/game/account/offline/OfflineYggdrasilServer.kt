/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.game.account.offline

import com.lanrhyme.shardlauncher.info.InfoDistributor
import com.lanrhyme.shardlauncher.path.PathManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object OfflineYggdrasilServer {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val characters = mutableMapOf<String, CharacterInfo>()

    @Serializable
    data class CharacterInfo(
        val name: String,
        val uuid: String,
        val skinHash: String?,
        val capeHash: String?
    )

    suspend fun start(port: Int = 0): Int {
        val engine = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/") {
                    call.respond(buildJsonObject {
                        put("meta", buildJsonObject {
                            put("serverName", InfoDistributor.LAUNCHER_NAME)
                            put("implementationName", "ShardOfflineYggdrasil")
                            put("implementationVersion", "1.0.0")
                        })
                        put("skinDomains", Json.parseToJsonElement("[\"localhost\"]"))
                        put("signaturePublickey", "none")
                    })
                }

                get("/sessionserver/session/minecraft/profile/{uuid}") {
                    val uuid = call.parameters["uuid"]?.replace("-", "") ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val info = characters.values.find { it.uuid.replace("-", "") == uuid }
                        ?: return@get call.respond(HttpStatusCode.NoContent)

                    call.respond(buildJsonObject {
                        put("id", info.uuid.replace("-", ""))
                        put("name", info.name)
                        put("properties", Json.parseToJsonElement("[${createTextureProperty(info)}]"))
                    })
                }

                get("/textures/{hash}") {
                    val hash = call.parameters["hash"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val skinFile = File(PathManager.DIR_ACCOUNT_SKIN, "$hash.png")
                    if (skinFile.exists()) {
                        call.respondFile(skinFile)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
        server = engine
        engine.start(wait = false)
        return engine.engine.resolvedConnectors().first().port
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun addCharacter(name: String, uuid: String, skinHash: String? = null, capeHash: String? = null) {
        characters[uuid] = CharacterInfo(name, uuid, skinHash, capeHash)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun createTextureProperty(info: CharacterInfo): String {
        val textureJson = buildJsonObject {
            put("timestamp", System.currentTimeMillis())
            put("profileId", info.uuid.replace("-", ""))
            put("profileName", info.name)
            put("textures", buildJsonObject {
                info.skinHash?.let {
                    put("SKIN", buildJsonObject {
                        put("url", "http://localhost/textures/$it")
                    })
                }
                info.capeHash?.let {
                    put("CAPE", buildJsonObject {
                        put("url", "http://localhost/textures/$it")
                    })
                }
            })
        }
        val value = Base64.encode(Json.encodeToString(textureJson).toByteArray())
        return "{\"name\":\"textures\",\"value\":\"$value\"}"
    }
}
