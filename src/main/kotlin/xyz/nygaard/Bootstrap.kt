package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.HttpsRedirect
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
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
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.invoice.InvoiceService
import java.util.Base64
import java.util.UUID
import javax.xml.bind.DatatypeConverter

val log = LoggerFactory.getLogger("Bootstrap")

fun main() {
    embeddedServer(Netty, port = 8000, host="localhost") {

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

        installContentNegotiation()

        routing {
            get ("/") {
                call.respondText("Hello, world!")
            }
            registerSelftestApi(lndClient)
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

fun Routing.registerInvoiceApi(invoiceService: InvoiceService) {
    post("/invoices") {
        val req = call.receive(CreateInvoiceRequest::class)

        log.info("Creating invoice req={}", req)

        val inv = invoiceService.createInvoice(
                amount = 500L,
                memo = req.memo
        )

        log.info("Created invoice inv={}", inv)

        val response = CreateInvoiceResponse(
                id = inv.id.toString(),
                memo = inv.memo,
                rhash = inv.rhash,
                paymentRequest = inv.paymentRequest
        )

        call.respond(response)
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
        val cert: String,
        val mocks: Boolean
)

class EnvironmentMacaroonContext(var currentMacaroonData: String) : MacaroonContext {
    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(Base64.getDecoder().decode(currentMacaroonData))
    }
}
