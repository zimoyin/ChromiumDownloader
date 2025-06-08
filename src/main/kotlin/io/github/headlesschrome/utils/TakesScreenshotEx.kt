package io.github.headlesschrome.utils

import org.openqa.selenium.*
import org.openqa.selenium.remote.RemoteWebElement
import ru.yandex.qatools.ashot.AShot
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider
import ru.yandex.qatools.ashot.shooting.ShootingStrategies
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.java


/**
 *
 * @author : zimo
 * @date : 2025/06/09
 */


fun TakesScreenshot.screenshotAsFile(path: String? = null): File {
    return screenshotAsT<File>().let { if (path != null) it.copyTo(File(path), true) else it }
}

/**
 * 截图并返回对应类型的截图
 * * T = File | Path | URL | URI | Base64 | Base64.Encoder | BufferedImage | String | File | ByteArray | InputStream | Any
 */
@Deprecated("please use screenshotAsT()")
inline fun <reified T : Any> TakesScreenshot.screenshot(): T {
    return screenshotAsT()
}

/**
 * 截图并返回对应类型的截图
 * * T = File | Path | URL | URI | Base64 | Base64.Encoder | BufferedImage | String | File | ByteArray | InputStream | Any
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> TakesScreenshot.screenshotAsT(): T {
    return when (T::class.java) {

        ImageIO::class.java -> {
            ImageIO.read(getScreenshotAs<File>(OutputType.FILE)) as T
        }

        Path::class.java -> {
            getScreenshotAs<File>(OutputType.FILE).toPath() as T
        }

        URL::class.java -> {
            getScreenshotAs<File>(OutputType.FILE).toURI().toURL() as T
        }

        URI::class.java -> {
            getScreenshotAs<File>(OutputType.FILE).toURI() as T
        }

        Base64::class.java -> {
            getScreenshotAs<String>(OutputType.BASE64) as T
        }

        kotlin.io.encoding.Base64::class.java -> {
            Base64.getEncoder().encodeToString(getScreenshotAs<ByteArray>(OutputType.BYTES)) as T
        }

        Base64::getEncoder::class.java -> {
            Base64.getEncoder().encodeToString(getScreenshotAs<ByteArray>(OutputType.BYTES)) as T
        }

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

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> TakesScreenshot.fullScreenshotAsT(): T {
    val driver = when (this) {
        is WebDriver -> this

        is WebElement -> this.driver

        else -> throw UnsupportedOperationException(
            "Unsupported TakesScreenshot type: ${this::class.java.name}"
        )
    }
    val size = driver.manage().window().size
    driver.manage().window().size = driver.scrollSize
    return screenshotAsT<T>().apply {
        driver.manage().window().size = size
    }
}

fun TakesScreenshot.fullScreenshotAsFile(path: String? = null): File {
    return fullScreenshotAsT<File>().let { if (path != null) it.copyTo(File(path), true) else it }
}

/**
 * 滚动截图
 * 截图并返回任意类型的全屏截图（支持 WebDriver 整页截图和 WebElement 元素截图）
 * 支持类型：File | Path | URL | URI | Base64 | Base64.Encoder | BufferedImage | String | ByteArray | InputStream | Any
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> TakesScreenshot.takeFullPageScreenshot(): T {
    // 指定 dpr
    val dpr = (this as JavascriptExecutor).executeScript("return window.devicePixelRatio;") as Long
    val shootingStrategy = ShootingStrategies.viewportRetina(800, 0, 0, dpr.toFloat())
//    val shootingStrategy = ShootingStrategies.viewportPasting(800)

    val shot = AShot()
        .shootingStrategy(shootingStrategy)
        .coordsProvider(WebDriverCoordsProvider())

    // 获取 BufferedImage
    val bufferedImage = when (this) {
        is WebDriver -> shot.takeScreenshot(this).image

        is WebElement -> shot.takeScreenshot(this.driver, this).image

        else -> throw UnsupportedOperationException(
            "Unsupported TakesScreenshot type: ${this::class.java.name}"
        )
    }

    return when (T::class) {
        BufferedImage::class -> bufferedImage as T

        File::class -> {
            val tempFile = File.createTempFile("screenshot", ".png").apply {
                deleteOnExit()
                ImageIO.write(bufferedImage, "PNG", this)
            }
            tempFile as T
        }

        Path::class -> {
            val tempFile = File.createTempFile("screenshot", ".png").apply {
                deleteOnExit()
                ImageIO.write(bufferedImage, "PNG", this)
            }
            tempFile.toPath() as T
        }

        URI::class -> {
            val tempFile = File.createTempFile("screenshot", ".png").apply {
                deleteOnExit()
                ImageIO.write(bufferedImage, "PNG", this)
            }
            tempFile.toURI() as T
        }

        URL::class -> {
            val tempFile = File.createTempFile("screenshot", ".png").apply {
                deleteOnExit()
                ImageIO.write(bufferedImage, "PNG", this)
            }
            tempFile.toURI().toURL() as T
        }

        String::class -> {
            val baos = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "PNG", baos)
            Base64.getEncoder().encodeToString(baos.toByteArray()) as T
        }

        ByteArray::class -> {
            val baos = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "PNG", baos)
            baos.toByteArray() as T
        }

        InputStream::class -> {
            val baos = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "PNG", baos)
            ByteArrayInputStream(baos.toByteArray()) as T
        }

        Base64::class -> {
            val baos = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "PNG", baos)
            Base64.getEncoder().encodeToString(baos.toByteArray()) as T
        }

        Base64.Encoder::class -> {
            Base64.getEncoder() as T
        }

        Any::class -> {
            val baos = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "PNG", baos)
            Base64.getEncoder().encodeToString(baos.toByteArray()) as T
        }

        else -> throw IllegalArgumentException("Unsupported return type: ${T::class.java}")
    }
}

/**
 * 滚动截图，保存全屏截图到文件
 * @param path 可选目标路径，null 时返回临时文件
 * @return 文件对象
 */
fun TakesScreenshot.takeFullScreenshotAsFile(path: String): File {
    return File(path).apply {
        ImageIO.write(fullScreenshotAsT<BufferedImage>(), this.extension, this)
    }
}
