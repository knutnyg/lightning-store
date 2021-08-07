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
    fun createToken(macaroon: Macaroon, balance: Int = 0): Int {
        dataSource.connectionAutoCommit().use { connection ->
            return connection.prepareStatement("INSERT INTO token(id, macaroon, balance, revoked) VALUES (?, ?, ?, false)")
                .use {
                    it.setString(1, macaroon.extractUserId().toString())
                    it.setString(2, macaroon.serialize())
                    it.setInt(3, balance)
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
                                balance = getInt("balance"),
                                revoked = getBoolean("revoked")
                            )
                        }.firstOrNull()
                }
        }
    }

    fun fetchToken(macaroon: Macaroon) = fetchToken(macaroon.extractUserId())

    fun increaseBalance(macaroon: Macaroon, numberOfSatoshis: Int) {
        fetchToken(macaroon)?.let { token ->
            dataSource.connectionAutoCommit().use { connection ->
                connection.prepareStatement("UPDATE token SET balance = ? WHERE id = ?")
                    .use {
                        it.setInt(1, token.balance + numberOfSatoshis)
                        it.setString(2, token.id.toString())
                        it.executeUpdate()
                    }
            }
        } ?: throw NotFoundException("token not found")
    }

    fun decreaseBalance(macaroon: Macaroon, numberOfSatoshis: Int) {
        fetchToken(macaroon)?.let { token ->
            if (token.balance < numberOfSatoshis) throw IllegalArgumentException("Attempting to decrease balance < 0")
            dataSource.connectionAutoCommit().use { connection ->
                connection.prepareStatement("UPDATE token SET balance = ? WHERE id = ?")
                    .use {
                        it.setInt(1, token.balance - numberOfSatoshis)
                        it.setString(2, token.id.toString())
                        it.executeUpdate()
                    }
            }
        } ?: throw NotFoundException("token not found")
    }
}
