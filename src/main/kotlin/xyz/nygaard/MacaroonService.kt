package xyz.nygaard

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import com.github.nitram509.jmacaroons.MacaroonsVerifier
import xyz.nygaard.store.invoice.Invoice
import java.util.*

class Identifier(
    val version: Int,
    val userId: UUID,
    val paymentHash: String
) {
    companion object {
        fun deserialize(identifier: String): Identifier {
            val split = identifier.split('\n')
            val version = split[0].split(" = ").last().trim().toInt()
            val userId = split[1].split(" = ").last().trim().let { UUID.fromString(it) }
            val paymentHash = split[2].split(" = ").last().trim()

            return Identifier(version, userId, paymentHash)
        }
    }

    fun serialize(): String {
        return """
            version = $version
            user_id = $userId
            payment_hash = $paymentHash
        """.trimIndent()
    }
}

class MacaroonService {

    val secret = "mysecret"

    fun createMacaroon(invoice: Invoice, userId: UUID = UUID.randomUUID()): Macaroon {
        return MacaroonsBuilder(
            "localhost:8000",
            secret,
            Identifier(0, userId, invoice.rhash).serialize()
        )
            .add_first_party_caveat("services = invoices:0")
            .macaroon
    }

    fun extractPaymentHash(macaroon: Macaroon) = Identifier.deserialize(macaroon.identifier).paymentHash

    fun isValid(macaroon: Macaroon) = MacaroonsVerifier(macaroon)
        .satisfyExact("services = invoices:0")
        .isValid(secret)
}