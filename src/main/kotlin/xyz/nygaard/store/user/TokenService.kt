package xyz.nygaard.store.user

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import io.ktor.features.*
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.db.connectionAutoCommit
import xyz.nygaard.db.toList
import xyz.nygaard.extractUserId
import java.util.*
import javax.sql.DataSource


class TokenService(val dataSource: DataSource) {
    fun createToken(macaroon: Macaroon, balance: Long = 0L): Int {
        dataSource.connectionAutoCommit().use { connection ->
            return connection.prepareStatement("INSERT INTO token(id, macaroon, balance, revoked) VALUES (?, ?, ?, false)")
                .use {
                    it.setString(1, macaroon.extractUserId().toString())
                    it.setString(2, macaroon.serialize())
                    it.setLong(3, balance)
                    it.executeUpdate()
                }
        }
    }

    private fun fetchToken(id: UUID): Token? {
        return dataSource.connectionAutoCommit().use { connection ->
            connection.prepareStatement("SELECT * FROM token WHERE id = ?")
                .use {
                    it.setString(1, id.toString())
                    it.executeQuery()
                        .toList {
                            Token(
                                macaroon = MacaroonsBuilder.deserialize(getString("macaroon")),
                                balance = getLong("balance"),
                                revoked = getBoolean("revoked")
                            )
                        }.firstOrNull()
                }
        }
    }

    fun fetchToken(macaroon: Macaroon) = fetchToken(macaroon.extractUserId())

    fun increaseBalance(macaroon: Macaroon, numberOfSatoshis: Long) {
        fetchToken(macaroon)?.let { token ->
            dataSource.connectionAutoCommit().use { connection ->
                connection.prepareStatement("UPDATE token SET balance = ? WHERE id = ?")
                    .use {
                        it.setLong(1, token.balance + numberOfSatoshis)
                        it.setString(2, token.id.toString())
                        it.executeUpdate()
                    }
            }
        } ?: throw NotFoundException("token not found")
    }
}
