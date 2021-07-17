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
import xyz.nygaard.util.decodeHex
import xyz.nygaard.util.toHex
import java.io.ByteArrayInputStream
import java.util.*

class LndClient(environment: Config) : LndApiWrapper {

    private val cert = GrpcSslContexts.configure(
        io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder.forClient(),
        SslProvider.OPENSSL
    )
        .trustManager(ByteArrayInputStream(Base64.getDecoder().decode(environment.cert)))
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

    override fun addInvoice(value: Long, memo: String): LndCreatedInvoice =
        lndInvoiceApi.addInvoice(org.lightningj.lnd.wrapper.message.Invoice()
            .apply {
                this.value = value
                this.memo = memo
            })
            .map2()


    override fun lookupInvoice(invoice:Invoice) : LndInvoice =
        lndInvoiceApi.lookupInvoice(PaymentHash()
            .apply {
                rHash = invoice.rhash.decodeHex()
            })
            .map2()


    override fun getInfo(): NodeInfo =
        readOnlyApi.getInfo(GetInfoRequest()).map2()
}

fun GetInfoResponse.map2(): NodeInfo =
    NodeInfo(
        blockHeight = blockHeight,
        alias = alias,
        uri = uris.firstOrNull()
    )

fun AddInvoiceResponse.map2(): LndCreatedInvoice =
    LndCreatedInvoice(
        rhash = rHash.toHex(),
        paymentRequest = paymentRequest,
    )

fun org.lightningj.lnd.wrapper.message.Invoice.map2() =
    LndInvoice(
        memo = memo,
        rhash = rHash.toHex(),
        settled = settled,
        paymentRequest = paymentRequest
    )

interface LndApiWrapper {
    fun addInvoice(value: Long, memo: String): LndCreatedInvoice
    fun lookupInvoice(invoice: Invoice): LndInvoice
    fun getInfo(): NodeInfo
}
