package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.date.GMTDate
import org.flywaydb.core.Flyway
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.article.ArticleService
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.login.LoginService


val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun main() {
    embeddedServer(Netty, 8081) {
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

        val embeddedPostgres = EmbeddedPostgres.start()
        val localDb = TestDatabase(embeddedPostgres.postgresDatabase)

        Flyway.configure().run {
            dataSource(embeddedPostgres.postgresDatabase).load().migrate()
        }

        /* We run with a mock of LndClient. Can be exchanged with the proper implementation to run against a LND */
        val lndClient = LndClientMock()

        val invoiceService = InvoiceService(localDb, lndClient)
        val articleService = ArticleService(localDb)
        val loginService = LoginService(localDb)

        installContentNegotiation()

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
            registerLoginApi(loginService)
            registerArticlesApi(articleService)
            registerResourcesApi()
        }
    }.start(wait = true)
}

/*  Since we run on localhost with http it's easier to just have this implemented 2-ways instead of bleeding all over
    the production code
 */
fun Routing.registerLoginApi(loginService: LoginService) {
    get("/login") {
        val maybeKey = call.request.cookies["key"]

        if (loginService.isValidToken(maybeKey)) {
            log.info("User is already logged in")
            call.response.cookies.append(
                cookie(maybeKey!!)
            )
            call.respond(LoginResponse(status = "LOGGED_IN", key = maybeKey))
        } else {
            log.info("User is not logged in")
            call.respond(LoginResponse("NOT_LOGGED_IN"))
        }
    }

    post("/login") {
        val request: LoginRequest = call.receive()
        val key: String =
            when (loginService.isValidToken(request.key)) {
                false ->
                    loginService.createAndSavePrivateKey()
                        .also { log.info("Creating new key for user") }
                else -> request.key
                    .also { log.info("User has a existing key -> returning this in a cookie") }
            }
        call.response.cookies.append(cookie(key))
        call.respond(LoginResponse(status = "LOGGED_IN", key = key))
    }
}

private fun cookie(key: String): Cookie {
    return Cookie(
        name = "key",
        value = key,
        secure = false,
        httpOnly = true,
        encoding = CookieEncoding.RAW,
        domain = "localhost",
        expires = GMTDate(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)) // 7 days
    )
}
