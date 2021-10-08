package xyz.nygaard.store

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.nygaard.log
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.*
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

interface Fetcher {
    fun requestNewImage(): ByteArray
}

class LoggingClient(private val delegate: HttpClient): HttpClient() {
    private val log: Logger = LoggerFactory.getLogger(LoggingClient::class.java)

    override fun <T : Any?> send(request: HttpRequest, responseBodyHandler: BodyHandler<T>?): HttpResponse<T> {
        log.info("----> ${request.method()} ${request.uri().path}")
        try {
            val start = System.currentTimeMillis()
            val res = delegate.send(request, responseBodyHandler)
            val elapsedMs = System.currentTimeMillis() - start
            log.info("<---- ${request.method()} ${request.uri().path}: ${res.statusCode()} (took ${elapsedMs}ms")
            return res
        } catch (e: Throwable) {
            log.error("<---- ${request.method()} ${request.uri().path}: ERROR: ${e.message}", e)
            throw e
        }
    }

    override fun <T : Any?> sendAsync(
        request: HttpRequest,
        responseBodyHandler: BodyHandler<T>?
    ): CompletableFuture<HttpResponse<T>> {
        val start = System.currentTimeMillis()
        log.info("----> ${request.method()} ${request.uri().path}")
        return delegate.sendAsync(request, responseBodyHandler)
            .handleAsync(BiFunction { res, e ->
                val elapsedMs = System.currentTimeMillis() - start
                if (e != null) {
                    log.error("<---- ${request.method()} ${request.uri()?.path}: ERROR: ${e.message}", e)
                    throw e
                } else {
                    log.info("<---- ${request.method()} ${request.uri().path}: ${res.statusCode()} (took ${elapsedMs}ms")
                    return@BiFunction res
                }
            })
    }

    override fun <T : Any?> sendAsync(
        request: HttpRequest,
        responseBodyHandler: BodyHandler<T>?,
        pushPromiseHandler: PushPromiseHandler<T>?
    ): CompletableFuture<HttpResponse<T>> {
        val start = System.currentTimeMillis()
        log.info("----> ${request.method()} ${request.uri().path}")
        return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler)
            .handleAsync(BiFunction { res, e ->
                val elapsedMs = System.currentTimeMillis() - start
                if (e != null) {
                    log.error("<---- ${request.method()} ${request.uri()?.path}: ERROR: ${e.message}", e)
                    throw e
                } else {
                    log.info("<---- ${request.method()} ${request.uri().path}: ${res.statusCode()} (took ${elapsedMs}ms")
                    return@BiFunction res
                }
            })
    }

    override fun cookieHandler(): Optional<CookieHandler> = delegate.cookieHandler()
    override fun connectTimeout(): Optional<Duration> = delegate.connectTimeout()
    override fun followRedirects(): Redirect = delegate.followRedirects()
    override fun proxy(): Optional<ProxySelector> = delegate.proxy()
    override fun sslContext(): SSLContext = delegate.sslContext()
    override fun sslParameters(): SSLParameters = delegate.sslParameters()
    override fun authenticator(): Optional<Authenticator> = delegate.authenticator()
    override fun version(): Version = delegate.version()
    override fun executor(): Optional<Executor> = delegate.executor()

}

class ResourceFetcher(private val kunstigUri: String): Fetcher, AutoCloseable {
    private val client: LoggingClient = LoggingClient(HttpClient.newBuilder().build())
    private val keepAlive: ScheduledFuture<*> = Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate({
            try {
                loadRemote(URI.create(kunstigUri))
            } catch (e: Throwable) {
                log.error("scheduler: error loading uri=$kunstigUri", e)
            }
        }, 1, 120, TimeUnit.SECONDS)

    override fun requestNewImage() = loadRemote(URI.create(kunstigUri))

    private fun loadRemote(uri: URI): ByteArray {
        log.info("Fetching new image uri={}", uri)
        val req = HttpRequest.newBuilder().GET().uri(uri)
            .timeout(Duration.of(60, ChronoUnit.SECONDS))
            .build()
        val resp = client.send(req, BodyHandlers.ofByteArray())
        if (resp.statusCode() == 200) {
            return resp.body()!!
        } else {
            throw RuntimeException("error status=${resp.statusCode()} body=${String(resp.body()!!, Charsets.UTF_8)}")
        }
    }

    override fun close() {
        keepAlive.cancel(false)
    }
}