package xyz.nygaard.store

import xyz.nygaard.log
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

interface Fetcher {
    fun requestNewImage(): ByteArray
}

class ResourceFetcher(private val kunstigUri: String): Fetcher {
    val client: HttpClient = HttpClient.newBuilder().build()

    override fun requestNewImage() = loadRemote(URI.create(kunstigUri))

    private fun loadRemote(uri: URI): ByteArray {
        log.info("Fetching new image")
        val resp = client.send(HttpRequest.newBuilder().GET().uri(uri).build(), BodyHandlers.ofByteArray())
        if (resp.statusCode() == 200) {
            return resp.body()!!
        } else {
            throw RuntimeException("error status=${resp.statusCode()} body=${String(resp.body()!!, Charsets.UTF_8)}")
        }
    }
}