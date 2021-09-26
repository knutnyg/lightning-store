package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthorizationKey
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.user.TokenService
import java.util.*

fun Route.registerOrders(
    orderService: OrderService,
    tokenService: TokenService,
    productService: ProductService,
    invoiceService: InvoiceService
) {
    get("/invoices/{uuid}") {
        val authorization = call.attributes[AuthorizationKey]
        val invoiceUUID =
            call.parameters["uuid"].let { UUID.fromString(it) } ?: throw RuntimeException("Missing invoice uuid")

        log.info("Lookup on invoice: $invoiceUUID")

        val invoice = invoiceService.lookupAndUpdate(invoiceUUID, authorization.macaroon)

        if (invoice != null) {
            call.respond(invoice)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/orders") {
        val authorization = call.attributes[AuthorizationKey]

        call.respond(orderService.getOrders(authorization.macaroon.extractUserId()).map { it.toDto() })
    }

    post("/orders/invoice/{id}") {
        val authorization = call.attributes[AuthorizationKey]

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")

        val product = productService.getProduct(productId)
        val invoice = invoiceService.createInvoice(product.price, product.name)
        return@post call.respond(
            orderService.createWithInvoice(
                macaroon = authorization.macaroon,
                productId = product.id,
                invoice = invoice
            )
        )
    }

    post("/orders/balance/{id}") {
        val authorization = call.attributes[AuthorizationKey]

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")

        val token = tokenService.fetchToken(authorization.macaroon) ?: throw IllegalStateException()
        val product = productService.getProduct(productId)

        if (token.balance < product.price) return@post call.respond(HttpStatusCode.BadRequest, "Insufficent balance")

        orderService.placeOrderWithBalance(authorization.macaroon, product)
        call.respond(HttpStatusCode.OK)
    }



    get("/orders/{id}") {
        val authorization = call.attributes[AuthorizationKey]
        val orderId = call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Missing order id"
        )

        orderService.getOrder(authorization.macaroon.extractUserId(), orderId)
    }


}