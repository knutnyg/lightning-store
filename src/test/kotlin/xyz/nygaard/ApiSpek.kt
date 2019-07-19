package xyz.nygaard

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotEqual
import org.flywaydb.core.Flyway
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.lnd.LndClient
import xyz.nygaard.lnd.LndInvoice
import xyz.nygaard.store.invoice.Invoice
import xyz.nygaard.store.invoice.InvoiceService
import xyz.nygaard.store.login.LoginService
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
    val loginService = LoginService(testDatabase)


    describe("Apitests") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerInvoiceApi(invoiceService)
                registerLoginApi(loginService, false)
            }

            application.installContentNegotiation()

            describe("Invoice") {
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

            describe("Login") {
                val validKey = loginService.createAndSavePrivateKey()

                it("Check login responds not logged in with no cookie") {
                    with(handleRequest(HttpMethod.Get, "/login")) {
                        val login = objectMapper.readValue<LoginResponse>(response.content!!)
                        login.status.shouldEqual("NOT_LOGGED_IN")
                        login.key.shouldBeNull()
                    }
                }

                it("Check login responds logged in with cookie") {
                    with(handleRequest(HttpMethod.Get, "/login") {
                        addHeader("COOKIE", "key=$validKey")
                    }

                    ) {
                        val login = objectMapper.readValue<LoginResponse>(response.content!!)
                        login.status.shouldEqual("LOGGED_IN")
                        login.key.shouldEqual(validKey)
                    }
                }

                it("Check login invalid key returns not logged in") {
                    with(handleRequest(HttpMethod.Get, "/login") {
                        addHeader("COOKIE", "key=123")
                    }

                    ) {
                        val login = objectMapper.readValue<LoginResponse>(response.content!!)
                        login.status.shouldEqual("NOT_LOGGED_IN")
                        login.key.shouldBeNull()
                    }
                }

                it("Post with valid key returns cookie and key") {
                    with(handleRequest(HttpMethod.Post, "/login") {
                        addHeader("COOKIE", "key=$validKey")
                        addHeader("Content-Type", "application/json")
                        addHeader("Accept", "application/json")
                        setBody("{\"key\": \"$validKey\"}")
                    }

                    ) {
                        val login = objectMapper.readValue<LoginResponse>(response.content!!)
                        login.status.shouldEqual("LOGGED_IN")
                        login.key.shouldEqual(validKey)
                        response.cookies["key"]?.value.shouldEqual(validKey)
                    }
                }

                it("Post with without key returns new key and cookie") {
                    with(handleRequest(HttpMethod.Post, "/login") {
                        addHeader("Content-Type", "application/json")
                        addHeader("Accept", "application/json")
                        setBody("{\"key\": \"\"}")
                    }

                    ) {
                        val login = objectMapper.readValue<LoginResponse>(response.content!!)
                        login.status.shouldEqual("LOGGED_IN")
                        login.key.shouldNotEqual(validKey)
                        response.cookies["key"]?.value.shouldEqual(login.key)
                    }
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
