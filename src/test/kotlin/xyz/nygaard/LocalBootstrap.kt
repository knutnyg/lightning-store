package xyz.nygaard

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.log
import java.io.FileInputStream
import java.util.*


fun main() {
    embeddedServer(Netty, 8081) {
        val props = Properties()
        props.load(FileInputStream("src/test/resources/local.properties"))
        val environment = Config(
            hostUrl = props.getProperty("lshost"),
            hostPort = props.getProperty("lsport").toInt(),
            readOnlyMacaroon = props.getProperty("readonly_macaroon"),
            invoiceMacaroon = props.getProperty("invoice_macaroon"),
            cert = props.getProperty("tls_cert"),
            databaseName = "",
            databaseUsername = "postgres",
            databasePassword = "",
            macaroonGeneratorSecret = props.getProperty("ls_macaroon_secret"),
            location = "localhost",
            resourcesPath = "/Users/knut",
            staticResourcesPath = "src/main/frontend/build"
        )

        val database = Database(
            "jdbc:postgresql://localhost:5432/${environment.databaseName}",
            environment.databaseUsername,
            environment.databasePassword
        )

        val macaroonService = MacaroonService(environment.location, environment.macaroonGeneratorSecret)
        val lndClient = LndClient(
            environment.cert,
            environment.hostUrl,
            environment.hostPort,
            environment.readOnlyMacaroon,
            environment.invoiceMacaroon
        )

        Flyway.configure().run {
            dataSource(database.dataSource).load().migrate()
        }
        buildApplication(
            dataSource = database.dataSource,
            macaroonService = macaroonService,
            lndClient = lndClient,
            inProduction = false,
            staticResourcesPath = environment.staticResourcesPath
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
                host("localhost:8081", listOf("http", "https"))
                log.info("CORS enabled for $hosts")
            }
        }
    }.start(wait = true)
}
