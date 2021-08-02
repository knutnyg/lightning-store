package xyz.nygaard.store.signpost

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.registerSignpostApi() {
    get("/signpost") { call.respond(HttpStatusCode.NotImplemented, "Not implemented") }
    put("/signpost") { call.respond(HttpStatusCode.NotImplemented, "Not implemented") }
}