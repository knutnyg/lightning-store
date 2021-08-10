package xyz.nygaard.store.user

import com.github.nitram509.jmacaroons.Macaroon
import xyz.nygaard.extractUserId
import java.util.*

class Token(val macaroon: Macaroon, val balance: Long, val revoked: Boolean) {
    val id = macaroon.extractUserId()

    fun toDTO() = TokenResponse(id, balance)

}

data class TokenResponse(
    val userId: UUID,
    val balance: Number
)
