package xyz.nygaard.store.invoice

import xyz.nygaard.lnd.LndApiWrapper
import xyz.nygaard.lnd.LndCreatedInvoice
import xyz.nygaard.lnd.LndInvoice
import xyz.nygaard.lnd.NodeInfo
import xyz.nygaard.log
import xyz.nygaard.util.sha256
import java.util.*

class LndClientMock : LndApiWrapper {

    private val preimage = "1234"
    private val rhash = preimage.sha256()

    private var _invoice = LndInvoice(
        id = UUID.randomUUID(),
        memo = "memo",
        rhash = rhash,
        settled = false,
        paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
    )

    fun markSettled(settled: Boolean = true) {
        _invoice = _invoice.copy(
            settled = settled,
            preimage = if(settled) preimage else null
        )
    }


    override fun addInvoice(value: Long, memo: String): LndCreatedInvoice {
        log.info("addInvoice returning mock")
        return LndCreatedInvoice(
            rhash = rhash,
            paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
        )
    }

    override fun lookupInvoice(invoice: Invoice): LndInvoice {
        return _invoice
    }

    override fun getInfo(): NodeInfo {
        log.info("getInfo returning mock")
        return NodeInfo(
            blockHeight = 666,
            alias = "Mockingbird",
            uri = "027986b16bb9d8c541aff8e8df339548189f3077d0f42a517f7ce57135e8a9c19d@84.214.74.65:9736"
        )
    }
}
