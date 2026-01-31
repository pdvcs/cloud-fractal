package net.pdutta.cloudfractal.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import kotlin.system.measureTimeMillis

class MandelbrotServiceTest {

    private val service = MandelbrotService()

    @Test
    fun `generateFractal returns BufferedImage with correct dimensions`() = runTest {
        val params = FractalParameters(
            width = 400,
            height = 300,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        val image = service.generateFractal(params)

        assertNotNull(image)
        assertEquals(400, image.width)
        assertEquals(300, image.height)
    }

    @Test
    fun `generateFractal with SOL color scheme`() = runTest {
        val params = FractalParameters(
            width = 100,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL
        )

        val image = service.generateFractal(params)

        assertNotNull(image)
        assertEquals(100, image.width)
        assertEquals(100, image.height)
    }

    @Test
    fun `generateFractal with SUNRISE color scheme`() = runTest {
        val params = FractalParameters(
            width = 100,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SUNRISE
        )

        val image = service.generateFractal(params)

        assertNotNull(image)
    }

    @Test
    fun `generateFractal with DARK color scheme`() = runTest {
        val params = FractalParameters(
            width = 100,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.DARK
        )

        val image = service.generateFractal(params)

        assertNotNull(image)
    }

    @Test
    fun `generateFractal with high zoom produces different image`() = runTest {
        val paramsLowZoom = FractalParameters(
            width = 200,
            height = 200,
            centerX = -0.75,
            centerY = 0.1,
            zoom = 1.0
        )

        val paramsHighZoom = FractalParameters(
            width = 200,
            height = 200,
            centerX = -0.75,
            centerY = 0.1,
            zoom = 8.0
        )

        val imageLowZoom = service.generateFractal(paramsLowZoom)
        val imageHighZoom = service.generateFractal(paramsHighZoom)

        // Images should differ across a sample grid
        var differingPixels = 0
        for (x in 0 until 200 step 20) {
            for (y in 0 until 200 step 20) {
                if (imageLowZoom.getRGB(x, y) != imageHighZoom.getRGB(x, y)) {
                    differingPixels++
                }
            }
        }
        assertTrue(differingPixels > 5, "Images with different zoom levels should differ")
    }

    @Test
    fun `generateFractal completes within reasonable time`() = runTest {
        val params = FractalParameters(
            width = 1000,
            height = 1000,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        val elapsedTime = measureTimeMillis {
            service.generateFractal(params)
        }

        // Should complete in less than 5 seconds
        assertTrue(elapsedTime < 5000, "Generation took ${elapsedTime}ms, should be < 5000ms")
    }

    @Test
    fun `generateFractal with minimal dimensions`() = runTest {
        val params = FractalParameters(
            width = 1,
            height = 1,
            centerX = 0.0,
            centerY = 0.0,
            zoom = 1.0
        )

        val image = service.generateFractal(params)

        assertNotNull(image)
        assertEquals(1, image.width)
        assertEquals(1, image.height)
    }

    @Test
    fun `known point in Mandelbrot set is colored black`() = runTest {
        // Test that point (0, 0) which is in the Mandelbrot set renders as black
        val params = FractalParameters(
            width = 100,
            height = 100,
            centerX = 0.0,
            centerY = 0.0,
            zoom = 100.0 // High zoom centered on origin
        )

        val image = service.generateFractal(params)

        // Center pixel should be black (in the set)
        val centerPixel = image.getRGB(50, 50)
        assertEquals(Color.BLACK.rgb, centerPixel)
    }

    @Test
    fun `different center points produce different images`() = runTest {
        val params1 = FractalParameters(
            width = 200,
            height = 200,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        val params2 = FractalParameters(
            width = 200,
            height = 200,
            centerX = 0.0,
            centerY = 0.0,
            zoom = 1.0
        )

        val image1 = service.generateFractal(params1)
        val image2 = service.generateFractal(params2)

        // Check several pixels to ensure images differ
        var differingPixels = 0
        for (x in 0 until 200 step 20) {
            for (y in 0 until 200 step 20) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    differingPixels++
                }
            }
        }

        assertTrue(differingPixels > 10, "Images with different centers should have many differing pixels")
    }

    @Test
    fun `extreme zoom levels work correctly`() = runTest {
        val params = FractalParameters(
            width = 100,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1000.0
        )

        val image = service.generateFractal(params)

        assertNotNull(image)
        assertEquals(100, image.width)
        assertEquals(100, image.height)
    }

    @Test
    fun `non-square aspect ratios work correctly`() = runTest {
        val wideParams = FractalParameters(
            width = 300,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        val tallParams = FractalParameters(
            width = 100,
            height = 300,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        val wideImage = service.generateFractal(wideParams)
        val tallImage = service.generateFractal(tallParams)

        assertEquals(300, wideImage.width)
        assertEquals(100, wideImage.height)

        assertEquals(100, tallImage.width)
        assertEquals(300, tallImage.height)
    }

    @Test
    fun `maxIterations parameter affects output`() = runTest {
        val lowIterParams = FractalParameters(
            width = 100,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            maxIterations = 100
        )

        val highIterParams = FractalParameters(
            width = 100,
            height = 100,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            maxIterations = 2000
        )

        val lowIterImage = service.generateFractal(lowIterParams)
        val highIterImage = service.generateFractal(highIterParams)

        // Images should differ due to different iteration counts
        var differingPixels = 0
        for (x in 0 until 100 step 10) {
            for (y in 0 until 100 step 10) {
                if (lowIterImage.getRGB(x, y) != highIterImage.getRGB(x, y)) {
                    differingPixels++
                }
            }
        }

        assertTrue(differingPixels > 0, "Different maxIterations should produce different images")
    }

    @Test
    fun `concurrent calls complete successfully`() = runTest {
        val params1 = FractalParameters(200, 200, -0.5, 0.0, 1.0, ColorScheme.SOL)
        val params2 = FractalParameters(200, 200, 0.0, 0.0, 1.0, ColorScheme.SUNRISE)
        val params3 = FractalParameters(200, 200, -0.7, 0.0, 2.0, ColorScheme.DARK)

        // Launch multiple concurrent fractal generations
        val results = listOf(
            async { service.generateFractal(params1) },
            async { service.generateFractal(params2) },
            async { service.generateFractal(params3) }
        ).awaitAll()

        // All should complete successfully
        assertEquals(3, results.size)
        results.forEach { image ->
            assertNotNull(image)
            assertEquals(200, image.width)
            assertEquals(200, image.height)
        }
    }
}
