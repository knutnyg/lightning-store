package xyz.nygaard.store.user

import com.github.nitram509.jmacaroons.Macaroon
import com.github.nitram509.jmacaroons.MacaroonsBuilder
import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.db.toList
import xyz.nygaard.extractUserId
import java.util.*
import javax.sql.DataSource


class TokenService(val database: DataSource) {
    fun createToken(macaroon: Macaroon, balance: Int = 0): Int {
        database.connection.apply { autoCommit = false }.use { connection ->
            return connection.prepareStatement("INSERT INTO token(id, macaroon, balance, revoked) VALUES (?, ?, ?, false)")
                .use {
                    it.setString(1, macaroon.extractUserId().toString())
                    it.setString(2, macaroon.serialize())
                    it.setInt(3, balance)
                    it.executeUpdate()
                }.also { connection.commit() }
        }
    }

    private fun fetchToken(id: UUID): Token {
        return database.connection.use { connection ->
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
                        }.first()
                }
        }
    }

    fun fetchToken(macaroon: Macaroon) = fetchToken(macaroon.extractUserId())

    fun increaseBalance(macaroon: Macaroon, numberOfSatoshis: Int) {
        fetchToken(macaroon).let { token ->
            database.connection.use { connection ->
                connection.prepareStatement("UPDATE token SET balance = ? WHERE id = ?")
                    .use {
                        it.setInt(1, token.balance + numberOfSatoshis)
                        it.setString(2, token.id.toString())
                        it.executeUpdate()
                    }
            }
        }
    }

    fun decreaseBalance(macaroon: Macaroon, numberOfSatoshis: Int) {
        fetchToken(macaroon).let { token ->
            if (token.balance < numberOfSatoshis) throw IllegalArgumentException("Attempting to decrease balance < 0")
            database.connection.use { connection ->
                connection.prepareStatement("UPDATE token SET balance = ? WHERE id = ?")
                    .use {
                        it.setInt(1, token.balance - numberOfSatoshis)
                        it.setString(2, token.id.toString())
                        it.executeUpdate()
                    }
            }
        }
    }
}
