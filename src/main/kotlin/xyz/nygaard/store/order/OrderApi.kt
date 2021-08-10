package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.AuthHeader
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.user.TokenService
import java.util.*

fun Routing.registerOrders(
    orderService: OrderService,
    tokenService: TokenService,
    productService: ProductService,
    invoiceService: InvoiceService
) {
    put("/orders/invoice/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: return@put call.respond(HttpStatusCode.Unauthorized)

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")
        val authorization = AuthHeader.deserialize(authHeader)

        val product = productService.getProduct(productId)
        invoiceService.createInvoice(product.price, "1x${product.name}: $productId")
            .let {
                return@put call.respond(
                    orderService.placeOrderWithInvoice(
                        macaroon = authorization.macaroon,
                        product = product,
                    )
                )
            }

    }

    put("/orders/balance/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: return@put call.respond(HttpStatusCode.Unauthorized)

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")
        val authorization = AuthHeader.deserialize(authHeader)

        val token = tokenService.fetchToken(authorization.macaroon) ?: throw IllegalStateException()
        val product = productService.getProduct(productId)

        if (token.balance < product.price) return@put call.respond(HttpStatusCode.BadRequest, "Insufficent balance")

        orderService.placeOrderWithBalance(authorization.macaroon, product)
        call.respond(HttpStatusCode.OK)
    }

    get("/orders") {
        val authHeader = call.request.header("Authorization")
            ?: return@get call.respond(HttpStatusCode.Unauthorized)

        val authorization = AuthHeader.deserialize(authHeader)
        call.respond(orderService.getOrders(authorization.macaroon.extractUserId()).map { it.toDto() })
    }

    get("/orders/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val orderId = call.parameters["id"].let { UUID.fromString(it) } ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Missing order id"
        )

        val authorization = AuthHeader.deserialize(authHeader)
        orderService.getOrder(authorization.macaroon.extractUserId(), orderId)
    }
}