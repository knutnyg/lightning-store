package xyz.nygaard.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

fun DataSource.connectionAutoCommit(): Connection = this.connection.apply { autoCommit = true }

class Database(val url: String, private val databaseUsername: String, private val databasePassword: String) : DatabaseInterface {
    internal val dataSource: HikariDataSource

    override val connection: Connection
        get() = dataSource.connection

    init {
        runFlywayMigrations(url, databaseUsername, databasePassword)

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = databaseUsername
            password = databasePassword
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })
    }

    private fun runFlywayMigrations(dburl: String, username: String, password: String) = Flyway.configure().run {
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
