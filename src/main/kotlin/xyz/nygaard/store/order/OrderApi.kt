package xyz.nygaard.store.order

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.CreateInvoiceRequest
import xyz.nygaard.extractUserId
import xyz.nygaard.log
import xyz.nygaard.store.AuthHeader
import xyz.nygaard.store.invoice.Invoice
import xyz.nygaard.store.invoice.InvoiceService
import java.util.*

fun Routing.registerOrders(orderService: OrderService) {
    put("/orders/{id}") {
        val authHeader = call.request.header("Authorization")
            ?: return@put call.respond(HttpStatusCode.Unauthorized)

        val productId = UUID.fromString(call.parameters["id"])

        log.info("Caller looking up preimage for registration")
        val authorization = AuthHeader.deserialize(authHeader)

        val invoice = orderService.createOrder(
            macaroon = authorization.macaroon,
            productId = productId,
        )
        call.respond(invoice)
    }

    get("/orders") {
//        orderService.getOrder()
    }
}