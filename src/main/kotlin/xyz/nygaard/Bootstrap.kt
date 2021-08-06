package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.lightningj.lnd.wrapper.MacaroonContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.invoice.registerInvoiceApi
import xyz.nygaard.store.user.Token
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.util.*
import javax.sql.DataSource
import javax.xml.bind.DatatypeConverter

val log: Logger = LoggerFactory.getLogger("Lightning Store")

fun main() {
    embeddedServer(Netty, port = 8020, host = "localhost") {

        val environment = Config(
            hostUrl = System.getenv("LS_HOST_URL"),
            hostPort = System.getenv("LS_HOST_PORT").toInt(),
            readOnlyMacaroon = System.getenv("LS_READONLY_MACAROON"),
            invoiceMacaroon = System.getenv("LS_INVOICES_MACAROON"),
            cert = System.getenv("LS_TLS_CERT"),
            databaseName = System.getenv("LS_DATABASE_NAME"),
            databaseUsername = System.getenv("LS_DATABASE_USERNAME"),
            databasePassword = System.getenv("LS_DATABASE_PASSWORD"),
            macaroonGeneratorSecret = System.getenv("LS_MACAROON_SECRET"),
            location = System.getenv("LS_LOCATION")
        )

        val database = Database(
            "jdbc:postgresql://localhost:5432/${environment.databaseName}",
            environment.databaseUsername,
            environment.databasePassword
        )
        buildApplication(environment, database.dataSource)
    }.start(wait = true)
}

internal fun Application.buildApplication(
    environment: Config,
    dataSource: DataSource
) {
    val lndClient = LndClient(environment)
    val invoiceService = InvoiceService(dataSource, lndClient)
    val macaroonService = MacaroonService(environment.location, environment.macaroonGeneratorSecret)
    val tokenService = TokenService(dataSource)

    installContentNegotiation()
    install(XForwardedHeaderSupport)
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        method(HttpMethod.Get)
        method(HttpMethod.Put)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlExposeHeaders)
        allowSameOrigin = true
        host("store.nygaard.xyz", listOf("http", "https"))
        log.info("CORS enabled for $hosts")
    }
    install(CallLogging)
//        installLsatInterceptor(invoiceService, macaroonService)
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        registerSelftestApi(lndClient)
        registerInvoiceApi(invoiceService)
        registerRegisterApi(invoiceService, macaroonService, tokenService)
    }
}

fun Routing.registerRegisterApi(
    invoiceService: InvoiceService,
    macaroonService: MacaroonService,
    tokenService: TokenService,
) {
    put("/register") {
        val authHeader = call.request.header("Authorization")
        if (authHeader == null) {
            log.info("Caller missing authentication")
            val userId = UUID.randomUUID()
            val invoice = invoiceService.createInvoice(1, userId.toString())
            val macaroon = macaroonService.createMacaroon(invoice.rhash)
            call.response.headers.append(
                "WWW-Authenticate",
                "LSAT macaroon=\"${macaroon.serialize()}\", invoice=\"${invoice.paymentRequest}\""
            )
            call.response.headers.append(
                "Access-Control-Expose-Headers", "WWW-Authenticate"
            )
            tokenService.createToken(macaroon)
            call.respond(HttpStatusCode.PaymentRequired, "Payment Required")
            return@put
        }
        call.respond("Ok")
    }
    get("/register") {
        val authHeader = call.request.header("Authorization")
        if (authHeader == null) {
            log.info("Caller missing authentication")
            return@get call.respond(HttpStatusCode.Unauthorized)
        } else {
            log.info("Caller looking up preimage for registration")
            val (type, rest) = authHeader.split(" ").let { it.first() to it.last() }
            val (incomingMacaroon, _) = rest.split(":")
                .let { split -> split.first().let { MacaroonsBuilder.deserialize(it) } to split.last() }

            if (type != "LSAT") {
                log.info("Caller using wrong authentication type, got $type")
                return@get call.respond(HttpStatusCode.BadRequest, "Authentication digest must be LSAT")
            }
            if (!macaroonService.isValid(incomingMacaroon)) {
                log.info("Macaroon is invalid")
                return@get call.respond(HttpStatusCode.Unauthorized)
            }
            val invoice = invoiceService.lookupAndUpdate(incomingMacaroon.extractRHash())!!
            return@get call.respond(invoice)
        }
    }
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

fun Application.installLsatInterceptor(invoiceService: InvoiceService, macaroonService: MacaroonService) {
    intercept(ApplicationCallPipeline.Setup) {
        if (call.request.path() == "/register") {
            val authHeader = call.request.header("Authorization")
            if (authHeader == null) {
                log.info("Caller missing authentication")
                val userId = UUID.randomUUID()
                val invoice = invoiceService.createInvoice(1, userId.toString())
                val macaroon = macaroonService.createMacaroon(invoice.rhash)
                call.response.headers.append(
                    "WWW-Authenticate",
                    "LSAT macaroon=\"${macaroon.serialize()}\", invoice=\"${invoice.paymentRequest}\""
                )
                call.respond(HttpStatusCode.PaymentRequired, "Payment Required")
                return@intercept finish()
            }

            val (type, rest) = authHeader.split(" ").let { it.first() to it.last() }
            val (incomingMacaroon, preimage) = rest.split(":")
                .let { split -> split.first().let { MacaroonsBuilder.deserialize(it) } to split.last() }

            if (type != "LSAT") {
                log.info("Caller using wrong authentication type, got $type")
                call.respond(HttpStatusCode.BadRequest, "Authentication digest must be LSAT")
                return@intercept finish()
            }
            if (!macaroonService.isValid(incomingMacaroon)) {
                log.info("Macaroon is invalid")
                call.respond(HttpStatusCode.Unauthorized)
                return@intercept finish()
            }

            if (preimage.sha256() != macaroonService.extractPaymentHash(incomingMacaroon)) {
                log.info("Preimage does not correspond to payment hash")
                call.respond(HttpStatusCode.BadRequest, "Preimage does not correspond to payment hash")
                return@intercept finish()
            }
            // Truncate the route response. If there is no finish() function,
            // the route /book will still respond to the processing, and the pipeline will be unwritable.
            return@intercept finish()
        }
        return@intercept proceed()
    }
}

data class CreateInvoiceRequest(val memo: String, val amount: Int)
data class CreateInvoiceResponse(
    val id: String,
    val memo: String?,
    val rhash: String,
    val paymentRequest: String
)


data class Config(
    val hostUrl: String,
    val hostPort: Int,
    val databaseName: String,
    val databaseUsername: String,
    val databasePassword: String,
    val readOnlyMacaroon: String,
    val invoiceMacaroon: String,
    val cert: String,
    val macaroonGeneratorSecret: String,
    val location: String,
)

class EnvironmentMacaroonContext(var currentMacaroonData: String) : MacaroonContext {
    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(Base64.getDecoder().decode(currentMacaroonData))
    }
}
