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
    embeddedServer(Netty, System.getenv("PORT").toInt()) {
        val environment = Config(
                hostUrl = System.getenv("lshost"),
                hostPort = System.getenv("lsport").toInt(),
                readOnlyMacaroon = System.getenv("readonly_macaroon"),
                invoiceMacaroon = System.getenv("invoice_macaroon"),
                cert = System.getenv("tls_cert"),
                mocks = System.getenv("lsmocks")?.toBoolean() ?: false,
                adminUser = System.getenv("lsadminuser"),
                adminPass = System.getenv("lsadminpass"),
                isProd = System.getenv("lsIsProd")?.toBoolean() ?: true
        )

        val database = Database(environment.isProd)

        val lndClient = when (environment.mocks) {
            true -> LndClientMock()
            else -> LndClient(environment)
        }
        val invoiceService = InvoiceService(database, lndClient)

        installContentNegotiation()
        install(CORS) {
            allowCredentials = true
            host("store.nygaard.xyz", listOf("https"))
            host("localhost:3000", listOf("http"))
        }
        install(XForwardedHeaderSupport)
        install(HttpsRedirect)
        install(Authentication) {
            basic(name = "basic") {
                realm = "Ktor Server"
                validate { credentials ->
                    if (credentials.name == environment.adminUser && credentials.password == environment.adminPass) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        routing {
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
        val mocks: Boolean,
        val adminUser: String,
        val adminPass: String,
        val isProd: Boolean
)

class EnvironmentMacaroonContext(var currentMacaroonData: String) : MacaroonContext {
    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(Base64.getDecoder().decode(currentMacaroonData))
    }
}
