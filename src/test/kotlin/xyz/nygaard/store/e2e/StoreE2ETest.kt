package xyz.nygaard.store.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.installContentNegotiation
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.AuthChallengeHeader
import xyz.nygaard.store.installLsatInterceptor
import xyz.nygaard.store.invoice.Invoice
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.order.Order
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.order.registerOrders
import xyz.nygaard.store.register.registerRegisterApi
import xyz.nygaard.store.user.TokenResponse
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreE2ETest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val invoiceService = InvoiceService(
        embeddedPostgres.postgresDatabase,
        LndClientMock()
    )

    private val macaroonService = MacaroonService("localhost", "secret")
    private val tokenService = TokenService(embeddedPostgres.postgresDatabase)
    private val productService = ProductService(embeddedPostgres.postgresDatabase)
    private val orderService = OrderService(embeddedPostgres.postgresDatabase, invoiceService, productService)

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
        }
    }

    @Test
    fun `sign up as new user`() {
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerRegisterApi(invoiceService, tokenService)
            }
        }) {
            var authHeader: AuthChallengeHeader
            with(handleRequest(HttpMethod.Put, "/register") {
                addHeader(HttpHeaders.Accept, "application/json")
            }) {
                assertTrue(response.headers.contains("WWW-Authenticate"))
                authHeader = AuthChallengeHeader.deserialize(response.headers["WWW-Authenticate"]!!)

                assertEquals(HttpStatusCode.PaymentRequired, response.status())
                assertNotNull(authHeader.macaroon)
                assertEquals("LSAT", authHeader.type)
            }
        }
    }

    @Test
    fun `fetch token balance as an authenticated user`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerRegisterApi(invoiceService, tokenService)
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/register") {
                addHeader(HttpHeaders.Accept, "application/json")
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
    fun `top off balance`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerRegisterApi(invoiceService, tokenService)
                registerOrders(orderService)
            }
        }) {
            with(handleRequest(HttpMethod.Put, "/orders/5c3ae6cf-1ecc-4ae4-ba86-60e66ef2625b") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response = mapper.readValue(response.content, Invoice::class.java)
                assertEquals(100, response.amount)
            }
        }
    }
}