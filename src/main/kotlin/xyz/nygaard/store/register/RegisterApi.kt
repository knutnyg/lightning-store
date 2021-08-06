package xyz.nygaard.store.register

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.extractRHash
import xyz.nygaard.log
import xyz.nygaard.store.AuthHeader
import xyz.nygaard.store.invoice.InvoiceService

fun Routing.registerRegisterApi(
    invoiceService: InvoiceService,
) {
    put("/register") {
        call.respond("Ok")
    }
    get("/open/register") {
        val authHeader = call.request.header("Authorization")
            ?: return@get call.respond(HttpStatusCode.Unauthorized)

        log.info("Caller looking up preimage for registration")
        val authorization = AuthHeader.deserialize(authHeader)

        val invoice = invoiceService.lookupAndUpdate(authorization.macaroon.extractRHash()) ?: return@get call.respond(
            HttpStatusCode.NotFound
        )
        return@get call.respond(invoice)
    }
}