package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.lightningj.lnd.wrapper.MacaroonContext
import org.slf4j.LoggerFactory
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.invoice.InvoiceService
import java.util.Base64
import java.util.UUID
import javax.xml.bind.DatatypeConverter

val log = LoggerFactory.getLogger("Bootstrap")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun main() {
    embeddedServer(Netty, System.getenv("PORT").toInt()) {
        val environment = Config(
            hostUrl = System.getenv("lshost"),
            hostPort = System.getenv("lsport").toInt(),
            readOnlyMacaroon = System.getenv("readonly_macaroon"),
            invoiceMacaroon = System.getenv("invoice_macaroon"),
            cert = System.getenv("tls_cert")
        )

        val database = Database()
        val lndClient = LndClient(environment)
        val invoiceService = InvoiceService(database, lndClient)

        installContentNegotiation()
        install(CORS) {
            host("store.nygaard.xyz", listOf("https"))
            host("localhost:3000", listOf("http"))
        }

        routing {
            registerSelftestApi(lndClient)
            registerInvoiceApi(invoiceService)
        }
    }.start(wait = true)
}

private fun Routing.registerSelftestApi(lndClient: LndClient) {
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

fun Routing.registerInvoiceApi(invoiceService: InvoiceService) {
    post("/invoices") {
        log.info("Creating invoice")
        call.respond(invoiceService.createInvoice())
    }

    get("/invoices/{uuid}") {
        val uuid = call.parameters["uuid"] ?: throw RuntimeException("Missing invoice uuid")
        val invoice = invoiceService.getInvoice(UUID.fromString(uuid))

        if (invoice != null) {
            call.respond(invoice)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}

data class Config(
    val hostUrl: String,
    val hostPort: Int,
    val readOnlyMacaroon: String,
    val invoiceMacaroon: String,
    val cert: String
)

class EnvironmentMacaroonContext(var currentMacaroonData: String) : MacaroonContext {
    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(Base64.getDecoder().decode(currentMacaroonData))
    }
}
