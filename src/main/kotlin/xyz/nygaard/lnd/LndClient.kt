package xyz.nygaard.lnd

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider
import org.lightningj.lnd.wrapper.SynchronousLndAPI
import org.lightningj.lnd.wrapper.message.AddInvoiceResponse
import org.lightningj.lnd.wrapper.message.GetInfoRequest
import org.lightningj.lnd.wrapper.message.GetInfoResponse
import org.lightningj.lnd.wrapper.message.PaymentHash
import xyz.nygaard.Config
import xyz.nygaard.EnvironmentMacaroonContext
import xyz.nygaard.store.invoice.Invoice
import java.io.ByteArrayInputStream

class LndClient(environment: Config) {

    private val cert = GrpcSslContexts.configure(
        io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder.forClient(),
        SslProvider.OPENSSL
    )
        .trustManager(ByteArrayInputStream(environment.cert.toByteArray()))
        .build()

    private val readonlyMacaroon = EnvironmentMacaroonContext(currentMacaroonData = environment.readOnlyMacaroon)
    private val readOnlyApi = SynchronousLndAPI(
        environment.hostUrl,
        environment.hostPort,
        cert,
        readonlyMacaroon
    )

    private val invoiceMacaroon = EnvironmentMacaroonContext(currentMacaroonData = environment.invoiceMacaroon)
    private val lndInvoiceApi = SynchronousLndAPI(
        environment.hostUrl,
        environment.hostPort,
        cert,
        invoiceMacaroon
    )

    fun addInvoice(value: Long, memo: String): Invoice =
        lndInvoiceApi.addInvoice(org.lightningj.lnd.wrapper.message.Invoice()
            .apply {
                this.value = value
                this.memo = memo
            })
            .map2()


    fun lookupInvoice(invoice:Invoice) : LndInvoice =
        lndInvoiceApi.lookupInvoice(PaymentHash()
            .apply {
                rHashStr = invoice.rhash
            })
            .map2()


    fun getInfo(): NodeInfo =
        readOnlyApi.getInfo(GetInfoRequest()).map2()
}

fun GetInfoResponse.map2(): NodeInfo =
    NodeInfo(
        blockHeight = blockHeight,
        alias = alias,
        uri = uris.firstOrNull()
    )

fun AddInvoiceResponse.map2(): Invoice =
    Invoice(
        rhash = String(rHash),
        paymentRequest = paymentRequest
    )

fun org.lightningj.lnd.wrapper.message.Invoice.map2() =
    LndInvoice(
        memo = this.memo,
        rhash = String(this.rHash),
        settled = this.settled,
        paymentRequest = this.paymentRequest
    )
