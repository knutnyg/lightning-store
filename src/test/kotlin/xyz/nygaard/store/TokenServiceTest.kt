package xyz.nygaard.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.MacaroonService
import xyz.nygaard.extractUserId
import xyz.nygaard.store.e2e.AbstractE2ETest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenServiceTest : AbstractE2ETest() {

    private val macaroonService: MacaroonService = MacaroonService("localhost", "secret")

    @Test
    fun `create and fetch user`() {
        val macaroon = macaroonService.createMacaroon("rhash")
        val result = tokenService.createToken(macaroon = macaroon)
        assertEquals(result, 1)

        val token = tokenService.fetchToken(macaroon)!!
        assertEquals(token.macaroon, macaroon)
        assertEquals(token.id, macaroon.extractUserId())
        assertEquals(token.balance, 0)
        assertEquals(token.revoked, false)
    }

    @Test
    fun `increase balance`() {
        val macaroon = macaroonService.createMacaroon("rhash")
        tokenService.createToken(macaroon)
        tokenService.increaseBalance(macaroon, 100)

        val token = requireNotNull(tokenService.fetchToken(macaroon))
        assertEquals(100, token.balance)
    }
}