package xyz.nygaard.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.net.URI
import java.sql.Connection
import java.sql.ResultSet

class Database : DatabaseInterface {
    private val dataSource: HikariDataSource

    private val postgresUsername:String
    private val postgresPassword:String?
    private val dbUrl:String

    override val connection: Connection
        get() = dataSource.connection

    init {
        val dbUri = URI(System.getenv("DATABASE_URL"))
        when (isProduction(dbUri)) {
            true -> {
                postgresUsername = dbUri.userInfo.split(":")[0]
                postgresPassword = dbUri.userInfo.split(":")[1]
                dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require"
            }
            else -> {
                postgresUsername = dbUri.userInfo.split(":")[0]
                postgresPassword = null
                dbUrl = "jdbc:postgresql://" + dbUri.host + ":" + dbUri.port + dbUri.path
            }
        }

        runFlywayMigrations(dbUrl, postgresUsername, postgresPassword ?: "")

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbUrl
            username = postgresUsername
            password = postgresPassword
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
