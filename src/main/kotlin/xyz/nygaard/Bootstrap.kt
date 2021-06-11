package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.lightningj.lnd.wrapper.MacaroonContext
import org.slf4j.LoggerFactory
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.invoice.registerInvoiceApi
import java.util.*
import javax.xml.bind.DatatypeConverter

val log = LoggerFactory.getLogger("Bootstrap")

fun main() {
    embeddedServer(Netty, port = 8000, host = "localhost") {

        val environment = Config(
            hostUrl = System.getenv("LS_HOST_URL"),
            hostPort = System.getenv("LS_HOST_PORT").toInt(),
            readOnlyMacaroon = System.getenv("LS_READONLY_MACAROON"),
            invoiceMacaroon = System.getenv("LS_INVOICES_MACAROON"),
            cert = System.getenv("LS_TLS_CERT"),
            mocks = false
        )

        val database = Database("jdbc:postgresql://localhost:5432/knut")
        val lndClient = LndClient(environment)
        val invoiceService = InvoiceService(database = database, lndClient = lndClient)

        installContentNegotiation()
        install(XForwardedHeaderSupport)
        install(CORS) {
            method(HttpMethod.Options)
            method(HttpMethod.Post)
            method(HttpMethod.Get)
            header(HttpHeaders.Authorization)
            header(HttpHeaders.AccessControlAllowOrigin)
            header(HttpHeaders.ContentType)
            allowSameOrigin = true
            host("localhost:8080", listOf("http", "https")) // frontendHost might be "*"
            log.info("CORS enabled for $hosts")
        }
        install(CallLogging)
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
            registerSelftestApi(lndClient)
            registerInvoiceApi(invoiceService)
        }
    }.start(wait = true)
}

fun Routing.registerSelftestApi(lndClient: LndApiWrapper) {
    get("/nodeInfo") {
        log.info("Requesting Status")
        call.respond(lndClient.getInfo())
    }
    get("/isAlive") {
        call.respondText("I'm alive! :)")
    }
}

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

data class CreateInvoiceRequest(val memo: String)
data class CreateInvoiceResponse(
    val id: String,
    val memo: String?,
    val rhash: String,
    val paymentRequest: String
)


data class Config(
    val hostUrl: String,
    val hostPort: Int,
    val readOnlyMacaroon: String,
    val invoiceMacaroon: String,
    val cert: String,
    val mocks: Boolean
)

class EnvironmentMacaroonContext(var currentMacaroonData: String) : MacaroonContext {
    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(Base64.getDecoder().decode(currentMacaroonData))
    }
}
