package xyz.nygaard

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xyz.nygaard.store.invoice.Invoice
import java.util.*

internal class MacaroonServiceTest {

    private val macaroonService = MacaroonService("localhost", "secret")

    @Test
    fun getMacaroon() {
        val macaroon = macaroonService.createMacaroon(validInvoice().rhash)
        println(macaroon.serialize())
        assertNotNull(macaroon)
        assertTrue(macaroonService.isValid(macaroon))
    }

    @Test
    fun `deserialize identifier`() {
        val userId = UUID.randomUUID()
        val macaroon = macaroonService.createMacaroon(validInvoice().rhash, userId)
        val identifier = Identifier.deserialize(macaroon.identifier)
        assertEquals(0, identifier.version)
        assertEquals(userId, identifier.userId)
        assertEquals("396c9e196c1a21f4d8b99ab7cb3685e7f6582e5f1740ad0d960c9d16b4a5c622", identifier.paymentHash)
    }

    fun validInvoice() = Invoice(
        rhash = "396c9e196c1a21f4d8b99ab7cb3685e7f6582e5f1740ad0d960c9d16b4a5c622",
        paymentRequest = "lnbc100n1ps09w9hpp589kfuxtvrgslfk9en2mukd59ulm9stjlzaq26rvkpjw3dd99cc3qdqddpjkjgrtde6hgcqzpgsp57dcm36skn9hzrfzvrfpn30gj6uzkmfwh9lknxy2cudu6z4ng98aq9qyyssq88taygk57gsumjp9m9v8yfkvrsy6mss23kt4rlhgaxjz6fzk7anntza7qa79907nslcg9ygf98pmce4mkky5d7jtcg9cp8nkee6r7scp6fwsvc",
        amount = 40L
    )
    // preimage: 9ec2d9ee21189cde57964e8af3d798eccf9a13d2ac7b06da03371f9a9e0b9d50
}