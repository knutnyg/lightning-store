package xyz.nygaard.util

import java.security.MessageDigest


fun String.sha256() =
    MessageDigest.getInstance("SHA-256")
        .digest(this.decodeHex())
        .fold("") { str, it -> str + "%02x".format(it) }

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}