package xyz.nygaard.store.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import xyz.nygaard.*
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.auth.AuthChallengeHeader
import xyz.nygaard.store.invoice.InvoiceDto
import xyz.nygaard.store.order.*
import xyz.nygaard.store.user.TokenResponse
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.io.FileInputStream
import java.net.URI
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreE2ETest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val lndMock = LndClientMock()

    private val macaroonService = MacaroonService("localhost", "secret")
    private val tokenService = TokenService(embeddedPostgres.postgresDatabase)
    private val orderService = OrderService(embeddedPostgres.postgresDatabase)
    private val productService = ProductService(embeddedPostgres.postgresDatabase, TestFetcher())

    private val preimage = "1234"
    private val rhash = preimage.sha256()
    private val macaroon = macaroonService.createMacaroon(rhash)

    private val mapper = jacksonObjectMapper()

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

    @Test
    fun `sign up as new user`() {
        withTestApplication({
            setup()
        }) {
            var authHeader: AuthChallengeHeader
            with(handleRequest(HttpMethod.Put, "/api/register") {
                addHeader(HttpHeaders.Accept, "application/json")
            }) {
                assertTrue(response.headers.contains("WWW-Authenticate"))
                authHeader = AuthChallengeHeader.deserialize(response.headers["WWW-Authenticate"]!!)

                assertEquals(HttpStatusCode.PaymentRequired, response.status())
                assertNotNull(authHeader.macaroon)
                assertEquals("LSAT", authHeader.type)
                assertEquals(1, orderService.getOrders(authHeader.macaroon.extractUserId()).size)
            }
        }
    }

    @Test
    fun `fetch token balance as an authenticated user`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            setup()
        }) {
            with(handleRequest(HttpMethod.Get, "/api/register") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response = mapper.readValue(response.content, TokenResponse::class.java)
                assertEquals(0, response.balance)
                assertNotNull(response.userId)
            }
        }
    }

    @Test
    fun `purchase blogpost with balance`() {
        tokenService.createToken(macaroon, 110)
        withTestApplication({
            setup()
        }) {
            with(handleRequest(HttpMethod.Post, "/api/orders/balance/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(109, tokenService.fetchToken(macaroon)?.balance)
                assertEquals(1, orderService.getOrders(macaroon.extractUserId()).size)
            }
            with(handleRequest(HttpMethod.Get, "/api/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun `purchase blogpost with invoice`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            setup()
        }) {
            var invoiceId: UUID? = null
            with(authenticated(HttpMethod.Post, "/api/orders/invoice/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val invoice = mapper.readValue(response.content, InvoiceDto::class.java)
                invoiceId = invoice.id
                assertEquals(1, orderService.getOrders(macaroon.extractUserId()).size)
                assertEquals(1, orderService.getOrders(macaroon.extractUserId()).size)
                assertEquals(1, invoice.amount)
                assertNull(invoice.settled)
            }

            with(authenticated(HttpMethod.Get, "/api/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")) {
                assertEquals(HttpStatusCode.PaymentRequired, response.status())
            }

            lndMock.markInvoiceAsPaid()

            with(authenticated(HttpMethod.Get, "/api/invoices/$invoiceId")) {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            with(authenticated(HttpMethod.Get, "/api/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")) {
                assertNotNull(mapper.readValue(response.content, ProductDto::class.java).payload)
            }
        }
    }

    @Test
    fun `fail to purchase blogpost with 0 balance`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            setup()
        }) {
            with(authenticated(HttpMethod.Post, "/api/orders/balance/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
            with(authenticated(HttpMethod.Get, "/api/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")) {
                assertEquals(HttpStatusCode.PaymentRequired, response.status())
            }
        }
    }

    @Test
    fun `purchase image via bundle`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            setup()
        }) {
            var invoiceId: UUID? = null
            with(authenticated(HttpMethod.Post, "/api/orders/invoice/ec533145-47fa-464e-8cf0-fd36e3709ad3")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val invoice = mapper.readValue(response.content, InvoiceDto::class.java)
                invoiceId = invoice.id
            }
            lndMock.markInvoiceAsPaid()

            with(authenticated(HttpMethod.Get, "/api/invoices/$invoiceId")) {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            with(authenticated(HttpMethod.Get, "/api/products/a1afc48b-23bc-4297-872a-5e7884d6975a")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.byteContent?.size)
            }
        }
    }


    private fun TestApplicationEngine.authenticated(method: HttpMethod, uri: String) =
        handleRequest(method, uri) {
            addHeader(HttpHeaders.Accept, "application/json")
            addHeader(HttpHeaders.XForwardedProto, "https")
            addHeader(HttpHeaders.Cookie, "authorization=LSAT ${macaroon.serialize()}:${preimage}")
        }

    private fun Application.setup() = buildApplication(
        dataSource = embeddedPostgres.postgresDatabase,
        macaroonService = macaroonService,
        productService = productService,
        lndClient = lndMock,
        staticResourcesPath = "static"
    )
}

class TestFetcher : Fetcher {
    override fun fetch(uri: URI) = requireNotNull(FileInputStream("src/test/resources/working.jpg").readAllBytes())
}