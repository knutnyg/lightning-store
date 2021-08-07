package xyz.nygaard.store.register

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.installContentNegotiation
import xyz.nygaard.lnd.LndClientMock
import xyz.nygaard.store.installLsatInterceptor
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.user.TokenResponse
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RegisterApiKtTest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val invoiceService = InvoiceService(
        embeddedPostgres.postgresDatabase,
        LndClientMock()
    )

    private val macaroonService = MacaroonService("localhost", "secret")
    private val tokenService = TokenService(embeddedPostgres.postgresDatabase)

    private val preimage = "1234"
    private val rhash = preimage.sha256()
    private val macaroon = macaroonService.createMacaroon(rhash)

    private val mapper = jacksonObjectMapper()

    @BeforeAll
    fun test() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
        tokenService.createToken(macaroon)
    }

    @Test
    fun `fetch user balance`() {
        withTestApplication({
            installContentNegotiation()
            installLsatInterceptor(invoiceService, macaroonService, tokenService)
            routing {
                registerRegisterApi(invoiceService, tokenService)
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/register") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader(HttpHeaders.Authorization, "LSAT ${macaroon.serialize()}:$preimage")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val response = mapper.readValue(response.content, TokenResponse::class.java)
                assertEquals(0, response.balance)
                assertNotNull(response.userId)
            }
        }
    }
}