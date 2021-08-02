package xyz.nygaard.store.user

import com.github.nitram509.jmacaroons.Macaroon
import xyz.nygaard.extractUserId

class Token(val macaroon: Macaroon, val balance: Int, val revoked: Boolean) {
    val id = macaroon.extractUserId()
}