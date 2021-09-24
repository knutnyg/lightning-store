package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthorizationKey
import java.util.*

fun Route.registerAdmin(
    productService: ProductService,
) {
    post("/admin/product/{id}/upload") {
        val authorization = call.attributes[AuthorizationKey]
        log.info("in upload")

        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest)

        log.info("productid", productId)

        val mediaType = call.request.contentType().toString()

        log.info("mediatype", mediaType)

        val product = withContext(Dispatchers.IO) {
            log.info("in dispatcher")
            val data = call.receiveStream().use { it.readBytes() }
            log.info("read stream")
            try {
                log.info("in try")
                productService.updateProduct(
                    UpdateProduct(
                        id = productId,
                        mediaType = mediaType,
                        payload_v2 = data,
                    )
                )
                log.info("after update")
            } catch (e: Exception) {
                log.error("Failed to update product", e)
            }
            log.info("get product")
            productService.getProduct(productId).toDto()
        }
        log.info("respond")
        call.respond(product)
    }
}
