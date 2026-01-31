package net.pdutta.cloudfractal.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FractalParametersTest {

    @Test
    fun `data class equality works correctly`() {
        val params1 = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL,
            maxIterations = 1024
        )

        val params2 = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL,
            maxIterations = 1024
        )

        assertEquals(params1, params2)
    }

    @Test
    fun `hashCode is consistent with equality`() {
        val params1 = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL
        )

        val params2 = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL
        )

        assertEquals(params1.hashCode(), params2.hashCode())
    }

    @Test
    fun `different parameters produce different hashCodes`() {
        val params1 = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL
        )

        val params2 = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.7, // Different centerX
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL
        )

        assertNotEquals(params1.hashCode(), params2.hashCode())
        assertNotEquals(params1, params2)
    }

    @Test
    fun `default maxIterations is 1024`() {
        val params = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        assertEquals(1024, params.maxIterations)
    }

    @Test
    fun `custom maxIterations can be set`() {
        val params = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            maxIterations = 2048
        )

        assertEquals(2048, params.maxIterations)
    }

    @Test
    fun `copy with modifications works`() {
        val original = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0,
            palette = ColorScheme.SOL
        )

        val modified = original.copy(zoom = 2.0, palette = ColorScheme.DARK)

        assertEquals(800, modified.width)
        assertEquals(600, modified.height)
        assertEquals(-0.5, modified.centerX)
        assertEquals(0.0, modified.centerY)
        assertEquals(2.0, modified.zoom)
        assertEquals(ColorScheme.DARK, modified.palette)
    }

    @Test
    fun `ColorScheme enum has expected values`() {
        val values = ColorScheme.entries.toTypedArray()

        assertEquals(3, values.size)
        assertTrue(values.contains(ColorScheme.SUNRISE))
        assertTrue(values.contains(ColorScheme.SOL))
        assertTrue(values.contains(ColorScheme.DARK))
    }

    @Test
    fun `ColorScheme valueOf works correctly`() {
        assertEquals(ColorScheme.SOL, ColorScheme.valueOf("SOL"))
        assertEquals(ColorScheme.SUNRISE, ColorScheme.valueOf("SUNRISE"))
        assertEquals(ColorScheme.DARK, ColorScheme.valueOf("DARK"))
    }

    @Test
    fun `default palette is SOL`() {
        val params = FractalParameters(
            width = 800,
            height = 600,
            centerX = -0.5,
            centerY = 0.0,
            zoom = 1.0
        )

        assertEquals(ColorScheme.SOL, params.palette)
    }

    @Test
    fun `parameters support different dimensions`() {
        val square = FractalParameters(1000, 1000, 0.0, 0.0, 1.0)
        val widescreen = FractalParameters(1920, 1080, 0.0, 0.0, 1.0)
        val portrait = FractalParameters(600, 800, 0.0, 0.0, 1.0)

        assertEquals(1000, square.width)
        assertEquals(1000, square.height)

        assertEquals(1920, widescreen.width)
        assertEquals(1080, widescreen.height)

        assertEquals(600, portrait.width)
        assertEquals(800, portrait.height)
    }
}
