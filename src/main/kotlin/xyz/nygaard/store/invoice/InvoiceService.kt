package xyz.nygaard.store.invoice

import xyz.nygaard.db.toList
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndCreatedInvoice
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class InvoiceService(
    private val dataSource: DataSource,
    private val lndClient: LndApiWrapper
) {
    internal fun getInvoice(uuid: UUID): Invoice? {
        return dataSource.connection.use { connection ->
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
                                settled = if (getTimestamp("settled") != null) (getTimestamp("settled").toLocalDateTime()) else null,
                                preimage = getString("preimage")
                            )
                        }.firstOrNull()
                }
        }
    }

    internal fun getInvoice(rhash: String): Invoice? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM INVOICES WHERE rhash = ?")
                .use {
                    it.setString(1, rhash)
                    it.executeQuery()
                        .toList {
                            Invoice(
                                id = UUID.fromString(getString("id")),
                                memo = getString("memo"),
                                rhash = getString("rhash"),
                                paymentRequest = getString("payment_req"),
                                settled = if (getTimestamp("settled") != null) (getTimestamp("settled").toLocalDateTime()) else null,
                                preimage = getString("preimage")
                            )
                        }.firstOrNull()
                }
        }
    }

    fun lookupAndUpdate(uuid: UUID): Invoice? {
        return getInvoice(uuid)?.let { lookupAndUpdate(it) }
    }

    fun lookupAndUpdate(rhash: String): Invoice? {
        return getInvoice(rhash)?.let { lookupAndUpdate(it) }
    }

    private fun lookupAndUpdate(invoice: Invoice): Invoice? {
        val updatedLndInvoice = lndClient.lookupInvoice(invoice)

        return if (updatedLndInvoice.settled && invoice.settled == null) {
            val settledTimestamp = updateSettled(invoice, updatedLndInvoice.preimage!!)
            invoice.copy(settled = settledTimestamp, id = invoice.id, preimage = updatedLndInvoice.preimage)
        } else {
            updateLookup(invoice)
            invoice.copy(id = invoice.id)
        }
    }

    fun createInvoice(
        amount: Long = 10L,
        memo: String = ""
    ): Invoice {
        val lndInvoice = lndClient.addInvoice(amount, memo)
        val uuid = newInvoice(lndInvoice, memo)

        return Invoice(
            id = uuid,
            memo = memo,
            rhash = lndInvoice.rhash,
            paymentRequest = lndInvoice.paymentRequest
        )
    }

    private fun newInvoice(createdInvoice: LndCreatedInvoice, memo: String): UUID {
        val uuid = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
               INSERT INTO INVOICES(id, rhash, payment_req, settled, memo)
                VALUES (?, ?, ?, ?, ?)
            """
            ).use {
                it.setString(1, uuid.toString())
                it.setString(2, createdInvoice.rhash)
                it.setString(3, createdInvoice.paymentRequest)
                it.setTimestamp(4, null)
                it.setString(5, memo)
                it.executeUpdate()
            }
        }
        return uuid
    }

    private fun updateSettled(invoice: Invoice, preimage: String): LocalDateTime {
        val settled = LocalDateTime.now()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE invoices
                SET settled = ?, last_lookup = ?, preimage = ?
                WHERE id = ? 
            """
            ).use {
                it.setTimestamp(1, Timestamp.valueOf(settled))
                it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                it.setString(3, preimage)
                it.setString(4, invoice.id.toString())
                it.executeUpdate()
            }
        }
        return settled
    }

    private fun updateLookup(invoice: Invoice): LocalDateTime {
        val lookup = LocalDateTime.now()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE invoices
                SET last_lookup = ?
                WHERE id = ? 
            """
            ).use {
                it.setTimestamp(1, Timestamp.valueOf(lookup))
                it.setString(2, invoice.id.toString())
                it.executeUpdate()
            }
        }
        return lookup
    }
}

data class Invoice(
    val id: UUID? = null,
    val memo: String? = null,
    val rhash: String,
    val settled: LocalDateTime? = null,
    val paymentRequest: String,
    val preimage: String? = null
)
