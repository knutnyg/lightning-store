package xyz.nygaard.store.invoice

import com.github.nitram509.jmacaroons.Macaroon
import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import xyz.nygaard.extractUserId
import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndCreatedInvoice
import xyz.nygaard.log
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class InvoiceService(
    private val dataSource: DataSource,
    private val lndClient: LndApiWrapper
) {
    private fun getInvoice(uuid: UUID, macaroon: Macaroon): Invoice? {
        return dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement("SELECT * FROM INVOICES i INNER JOIN orders o on i.id = o.invoice_id WHERE i.id = ? AND o.token_id = ?")
                .use {
                    it.setString(1, uuid.toString())
                    it.setString(2, macaroon.extractUserId().toString())
                    it.executeQuery()
                        .toList {
                            Invoice(
                                id = UUID.fromString(getString("id")),
                                memo = getString("memo"),
                                rhash = getString("rhash"),
                                paymentRequest = getString("payment_req"),
                                settled = if (getTimestamp("settled") != null) (getTimestamp("settled").toLocalDateTime()) else null,
                                preimage = getString("preimage"),
                                amount = getLong("amount")
                            )
                        }.firstOrNull()
                }
        }
    }

    private fun getInvoice(rhash: String): Invoice? {
        return dataSource.connectionAutoCommit().use { connection ->
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
                                preimage = getString("preimage"),
                                amount = getLong("amount")
                            )
                        }.firstOrNull()
                }
        }
    }

    fun lookupAndUpdate(invoiceId: UUID, macaroon: Macaroon): Invoice? {
        return getInvoice(invoiceId, macaroon)?.let { lookupAndUpdate(it) }
    }

    fun lookupAndUpdate(rhash: String): Invoice? {
        return getInvoice(rhash)?.let { lookupAndUpdate(it) }
    }

    private fun lookupAndUpdate(invoice: Invoice): Invoice? {
        val updatedLndInvoice = lndClient.lookupInvoice(invoice)

        return if (updatedLndInvoice.settled && invoice.settled == null) {
            // We have an invoice that has been settled and need to do side effect
            val settledTimestamp = updateSettled(invoice, updatedLndInvoice.preimage!!)
            invoice.copy(settled = settledTimestamp, id = invoice.id, preimage = updatedLndInvoice.preimage)
        } else {
            updateLookup(invoice)
            invoice.copy(id = invoice.id)
        }
    }

    fun createInvoice(
        amount: Long = 10,
        memo: String = ""
    ): Invoice {
        val lndInvoice = lndClient.addInvoice(amount, memo)
        val uuid = newInvoice(lndInvoice, memo, amount)

        return Invoice(
            id = uuid,
            memo = memo,
            rhash = lndInvoice.rhash,
            paymentRequest = lndInvoice.paymentRequest,
            amount = amount
        )
    }

    private fun newInvoice(createdInvoice: LndCreatedInvoice, memo: String, amount: Long): UUID {
        val uuid = UUID.randomUUID()
        dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement(
                """
               INSERT INTO INVOICES(id, rhash, payment_req, settled, memo, amount)
                VALUES (?, ?, ?, ?, ?, ?)
            """
            ).use {
                it.setString(1, uuid.toString())
                it.setString(2, createdInvoice.rhash)
                it.setString(3, createdInvoice.paymentRequest)
                it.setTimestamp(4, null)
                it.setString(5, memo)
                it.setLong(6, amount)
                it.executeUpdate()
            }
        }
        return uuid
    }

    private fun updateSettled(invoice: Invoice, preimage: String): LocalDateTime {
        val settled = LocalDateTime.now()
        dataSource.connection.apply { autoCommit = false }.use { connection ->
            try {
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

                connection.prepareStatement("UPDATE orders o SET settled = ? WHERE o.invoice_id = ?").use {
                    it.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                    it.setString(2, invoice.id.toString())
                    it.executeUpdate().run {
                        if (this > 0) {
                            log.info("Updated $this orders with settled = true")
                        }
                    }
                    connection.commit()
                }
            } catch (e: Exception) {
                connection.rollback()
                log.error("Failed to update invoice")
                throw RuntimeException("")
            }
            return settled
        }
    }

    private fun updateLookup(invoice: Invoice): LocalDateTime {
        val lookup = LocalDateTime.now()
        dataSource.connectionAutoCommit().use { connection ->
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
    val preimage: String? = null,
    val amount: Long
) {
    fun toDto() = InvoiceDto(id, memo, rhash, settled, paymentRequest, preimage, amount)
}

data class InvoiceDto(
    val id: UUID? = null,
    val memo: String? = null,
    val rhash: String,
    val settled: LocalDateTime? = null,
    val paymentRequest: String,
    val preimage: String? = null,
    val amount: Long
)
