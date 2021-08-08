package xyz.nygaard.store.order

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import xyz.nygaard.extractUserId
import xyz.nygaard.store.invoice.Invoice
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.user.Token
import java.util.*
import javax.sql.DataSource

class OrderService(
    private val dataSource: DataSource,
    private val invoiceService: InvoiceService,
    private val productService: ProductService
) {
    fun createOrder(macaroon: Macaroon, productId: UUID): Invoice {
        val product = productService.getProduct(productId)
        val invoice = invoiceService.createInvoice(product.price, "1x${product.name}: $productId")
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

    fun getOrder(id: UUID): Order {
        return dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement("SELECT * from orders o INNER JOIN invoices i on o.invoice_id = i.id INNER JOIN token t on o.token_id = t.id INNER JOIN products p on o.product_id = p.id WHERE id = ?")
                .use { statement ->
                    statement.setString(1, id.toString())
                    statement.executeQuery()
                }.toList {
                    Order(
                        id = UUID.fromString(this.getString("id")),
                        invoice = Invoice(
                            id = UUID.fromString(this.getString("i.id")),
                            memo = this.getString("memo"),
                            rhash = this.getString("rhash"),
                            settled = this.getTimestamp("settled")?.toLocalDateTime(),
                            paymentRequest = this.getString("paymentRequest"),
                            preimage = this.getString("preimage"),
                            amount = this.getLong("amount")
                        ),
                        token = Token(
                            macaroon = MacaroonsBuilder.deserialize("macaroon"),
                            balance = this.getInt("balance"),
                            revoked = this.getBoolean("revoked")
                        ),
                        product = ""
                    )
                }.first()
        }
    }

    fun isSettled() {

    }
}

class Order(
    val id: UUID,
    val invoice: Invoice,
    val token: Token,
    val product: String
)