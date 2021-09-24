package xyz.nygaard.api.register

import io.grpc.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.extractRHash
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthHeader
import xyz.nygaard.store.auth.AuthorizationTypeLSAT
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.util.*


val AuthorizationContextKey = Context.key<AuthHeader>("auth")

fun withAuth(authHeader: AuthHeader): Context = Context.current().withValue(AuthorizationContextKey, authHeader)

fun getAuth(): AuthHeader? = AuthorizationContextKey.get()

fun requireAuth(): AuthHeader {
    val auth = AuthorizationContextKey.get()
    if (auth == null) {
        throw StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authorization"))
    } else {
        return auth
    }
}

val AuthorizationHeaderKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)

class AuthorizationInterceptor(
    val macaroonService: MacaroonService,
) : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>?,
    ): ServerCall.Listener<ReqT> {
        val authorizationHeader = headers.get(AuthorizationHeaderKey)

        if (authorizationHeader != null) {
            val auth = AuthHeader.deserialize(authorizationHeader)
            if (isValid(auth)) {
                val ctx = withAuth(auth)
                return Contexts.interceptCall(ctx, call, headers, next)
            }
        }

        call.close(Status.UNAUTHENTICATED.withDescription("header format: Authorization: LSAT <token>"), headers)
        return object : ServerCall.Listener<ReqT>() {}
    }

    private fun isValid(auth: AuthHeader): Boolean {
        if (auth.preimage?.sha256() != auth.macaroon.extractRHash()) {
            log.info("Preimage does not correspond to payment hash")
            return false
        }

        return auth.type == AuthorizationTypeLSAT && macaroonService.isValid(auth.macaroon)
    }

}

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
