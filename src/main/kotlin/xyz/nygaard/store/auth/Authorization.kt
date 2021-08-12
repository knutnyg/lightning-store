package xyz.nygaard.store.auth

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.util.*

fun Application.installLsatInterceptor(
    invoiceService: InvoiceService,
    macaroonService: MacaroonService,
    tokenService: TokenService,
    orderService: OrderService,
    productService: ProductService
) {
    intercept(ApplicationCallPipeline.Call) {
        if (!call.request.path().contains("/open")) {
            val authHeader = call.request.header("Authorization")
            if (authHeader == null) {
                log.info("Caller missing authentication")
                val tokenProduct = productService.getProduct(UUID.fromString("a64d4344-f964-4dfe-99a6-7b39a7eb91c1"))
                val invoice =
                    invoiceService.createInvoice(tokenProduct.price, "1x${tokenProduct.name}: ${tokenProduct.id}")
                val macaroon = macaroonService.createMacaroon(invoice.rhash)
                tokenService.createToken(macaroon)
                orderService.placeOrderWithInvoice(invoice, tokenProduct, macaroon)
                call.response.headers.append(
                    "WWW-Authenticate",
                    "LSAT macaroon=\"${macaroon.serialize()}\", invoice=\"${invoice.paymentRequest}\""
                )
                call.response.headers.append(
                    "Access-Control-Expose-Headers", "WWW-Authenticate"
                )
                call.respond(HttpStatusCode.PaymentRequired, "Payment Required")
                return@intercept finish()
            }

            val authorization = AuthHeader.deserialize(authHeader)

            if (authorization.type != "LSAT") {
                log.info("Caller using wrong authentication type, got ${authorization.type}")
                call.respond(HttpStatusCode.BadRequest, "Authentication digest must be LSAT")
                return@intercept finish()
            }
            if (!macaroonService.isValid(authorization.macaroon)) {
                log.info("Macaroon is invalid")
                call.respond(HttpStatusCode.Unauthorized)
                return@intercept finish()
            }

            if (authorization.preimage?.sha256() != macaroonService.extractPaymentHash(authorization.macaroon)) {
                log.info("Preimage does not correspond to payment hash")
                call.respond(HttpStatusCode.BadRequest, "Preimage does not correspond to payment hash")
                return@intercept finish()
            }
            return@intercept proceed()
        }
        return@intercept proceed()
    }
}

class AuthChallengeHeader(val type: String, val invoice: String, val macaroon: Macaroon) {
    companion object {
        fun deserialize(header: String): AuthChallengeHeader {
            val type = header.substring(0, 4)
            val invoiceAndMacaroon = header.drop(4).split(",").map { it.trim() }
            val rest2 = invoiceAndMacaroon.map { it.split("=") }
            val macaroon = rest2[0].last().replace("\"", "")
            val invoice = rest2[1].last().replace("\"", "")

            return AuthChallengeHeader(type, invoice, MacaroonsBuilder.deserialize(macaroon))
        }
    }
}

class AuthHeader(
    val type: String,
    val macaroon: Macaroon,
    val preimage: String?
) {
    companion object {
        fun deserialize(header: String): AuthHeader {
            val (type, rest) = header.split(" ").let { it.first() to it.last() }
            val split = rest.split(":")
            val (macaroon, preimage) = if (split.size == 2)
                split.first().let { MacaroonsBuilder.deserialize(it) } to split.last()
            else
                split.first().let { MacaroonsBuilder.deserialize(it) } to null

            return AuthHeader(type, macaroon, preimage)
        }
    }
}