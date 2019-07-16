package xyz.nygaard

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.flywaydb.core.Flyway
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndInvoice
import xyz.nygaard.store.invoice.Invoice
import xyz.nygaard.store.invoice.InvoiceService
import java.sql.Connection
import javax.sql.DataSource

object ApiSpek : Spek({

    val lndClientMock = mockk<LndClient>()
    val embeddedPostgres = EmbeddedPostgres.start()
    val testDatabase = TestDatabase(embeddedPostgres.postgresDatabase)

    Flyway.configure().run {
        dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    val invoiceService = InvoiceService(testDatabase, lndClientMock)

    describe("Apitests") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerInvoiceApi(invoiceService)
            }

            application.installContentNegotiation()

            it("Adds an invoice then fetches it") {
                val invoice: Invoice?
                every { lndClientMock.addInvoice(500L, "test") } returns
                        Invoice(
                            rhash = "/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=",
                            paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
                        )
                every { lndClientMock.lookupInvoice(any()) } returns LndInvoice(
                    rhash = "/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=",
                    settled = true,
                    paymentRequest = "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0"
                )

                with(handleRequest(HttpMethod.Post, "/invoices")) {
                    invoice = objectMapper.readValue<Invoice>(response.content!!)
                    invoice.id.shouldNotBeNull()
                    invoice.memo.shouldBeNull()
                    invoice.rhash.shouldEqual("/jmv5V9elr3JnMSrzflPDJtymyopSxieKCjK10jdb9E=")
                    invoice.paymentRequest.shouldEqual("lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0")
                }

                with(handleRequest(HttpMethod.Get, "/invoices/${invoice?.id}")) {
                    val fetchedInvoie = objectMapper.readValue<Invoice>(response.content!!)
                    fetchedInvoie.id.shouldEqual(invoice?.id)
                }
            }
        }

    }

    afterGroup {
        embeddedPostgres.close()
    }
})

class TestDatabase(private val datasource: DataSource) : DatabaseInterface {
    override val connection: Connection
        get() = datasource.connection.apply { autoCommit = false }
}
