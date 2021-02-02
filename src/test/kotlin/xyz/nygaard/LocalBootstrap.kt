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
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.invoice.InvoiceService
import java.sql.Connection
import javax.sql.DataSource


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

        installContentNegotiation()
        install(CORS) {
            host("localhost:3000", listOf("http"))
            allowCredentials = true
        }

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
        }
    }.start(wait = true)
}

class TestDatabase(private val datasource: DataSource) : DatabaseInterface {
    override val connection: Connection
        get() = datasource.connection.apply { autoCommit = false }
}