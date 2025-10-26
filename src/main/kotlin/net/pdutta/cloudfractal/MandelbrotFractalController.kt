package net.pdutta.cloudfractal

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.server.types.files.StreamedFile
import net.pdutta.cloudfractal.service.ColorScheme
import net.pdutta.cloudfractal.service.FractalParameters
import net.pdutta.cloudfractal.service.ImageEncoder
import net.pdutta.cloudfractal.service.MandelbrotService
import java.net.URL

@Controller("/mandelbrot")
class MandelbrotFractalController(
    private val mandelbrotService: MandelbrotService,
    private val imageEncoder: ImageEncoder
) {

    @Get(produces = [MediaType.TEXT_HTML])
    fun index(): StreamedFile {
        val url: URL = javaClass.getResource("/public/mandelbrot.html")!!
        return StreamedFile(url)
    }

    @Get("/image", produces = [MediaType.IMAGE_PNG])
    suspend fun getMandelbrot(
        @QueryValue(defaultValue = "1000") width: Int,
        @QueryValue(defaultValue = "1000") height: Int,
        @QueryValue(defaultValue = "-0.5") centerX: Double,
        @QueryValue(defaultValue = "0.0") centerY: Double,
        @QueryValue(defaultValue = "1.0") zoom: Double,
        @QueryValue(defaultValue = "sol") palette: String
    ): HttpResponse<ByteArray> {
        val colorScheme = try { // convert query param to a type-safe enum
            ColorScheme.valueOf(palette.uppercase())
        } catch (_: IllegalArgumentException) {
            ColorScheme.SOL // default color scheme
        }

        val params = FractalParameters(
            width = width,
            height = height,
            centerX = centerX,
            centerY = centerY,
            zoom = zoom,
            palette = colorScheme
        )

        val image = mandelbrotService.generateFractal(params)
        return HttpResponse.ok(imageEncoder.encodeToPng(image))
    }
}