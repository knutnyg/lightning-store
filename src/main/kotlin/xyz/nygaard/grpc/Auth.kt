package xyz.nygaard.grpc

import io.grpc.*
import xyz.nygaard.MacaroonService
import xyz.nygaard.extractRHash
import xyz.nygaard.log
import xyz.nygaard.store.auth.AuthHeader
import xyz.nygaard.store.auth.AuthorizationTypeLSAT
import xyz.nygaard.util.sha256


val AuthorizationHeaderKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
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
