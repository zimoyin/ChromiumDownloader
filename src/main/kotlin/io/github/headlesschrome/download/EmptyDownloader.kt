package io.github.headlesschrome.download

import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
import io.github.headlesschrome.utils.Zip
import io.github.headlesschrome.utils.connection
import org.openqa.selenium.json.Json
import org.openqa.selenium.json.JsonInput
import java.io.File
import java.io.FileNotFoundException
import java.io.StringReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL


/**
 * 创建一个空的下载器, 如果不存在则不下载，直接抛出异常
 */
class EmptyDownloader(
    proxy: Proxy? = null,
    positioner: Positioner = Positioner(Platform.currentPlatform(), "null"),
    path: String = "./chrome",
    rootDir: File = File(path).resolve(positioner.revision),
    appDir: File = rootDir.resolve("app"),
    driverDir: File = rootDir.resolve("driver"),
) : AbsChromiumDownloader(positioner, proxy, path, rootDir, appDir, driverDir) {
    override fun downloadChrome() {
        throw FileNotFoundException("Not fund chrome")
    }

    override fun downloadChromeDriver() {
        throw FileNotFoundException("Not fund chrome driver")
    }
}