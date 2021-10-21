package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.auth.AuthorizationKey
import xyz.nygaard.store.invoice.InvoiceService
import java.util.*

fun Route.registerProducts(
    productService: ProductService,
    invoiceService: InvoiceService,
    orderService: OrderService,
    resourceFetcher: Fetcher
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

        //TODO: As long as the user has a token we are good
        //if (productService.hasPurchased(authorization.macaroon.extractUserId(), productId)) {
        val product = productService.getProduct(productId)
        if (product.payload_v2 == null || product.payload_v2.isEmpty()) {

            val image = resourceFetcher.requestNewImage()
            productService.updateProduct(UpdateProduct(productId, "image/png", image))
            productService.addToGalleryBundle(productId)

            call.respondBytes(
                image,
                contentType = ContentType.Image.PNG
            )
        } else {
            call.respondBytes(
                product.payload_v2,
                contentType = ContentType.parse(product.mediaType ?: "application/octet-stream")
            )
        }
    }

    post("/products/image") {
        val authorization = call.attributes[AuthorizationKey]
        val imageId = UUID.randomUUID()
        val invoice = invoiceService.createInvoice(1, imageId.toString())
        productService.insertProduct(
            InsertProduct(
                id = imageId,
                name = "image-${imageId}",
                price = 1L
            )
        )
        orderService.createWithInvoice(invoice, imageId, authorization.macaroon)
        call.respond(HttpStatusCode.OK, invoice)
    }

    get("/bundle/{id}") {
        val bundleId = call.parameters["id"]?.toInt()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "parameter 'id' is missing")
        val ids = productService.getProductIds(bundleId)
        call.respond(ids.shuffled().take(15))
    }

    get("/minigallery") {
        val ids = productService.getProductIds(2)
        call.respond(ids.reversed().take(8))
    }
}
