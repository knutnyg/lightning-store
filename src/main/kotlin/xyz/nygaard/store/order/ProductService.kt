package xyz.nygaard.store.order

import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.ResourceFetcher
import java.net.URI
import java.util.*
import javax.sql.DataSource

class ProductService(val dataSource: DataSource, val resourceFetcher: Fetcher) {
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
                                mediaType = this.getString("mediatype"),
                                payload_v2 = this.getBytes("payload_v2"),
                                uri = this.getString("uri")?.let { URI.create(it) }
                            )
                        }.first()
                }
                .let {
                    if (it.uri != null) {
                        it.payload = Base64.getEncoder().encodeToString(resourceFetcher.fetch(it.uri))
                    }
                    it
                }
        }
    }

    fun hasPurchased(tokenId: UUID, productId: UUID): Boolean {
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("SELECT * FROM products p INNER JOIN orders o on p.id = o.product_id LEFT JOIN bundle b on b.id = p.bundle_id LEFT JOIN bundle_product bp on bp.bundle_id = b.id WHERE o.token_id = ? AND (p.id = ? OR bp.product_id = ?) AND settled IS NOT null")
                .use { preparedStatement ->
                    preparedStatement.setString(1, tokenId.toString())
                    preparedStatement.setString(2, productId.toString())
                    preparedStatement.setString(3, productId.toString())
                    preparedStatement.executeQuery()
                        .toList {
                            true
                        }.size == 1
                }
        }
    }

    fun updateProduct(update: UpdateProduct): Int {
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("UPDATE products SET mediatype = ?, payload_v2 = ? WHERE id = ? ").use { preparedStatement ->
                preparedStatement.setString(1, update.mediaType)
                preparedStatement.setBytes(2, update.payload_v2)
                preparedStatement.setString(3, update.id.toString())
                preparedStatement.executeUpdate()
            }
        }
    }
}

data class UpdateProduct(
    val id: UUID,
    val mediaType: String,
    val payload_v2: ByteArray,
)

data class Product(
    val id: UUID,
    val name: String,
    val mediaType: String? = null,
    val payload_v2: ByteArray? = null,
    val price: Long,
    var payload: String?,
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