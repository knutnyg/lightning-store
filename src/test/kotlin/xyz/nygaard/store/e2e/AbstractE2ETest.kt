package xyz.nygaard.store.e2e

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import xyz.nygaard.MacaroonService
import xyz.nygaard.buildApplication
import xyz.nygaard.store.Fetcher
import xyz.nygaard.store.invoice.LndClientMock
import xyz.nygaard.store.order.OrderService
import xyz.nygaard.store.order.ProductService
import xyz.nygaard.store.user.TokenService
import xyz.nygaard.util.sha256
import java.io.FileInputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {

    protected val lndMock = LndClientMock()

    private val postgres = PostgreSQLContainer<Nothing>("postgres:13").apply {
        withReuse(true)
        withLabel("app-navn", "lightning-store")
        start()
    }

    protected val dataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 500001
            connectionTimeout = 10000
            maxLifetime = 600001
        })

    private val macaroonService = MacaroonService("localhost", "secret")
    protected val tokenService = TokenService(dataSource)
    protected val orderService = OrderService(dataSource)
    private val productService = ProductService(dataSource)

    protected val preimage = "1234"
    private val rhash = preimage.sha256()
    protected val macaroon = macaroonService.createMacaroon(rhash)

    protected val mapper = jacksonObjectMapper()

    @BeforeAll
    fun setup() {
        Flyway.configure().dataSource(dataSource).load().migrate()
    }

    @BeforeEach
    fun reset() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM orders;").execute()
            it.prepareStatement("DELETE FROM token;").execute()
            it.prepareStatement("DELETE FROM invoices;").execute()
        }
    }

    @AfterAll
    fun tearDown() {
        postgres.close()
    }

    protected fun TestApplicationEngine.authenticated(method: HttpMethod, uri: String) =
        handleRequest(method, uri) {
            addHeader(HttpHeaders.Accept, "application/json")
            addHeader(HttpHeaders.XForwardedProto, "https")
            addHeader(HttpHeaders.Cookie, "authorization=LSAT ${macaroon.serialize()}:${preimage}")
        }

    protected fun Application.setup() = buildApplication(
        dataSource = dataSource,
        macaroonService = macaroonService,
        productService = productService,
        lndClient = lndMock,
        staticResourcesPath = "static",
        resourceFetcher = FakeFetcher(imgData)
    )
}

val imgData = requireNotNull(FileInputStream("src/test/resources/working.jpg").readAllBytes())

class FakeFetcher(private val data: ByteArray, val delay: Long = 0L) : Fetcher {
    override fun requestNewImage(): ByteArray {
        Thread.sleep(delay)
        return data
    }
}