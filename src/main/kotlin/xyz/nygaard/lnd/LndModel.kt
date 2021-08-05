package xyz.nygaard.lnd

import java.util.UUID

data class NodeInfo(
    val blockHeight: Int,
    val alias: String,
    val uri: String?
)

data class LndInvoice(
    val id: UUID? = null,
    val memo: String? = null,
    val rhash: String,
    val settled: Boolean = false,
    val paymentRequest: String,
    val preimage: String? = null
)

data class LndCreatedInvoice(
    val rhash: String,
    val paymentRequest: String
)
