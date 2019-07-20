package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.date.GMTDate
import org.lightningj.lnd.wrapper.MacaroonContext
import org.slf4j.LoggerFactory
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.article.ArticleService
import xyz.nygaard.store.article.NewArticle
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.login.LoginService
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
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
        val articleService = ArticleService(database)
        val loginService = LoginService(database)

        installContentNegotiation()
        install(CORS) {
            allowCredentials = true
            host("store.nygaard.xyz", listOf("https"))
            host("localhost:3000", listOf("http"))
        }
        install(XForwardedHeaderSupport)
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
            registerLoginApi(loginService, environment.isProd)

            get("/images/{name}") {
                val name = call.parameters["name"] ?: RuntimeException("Must specify resource")
                try {
                    call.respond(Files.readAllBytes(Path.of("src/main/resources/images/$name")))
                } catch (e: IOException) {
                    log.error(e.toString())
                    call.respond(HttpStatusCode.NotFound, "Resource not found")
                }
            }
            get("/articles") {
                call.respond(articleService.getLimitedArticles())
            }
            get("/articles/{uuid}") {
                val uuid: String = call.parameters["uuid"] ?: throw RuntimeException("Missing uuid param")
                val key: String = call.request.header("key") ?: throw RuntimeException("Missing key header")

                val article = articleService.getFullArticle(uuid, key)
                if (article != null) {
                    call.respond(article)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Article not found")
                }
            }
            authenticate("basic") {
                put("/articles/{uuid?}") {
                    val article: NewArticle = call.receive()

                    when (call.parameters["uuid"]) {
                        null -> {
                            val uuid = articleService.createArticle(article)
                            call.respond(uuid)
                        }
                        else -> articleService.updateArticle(article)
                    }
                }
            }

        }
    }.start(wait = true)
}

fun Routing.registerLoginApi(loginService: LoginService, isProd: Boolean) {
    get("/login") {
        val maybeKey = call.request.cookies["key"]

        if (loginService.isValidToken(maybeKey)) {
            log.info("User is already logged in")
            call.respond(LoginResponse(status = "LOGGED_IN", key = maybeKey))
        } else {
            log.info("User is not logged in")
            call.respond(LoginResponse("NOT_LOGGED_IN"))
        }
    }

    post("/login") {
        val request:LoginRequest = call.receive()

        if (loginService.isValidToken(request.key)) {
            log.info("User has a existing key -> returning this in a cookie")
            call.response.cookies.append(
                Cookie(
                    name = "key",
                    value = request.key,
                    secure = isProd,
                    httpOnly = true,
                    encoding = CookieEncoding.RAW,
                    domain = "nygaard.xyz",
                    expires = GMTDate(LocalDate.now().plusDays(14).toEpochDay())
                )
            )
            call.respond(LoginResponse(status = "LOGGED_IN", key = request.key))
        } else {
            log.info("Creating new key for user")
            val key = loginService.createAndSavePrivateKey()
            call.response.cookies.append(
                Cookie(
                    name = "key",
                    value = key,
                    secure = isProd,
                    httpOnly = true,
                    encoding = CookieEncoding.RAW,
                    domain = "nygaard.xyz",
                    expires = GMTDate(LocalDate.now().plusDays(14).toEpochDay())
                )
            )
            call.respond(LoginResponse(status = "LOGGED_IN", key = key))
        }
    }
}

data class LoginRequest(val key: String)
data class LoginResponse(val status: String, val key: String? = null)

private fun Routing.registerSelftestApi(lndClient: LndApiWrapper) {
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
