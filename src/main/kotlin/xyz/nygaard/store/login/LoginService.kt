package xyz.nygaard.store.login

import java.util.Base64

class LoginService {

    private val secureRandom = java.security.SecureRandom()

    fun createPrivateKey(): String {
        val array = ByteArray(32)
        secureRandom.nextBytes(array)

        return String(Base64.getUrlEncoder().encode(array))
    }

    fun login(key: String) {

    }
}
