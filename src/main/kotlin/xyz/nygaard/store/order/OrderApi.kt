package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthHeader
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.user.TokenService
import java.util.*

fun Routing.registerOrders(
    orderService: OrderService,
    tokenService: TokenService,
    productService: ProductService,
    invoiceService: InvoiceService
) {
    post("/orders/invoice/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: call.request.cookies["authorization"] ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")
        val authorization = AuthHeader.deserialize(authHeader)

        val product = productService.getProduct(productId)
        val invoice = invoiceService.createInvoice(product.price, product.name)
        return@post call.respond(
            orderService.createWithInvoice(
                macaroon = authorization.macaroon,
                product = product,
                invoice = invoice
            )
        )
    }

    post("/orders/balance/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: call.request.cookies["authorization"] ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")
        val authorization = AuthHeader.deserialize(authHeader)

        val token = tokenService.fetchToken(authorization.macaroon) ?: throw IllegalStateException()
        val product = productService.getProduct(productId)

        if (token.balance < product.price) return@post call.respond(HttpStatusCode.BadRequest, "Insufficent balance")

        orderService.placeOrderWithBalance(authorization.macaroon, product)
        call.respond(HttpStatusCode.OK)
    }

    get("/orders") {
        val authHeader = call.request.header("Authorization")
            ?: call.request.cookies["authorization"] ?: return@get call.respond(HttpStatusCode.Unauthorized)

        val authorization = AuthHeader.deserialize(authHeader)
        call.respond(orderService.getOrders(authorization.macaroon.extractUserId()).map { it.toDto() })
    }

    get("/orders/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: call.request.cookies["authorization"] ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val orderId = call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Missing order id"
        )

        val authorization = AuthHeader.deserialize(authHeader)
        orderService.getOrder(authorization.macaroon.extractUserId(), orderId)
    }

    get("/invoices/{uuid}") {
        val authHeader = call.request.header("Authorization")
            ?: call.request.cookies["authorization"] ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val invoiceUUID =
            call.parameters["uuid"].let { UUID.fromString(it) } ?: throw RuntimeException("Missing invoice uuid")

        val authorization = AuthHeader.deserialize(authHeader)
        log.info("Lookup on invoice: $invoiceUUID")

        val invoice = invoiceService.lookupAndUpdate(invoiceUUID, authorization.macaroon)

        if (invoice != null) {
            call.respond(invoice)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}