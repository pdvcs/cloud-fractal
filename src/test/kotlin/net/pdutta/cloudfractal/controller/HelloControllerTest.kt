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
class HelloControllerTest {

    @field:Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `GET root serves mandelbrot page`() {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/"),
            String::class.java
        )

        // Client follows redirect; ensure we land on the HTML page
        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.body()!!.contains("Mandelbrot"))
    }

    @Test
    fun `GET root returns HTML content`() {
        // Verify that when we try to GET / we can reach the UI
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/"),
            String::class.java
        )
        assertEquals(HttpStatus.OK, response.status)
    }
}
