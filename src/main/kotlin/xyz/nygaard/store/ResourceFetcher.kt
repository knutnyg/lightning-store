package xyz.nygaard.store

import java.io.File
import java.lang.RuntimeException
import java.net.URI

interface Fetcher {
    fun fetch(uri: URI): ByteArray
}

class ResourceFetcher : Fetcher {
    override fun fetch(uri: URI): ByteArray {
        when (uri.scheme) {
            "file" -> return File("/Users/knut/code/lightning-store/resources/${uri.authority}").readBytes()
            "http" -> TODO()
            else -> TODO()
        }
    }
}