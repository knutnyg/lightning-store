package xyz.nygaard.store.signpost

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.invoice.InvoiceService

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignpostE2ETest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val invoiceService = InvoiceService(
        embeddedPostgres.postgresDatabase,
        LndClientMock()
    )

    private val macaroonService = MacaroonService("localhost", "secret")

    @BeforeAll
    fun test() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

//    @Test
//    fun `unauthorized`() {
//        withTestApplication({
//            installContentNegotiation()
//            installLsatInterceptor(invoiceService, macaroonService)
//            routing {
//                registerSignpostApi(invoiceService, macaroonService)
//            }
//        }) {
//            with(handleRequest(HttpMethod.Get, "/signpost") {
//                addHeader(HttpHeaders.ContentType, "application/json")
//            }) {
//                Assertions.assertEquals(response.status(), HttpStatusCode.PaymentRequired)
//            }
//        }
//    }
//
//    @Test
//    fun `insufficient funds`() {
//        withTestApplication({
//            installContentNegotiation()
//            installLsatInterceptor(invoiceService, macaroonService)
//            routing {
//                registerSignpostApi(invoiceService, macaroonService)
//            }
//        }) {
//            with(handleRequest(HttpMethod.Get, "/signpost") {
//                addHeader(HttpHeaders.ContentType, "application/json")
//                addHeader(HttpHeaders.Authorization, "LSAT MDAxY2xvY2F0aW9uIGxvY2FsaG9zdDo4MDAwCjAwOWFpZGVudGlmaWVyIHZlcnNpb24gPSAwCnVzZXJfaWQgPSA1YzMxY2RmNi1iNzI0LTQ0N2QtOWRlOS1kZjQ2NWFlNzcwYTEKcGF5bWVudF9oYXNoID0gMzk2YzllMTk2YzFhMjFmNGQ4Yjk5YWI3Y2IzNjg1ZTdmNjU4MmU1ZjE3NDBhZDBkOTYwYzlkMTZiNGE1YzYyMgowMDFlY2lkIHNlcnZpY2VzID0gaW52b2ljZXM6MAowMDJmc2lnbmF0dXJlIHAxY5PxtPU0xpOfB6x7qQAfYwt4lCAsHGFOo06GdtZ-Cg:9ec2d9ee21189cde57964e8af3d798eccf9a13d2ac7b06da03371f9a9e0b9d50")
//            }) {
//                Assertions.assertEquals(response.status(), HttpStatusCode.PaymentRequired)
//            }
//        }
//    }
//
//    @Test
//    fun `get signpost message`() {
//        withTestApplication({
//            installContentNegotiation()
//            routing {
//                registerSignpostApi(invoiceService, macaroonService)
//            }
//        }) {
//            with(handleRequest(HttpMethod.Get, "/signpost") {
//                addHeader(HttpHeaders.ContentType, "application/json")
//            }) {
//                Assertions.assertEquals(response.status(), HttpStatusCode.OK)
//            }
//        }
//    }
//
//    @Test
//    fun `set signpost message`() {
//        withTestApplication({
//            installContentNegotiation()
//            routing {
//                registerSignpostApi(invoiceService, macaroonService)
//            }
//        }) {
//            with(handleRequest(HttpMethod.Put, "/signpost") {
//                addHeader(HttpHeaders.ContentType, "application/json")
//                setBody("""{ "message": "hello this is dog" }""")
//            }) {
//                Assertions.assertEquals(response.status(), HttpStatusCode.Accepted)
//            }
//
//            with(handleRequest(HttpMethod.Get, "/signpost") {
//                addHeader(HttpHeaders.ContentType, "application/json")
//            }) {
//                assertEquals("hello this is dog", response.content.toString())
//                Assertions.assertEquals(response.status(), HttpStatusCode.OK)
//            }
//        }
//    }
}