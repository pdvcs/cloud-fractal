package net.pdutta.cloudfractal.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@MicronautTest
class MandelbrotFractalControllerTest {

    @field:Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `GET mandelbrot returns HTML page`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot"),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertEquals("text/html", response.contentType.get().toString())
        assertTrue(response.body()!!.contains("Mandelbrot"))
    }

    @Test
    fun `GET mandelbrot image with default parameters`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertEquals("image/png", response.contentType.get().toString())

        val body = response.body()!!
        assertValidPng(body)

        // Verify ETag header exists
        assertTrue(response.headers.contains("ETag"))
        val etag = response.header("ETag")!!
        assertTrue(etag.startsWith("\"") && etag.endsWith("\""), "ETag should be quoted")

        // Verify cache headers
        val cacheControl = response.header("Cache-Control")!!
        assertTrue(cacheControl.contains("public"))
        assertTrue(cacheControl.contains("max-age=31536000"))
        assertTrue(cacheControl.contains("immutable"))
    }

    @Test
    fun `GET mandelbrot image with custom dimensions`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?width=500&height=300"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body()!!
        assertValidPng(body)
    }

    @Test
    fun `GET mandelbrot image with custom coordinates`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?centerX=-0.7&centerY=0.2&zoom=2.0"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body()!!
        assertValidPng(body)
    }

    @Test
    fun `GET mandelbrot image with SOL palette`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?palette=sol"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertValidPng(response.body()!!)
    }

    @Test
    fun `GET mandelbrot image with SUNRISE palette`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?palette=sunrise"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertValidPng(response.body()!!)
    }

    @Test
    fun `GET mandelbrot image with DARK palette`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?palette=dark"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertValidPng(response.body()!!)
    }

    @Test
    fun `GET mandelbrot image with invalid palette defaults to SOL`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?palette=invalid"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertValidPng(response.body()!!)

        // Should still work, just use default palette
        assertTrue(response.headers.contains("ETag"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["sol", "SOL", "Sol", "sOl"])
    fun `palette parameter is case insensitive`(palette: String) {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?palette=$palette"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertValidPng(response.body()!!)
    }

    @Test
    fun `GET mandelbrot image with same parameters returns same ETag`() {
        val request1 = HttpRequest.GET<Any>("/mandelbrot/image?width=200&height=200&centerX=-0.5&centerY=0.0&zoom=1.0&palette=sol")
        val request2 = HttpRequest.GET<Any>("/mandelbrot/image?width=200&height=200&centerX=-0.5&centerY=0.0&zoom=1.0&palette=sol")

        val response1 = client.toBlocking().exchange(request1, ByteArray::class.java)
        val response2 = client.toBlocking().exchange(request2, ByteArray::class.java)

        assertEquals(HttpStatus.OK, response1.status)
        assertEquals(HttpStatus.OK, response2.status)

        val etag1 = response1.header("ETag")!!
        val etag2 = response2.header("ETag")!!

        assertEquals(etag1, etag2, "Same parameters should produce same ETag")
    }

    @Test
    fun `GET mandelbrot image with different parameters returns different ETag`() {
        val request1 = HttpRequest.GET<Any>("/mandelbrot/image?width=200&height=200&zoom=1.0")
        val request2 = HttpRequest.GET<Any>("/mandelbrot/image?width=200&height=200&zoom=2.0")

        val response1 = client.toBlocking().exchange(request1, ByteArray::class.java)
        val response2 = client.toBlocking().exchange(request2, ByteArray::class.java)

        val etag1 = response1.header("ETag")!!
        val etag2 = response2.header("ETag")!!

        assertNotEquals(etag1, etag2, "Different parameters should produce different ETags")
    }

    @Test
    fun `GET mandelbrot image handles edge case dimensions`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?width=1&height=1"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body()!!
        assertValidPng(body)
    }

    @Test
    fun `PNG signature validation for all responses`() {
        val endpoints = listOf(
            "/mandelbrot/image",
            "/mandelbrot/image?palette=sunrise",
            "/mandelbrot/image?palette=dark",
            "/mandelbrot/image?width=100&height=100"
        )

        for (endpoint in endpoints) {
            val response = client.toBlocking().exchange(
                HttpRequest.GET<Any>(endpoint),
                ByteArray::class.java
            )

            assertEquals(HttpStatus.OK, response.status)
            assertValidPng(response.body()!!)
        }
    }

    @Test
    fun `ETag format validation`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image"),
            ByteArray::class.java
        )

        val etag = response.header("ETag")!!

        // ETag should be in quoted string format
        assertTrue(etag.startsWith("\""), "ETag should start with quote")
        assertTrue(etag.endsWith("\""), "ETag should end with quote")
        assertTrue(etag.length > 2, "ETag should have content between quotes")

        // The content between quotes should be parseable as an integer (hashCode)
        val etagValue = etag.substring(1, etag.length - 1)
        assertDoesNotThrow {
            etagValue.toInt()
        }
    }

    @Test
    fun `response timing is reasonable`() {
        val startTime = System.currentTimeMillis()

        client.toBlocking().exchange(
            HttpRequest.GET<Any>("/mandelbrot/image?width=500&height=500"),
            ByteArray::class.java
        )

        val elapsedTime = System.currentTimeMillis() - startTime

        // Should complete in less than 3 seconds for 500x500 image
        assertTrue(elapsedTime < 3000, "Request took ${elapsedTime}ms, should be < 3000ms")
    }

    private fun assertValidPng(bytes: ByteArray) {
        assertTrue(bytes.size > 8, "PNG data should be at least 8 bytes")
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        assertEquals(0x89.toByte(), bytes[0], "Invalid PNG signature byte 0")
        assertEquals(0x50.toByte(), bytes[1], "Invalid PNG signature byte 1 ('P')")
        assertEquals(0x4E.toByte(), bytes[2], "Invalid PNG signature byte 2 ('N')")
        assertEquals(0x47.toByte(), bytes[3], "Invalid PNG signature byte 3 ('G')")
    }
}
