package xyz.nygaard

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.invoice.registerInvoiceApi
import xyz.nygaard.store.user.TokenService
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
            macaroonGeneratorSecret = props.getProperty("LS_MACAROON_SECRET")
        )

        val embeddedPostgres = EmbeddedPostgres.builder().setPort(5532).start()
        val localDb = TestDatabase(embeddedPostgres.postgresDatabase)

        Flyway.configure().run {
            dataSource(embeddedPostgres.postgresDatabase).load().migrate()
        }

        /* We run with a mock of LndClient. Can be exchanged with the proper implementation to run against a LND */
        val lndClient = LndClient(environment)
//        val lndClient = LndClientMock()
        val invoiceService = InvoiceService(localDb, lndClient)
        val macaroonService = MacaroonService()
        val tokenService = TokenService(embeddedPostgres.postgresDatabase)

        installContentNegotiation()
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
            host("localhost:8080", listOf("http", "https")) // frontendHost might be "*"
            log.info("CORS enabled for $hosts")
        }

        routing {
            registerSelftestApi(lndClient)
            registerInvoiceApi(invoiceService)
            registerRegisterApi(invoiceService, macaroonService, tokenService)
        }
    }.start(wait = true)
}

class TestDatabase(private val datasource: DataSource) : DatabaseInterface {
    override val connection: Connection
        get() = datasource.connection.apply { autoCommit = false }
}