package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import xyz.nygaard.extractUserId
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.ResourceFetcher
import xyz.nygaard.store.auth.AuthorizationKey
import java.util.*

fun Route.registerProducts(
    productService: ProductService,
) {
    get("/products/{id}") {
        val authorization = call.attributes[AuthorizationKey]

        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest)
        if (productService.hasPurchased(authorization.macaroon.extractUserId(), productId)) {
            call.respond(productService.getProduct(productId).toDto())
        } else {
            call.respond(HttpStatusCode.PaymentRequired)
        }
    }

    get("/products/{id}/data") {
        val authorization = call.attributes[AuthorizationKey]

        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest)
        if (productService.hasPurchased(authorization.macaroon.extractUserId(), productId)) {
            val product = productService.getProduct(productId)
            if (product == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                if (product.payload_v2 != null) {
                    call.respondBytes(
                        product.payload_v2,
                        contentType = ContentType.parse(product.mediaType ?: "application/octet-stream")
                    )
                } else {
                    call.respondText(product.payload ?: "")
                }
            }
        } else {
            call.respond(HttpStatusCode.PaymentRequired)
        }
    }
}
