package xyz.nygaard.store.invoice

import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.db.toList
import xyz.nygaard.lnd.LndApiWrapper
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

class InvoiceService(
    private val database: DatabaseInterface,
    private val lndClient: LndApiWrapper
) {
    internal fun getInvoice(uuid: UUID): Invoice? {
        return database.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM INVOICES WHERE ID = ?")
                .use {
                    it.setString(1, uuid.toString())
                    it.executeQuery()
                        .toList {
                            Invoice(
                                id = UUID.fromString(getString("id")),
                                memo = getString("memo"),
                                rhash = getString("rhash"),
                                paymentRequest = getString("payment_req"),
                                settled = if (getTimestamp("settled") != null) (getTimestamp("settled").toLocalDateTime()) else null
                            )
                        }.firstOrNull()
                }
        }
    }

    fun lookupAndUpdate(uuid: UUID): Invoice {
        getInvoice(uuid)
            ?.let { invoiceFromDB ->
                val updatedLndInvoice = lndClient.lookupInvoice(invoiceFromDB)

                if (updatedLndInvoice.settled && invoiceFromDB.settled == null) {
                    val settledTimestamp = updateSettled(invoiceFromDB)
                    return invoiceFromDB.copy(settled = settledTimestamp, id = invoiceFromDB.id)
                }
            }
        throw RuntimeException("Could not find invoice")
    }

    fun createInvoice(
        amount: Long = 500L,
        memo: String = ""
    ): Invoice {
        val lndInvoice = lndClient.addInvoice(amount, memo)
        val uuid = newInvoice(lndInvoice)

        return lndInvoice.copy(id = uuid)
    }


    private fun newInvoice(invoice: Invoice): UUID {
        val uuid = UUID.randomUUID()
        database.connection.use { connection ->
            connection.prepareStatement(
                """
               INSERT INTO INVOICES(id, rhash, payment_req, settled, memo)
                VALUES (?, ?, ?,? ,?)
            """
            ).use {
                it.setString(1, uuid.toString())
                it.setString(2, invoice.rhash)
                it.setString(3, invoice.paymentRequest)
                it.setTimestamp(4, if (invoice.settled != null) Timestamp.valueOf(invoice.settled) else null)
                it.setString(5, invoice.memo)
                it.executeUpdate()
            }
            connection.commit()
        }
        return uuid
    }

    private fun updateSettled(invoice: Invoice): LocalDateTime {
        val settled = LocalDateTime.now()
        database.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE INVOICES
                SET SETTLED = ?
                WHERE id = ? 
            """
            ).use {
                it.setTimestamp(1, Timestamp.valueOf(settled))
                it.setString(2, invoice.id.toString())
                it.executeUpdate()
            }
            connection.commit()
        }
        return settled
    }
}

data class Invoice(
    val id: UUID? = null,
    val memo: String? = null,
    val rhash: String,
    val settled: LocalDateTime? = null,
    val paymentRequest: String
)
