package xyz.nygaard

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import com.github.nitram509.jmacaroons.MacaroonsVerifier
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

class MacaroonService(
    val location: String,
    val secret: String,
) {

    //TODO: Fix secret and location
    fun createMacaroon(invoiceRhash: String, userId: UUID = UUID.randomUUID()): Macaroon {
        return MacaroonsBuilder(
            location,
            secret,
            Identifier(0, userId, invoiceRhash).serialize()
        )
            .add_first_party_caveat("services = invoices:0")
            .macaroon
    }

    fun extractPaymentHash(macaroon: Macaroon) = Identifier.deserialize(macaroon.identifier).paymentHash

    fun isValid(macaroon: Macaroon) = MacaroonsVerifier(macaroon)
        .satisfyExact("services = invoices:0")
        .isValid(secret)
}

fun Macaroon.extractUserId() = Identifier.deserialize(this.identifier).userId
fun Macaroon.extractRHash() = Identifier.deserialize(this.identifier).paymentHash
