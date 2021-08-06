package xyz.nygaard

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
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
            databaseUsername = "",
            databasePassword = "",
            macaroonGeneratorSecret = props.getProperty("LS_MACAROON_SECRET"),
            location = "localhost"
        )

        val embeddedPostgres = EmbeddedPostgres.builder().setPort(5532).start()

        Flyway.configure().run {
            dataSource(embeddedPostgres.postgresDatabase).load().migrate()
        }
        buildApplication(environment, embeddedPostgres.postgresDatabase)
    }.start(wait = true)
}
