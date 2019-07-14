package xyz.nygaard

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.lightningj.lnd.wrapper.MacaroonContext
import org.lightningj.lnd.wrapper.SynchronousLndAPI
import org.lightningj.lnd.wrapper.message.GetInfoRequest
import org.slf4j.LoggerFactory
import xyz.nygaard.db.Database
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.xml.bind.DatatypeConverter

val log = LoggerFactory.getLogger("Bootstrap")

fun main() {
    embeddedServer(Netty, System.getenv("PORT").toInt()) {
        val environment = Config(
                hostUrl = System.getenv("lshost"),
                hostPort = System.getenv("lsport").toInt(),
                macaroon = Base64.getDecoder().decode(System.getenv("macaroon")),
                cert = String(Base64.getDecoder().decode(System.getenv("tls_cert")))
        )

        //TODO: Denne må bort...
        install(CORS) {
            anyHost()
        }

        val database = Database()

        val cert = GrpcSslContexts.configure(io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder.forClient(), SslProvider.OPENSSL)
                .trustManager(ByteArrayInputStream(environment.cert.toByteArray()))
                .build()

        val macaroon = EnvironmentMacaroonContext(currentMacaroonData = environment.macaroon)

        val syncApi = SynchronousLndAPI(
                environment.hostUrl,
                environment.hostPort,
                cert,
                macaroon)

        routing {
            get("/status") {
                log.info("Requesting Status")
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
        val macaroon: ByteArray,
        val cert: String
)

class EnvironmentMacaroonContext(var currentMacaroonData: ByteArray) : MacaroonContext {

    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(currentMacaroonData)
    }
}
