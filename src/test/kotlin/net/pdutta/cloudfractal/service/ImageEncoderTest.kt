package net.pdutta.cloudfractal.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class ImageEncoderTest {

    private val encoder = ImageEncoder()

    @Test
    fun `encodeToPng returns valid PNG byte array`() {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val pngBytes = encoder.encodeToPng(image)

        assertValidPng(pngBytes)
    }

    @Test
    fun `encodeToPng handles TYPE_INT_RGB images`() {
        val image = BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.RED
        graphics.fillRect(0, 0, 50, 50)
        graphics.dispose()

        val pngBytes = encoder.encodeToPng(image)

        assertValidPng(pngBytes)
        assertTrue(pngBytes.isNotEmpty())
    }

    @Test
    fun `encodeToPng handles TYPE_INT_ARGB images`() {
        val image = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(255, 0, 0, 128) // Red with 50% alpha
        graphics.fillRect(0, 0, 50, 50)
        graphics.dispose()

        val pngBytes = encoder.encodeToPng(image)

        assertValidPng(pngBytes)
        assertTrue(pngBytes.isNotEmpty())
    }

    @Test
    fun `encodeToPng produces decodable image`() {
        val originalImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val graphics = originalImage.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, 100, 100)
        graphics.dispose()

        val pngBytes = encoder.encodeToPng(originalImage)

        // Decode the PNG bytes back to a BufferedImage
        val decodedImage = ImageIO.read(ByteArrayInputStream(pngBytes))

        assertNotNull(decodedImage)
        assertEquals(originalImage.width, decodedImage.width)
        assertEquals(originalImage.height, decodedImage.height)
    }

    @Test
    fun `encodeToPng handles minimal 1x1 image`() {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, Color.BLACK.rgb)

        val pngBytes = encoder.encodeToPng(image)

        assertValidPng(pngBytes)

        // Verify it can be decoded
        val decodedImage = ImageIO.read(ByteArrayInputStream(pngBytes))
        assertNotNull(decodedImage)
        assertEquals(1, decodedImage.width)
        assertEquals(1, decodedImage.height)
    }

    @Test
    fun `encodeToPng produces consistent output`() {
        val image = BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.GREEN
        graphics.fillRect(0, 0, 50, 50)
        graphics.dispose()

        val pngBytes1 = encoder.encodeToPng(image)
        val pngBytes2 = encoder.encodeToPng(image)

        assertArrayEquals(pngBytes1, pngBytes2)
    }

    @Test
    fun `encodeToPng handles different dimensions`() {
        val sizes = listOf(1 to 1, 10 to 10, 100 to 50, 500 to 300, 1000 to 1000)

        for ((width, height) in sizes) {
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val pngBytes = encoder.encodeToPng(image)

            assertValidPng(pngBytes)

            // Verify dimensions are preserved
            val decodedImage = ImageIO.read(ByteArrayInputStream(pngBytes))
            assertEquals(width, decodedImage.width, "Width mismatch for ${width}x${height}")
            assertEquals(height, decodedImage.height, "Height mismatch for ${width}x${height}")
        }
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
