package xyz.nygaard.store

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.extractUserId
import xyz.nygaard.store.user.TokenService
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenServiceTest {

    private val embeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    private val tokenService: TokenService = TokenService(embeddedPostgres.postgresDatabase)
    private val macaroonService: MacaroonService = MacaroonService("localhost", "secret")

    @BeforeAll
    fun setup() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    @AfterAll
    fun tearDown() {
        embeddedPostgres.close()
    }

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