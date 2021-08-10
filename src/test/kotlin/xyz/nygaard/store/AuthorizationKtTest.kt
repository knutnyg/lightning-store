package xyz.nygaard.store

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xyz.nygaard.store.auth.AuthChallengeHeader

internal class AuthorizationKtTest {

    @Test
    fun `deserialize auth challenge`() {
        val challenge =
            "LSAT macaroon=\"MDAxN2xvY2F0aW9uIGxvY2FsaG9zdAowMDg2aWRlbnRpZmllciB2ZXJzaW9uID0gMAp1c2VyX2lkID0gYzYxMjY4MWQtYmMxYS00OTYxLWI4MWItN2JhN2E1ZDY3YjZkCnBheW1lbnRfaGFzaCA9IC9qbXY1VjllbHIzSm5NU3J6ZmxQREp0eW15b3BTeGllS0NqSzEwamRiOUU9CjAwMWVjaWQgc2VydmljZXMgPSBpbnZvaWNlczowCjAwMmZzaWduYXR1cmUgVRqpuF029pOVDYhovn1-ShgqdFxz8MbKLkZbztDWBBUK\", invoice=\"lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0\""
        val deserialized = AuthChallengeHeader.deserialize(challenge)
        assertEquals("LSAT", deserialized.type)
        assertEquals(
            "MDAxN2xvY2F0aW9uIGxvY2FsaG9zdAowMDg2aWRlbnRpZmllciB2ZXJzaW9uID0gMAp1c2VyX2lkID0gYzYxMjY4MWQtYmMxYS00OTYxLWI4MWItN2JhN2E1ZDY3YjZkCnBheW1lbnRfaGFzaCA9IC9qbXY1VjllbHIzSm5NU3J6ZmxQREp0eW15b3BTeGllS0NqSzEwamRiOUU9CjAwMWVjaWQgc2VydmljZXMgPSBpbnZvaWNlczowCjAwMmZzaWduYXR1cmUgVRqpuF029pOVDYhovn1-ShgqdFxz8MbKLkZbztDWBBUK",
            deserialized.macaroon.serialize()
        )
        assertEquals(
            "lnbc5u1pwjefggpp5lcu6le2lt6ttmjvucj4um720pjdh9xe29993383g9r9dwjxadlgsdq523jhxapqf9h8vmmfvdjscqzpgj5cqeemavasg8uqu7ec85k3792q02czxzregkdae5ylqvytgvrcsq4t2spjzrnv3sh8pkckv4y04urwzmzsu9h8kthcvwk3evr4z8ksqkdj8c0",
            deserialized.invoice
        )
    }
}