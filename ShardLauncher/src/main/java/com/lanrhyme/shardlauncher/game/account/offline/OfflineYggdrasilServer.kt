/*
 * Shard Launcher
 * Simplified Offline Yggdrasil Server
 * TODO: Full implementation with authlibinjector support
 */

package com.lanrhyme.shardlauncher.game.account.offline

import com.lanrhyme.shardlauncher.game.account.Account
import com.lanrhyme.shardlauncher.info.InfoDistributor
import com.lanrhyme.shardlauncher.utils.logging.Logger.lInfo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 离线账号 Yggdrasil 服务器（简化版）
 * 仅支持本地皮肤加载，暂不支持第三方认证
 */
class OfflineYggdrasilServer(
    private val port: Int = 0,
    val serverName: String = "${InfoDistributor.LAUNCHER_IDENTIFIER}_Offline",
    val implementationName: String = InfoDistributor.LAUNCHER_SHORT_NAME,
    val implementationVersion: String = "1.0"
) {
    private val charactersByUuid = ConcurrentHashMap<String, Character>()
    private val charactersByName = ConcurrentHashMap<String, Character>()
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.genKeyPair()

    private val serverStartedLatch = CountDownLatch(1)
    private var isServerRunning = false
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start() {
        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    encodeDefaults = true
                })
            }

            routing {
                get("/") {
                    call.respondText(root(), ContentType.Application.Json)
                }
                get("/sessionserver/session/minecraft/profile/{uuid}") {
                    val uuid = call.parameters["uuid"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val character = charactersByUuid[uuid.lowercase()]
                        ?: return@get call.respond(HttpStatusCode.NoContent)
                    call.respondText(character.toProfileJson(), ContentType.Application.Json)
                }
                get("/textures/{hash}") {
                    val hash = call.parameters["hash"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val match = charactersByUuid.values.firstNotNullOfOrNull { char ->
                        char.skinBytes?.takeIf { char.skinHash == hash }
                    }
                    if (match != null) {
                        call.respondBytes(match, ContentType.Image.PNG)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }.apply {
            monitor.subscribe(io.ktor.server.application.ApplicationStarted) {
                serverStartedLatch.countDown()
            }
        }

        server?.start(wait = false)
        if (serverStartedLatch.await(10, TimeUnit.SECONDS)) {
            isServerRunning = true
        }
    }

    fun stop() {
        isServerRunning = false
        server?.stop(1000, 5000)
    }

    fun getPort(): Int? {
        if (!isServerRunning) return null
        return runBlocking {
            server?.engine?.resolvedConnectors()?.firstOrNull()?.port
        }
    }

    fun addCharacter(account: Account) {
        val skinFile = account.getSkinFile()
        val skinBytes = skinFile?.takeIf { it.exists() }?.readBytes()
        val skinHash = skinBytes?.let { 
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(it)
                .joinToString("") { "%02x".format(it) }
        }

        val character = Character(
            uuid = account.profileId.replace("-", ""),
            name = account.username,
            skinHash = skinHash,
            skinBytes = skinBytes
        )

        charactersByUuid[character.uuid.lowercase()] = character
        charactersByName[character.name.lowercase()] = character
        lInfo("Added character ${character.name} (${character.uuid})")
    }

    private fun PublicKey.toPEM(): String {
        val base64Key = Base64.getEncoder().encodeToString(encoded)
        return "-----BEGIN PUBLIC KEY-----\n$base64Key\n-----END PUBLIC KEY-----"
    }

    private fun root(): String = buildJsonObject {
        put("skinDomains", """["127.0.0.1","localhost"]""")
        put("meta", buildJsonObject {
            put("serverName", JsonPrimitive(serverName))
            put("implementationName", JsonPrimitive(implementationName))
            put("implementationVersion", JsonPrimitive(implementationVersion))
        })
        put("signaturePublickey", JsonPrimitive(keyPair.public.toPEM()))
    }.toString()

    data class Character(
        val uuid: String,
        val name: String,
        val skinHash: String? = null,
        val skinBytes: ByteArray? = null
    ) {
        fun toProfileJson(): String = buildJsonObject {
            put("id", JsonPrimitive(uuid))
            put("name", JsonPrimitive(name))
            // Simplified: no textures property for now
        }.toString()
    }
}

// Skin model type enum
enum class SkinModelType {
    STEVE,
    ALEX
}

// Loaded skin data class
data class LoadedSkin(
    val skinHash: String?,
    val skinBytes: ByteArray?,
    val model: SkinModelType = SkinModelType.STEVE,
    val capeHash: String? = null,
    val capeBytes: ByteArray? = null
)