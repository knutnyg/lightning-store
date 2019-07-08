package xyz.nygaard

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import org.lightningj.lnd.wrapper.SynchronousLndAPI
import org.lightningj.lnd.wrapper.message.GetInfoRequest
import java.io.File


fun main() {
    val logger = LoggerFactory.getLogger("xyz.nygaard.lightnig-store")
    embeddedServer(Netty, 8080) {
        val objectMapper = ObjectMapper()
        val environment = objectMapper.readValue(File("src/main/resources/config.json"), Config::class.java)

        val syncApi = SynchronousLndAPI(
            environment.hostUrl,
            environment.hostPort,
            File(environment.tlscertPath),
            File(environment.macaroonPath)
        )
        routing {
            get("/status") {
                val request = GetInfoRequest()
                call.respondText(syncApi.getInfo(request).toJsonAsString(true))
            }
            get("/isAlive") {
                call.respondText("I'm alive! :)")

            }
            get("/isReady") {
                call.respondText("I'm ready! :)")
            }
        }
    }.start(wait = true)
}

data class Config(
    val hostUrl: String,
    val hostPort: Int,
    val macaroonPath: String,
    val tlscertPath: String
)
