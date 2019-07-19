package xyz.nygaard.store.login

import xyz.nygaard.db.DatabaseInterface
import xyz.nygaard.db.toList
import java.util.Base64
import java.util.UUID

class LoginService(val database: DatabaseInterface) {

    private val secureRandom = java.security.SecureRandom()

    fun createAndSavePrivateKey(): String {
        val array = ByteArray(32)
        secureRandom.nextBytes(array)

        val key = String(Base64.getUrlEncoder().withoutPadding().encode(array))
        saveKey(key)

        return key
    }

    fun isValidToken(key: String? = ""): Boolean {
        return 1 == database.connection.use { connection ->
            connection.prepareStatement("SELECT * FROM USERS WHERE privatekey = ?")
                .use {
                    it.setString(1, key)
                    it.executeQuery()
                        .toList { 1 }.size
                }
        }
    }

    private fun saveKey(key: String) : String{
        val uuid = UUID.randomUUID().toString()
        database.connection.use { connection ->
            connection
                .prepareStatement("INSERT INTO USERS(id, privatekey) VALUES (?, ?)")
                .use {
                    it.setString(1, uuid)
                    it.setString(2, key)
                    it.executeUpdate()
                }
            connection.commit()
        }
        return uuid
    }
}
