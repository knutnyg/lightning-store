package xyz.nygaard.store.signpost

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.log
import xyz.nygaard.store.invoice.InvoiceService
import java.util.*

fun Routing.registerSignpostApi(invoiceService: InvoiceService, macaroonService: MacaroonService) {
    get("/signpost") {
        val authHeader = call.request.header("Authorization")
        if (authHeader == null) {
            log.info("Caller missing authentication")
            val userId = UUID.randomUUID()
            val invoice = invoiceService.createInvoice(1, userId.toString())
            val macaroon = macaroonService.createMacaroon(invoice.rhash)
            call.response.headers.append(
                "WWW-Authenticate",
                "LSAT macaroon=\"${macaroon.serialize()}\", invoice=\"${invoice.paymentRequest}\""
            )
            return@get call.respond(HttpStatusCode.PaymentRequired, "Payment Required")
        }

        call.respond(HttpStatusCode.NotImplemented, "Not implemented")
    }
    put("/signpost") { call.respond(HttpStatusCode.NotImplemented, "Not implemented") }
}