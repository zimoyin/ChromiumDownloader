package io.github.headlesschrome.utils

import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.chrome.ChromeDriver
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO


/**
 *
 * @author : zimo
 * @date : 2025/02/09
 */
inline fun ChromeDriver.use(block: ChromeDriver.() -> Unit) {
    try {
        block()
    } finally {
        quit()
    }
}


inline fun <reified T:Any> ChromeDriver.screenshot(): T {
    return when(T::class.java){
        BufferedImage::class.java -> {
            ImageIO.read(getScreenshotAs<File>(OutputType.FILE)) as T
        }
        String::class.java -> {
            getScreenshotAs<String>(OutputType.BASE64) as T
        }
        File::class.java -> {
            getScreenshotAs<File>(OutputType.FILE) as T
        }
        ByteArray::class.java -> {
            getScreenshotAs<ByteArray>(OutputType.BYTES) as T
        }
        InputStream::class.java -> {
            ByteArrayInputStream(getScreenshotAs<ByteArray>(OutputType.BYTES)) as T
        }
        Any::class.java -> {
            getScreenshotAs<String>(OutputType.BASE64) as T
        }
        else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
    }
}