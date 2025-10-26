package net.pdutta.cloudfractal.service

import jakarta.inject.Singleton
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Singleton
class ImageEncoder {

    fun encodeToPng(image: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }
}