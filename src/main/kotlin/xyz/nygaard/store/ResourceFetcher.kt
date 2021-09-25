package xyz.nygaard.store

import xyz.nygaard.log
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
    val fileCache: Map<String, ByteArray> = buildCache("/Users/knut/code/lightning-store/resources")

    private fun buildCache(path: String): Map<String, ByteArray> {
        val cache: MutableMap<String, ByteArray> = mutableMapOf()

        File(path).walk().forEach {
            if (it.isDirectory) {
                return@forEach
            }
            val data = it.readBytes()
            val relPath = it.path.removePrefix(path);
            cache[relPath] = data
        }
        cache.forEach() { log.info("loaded file: ${it.key}")}
        log.info("loaded files: ${cache.size}")
        return cache.toMap()
    }

    override fun fetch(uri: URI): ByteArray {
        return when (uri.scheme) {
            "file" -> loadCached(uri)
            "http" -> loadRemote(uri)
            "https" -> loadRemote(uri)
            else -> throw RuntimeException("unhandled uri=$uri")
        }
    }

    private fun loadCached(uri: URI): ByteArray {
        val id = uri.authority
        return fileCache[id] ?: throw RuntimeException("unknown file: $id")
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