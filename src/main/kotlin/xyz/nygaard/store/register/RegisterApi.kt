package xyz.nygaard.store.register

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import xyz.nygaard.extractRHash
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthHeader
import xyz.nygaard.store.auth.AuthorizationKey
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.user.TokenService

fun Routing.registerRegisterApi(
    invoiceService: InvoiceService,
    tokenService: TokenService
) {
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

    put("/register") { call.respond("Ok") }

    get("/register") {
        val authorization = call.attributes[AuthorizationKey]

        log.info("Caller looking up token")

        val token = tokenService.fetchToken(authorization.macaroon) ?: return@get call.respond(HttpStatusCode.NotFound)
            .also { log.info("Received token not stored in database. Probably because we have deleted our entry") }

        call.response.cookies.append(
            name = "authorization",
            value = authorization.pack(),
            secure = true,
            httpOnly = true,
            domain = "/"
        )
        return@get call.respond(token.toDTO())
    }
}