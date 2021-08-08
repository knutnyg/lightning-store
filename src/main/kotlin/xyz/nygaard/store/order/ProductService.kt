package xyz.nygaard.store.order

import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import java.util.*
import javax.sql.DataSource

class ProductService(val dataSource: DataSource) {
    fun getProduct(uuid: UUID): Product {
        return dataSource.connectionAutoCommit().use {
            it.prepareStatement("SELECT * FROM products WHERE id = ?")
                .use {
                    it.setString(1, uuid.toString())
                    it.executeQuery()
                        .toList {
                            Product(
                                id = UUID.fromString(this.getString("id")),
                                name = this.getString("name"),
                                price = this.getLong("price")
                            )
                        }.first()
                }
        }
    }
}

data class Product(
    val id: UUID,
    val name: String,
    val price: Long
)