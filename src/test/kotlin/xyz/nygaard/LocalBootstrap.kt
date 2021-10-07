package xyz.nygaard

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.auth.AuthHeader
import xyz.nygaard.store.auth.CookieBakery
import xyz.nygaard.store.e2e.FakeFetcher
import xyz.nygaard.store.e2e.imgData
import xyz.nygaard.store.invoice.LndClientMock
import java.io.File
import java.io.FileInputStream
import java.util.*


fun main() {
    embeddedServer(Netty, 8081) {
        val props = Properties()

        val propertiesFile = File("src/test/resources/local.properties")
        if (propertiesFile.exists()) {
            log.info("loaded local.properties")
            props.load(FileInputStream("src/test/resources/local.properties"))
        }

        val environment = Config(
            hostUrl = props.getProperty("lshost", "localhost"),
            hostPort = props.getProperty("lsport", "10009").toInt(),
            readOnlyMacaroon = props.getProperty("readonly_macaroon", "readonly_macaroon"),
            invoiceMacaroon = props.getProperty("invoice_macaroon", "invoice_macaroon"),
            cert = props.getProperty("tls_cert", "tls_cert"),
            databaseName = "",
            databaseUsername = "postgres",
            databasePassword = "",
            macaroonGeneratorSecret = props.getProperty("ls_macaroon_secret", "secret"),
            location = "localhost",
            staticResourcesPath = "src/main/frontend/build",
            kunstigUrl = "localhost"
        )

        val useRealPostgres = true
        val useRealLnd = false

        val dataSource = if (useRealPostgres) {
            Database(
                "jdbc:postgresql://localhost:5432/${environment.databaseName}",
                environment.databaseUsername,
                environment.databasePassword
            ).dataSource
        } else EmbeddedPostgres.builder()
            .setPort(5534).start().postgresDatabase

        val macaroonService = MacaroonService(environment.location, environment.macaroonGeneratorSecret)
        val lndClient = if (useRealLnd) {
            LndClient(
                environment.cert,
                environment.hostUrl,
                environment.hostPort,
                environment.readOnlyMacaroon,
                environment.invoiceMacaroon
            )
        } else LndClientMock()

        Flyway.configure().run {
            dataSource(dataSource).load().migrate()
        }
        buildApplication(
            dataSource = dataSource,
            macaroonService = macaroonService,
            lndClient = lndClient,
            cookieBakery = LocalhostCookieJar(),
            staticResourcesPath = environment.staticResourcesPath,
            resourceFetcher = FakeFetcher(imgData, 5000)
        ).apply {
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
                host("localhost:8080", listOf("http", "https"))
                host("localhost:3000", listOf("http", "https"))
                log.info("CORS enabled for $hosts")
            }
            routing {
                get("/local/invoice/markPaid") {
                    log.info("marking invoice as settled")
                    if (lndClient is LndClientMock) {
                        lndClient.markSettled(true)
                    }
                    call.respond("Invoice marked as paid")
                }
                get("/local/invoice/markUnpaid") {
                    log.info("marking invoice as unsettled")
                    if (lndClient is LndClientMock) {
                        lndClient.markSettled(false)
                    }
                    call.respond("Invoice marked as unpaid")
                }
            }
        }
    }.start(wait = true)
}

internal class LocalhostCookieJar : CookieBakery {
    override fun createAuthCookie(authHeader: AuthHeader): Cookie {
        return Cookie(
            name = "authorization",
            value = authHeader.pack(),
            secure = false,
            httpOnly = true,
            path = "/",
            extensions = mapOf("SameSite" to "Lax"),
            maxAge = 60000,
        )
    }
}