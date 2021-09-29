package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.ResourceFetcher
import xyz.nygaard.store.auth.AuthorizationKey
import xyz.nygaard.store.invoice.InvoiceService
import java.util.*

val inProgress = mutableListOf<UUID>()
val inProgressSynchronized = Collections.synchronizedList(inProgress)

fun Route.registerProducts(
    productService: ProductService,
    invoiceService: InvoiceService,
    orderService: OrderService,
    resourceFetcher: ResourceFetcher
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
            } else if (product.payload_v2 == null || product.payload_v2.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                if (product.payload_v2 != null) {
                    call.respondBytes(
                        product.payload_v2,
                        contentType = ContentType.parse(product.mediaType ?: "application/octet-stream")
                    )
                } else {
                    call.respond(HttpStatusCode.NoContent)
                    synchronized(inProgress) {
                        if (productId in inProgressSynchronized) {
                            return@get
                        }
                    }
                    withContext(Dispatchers.IO) {
                        inProgressSynchronized.add(productId)
                        log.info("Fetching new image")
                        val image = resourceFetcher.requestNewImage()
                        log.info("Fetched new image")
                        productService.updateProduct(UpdateProduct(productId, product.mediaType!!, image))
                        log.info("Saved new image")
                        inProgressSynchronized.remove(productId)
                    }
                }
            }
        } else {
            call.respond(HttpStatusCode.PaymentRequired)
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
}
