package net.pdutta.cloudfractal.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@MicronautTest
class StaticAssetControllerTest {

    @field:Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `GET favicon ico returns ICO file`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/favicon.ico"),
            ByteArray::class.java
        )

        assertEquals(HttpStatus.OK, response.status)
        assertEquals("image/x-icon", response.contentType.get().toString())

        val body = response.body()!!
        assertTrue(body.isNotEmpty(), "Favicon should not be empty")

        // Verify ICO file signature (first 4 bytes: 00 00 01 00 for .ico format)
        assertEquals(0x00.toByte(), body[0], "ICO signature byte 0")
        assertEquals(0x00.toByte(), body[1], "ICO signature byte 1")
        assertEquals(0x01.toByte(), body[2], "ICO signature byte 2")
        assertEquals(0x00.toByte(), body[3], "ICO signature byte 3")
    }

    @Test
    fun `favicon response is non-empty`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/favicon.ico"),
            ByteArray::class.java
        )

        val body = response.body()!!
        assertTrue(body.size > 100, "Favicon should be at least 100 bytes")
    }
}
