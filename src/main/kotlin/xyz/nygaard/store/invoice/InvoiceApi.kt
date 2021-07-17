package xyz.nygaard.store.invoice

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.CreateInvoiceRequest
import xyz.nygaard.CreateInvoiceResponse
import xyz.nygaard.log
import java.util.*

fun Routing.registerInvoiceApi(invoiceService: InvoiceService) {

    post("/invoices") {
        val req = call.receive(CreateInvoiceRequest::class)
        if (req.amount < 1) return@post call.respond(HttpStatusCode.BadRequest, "amount cannot be below 1 satoshi")
        if (req.memo.length > 200) return@post call.respond(
            HttpStatusCode.BadRequest,
            "memo got a max length of 200 characters"
        )

        log.info("Creating invoice req={}", req)

        val inv = invoiceService.createInvoice(
            amount = req.amount.toLong(),
            memo = req.memo
        )

        log.info("Created invoice inv={}", inv)

        val response = CreateInvoiceResponse(
            id = inv.id.toString(),
            memo = inv.memo,
            rhash = inv.rhash,
            paymentRequest = inv.paymentRequest
        )

        call.respond(response)
    }

    get("/invoices/{uuid}") {
        val uuid = call.parameters["uuid"] ?: throw RuntimeException("Missing invoice uuid")
        log.info("Lookup on invoice: $uuid")
        val invoice = invoiceService.lookupAndUpdate(UUID.fromString(uuid))

        if (invoice != null) {
            call.respond(invoice)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/invoices/forbidden") {
        call.respond(HttpStatusCode.OK)
    }
}