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
        call.respond(HttpStatusCode.NotImplemented, "Not implemented")
    }
    put("/signpost") { call.respond(HttpStatusCode.NotImplemented, "Not implemented") }
}