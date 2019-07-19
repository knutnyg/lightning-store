package xyz.nygaard.store.login

import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.db.toList
import java.util.Base64

class LoginService(val database: DatabaseInterface) {

    private val secureRandom = java.security.SecureRandom()

    fun createPrivateKey(): String {
        val array = ByteArray(32)
        secureRandom.nextBytes(array)

        return String(Base64.getUrlEncoder().withoutPadding().encode(array))
    }

    fun isValidToken(key: String? = ""): Boolean {

        return database.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM USERS WHERE privatekey = ?")
                .use {
                    it.setString(1, key)
                    it.executeQuery()
                }
        }.next()
    }
}
