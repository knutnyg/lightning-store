package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.extractUserId
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.ResourceFetcher
import xyz.nygaard.store.auth.AuthHeader
import java.io.File
import java.util.*

fun Routing.registerProducts(
    productService: ProductService,
    resourcesPath: String = "",
    resourceFetcher: Fetcher = ResourceFetcher()
) {
    get("products/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: return@get call.respond(HttpStatusCode.Unauthorized)

        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest)
        val authorization = AuthHeader.deserialize(authHeader)
        if (productService.hasPurchased(authorization.macaroon.extractUserId(), productId)) {
            val product = productService.getProduct(productId)
            if (product.uri != null) {
                call.respondBytes(contentType = ContentType.Image.JPEG, HttpStatusCode.OK) {
                    resourceFetcher.fetch(product.uri)
                }
            } else {
                call.respond(product.toDto())
            }
        } else {
            call.respond(HttpStatusCode.PaymentRequired)
        }
    }
}