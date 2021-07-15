package xyz.nygaard.store.invoice

import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xyz.nygaard.installContentNegotiation

internal class InvoiceApiKtTest {

    @Test
    fun `add invoice`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerInvoiceApi(mockk(relaxed = true))
            }
        }) {
            with(handleRequest(HttpMethod.Post, "/invoices") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""{ "amount": 10, "memo":"" }""")
            }) {
                assertEquals(response.status(), HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `enforce min amount`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerInvoiceApi(mockk())
            }
        }) {
            with(handleRequest(HttpMethod.Post, "/invoices") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""{ "amount": -2, "memo":"" }""")
            }) {
                assertEquals(response.status(), HttpStatusCode.BadRequest)
            }
        }
    }

    @Test
    fun `enforce max memo length`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerInvoiceApi(mockk())
            }
        }) {
            with(handleRequest(HttpMethod.Post, "/invoices") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""{ "amount": 10, "memo":"${(0..1000).map { "a" }}" }""")
            }) {
                assertEquals(response.status(), HttpStatusCode.BadRequest)
            }
        }
    }
}
