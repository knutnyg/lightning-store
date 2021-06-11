package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.CORS
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.flywaydb.core.Flyway
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.invoice.InvoiceService
import java.io.FileInputStream
import java.sql.Connection
import java.util.*
import javax.sql.DataSource


val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

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