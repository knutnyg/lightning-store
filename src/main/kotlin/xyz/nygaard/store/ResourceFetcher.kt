package xyz.nygaard.store

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

interface Fetcher {
    fun fetch(uri: URI): ByteArray
}

class ResourceFetcher : Fetcher {
    val client = HttpClient.newBuilder().build()

    override fun fetch(uri: URI): ByteArray {
        return when (uri.scheme) {
            "file" -> File("/Users/knut/code/lightning-store/resources/${uri.authority}").readBytes()
            "http" -> loadRemote(uri)
            "https" -> loadRemote(uri)
            else -> throw RuntimeException("unhandled uri=$uri")
        }
    }

    private fun loadRemote(uri: URI): ByteArray {
        val resp = client.send(HttpRequest.newBuilder().GET().uri(uri).build(), BodyHandlers.ofByteArray())
        if (resp.statusCode() == 200) {
            return resp.body()!!
        } else {
            throw RuntimeException("error status=${resp.statusCode()} body=${String(resp.body()!!, Charsets.UTF_8)}")
        }
    }
}