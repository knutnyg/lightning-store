package xyz.nygaard

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MacaroonServiceTest {

    private val macaroonService = MacaroonService()

    @Test
    fun getMacaroon() {
        val macaroon = macaroonService.createMacaroon(mockk(relaxed = true))
        macaroon.serialize()
        assertNotNull(macaroon)
    }
}