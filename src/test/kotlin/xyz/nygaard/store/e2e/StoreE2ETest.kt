package xyz.nygaard.store.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.extractUserId
import xyz.nygaard.installContentNegotiation
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.auth.AuthChallengeHeader
import xyz.nygaard.store.auth.installLsatInterceptor
import xyz.nygaard.store.invoice.InvoiceDto
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.order.*
import xyz.nygaard.store.register.registerRegisterApi
import xyz.nygaard.store.user.TokenResponse
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreE2ETest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val lndMock = LndClientMock()

    private val invoiceService = InvoiceService(
        embeddedPostgres.postgresDatabase,
        lndMock
    )

    private val macaroonService = MacaroonService("localhost", "secret")
    private val tokenService = TokenService(embeddedPostgres.postgresDatabase)
    private val productService = ProductService(embeddedPostgres.postgresDatabase)
    private val orderService = OrderService(embeddedPostgres.postgresDatabase, invoiceService)

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
            it.prepareStatement("DELETE FROM invoices").execute()
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
    fun `purchase blogpost with balance`() {
        tokenService.createToken(macaroon, 110)
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerOrders(orderService, tokenService, productService, invoiceService)
                registerProducts(productService)
            }
        }) {
            with(handleRequest(HttpMethod.Put, "/orders/balance/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(10L, tokenService.fetchToken(macaroon)?.balance)
                assertEquals(1, orderService.getOrders(macaroon.extractUserId()).size)
            }
            with(handleRequest(HttpMethod.Get, "/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
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
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerOrders(orderService, tokenService, productService, invoiceService)
                registerProducts(productService)
            }
        }) {
            var invoiceId: UUID? = null
            with(handleRequest(HttpMethod.Put, "/orders/invoice/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val invoice = mapper.readValue(response.content, InvoiceDto::class.java)
                invoiceId = invoice.id
                assertEquals(1, orderService.getOrders(macaroon.extractUserId()).size)
                assertEquals(1, orderService.getOrders(macaroon.extractUserId()).size)
                assertEquals(100, invoice.amount)
                assertNull(invoice.settled)
            }

            with(handleRequest(HttpMethod.Get, "/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.PaymentRequired, response.status())
            }

            lndMock.markInvoiceAsPaid()

            with(handleRequest(HttpMethod.Get, "/invoices/$invoiceId") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            with(handleRequest(HttpMethod.Get, "/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(mapper.readValue(response.content, ProductDto::class.java).payload)
            }
        }
    }

    @Test
    fun `fail to purchase blogpost with 0 balance`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerOrders(orderService, tokenService, productService, invoiceService)
                registerProducts(productService)
            }
        }) {
            with(handleRequest(HttpMethod.Put, "/orders/balance/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
            with(handleRequest(HttpMethod.Get, "/products/261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
            }) {
                assertEquals(HttpStatusCode.PaymentRequired, response.status())
            }
        }
    }
}