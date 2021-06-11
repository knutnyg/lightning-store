package xyz.nygaard.store.invoice

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import xyz.nygaard.TestDatabase
import xyz.nygaard.lnd.LndClientMock
import java.util.*
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InvoiceServiceTest {

    val lndClientMock = LndClientMock()
    val embeddedPostgres = EmbeddedPostgres.builder()
        .setPort(5533).start()

    val invoiceService = InvoiceService(
        TestDatabase(embeddedPostgres.postgresDatabase),
        lndClientMock
    )

    @BeforeAll
    fun test() {
        Flyway.configure().dataSource(embeddedPostgres.postgresDatabase).load().migrate()
    }

    @Test
    fun `unknown uuid`() {
        assertNull(invoiceService.getInvoice(UUID.randomUUID()))
    }
}