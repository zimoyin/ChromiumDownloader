package io.github.headlesschrome.download

import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
import io.github.headlesschrome.utils.Zip
import io.github.headlesschrome.utils.connection
import org.openqa.selenium.json.Json
import org.openqa.selenium.json.JsonInput
import java.io.File
import java.io.StringReader
import java.net.Proxy
import java.net.URI
import java.net.URL

/**
 *
 * @author : zimo
 * @date : 2025/03/10
 */
abstract class AbsChromiumDownloader(
    val positioner: Positioner,
    val proxy: Proxy? = null,
    val path: String = CHROME_DOWNLOAD_PATH,
    val rootDir: File = File(path).resolve(positioner.revision),
    val appDir: File = rootDir.resolve("app"),
    val driverDir: File = rootDir.resolve("driver"),
) {

    fun initRootDir() {
        rootDir.mkdirs()
        if (!rootDir.exists()) throw RuntimeException("rootDir not created")
    }

    fun initAppDir() {
        appDir.mkdirs()
        if (!appDir.exists()) throw RuntimeException("appDir not created")
    }

    fun initDriverDir() {
        driverDir.mkdirs()
        if (!driverDir.exists()) throw RuntimeException("driverDir not created")
    }

    abstract fun downloadChrome()

    abstract fun downloadChromeDriver()
}