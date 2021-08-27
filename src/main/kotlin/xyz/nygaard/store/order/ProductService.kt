package xyz.nygaard.store.order

import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import java.net.URI
import java.util.*
import javax.sql.DataSource

class ProductService(val dataSource: DataSource) {
    fun getProduct(uuid: UUID): Product {
        return dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement("SELECT * FROM products WHERE id = ?")
                .use { preparedStatement ->
                    preparedStatement.setString(1, uuid.toString())
                    preparedStatement.executeQuery()
                        .toList {
                            Product(
                                id = UUID.fromString(this.getString("id")),
                                name = this.getString("name"),
                                price = this.getLong("price"),
                                payload = this.getString("payload"),
                                uri = this.getString("uri")?.let { URI.create(it) }
                            )
                        }.first()
                }
        }
    }

    fun hasPurchased(tokenId: UUID, productId: UUID): Boolean {
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("SELECT * FROM products p INNER JOIN orders o on p.id = o.product_id WHERE o.token_id = ? AND p.id = ? AND settled IS NOT null")
                .use { preparedStatement ->
                    preparedStatement.setString(1, tokenId.toString())
                    preparedStatement.setString(2, productId.toString())
                    preparedStatement.executeQuery()
                        .toList {
                            true
                        }.size == 1
                }
        }
    }
}

class Product(
    val id: UUID,
    val name: String,
    val price: Long,
    val payload: String?,
    val uri: URI?
) {
    fun toDto() = ProductDto(id, name, price, payload)
}

data class ProductDto(
    val id: UUID,
    val name: String,
    val price: Long,
    val payload: String?
)