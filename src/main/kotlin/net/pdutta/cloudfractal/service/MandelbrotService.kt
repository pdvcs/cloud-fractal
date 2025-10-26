package net.pdutta.cloudfractal.service

import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.ln

data class FractalParameters(
    val width: Int,
    val height: Int,
    val centerX: Double,
    val centerY: Double,
    val zoom: Double,
    val palette: ColorScheme = ColorScheme.SOL,
    val maxIterations: Int = 1024
)

enum class ColorScheme() {
    SUNRISE,
    SOL,
    DARK;
}

@Singleton
class MandelbrotService {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(MandelbrotService::class.java)
    }

    // Dispatcher optimized for CPU-intensive work
    private val computationDispatcher = Dispatchers.Default

    suspend fun generateFractal(params: FractalParameters): BufferedImage {
        LOG.info(
            "Generating fractal: {}x{}, center=({}, {}), zoom={}, palette={}",
            params.width, params.height, params.centerX, params.centerY, params.zoom, params.palette
        )

        return generateWithCoroutines(params)
    }

    private suspend fun generateWithCoroutines(params: FractalParameters): BufferedImage =
        withContext(computationDispatcher) {
            val image = BufferedImage(params.width, params.height, BufferedImage.TYPE_INT_RGB)

            // Process rows in parallel using coroutines
            // Each coroutine calculates a row and returns the pixel data for that row.
            val rowJobs: List<Deferred<Pair<Int, IntArray>>> = (0 until params.height).map { y ->
                async {
                    y to processRow(y, params)
                }
            }

            // Wait for all rows to be calculated and then write them to the image
            rowJobs.awaitAll().forEach { (y, pixelData) ->
                image.setRGB(0, y, params.width, 1, pixelData, 0, params.width)
            }

            image
        }

    private fun processRow(y: Int, params: FractalParameters): IntArray {
        val rangeX = 4.0 / params.zoom
        val rangeY = 4.0 / params.zoom
        val rowPixels = IntArray(params.width)
        for (x in 0 until params.width) {
            val iterations = calculateIterations(x, y, params, rangeX, rangeY)
            rowPixels[x] = getColor(iterations, params.maxIterations, params.palette)
        }
        return rowPixels
    }

    private fun calculateIterations(
        x: Int,
        y: Int,
        params: FractalParameters,
        rangeX: Double,
        rangeY: Double
    ): IterationResult {
        var zx = 0.0
        var zy = 0.0

        // Map pixel to complex plane
        val cX = params.centerX + (x - params.width / 2.0) * rangeX / params.width
        val cY = params.centerY + (y - params.height / 2.0) * rangeY / params.height

        // Check if the point is within the main cardioid or period-2 bulb.
        // If so, we know it's in the set without iterating.
        val q = (cX - 0.25) * (cX - 0.25) + cY * cY
        if (q * (q + (cX - 0.25)) < 0.25 * cY * cY) {
            return IterationResult(params.maxIterations, 0.0, 0.0) // In the main cardioid
        }
        val xPlus1 = cX + 1.0
        if (xPlus1 * xPlus1 + cY * cY < 0.0625) { // 1/16
            return IterationResult(params.maxIterations, 0.0, 0.0) // In the period-2 bulb
        }

        var iter = 0
        while (zx * zx + zy * zy < 4.0 && iter < params.maxIterations) {
            val tmp = zx * zx - zy * zy + cX
            zy = 2.0 * zx * zy + cY
            zx = tmp
            iter++
        }

        return IterationResult(iter, zx, zy)
    }

    private fun getColor(result: IterationResult, maxIter: Int, palette: ColorScheme): Int {
        return if (result.iterations == maxIter) {
            Color.BLACK.rgb
        } else {
            // Smooth coloring
            val logZn = ln(result.zx * result.zx + result.zy * result.zy) / 2.0
            val nu = ln(logZn / ln(2.0)) / ln(2.0)
            val continuousIndex = result.iterations + 1 - nu

            
            when (palette) {
                ColorScheme.DARK -> {
                    val hue = (0.6f + 3 * continuousIndex.toFloat() / maxIter) % 1f
                    val brightness = 0.2f + (continuousIndex.toFloat() / maxIter * 4) % 0.5f
                    Color.HSBtoRGB(hue, 0.9f, brightness)
                }

                ColorScheme.SUNRISE -> {
                    Color.HSBtoRGB((0.95f + 10 * continuousIndex.toFloat() / maxIter) % 1f, 0.6f, 1.0f)
                }

                else -> { // "SOL", a vibrant, fast-cycling palette
                    val hue = (0.1f + 25 * continuousIndex.toFloat() / maxIter) % 1f
                    val saturation = 0.8f + (continuousIndex.toFloat() / maxIter * 5) % 0.2f
                    Color.HSBtoRGB(hue, saturation, 1.0f)
                }
            }
        }
    }

    private data class IterationResult(val iterations: Int, val zx: Double, val zy: Double)
}