package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

fun Route.registerAdmin(
    productService: ProductService,
) {
    post("/admin/product/{id}/upload") {
        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest)

        val mediaType = call.request.contentType().toString()

        val product = withContext(Dispatchers.IO) {
            val data = call.receiveStream().use { it.readBytes() }
                productService.updateProduct(
                    UpdateProduct(
                        id = productId,
                        mediaType = mediaType,
                        payload_v2 = data,
                    )
                )
            productService.getProduct(productId).toDto()
        }
        call.respond(product)
    }

    data class CreateProduct(
        val name: String,
        val mediaType: String,
        val data: ByteArray,
        val price: Long,
    )

    post("/admin/product") {
        val product = withContext(Dispatchers.IO) {
            val req = call.receive(CreateProduct::class)

            val id = UUID.randomUUID()

            productService.insertProduct(
                InsertProduct(
                    id = id,
                    name = req.name,
                    price = req.price,
                    mediaType = req.mediaType,
                    payload_v2 = req.data,
                    uri = null,
                )
            )
            productService.getProduct(id).toDto()
        }
        call.respond(product)
    }
}
