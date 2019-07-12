package xyz.nygaard.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.net.URI
import java.sql.Connection
import java.sql.ResultSet

class Database : DatabaseInterface {
    private val dataSource: HikariDataSource

    private val username:String
    private val password:String?
    private val dbUrl:String

    private val log = LoggerFactory.getLogger("Database")

    override val connection: Connection
        get() = dataSource.connection

    init {
        val dbUri = URI(System.getenv("DATABASE_URL"))
        when (isProduction(dbUri)) {
            true -> {
                username = dbUri.userInfo.split(":")[0]
                password = dbUri.userInfo.split(":")[1]
                log.info("username: {} | password: {}", username, password)
                dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require"

            }
            else -> {
                username = dbUri.userInfo.split(":")[0]
                password = null
                dbUrl = "jdbc:postgresql://" + dbUri.host + ":" + dbUri.port + dbUri.path
            }
        }

        runFlywayMigrations(dbUrl, username, password ?: "")

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbUrl
            username = username
            password = password
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })
    }

    private fun isProduction(dbUrl:URI) = !dbUrl.toString().contains("localhost")

    private fun runFlywayMigrations(dburl:String, username:String, password:String) = Flyway.configure().run {
        dataSource(dburl, username, password)
        load().migrate()
    }
}

fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
    while (next()) {
        add(mapper())
    }
}

interface DatabaseInterface {
    val connection: Connection
}