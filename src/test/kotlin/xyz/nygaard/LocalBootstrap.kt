package xyz.nygaard

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import xyz.nygaard.db.Database
import xyz.nygaard.lnd.LndClient
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
        )
    }.start(wait = true)
}
