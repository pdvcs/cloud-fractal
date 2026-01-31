package net.pdutta.cloudfractal.integration

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@MicronautTest
class EndToEndFractalGenerationTest {

    @field:Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `complete fractal generation workflow`() {
        // Step 1: Access root endpoint (redirects to UI)
        val rootResponse = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/"),
            String::class.java
        )
        assertEquals(HttpStatus.OK, rootResponse.status)
        assertTrue(rootResponse.body()!!.contains("Mandelbrot"))

        // Step 2: Access the HTML UI
        val htmlResponse = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot"),
            String::class.java
        )
        assertEquals(HttpStatus.OK, htmlResponse.status)
        assertEquals("text/html", htmlResponse.contentType.get().toString())
        assertTrue(htmlResponse.body()!!.contains("Mandelbrot"))

        // Step 3: Generate a fractal image
        val imageResponse = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?width=500&height=500&palette=sunrise"),
            ByteArray::class.java
        )
        assertEquals(HttpStatus.OK, imageResponse.status)
        assertEquals("image/png", imageResponse.contentType.get().toString())

        val imageBytes = imageResponse.body()!!
        assertValidPng(imageBytes)

        // Step 4: Verify caching headers
        val etag = imageResponse.header("ETag")
        assertNotNull(etag)
        assertTrue(etag!!.startsWith("\""))

        val cacheControl = imageResponse.header("Cache-Control")
        assertNotNull(cacheControl)
        assertTrue(cacheControl!!.contains("immutable"))

        // Step 5: Request same image again and verify ETag matches
        val imageResponse2 = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?width=500&height=500&palette=sunrise"),
            ByteArray::class.java
        )
        val etag2 = imageResponse2.header("ETag")
        assertEquals(etag, etag2, "ETags should match for same parameters")
    }

    @Test
    fun `favicon is accessible`() {
        // Complete workflow for accessing favicon
        val faviconResponse = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/favicon.ico"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, faviconResponse.status)
        assertEquals("image/x-icon", faviconResponse.contentType.get().toString())

        val faviconBytes = faviconResponse.body()!!
        assertTrue(faviconBytes.isNotEmpty())

        // Verify ICO signature
        assertEquals(0x00.toByte(), faviconBytes[0])
        assertEquals(0x00.toByte(), faviconBytes[1])
        assertEquals(0x01.toByte(), faviconBytes[2])
        assertEquals(0x00.toByte(), faviconBytes[3])
    }

    @Test
    fun `multiple concurrent fractal requests complete successfully`() = runBlocking {
        // Simulate multiple concurrent users requesting different fractals
        val requests = listOf(
            "/mandelbrot/image?width=300&height=300&palette=sol",
            "/mandelbrot/image?width=300&height=300&palette=sunrise",
            "/mandelbrot/image?width=300&height=300&palette=dark",
            "/mandelbrot/image?width=300&height=300&centerX=-0.7&centerY=0.0&zoom=2.0",
            "/mandelbrot/image?width=300&height=300&centerX=0.0&centerY=0.0&zoom=1.0"
        )

        // Launch all requests concurrently
        val responses = requests.map { endpoint ->
            async {
                client.toBlocking().exchange(
                    HttpRequest.GET<Any>(endpoint),
                    ByteArray::class.java
                )
            }
        }.awaitAll()

        // Verify all requests succeeded
        assertEquals(5, responses.size)
        responses.forEach { response ->
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("image/png", response.contentType.get().toString())

            val imageBytes = response.body()!!
            assertValidPng(imageBytes)

            // Verify each has proper headers
            assertNotNull(response.header("ETag"))
            assertNotNull(response.header("Cache-Control"))
        }

        // Verify each response has a unique ETag (different parameters)
        val etags = responses.map { it.header("ETag")!! }.toSet()
        assertEquals(5, etags.size, "All responses should have different ETags")
    }

    private fun assertValidPng(bytes: ByteArray) {
        assertTrue(bytes.size > 8, "PNG data should be at least 8 bytes")
        // PNG signature: 89 50 4E 47
        assertEquals(0x89.toByte(), bytes[0], "Invalid PNG signature byte 0")
        assertEquals(0x50.toByte(), bytes[1], "Invalid PNG signature byte 1 ('P')")
        assertEquals(0x4E.toByte(), bytes[2], "Invalid PNG signature byte 2 ('N')")
        assertEquals(0x47.toByte(), bytes[3], "Invalid PNG signature byte 3 ('G')")
    }
}
