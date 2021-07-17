package xyz.nygaard.store.invoice

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.mockk.every
import io.mockk.mockk
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.TestDatabase
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndCreatedInvoice
import xyz.nygaard.lnd.LndInvoice
import xyz.nygaard.lnd.NodeInfo
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InvoiceServiceTest {

    val embeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5534).start()

    val lndClientMock2 = mockk<LndClient> {
        every { addInvoice(any(), any()) } returns LndCreatedInvoice(
            rhash = "/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=",
            paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0",
        )

        every { lookupInvoice(any()) } returns LndInvoice(
            memo = "memo",
            rhash = "/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=",
            settled = true,
            paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
        )

        every { getInfo() } returns
                NodeInfo(
                    blockHeight = 666,
                    alias = "Mockingbird",
                    uri = "027986b16bb9d8c541aff8e8df339548189f3077d0f42a517f7ce57135e8a9c19d@84.214.74.65:9736"
                )
    }

    val invoiceService = InvoiceService(
        TestDatabase(embeddedPostgres.postgresDatabase),
        lndClientMock2
    )

    @BeforeAll
    fun test() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    @Test
    fun `unknown uuid`() {
        assertNull(invoiceService.getInvoice(UUID.randomUUID()))
    }

    @Test
    fun `create and fetch invoice`() {
        val invoice = invoiceService.createInvoice(amount = 10L, memo = "memo")
        val storedInvoice = requireNotNull(invoiceService.getInvoice(invoice.id!!))

        assertEquals(invoice, storedInvoice)
        assertNotNull(invoice.id)
        assertEquals("memo", storedInvoice.memo)
    }

    @Test
    fun `lookup updated settled`() {
        val invoice = invoiceService.createInvoice(amount = 500L, memo = "best invoice")

        val updatedInvoice = invoiceService.lookupAndUpdate(invoice.id!!)
        assertNotNull(updatedInvoice?.settled)
    }

    @Test
    fun `lookup not settled`() {
        every { lndClientMock2.lookupInvoice(any()) } returns LndInvoice(
            memo = "memo",
            rhash = "/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=",
            settled = false,
            paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
        )
        val invoice = invoiceService.createInvoice(amount = 500L, memo = "best invoice")

        val updatedInvoice = invoiceService.lookupAndUpdate(invoice.id!!)
        assertNull(updatedInvoice?.settled)
    }

    @Test
    fun `rhash is hexencoded string`() {
        val invoice = invoiceService.createInvoice()

    }
}