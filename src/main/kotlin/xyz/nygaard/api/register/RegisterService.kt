package xyz.nygaard.api.register

import io.grpc.Status
import io.grpc.StatusRuntimeException
import xyz.nygaard.MacaroonService
import xyz.nygaard.api.v1.Register.*
import xyz.nygaard.api.v1.RegisterServiceGrpcKt
import xyz.nygaard.extractRHash
import xyz.nygaard.grpc.requireAuth
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthorizationTypeLSAT
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.user.TokenService
import java.util.*

class RegisterService(
    private val productService: ProductService,
    private val invoiceService: InvoiceService,
    private val macaroonService: MacaroonService,
    private val tokenService: TokenService,
    private val orderService: OrderService,
) : RegisterServiceGrpcKt.RegisterServiceCoroutineImplBase() {

    override suspend fun start(request: StartRequest): StartResponse {
        val productId = request.productId.ifBlank { "a64d4344-f964-4dfe-99a6-7b39a7eb91c1" }

        val tokenProduct = productService.getProduct(UUID.fromString(productId))
        val invoice = invoiceService.createInvoice(tokenProduct.price, "1x${tokenProduct.name}: ${tokenProduct.id}")
        val macaroon = macaroonService.createMacaroon(invoice.rhash)

        tokenService.createToken(macaroon)
        orderService.createWithInvoice(invoice, tokenProduct, macaroon)

        return StartResponse.newBuilder()
            .setType(AuthorizationTypeLSAT)
            .setInvoice(invoice.paymentRequest)
            .setMacaroon(macaroon.serialize())
            .build();
    }

    override suspend fun registerPayment(request: RegisterPaymentRequest): RegisterPaymentResponse {
        val auth = requireAuth()

        val invoice = invoiceService.lookupAndUpdate(auth.macaroon.extractRHash())
            ?: throw StatusRuntimeException(Status.NOT_FOUND)

        //call.response.cookies.append(cookieBakery.createAuthCookie(authorization))

        return RegisterPaymentResponse.newBuilder()
            .setInvoice(invoice.toApiInvoice())
            .build()
    }

    override suspend fun getAccount(request: GetAccountRequest): GetAccountResponse {
        val auth = requireAuth()

        val token = tokenService.fetchToken(auth.macaroon) ?: throw StatusRuntimeException(Status.NOT_FOUND)
            .also { log.info("Received token not stored in database. Probably because we have deleted our entry") }

        //call.response.cookies.append(cookieBakery.createAuthCookie(authorization))

        return GetAccountResponse.newBuilder()
            .setAccount(Account.newBuilder()
                .setUserId(token.id.toString())
                .setBalance(token.balance)
                .build()
            )
            .build()
    }

}
