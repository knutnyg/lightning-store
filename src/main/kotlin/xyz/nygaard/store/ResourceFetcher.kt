package xyz.nygaard.store

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class ResourceFetcher(private val kunstigUri: URI) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun requestNewImage() = loadRemote(kunstigUri)

    private fun loadRemote(uri: URI): ByteArray {
        val resp = client.send(HttpRequest.newBuilder().GET().uri(uri).build(), BodyHandlers.ofByteArray())
        if (resp.statusCode() == 200) {
            return resp.body()!!
        } else {
            throw RuntimeException("error status=${resp.statusCode()} body=${String(resp.body()!!, Charsets.UTF_8)}")
        }
    }
}