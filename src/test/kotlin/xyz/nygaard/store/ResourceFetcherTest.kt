package xyz.nygaard.store

import com.google.common.hash.Hashing
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.Okio
import okio.Source
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileInputStream

internal class ResourceFetcherTest {
    private lateinit var mock: MockWebServer
    val imgData = requireNotNull(FileInputStream("src/test/resources/working.jpg").readAllBytes())

    @BeforeEach
    fun setup() {
        mock = MockWebServer()
        mock.start()
    }

    @AfterEach
    fun after() {
        mock.shutdown()
    }

    @Test
    fun resourcefetcher() {
        val body = Buffer().write(imgData)
        mock.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val f = ResourceFetcher(mock.url("/").toString())

        val img = f.requestNewImage()
        f.close()
        Assertions.assertThat(Hashing.sha256().hashBytes(img).toString()).isEqualTo("2a9cd8f4fdca9ef01d12d8f552dd25ec6430416b11febb1cc8226cea7bf766b7")
    }

}