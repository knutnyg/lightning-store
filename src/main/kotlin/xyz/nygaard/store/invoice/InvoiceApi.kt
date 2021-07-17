package xyz.nygaard.store.invoice

import com.github.nitram509.jmacaroons.MacaroonsBuilder
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.CreateInvoiceRequest
import xyz.nygaard.CreateInvoiceResponse
import xyz.nygaard.MacaroonService
import xyz.nygaard.log
import xyz.nygaard.util.sha256
import java.util.*

fun Routing.registerInvoiceApi(invoiceService: InvoiceService, macaroonService: MacaroonService) {

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
        val authHeader = call.request.header("Authorization")
        if (authHeader == null) {
            log.info("Call missing authentication")
            val userId = UUID.randomUUID()
            val invoice = invoiceService.createInvoice(1, userId.toString())
            val macaroon = macaroonService.createMacaroon(invoice)
            call.response.headers.append(
                "WWW-Authenticate",
                "LSAT macaroon=\"${macaroon.serialize()}\", invoice=\"${invoice.paymentRequest}\""
            )
            return@get call.respond(HttpStatusCode.PaymentRequired)
        }

        val (type, rest) = authHeader.split(" ").let { it.first() to it.last() }
        val (incomingMacaroon, preimage) = rest.split(":")
            .let { split -> split.first().let { MacaroonsBuilder.deserialize(it) } to split.last() }

        if (type != "LSAT") return@get call.respond(HttpStatusCode.BadRequest, "Authentication digest must be LSAT")
            .also {
                log.info("Caller using wrong authentication type, got $type")
            }
        if (!macaroonService.isValid(incomingMacaroon)) return@get call.respond(HttpStatusCode.Unauthorized)

        if (preimage.sha256() != macaroonService.extractPaymentHash(incomingMacaroon)) return@get call.respond(
            HttpStatusCode.BadRequest, "Preimage does not correspond to payment hash"
        ).also {
            log.info("Preimage does not correspond to payment hash")
        }

        call.respond(HttpStatusCode.OK)

    }
}