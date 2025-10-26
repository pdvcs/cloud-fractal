package net.pdutta.cloudfractal

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.Micronaut.run
import java.io.InputStream
import java.net.URI

fun main(args: Array<String>) {
	run(*args)
}

@Controller("/")
class HelloController {
	@Get
	fun hello(): HttpResponse<Any> {
		return HttpResponse.temporaryRedirect(URI.create("/mandelbrot"))
	}
}

@Controller
class StaticAssetController {

    @Get("/favicon.ico")
    fun favicon(): HttpResponse<ByteArray> {
        val inputStream: InputStream? = this.javaClass.getResourceAsStream("/public/favicon.ico")
        return if (inputStream != null) {
            HttpResponse.ok(inputStream.readAllBytes()).contentType(MediaType.IMAGE_X_ICON_TYPE)
        } else {
            HttpResponse.notFound()
        }
    }
}
