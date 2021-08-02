package xyz.nygaard.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet

class Database(val url:String) : DatabaseInterface {
    private val dataSource: HikariDataSource

    override val connection: Connection
        get() = dataSource.connection

    init {
        runFlywayMigrations(url, "knutnygaard", "")

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = "knutnygaard"
            password = ""
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })
    }

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
