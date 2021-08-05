package xyz.nygaard

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.invoice.registerInvoiceApi
import java.io.FileInputStream
import java.sql.Connection
import java.util.*
import javax.sql.DataSource


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
            mocks = props.getProperty("lsmocks")?.toBoolean() ?: false
        )

        val embeddedPostgres = EmbeddedPostgres.builder().setPort(5532).start()
        val localDb = TestDatabase(embeddedPostgres.postgresDatabase)

        Flyway.configure().run {
            dataSource(embeddedPostgres.postgresDatabase).load().migrate()
        }

        /* We run with a mock of LndClient. Can be exchanged with the proper implementation to run against a LND */
        val lndClient = LndClient(environment) //LndClientMock()
        val invoiceService = InvoiceService(localDb, lndClient)

        installContentNegotiation()
        install(CORS) {
            host("localhost:3000", listOf("http"))
            allowCredentials = true
        }

        routing {
            registerSelftestApi(lndClient)
            registerInvoiceApi(invoiceService)
        }
    }.start(wait = true)
}

class TestDatabase(private val datasource: DataSource) : DatabaseInterface {
    override val connection: Connection
        get() = datasource.connection.apply { autoCommit = false }
}