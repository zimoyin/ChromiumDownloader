package io.github.headlesschrome.download

import io.github.headlesschrome.download.ChromiumDownloader.Companion.createURL
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
 * HuaweicloudChromiumDownloader 是一个用于下载指定版本的 Chrome 浏览器及其驱动程序的工具类。
 * 它根据给定的平台和修订版号自动定位并下载的 Chrome 和 ChromeDriver 的二进制文件包，
 * 并将其解压到指定目录下。此工具适用于需要在不同环境中动态部署特定版本的 Chrome 浏览器和驱动程序的场景。
 * 值得一提的是华为云许久都未更新了，因此 884014 已经是最新版本了，但是 ChromeDriver 还在更新，因此下载驱动时可以手动指定特定版本的驱动。
 * @author : zimo
 * @date : 2025/03/10
 */
class HuaweicloudChromiumDownloader(
    path: String = "./chrome",
    positioner: Positioner = Positioner(Platform.currentPlatform(), "884014"),
    proxy: Proxy? = null,
    rootDir: File = File(path).resolve(positioner.revision),
    appDir: File = rootDir.resolve("app"),
    driverDir: File = rootDir.resolve("driver"),
) : AbsChromiumDownloader(positioner, proxy, path, rootDir, appDir, driverDir) {
    override fun downloadChrome() {
        val fileName = when (positioner.platform) {
            Platform.Linux -> "chrome-linux.zip"
            Platform.Mac -> "chrome-mac.zip"
            Platform.Win, Platform.Win_x64 -> "chrome-win.zip"
            else -> throw IllegalArgumentException("Unsupported platform: ${positioner.platform}")
        }
        val url = createURL(positioner.platform, positioner.revision, fileName)
        val zip = File(appDir, fileName)
        url.connection(proxy).getInputStream().use { input ->
            zip.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Zip.unzip(zip)
        zip.delete()
    }

    override fun downloadChromeDriver() {
        downloadChromeDriver("92.0.4515.43")
    }

    fun downloadChromeDriver(driverVersion: String) {
        val fileName = when (positioner.platform) {
            Platform.Linux, Platform.Linux_x64 -> "chromedriver-linux64.zip"
            Platform.Mac -> "chromedriver-mac-x64.zip"
            Platform.Mac_Arm -> "chromedriver-mac-arm64.zip"
            Platform.Win -> "chromedriver-win32.zip"
            Platform.Win_x64 -> "chromedriver-win64.zip"
            else -> throw IllegalArgumentException("Unsupported platform: ${positioner.platform}")
        }.let {
            if (driverVersion.split(".").first().toInt() <= 95) {
                it.replace("mac-x64", "mac64").replace("-", "_")
            } else {
                it
            }
        }
        val url = createURL(null, driverVersion, fileName, BASE_URL_DRIVER)
        val zip = File(driverDir, fileName)
        url.connection(proxy).getInputStream().use { input ->
            zip.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Zip.unzip(zip)
        zip.delete()
    }


    companion object {
        val BASE_URL_CHROME = "https://mirrors.huaweicloud.com/chromium-browser-snapshots/"
        val BASE_URL_DRIVER = "https://mirrors.huaweicloud.com/chromedriver"

        fun createURL(
            platform: Platform? = Platform.currentPlatform(),
            revision: String = "",
            fileName: String = "",
            base: String = BASE_URL_CHROME
        ): URL {
            return URI.create("$base${platform?.name ?: ""}/${revision}/$fileName").toURL()
        }

        fun getLastPosition(proxy: Proxy? = null): Positioner {
            return Positioner(Platform.currentPlatform(), "884014")
        }


        /**
         * 获取最新 ChromeDriver 版本信息
         */
        fun getLastChromeDriverPosition(proxy: Proxy? = null): Positioner {
            return Positioner(Platform.currentPlatform(), getChromeDriverVersions(proxy).maxOf { it.version })
        }

        /**
         * 获取所有 ChromeDriver 版本信息
         */
        fun getChromeDriverVersions(proxy: Proxy? = null): MutableList<ChromeDriver> {
            val url = createURL(null, fileName = ".index.json", base = BASE_URL_DRIVER)
            val connection = url.connection(proxy)
            val json = connection.inputStream.use {
                it.readAllBytes().decodeToString()
            }
            val jsonInput: JsonInput = Json().newInput(StringReader(json))
            val from =
                jsonInput.read<HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>>>(HashMap::class.java)
            val chromedrivers = from.getValue("chromedriver")
            val list = mutableListOf<ChromeDriver>()
            for (chromedriver in chromedrivers) {
                val version = chromedriver.key
                val items = mutableListOf<Item>()
                for (path in chromedriver.value.getValue("files")) {
                    val platform = when {
                        path.contains("mac-arm64") -> Platform.Mac_Arm
                        path.contains("mac") -> Platform.Mac
                        path.contains("win64") -> Platform.Win
                        path.contains("win32") || path.contains("win") -> Platform.Win
                        path.contains("linux") -> Platform.Linux
                        else -> throw IllegalArgumentException("Unsupported platform: $path")
                    }
                    items.add(Item(Positioner(platform, version), path))
                }
                list.add(ChromeDriver(version, items))
            }

            return list
        }
    }

    data class ChromeDriver(
        val version: String,
        val items: List<Item>
    )

    data class Item(
        val positioner: Positioner,
        val path: String,
        val url: URL = URI.create("$BASE_URL_DRIVER$path").toURL(),
        val fileName: String = File(path).name
    )
}