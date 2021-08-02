package xyz.nygaard.store.signpost

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.TestDatabase
import xyz.nygaard.installContentNegotiation
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.invoice.registerInvoiceApi
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignpostE2ETest {

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
    fun `get signpost message`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerSignpostApi()
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/signpost") {
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                Assertions.assertEquals(response.status(), HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `set signpost message`() {
        withTestApplication({
            installContentNegotiation()
            routing {
                registerSignpostApi()
            }
        }) {
            with(handleRequest(HttpMethod.Put, "/signpost") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""{ "message": "hello this is dog" }""")
            }) {
                Assertions.assertEquals(response.status(), HttpStatusCode.Accepted)
            }

            with(handleRequest(HttpMethod.Get, "/signpost") {
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                assertEquals("hello this is dog", response.content.toString())
                Assertions.assertEquals(response.status(), HttpStatusCode.OK)
            }
        }
    }
}