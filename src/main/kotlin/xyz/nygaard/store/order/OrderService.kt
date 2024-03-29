package xyz.nygaard.store.order

import com.github.nitram509.jmacaroons.Macaroon
import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.invoice.Invoice
import xyz.nygaard.store.invoice.InvoiceDto
import java.net.URI
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class OrderService(
    private val dataSource: DataSource
) {
    fun createWithInvoice(invoice: Invoice, productId: UUID, macaroon: Macaroon): Invoice {
        dataSource.connectionAutoCommit().use { connection ->
            val id = UUID.randomUUID()
            connection.prepareStatement("INSERT INTO orders(id, token_id, invoice_id, product_id) VALUES(?, ?, ?, ?)")
                .use { statement ->
                    statement.setString(1, id.toString())
                    statement.setString(2, macaroon.extractUserId().toString())
                    statement.setString(3, invoice.id.toString())
                    statement.setString(4, productId.toString())
                    statement.executeUpdate()
                }
            return invoice
        }
    }

    fun placeOrderWithBalance(macaroon: Macaroon, product: Product) {
        // Create order
        dataSource.connection.apply { autoCommit = false }
            .use { connection ->
                try {
                    val id = UUID.randomUUID()
                    connection.prepareStatement("INSERT INTO orders(id, token_id, invoice_id, product_id) VALUES(?, ?, ?, ?)")
                        .use { statement ->
                            statement.setString(1, id.toString())
                            statement.setString(2, macaroon.extractUserId().toString())
                            statement.setString(3, null)
                            statement.setString(4, product.id.toString())
                            statement.executeUpdate()
                        }
                    connection.prepareStatement("UPDATE token SET balance = balance - ? WHERE id = ?")
                        .use {
                            it.setLong(1, product.price)
                            it.setString(2, macaroon.extractUserId().toString())
                            it.executeUpdate()
                        }
                    connection.commit()
                } catch (e: Exception) {
                    log.error("placing order with balance failed. Rolling back")
                    connection.rollback()
                    throw IllegalStateException("Error placing order and updating balance")
                }
            }
    }

    fun getOrders(userId: UUID): List<Order> {
        return dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement("SELECT i.settled as invoice_settled, * from orders o LEFT OUTER JOIN invoices i on o.invoice_id = i.id INNER JOIN token t on o.token_id = t.id INNER JOIN products AS p on o.product_id = p.id WHERE t.id = ?")
                .use { statement ->
                    statement.setString(1, userId.toString())
                    statement.executeQuery()
                        .toList {
                            Order(
                                id = UUID.fromString(this.getString("id")),
                                if (this.getString("invoice_id") != null) Invoice(
                                    id = UUID.fromString(this.getString("invoice_id")),
                                    memo = this.getString("memo"),
                                    rhash = this.getString("rhash"),
                                    settled = this.getTimestamp("invoice_settled")?.toLocalDateTime(),
                                    paymentRequest = this.getString("payment_req"),
                                    preimage = this.getString("preimage"),
                                    amount = this.getLong("amount")
                                ) else null,
                                product = Product(
                                    id = this.getString("product_id").let { UUID.fromString(it) },
                                    name = this.getString("name"),
                                    price = this.getLong("price"),
                                    payload = this.getString("payload")
                                )
                            )
                        }
                }
        }
    }
}

class Order(
    val id: UUID,
    val invoice: Invoice?,
    val product: Product,
) {
    fun toDto(): OrderDto {
        return OrderDto(id, invoice?.toDto(), product.toDto())
    }
}

data class OrderDto(
    val id: UUID,
    val invoice: InvoiceDto?,
    val productId: ProductDto,
)
