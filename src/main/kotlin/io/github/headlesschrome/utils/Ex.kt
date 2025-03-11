package io.github.headlesschrome.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.chrome.ChromeDriver
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.io.encoding.ExperimentalEncodingApi


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

inline fun WebDriver.isQuit(): Boolean {
    return try {
        windowHandles.isEmpty()
    } catch (e: WebDriverException) {
        true
    }.also { isQuit ->
        if (isQuit) quit()
    }
}

/**
 * 浏览器关闭后执行
 */
fun ChromeDriver.onQuit(block: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        while (!isQuit()) {
            delay(200)
        }
        block()
    }
}

/**
 * 阻塞直到浏览器关闭
 */
fun ChromeDriver.blockUntilQuit(block: (ChromeDriver.() -> Unit) = {}) {
    this.finally()
    block()
    while (!isQuit()) {
        Thread.sleep(200)
    }
}

/**
 * 阻塞直到浏览器关闭
 */
suspend fun ChromeDriver.blockUntilQuitSuspend(block: (suspend ChromeDriver.() -> Unit) = {}) {
    this.finally()
    block()
    while (!isQuit()) {
        delay(200)
    }
}

/**
 * 在JVM关闭时退出浏览器
 */
inline fun ChromeDriver.finally(block: (ChromeDriver.() -> Unit) = { }) {
    Runtime.getRuntime().addShutdownHook(Thread {
        quit()
    })
    block()
}


inline fun ChromeDriver.screenshotAsFile(path: String? = null): File {
    return screenshot<File>().let { if (path != null) it.copyTo(File(path), true) else it }
}

/**
 * 截图并返回对应类型的截图
 * * T = File | Path | URL | URI | Base64 | Base64.Encoder | BufferedImage | String | File | ByteArray | InputStream | Any
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> ChromeDriver.screenshot(): T {
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