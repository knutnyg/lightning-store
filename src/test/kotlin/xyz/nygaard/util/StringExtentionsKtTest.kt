package xyz.nygaard.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class StringExtentionsKtTest {

    @Test
    fun hexing() {
        assertEquals(
            "88ad58bcb26045f9246e194ff1dd085c862fb7e41be38d4e080b2f2812f85e33",
            Base64.getDecoder().decode("iK1YvLJgRfkkbhlP8d0IXIYvt+Qb441OCAsvKBL4XjM=").toHex()
        )
    }

    @Test
    fun ting() {
        val hash = "396c9e196c1a21f4d8b99ab7cb3685e7f6582e5f1740ad0d960c9d16b4a5c622" //hex
        val preimage = "9ec2d9ee21189cde57964e8af3d798eccf9a13d2ac7b06da03371f9a9e0b9d50" //hex

        assertEquals(preimage.sha256(), hash)
    }
}