package xyz.nygaard.lnd

import org.slf4j.LoggerFactory
import xyz.nygaard.store.invoice.Invoice

class LndClientMock : LndApiWrapper {

    val log = LoggerFactory.getLogger("LndClientMock")

    override fun addInvoice(value: Long, memo: String): Invoice {
        log.info("addInvoice returning mock")
        return Invoice(
            rhash = "/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=",
            paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
        )
    }

    override fun lookupInvoice(invoice: Invoice): LndInvoice {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
