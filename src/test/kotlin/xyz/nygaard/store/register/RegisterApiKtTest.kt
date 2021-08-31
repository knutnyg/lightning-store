package xyz.nygaard.store.register

import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xyz.nygaard.store.e2e.AbstractE2ETest
import xyz.nygaard.store.user.TokenResponse

internal class RegisterApiKtTest : AbstractE2ETest() {

    @Test
    fun `fetch user balance`() {
        withTestApplication({
            setup()
        }) {
            tokenService.createToken(macaroon, 0)
            with(authenticated(HttpMethod.Get, "/api/register")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val data = mapper.readValue(response.content, TokenResponse::class.java)
                assertEquals(0, data.balance)
                assertNotNull(data.userId)
                assertEquals("LSAT ${macaroon.serialize()}:$preimage", response.cookies["authorization"]?.value)
            }
        }
    }
}