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
}
