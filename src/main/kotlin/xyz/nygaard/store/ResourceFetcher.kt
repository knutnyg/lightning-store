package xyz.nygaard.store

import java.io.File
import java.lang.RuntimeException
import java.net.URI

interface Fetcher {
    fun fetch(uri: URI): ByteArray
}

class ResourceFetcher : Fetcher {
    override fun fetch(uri: URI): ByteArray {
        when (uri.fragment) {
            "file" -> File(uri).readBytes()
            "http" -> TODO()
            else -> TODO()
        }
        throw RuntimeException("Halp")
    }
}