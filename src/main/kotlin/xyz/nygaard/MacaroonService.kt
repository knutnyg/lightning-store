package xyz.nygaard

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.store.invoice.Invoice
import java.util.*

class MacaroonService() {

    fun createMacaroon(invoice: Invoice): Macaroon {
        val userId = UUID.randomUUID()
        return MacaroonsBuilder(
            "localhost:8000", "mysecret", """
            version = 0
            user_id = $userId
            payment_hash = ${invoice.rhash}
        """.trimIndent()
        )
            .add_first_party_caveat("services = invoices:0")
            .macaroon
    }
}