package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.nitram509.jmacaroons.Macaroon
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
import xyz.nygaard.store.AuthHeader
import xyz.nygaard.store.installLsatInterceptor
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.invoice.registerInvoiceApi
import xyz.nygaard.store.register.registerRegisterApi
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.lang.RuntimeException
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
        host("localhost:8080", listOf("http", "https"))
        log.info("CORS enabled for $hosts")
    }
    install(CallLogging)
    installLsatInterceptor(invoiceService, macaroonService, tokenService)
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        registerSelftestApi(lndClient)
        registerInvoiceApi(invoiceService)
        registerRegisterApi(invoiceService, tokenService)
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
