package xyz.nygaard.store.invoice

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.TestDatabase
import xyz.nygaard.installContentNegotiation
import xyz.nygaard.installLsatInterceptor
import xyz.nygaard.lnd.LndClientMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InvoiceApiKtTest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val invoiceService = InvoiceService(
        TestDatabase(embeddedPostgres.postgresDatabase),
        LndClientMock()
    )

    private val macaroonService = MacaroonService()

    @BeforeAll
    fun test() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    @Test
    fun `add invoice`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerInvoiceApi(invoiceService)
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
                registerInvoiceApi(invoiceService)
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
                registerInvoiceApi(invoiceService)
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
            installLsatInterceptor(invoiceService, macaroonService)
            routing {
                registerInvoiceApi(invoiceService)
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/invoices/forbidden")) {
                assertEquals(response.status(), HttpStatusCode.PaymentRequired)
                assertTrue(response.headers.contains("WWW-Authenticate"))
            }
        }
    }

    @Test
    fun `valid macaroon invoice paid and valid preimage`() {
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService)
            routing {
                registerInvoiceApi(invoiceService)
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/invoices/forbidden") {
                addHeader(
                    "Authorization",
                    "LSAT MDAxY2xvY2F0aW9uIGxvY2FsaG9zdDo4MDAwCjAwOWFpZGVudGlmaWVyIHZlcnNpb24gPSAwCnVzZXJfaWQgPSA1YzMxY2RmNi1iNzI0LTQ0N2QtOWRlOS1kZjQ2NWFlNzcwYTEKcGF5bWVudF9oYXNoID0gMzk2YzllMTk2YzFhMjFmNGQ4Yjk5YWI3Y2IzNjg1ZTdmNjU4MmU1ZjE3NDBhZDBkOTYwYzlkMTZiNGE1YzYyMgowMDFlY2lkIHNlcnZpY2VzID0gaW52b2ljZXM6MAowMDJmc2lnbmF0dXJlIHAxY5PxtPU0xpOfB6x7qQAfYwt4lCAsHGFOo06GdtZ-Cg:9ec2d9ee21189cde57964e8af3d798eccf9a13d2ac7b06da03371f9a9e0b9d50"
                )
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
