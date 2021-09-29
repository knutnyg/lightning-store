package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.lightningj.lnd.wrapper.MacaroonContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.ResourceFetcher
import xyz.nygaard.store.auth.CookieBakery
import xyz.nygaard.store.auth.CookieJar
import xyz.nygaard.store.auth.installLsatInterceptor
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.order.registerAdmin
import xyz.nygaard.store.order.registerOrders
import xyz.nygaard.store.order.registerProducts
import xyz.nygaard.store.register.registerRegisterApi
import xyz.nygaard.store.user.TokenService
import java.io.File
import java.net.URI
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
            location = System.getenv("LS_LOCATION"),
            staticResourcesPath = getEnvOrDefault("LS_STATIC_RESOURCES", "src/main/frontend/build"),
            kunstigUrl = getEnvOrDefault("LS_KUNSTIG_URL", "localhost:8080")
        )

        val database = Database(
            "jdbc:postgresql://localhost:5432/${environment.databaseName}",
            environment.databaseUsername,
            environment.databasePassword,
        )
        val lndClient = LndClient(
            environment.cert,
            environment.hostUrl,
            environment.hostPort,
            environment.readOnlyMacaroon,
            environment.invoiceMacaroon,
        )
        val macaroonService = MacaroonService(environment.location, environment.macaroonGeneratorSecret)
        buildApplication(
            dataSource = database.dataSource,
            macaroonService = macaroonService,
            lndClient = lndClient,
            staticResourcesPath = environment.staticResourcesPath,
            resourceFetcher = ResourceFetcher(environment.kunstigUrl)
        )
    }.start(wait = true)
}

internal fun Application.buildApplication(
    dataSource: DataSource,
    staticResourcesPath: String,
    macaroonService: MacaroonService,
    lndClient: LndApiWrapper,
    resourceFetcher: Fetcher,
    productService: ProductService = ProductService(dataSource),
    cookieBakery: CookieBakery = CookieJar(),
) {
    val invoiceService = InvoiceService(dataSource, lndClient)
    val tokenService = TokenService(dataSource)
    val orderService = OrderService(dataSource)

    installContentNegotiation()
    install(XForwardedHeaderSupport)
    install(CallLogging) {
        level = Level.TRACE
    }
    installLsatInterceptor(invoiceService, macaroonService, tokenService, orderService, productService)
    routing {
        route("/api") {
            registerOrders(orderService, tokenService, productService, invoiceService)
            registerSelftestApi(lndClient)
            registerRegisterApi(invoiceService, tokenService, cookieBakery)
            registerProducts(productService, invoiceService, orderService, resourceFetcher)
            registerAdmin(productService)
        }

        // Serves all static content i.e: example.com/static/css/styles.css
        static("/static") {
            staticRootFolder = File(staticResourcesPath)
            files("static")
        }

        // Serves index.html on example.com
        static("/") {
            default("$staticResourcesPath/index.html")
        }

        // Serves index.html on other paths like: example.com/register
        static("*") {
            default("$staticResourcesPath/index.html")
        }
    }
}

fun Route.registerSelftestApi(lndClient: LndApiWrapper) {
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
    val staticResourcesPath: String,
    val kunstigUrl: String
)

class EnvironmentMacaroonContext(var currentMacaroonData: String) : MacaroonContext {
    override fun getCurrentMacaroonAsHex(): String {
        return DatatypeConverter.printHexBinary(Base64.getDecoder().decode(currentMacaroonData))
    }
}

fun getEnvOrDefault(name: String, defaultValue: String): String {
    return System.getenv(name) ?: defaultValue
}