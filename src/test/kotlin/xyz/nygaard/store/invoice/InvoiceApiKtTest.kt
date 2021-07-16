package xyz.nygaard.store.invoice

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.TestDatabase
import xyz.nygaard.installContentNegotiation
import xyz.nygaard.lnd.LndClientMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InvoiceApiKtTest {

    val embeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5533).start()

    val invoiceService = InvoiceService(
        TestDatabase(embeddedPostgres.postgresDatabase),
        LndClientMock()
    )

    @BeforeAll
    fun test() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    @Test
    fun `add invoice`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerInvoiceApi(invoiceService, MacaroonService())
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
                registerInvoiceApi(invoiceService, MacaroonService())
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
                registerInvoiceApi(invoiceService, MacaroonService())
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

    @Test
    fun `metered endpoints returns 402 given no macaroon`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerInvoiceApi(invoiceService, MacaroonService())
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/invoices/forbidden")) {
                assertEquals(response.status(), HttpStatusCode.PaymentRequired)
                assertTrue(response.headers.contains("WWW-Authenticate"))
            }
        }
    }
}
