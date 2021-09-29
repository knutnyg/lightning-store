package xyz.nygaard.store.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.buildApplication
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.invoice.LndClientMock
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.io.FileInputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    protected val lndMock = LndClientMock()

    private val macaroonService = MacaroonService("localhost", "secret")
    protected val tokenService = TokenService(embeddedPostgres.postgresDatabase)
    protected val orderService = OrderService(embeddedPostgres.postgresDatabase)
    protected val productService = ProductService(embeddedPostgres.postgresDatabase)

    protected val preimage = "1234"
    private val rhash = preimage.sha256()
    protected val macaroon = macaroonService.createMacaroon(rhash)

    protected val mapper = jacksonObjectMapper()

    val imgData = requireNotNull(FileInputStream("src/test/resources/working.jpg").readAllBytes())

    @BeforeAll
    fun setup() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    @BeforeEach
    fun reset() {
        embeddedPostgres.postgresDatabase.connection.use {
            it.prepareStatement("DELETE FROM orders;").execute()
            it.prepareStatement("DELETE FROM token;").execute()
            it.prepareStatement("DELETE FROM invoices;").execute()
        }
    }

    protected fun TestApplicationEngine.authenticated(method: HttpMethod, uri: String) =
        handleRequest(method, uri) {
            addHeader(HttpHeaders.Accept, "application/json")
            addHeader(HttpHeaders.XForwardedProto, "https")
            addHeader(HttpHeaders.Cookie, "authorization=LSAT ${macaroon.serialize()}:${preimage}")
        }

    protected fun Application.setup() = buildApplication(
        dataSource = embeddedPostgres.postgresDatabase,
        macaroonService = macaroonService,
        productService = productService,
        lndClient = lndMock,
        staticResourcesPath = "static",
        resourceFetcher = FakeFetcher(imgData)
    )

    class FakeFetcher(private val data: ByteArray): Fetcher {
        override fun requestNewImage(): ByteArray {
            return data
        }

    }
}