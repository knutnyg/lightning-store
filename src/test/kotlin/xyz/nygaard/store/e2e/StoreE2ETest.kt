package xyz.nygaard.store.e2e

import com.google.common.io.Resources
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import xyz.nygaard.*
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.auth.AuthChallengeHeader
import xyz.nygaard.store.invoice.InvoiceDto
import xyz.nygaard.store.order.*
import xyz.nygaard.store.user.TokenResponse
import java.io.FileInputStream
import java.net.URI
import java.util.*

val imgData = requireNotNull(FileInputStream("src/test/resources/working.jpg").readAllBytes())

class StoreE2ETest : AbstractE2ETest() {

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

    val productId = "ec533145-47fa-464e-8cf0-fd36e3709ad3"

    @Test
    fun `admin update product image`() {
        tokenService.createToken(macaroon, 0)
        withTestApplication({
            setup()
        }) {
            with(handleRequest(HttpMethod.Post, "/api/admin/product/$productId/upload") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(mediaTypeHeaderKey, "image/jpeg")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:${preimage}")
                setBody(imgData)
            }) {
                val body = response.content
                System.err.println("body=" + body)
                assertEquals(HttpStatusCode.OK, response.status())
                val response = mapper.readValue(response.content, ProductDto::class.java)

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
            var invoiceId: UUID?
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
            var invoiceId: UUID?
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
}

class TestFetcher : Fetcher {
    override fun fetch(uri: URI) = requireNotNull(FileInputStream("src/test/resources/working.jpg").readAllBytes())
}