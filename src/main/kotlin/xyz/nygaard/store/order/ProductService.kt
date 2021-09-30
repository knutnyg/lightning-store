package xyz.nygaard.store.order

import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import xyz.nygaard.log
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
                                mediaType = this.getString("mediatype"),
                                payload_v2 = this.getBytes("payload_v2"),
                            )
                        }.first()
                }
        }
    }

    fun addToGalleryBundle(productId: UUID) {
        log.info("Adding product: $productId to bundle: 2")
        return dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement("INSERT INTO bundle_product(bundle_id, product_id) VALUES (2, ?)")
                .use { preparedStatement ->
                    preparedStatement.setString(1, productId.toString())
                    preparedStatement.executeUpdate()
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

    fun insertProduct(p: InsertProduct): Int {
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("INSERT INTO products (id, name, price, mediatype, payload_v2) VALUES (?, ?, ?, ?, ?) ")
                .use { preparedStatement ->
                    preparedStatement.setString(1, p.id.toString())
                    preparedStatement.setString(2, p.name)
                    preparedStatement.setLong(3, p.price)
                    preparedStatement.setString(4, p.mediaType)
                    preparedStatement.setBytes(5, p.payload_v2)
                    preparedStatement.executeUpdate()
                }
        }
    }

    fun updateProduct(update: UpdateProduct): Int {
        log.info("Saving new image")
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("UPDATE products SET mediatype = ?, payload_v2 = ? WHERE id = ? ")
                .use { preparedStatement ->
                    preparedStatement.setString(1, update.mediaType)
                    preparedStatement.setBytes(2, update.payload_v2)
                    preparedStatement.setString(3, update.id.toString())
                    preparedStatement.executeUpdate()
                }
        }
    }

    fun getProductIds(bundleId: Int): List<UUID> {
        log.info("Fetching products in bundle: $bundleId")
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("SELECT * FROM bundle_product WHERE bundle_id = ?")
                .use { preparedStatement ->
                    preparedStatement.setInt(1, bundleId)
                    preparedStatement.executeQuery()
                        .toList { UUID.fromString(this.getString("product_id")) }
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
) {
    fun toDto() = ProductDto(id, name, price, payload)
}

data class InsertProduct(
    val id: UUID,
    val name: String,
    val mediaType: String? = null,
    val payload_v2: ByteArray? = null,
    val price: Long,
)

data class ProductDto(
    val id: UUID,
    val name: String,
    val price: Long,
    val payload: String?
)