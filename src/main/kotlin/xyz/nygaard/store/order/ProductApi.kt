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

fun Routing.registerProducts(
    productService: ProductService,
    resourcesPath: String = "",
    resourceFetcher: Fetcher = ResourceFetcher()
) {
    get("products/{id}") {
        val authorization = call.attributes[AuthorizationKey]

        val productId =
            call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(HttpStatusCode.BadRequest)
        if (productService.hasPurchased(authorization.macaroon.extractUserId(), productId)) {
            val product = productService.getProduct(productId)
            if (product.uri != null) {
                call.respond(
                    product.copy(
                        payload = Base64.getEncoder().encodeToString(resourceFetcher.fetch(product.uri))
                    ).toDto()
                )
            } else {
                call.respond(product.toDto())
            }
        } else {
            call.respond(HttpStatusCode.PaymentRequired)
        }
    }
}