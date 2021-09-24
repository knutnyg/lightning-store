package xyz.nygaard.store.order

import com.google.common.net.MediaType
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import xyz.nygaard.store.auth.AuthorizationKey
import java.util.*

fun Route.registerAdmin(
    productService: ProductService,
) {
    post("/admin/product/{id}/upload") {
        val authorization = call.attributes[AuthorizationKey]

        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@post call.respond(HttpStatusCode.BadRequest)

        val mediaType = call.request.contentType().toString()

        runBlocking {
            val data = call.receiveStream().readAllBytes();

            productService.updateProduct(UpdateProduct(
                id = productId,
                mediaType = mediaType,
                payload_v2 = data,
            ))
            call.respond(productService.getProduct(productId).toDto())
        }
    }
}
