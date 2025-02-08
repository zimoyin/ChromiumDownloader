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
 * ChromiumDownloader 是一个用于下载指定版本的 Chrome 浏览器及其驱动程序的工具类。
 * 它根据给定的平台和修订版号自动定位并下载适合的 Chrome 和 ChromeDriver 的二进制文件包，
 * 并将其解压到指定目录下。此工具适用于需要在不同环境中动态部署特定版本的 Chrome 浏览器和驱动程序的场景。
 *
 * @author zimo
 * @date 2025/02/08
 *
 * 使用示例：
 * ```
 * val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 8070))
 * val positioner = Positioner.getLastPosition(proxy = proxy)
 * val downloader = ChromiumDownloader(positioner, proxy)
 * downloader.downloadChrome()
 * downloader.downloadChromeDriver()
 * ```
 */
class ChromiumDownloader(
    val positioner: Positioner,
    val proxy: Proxy? = null,
    val path: String = "./chrome"
) {
    val rootDir = File(path).resolve(positioner.revision)
    val appDir = rootDir.resolve("app")
    val driverDir = rootDir.resolve("driver")

    init {
        rootDir.mkdirs()
        appDir.mkdirs()
        driverDir.mkdirs()
    }

    fun downloadChrome() {
        val regex = """^chrome-[a-z]+\.zip$""".toRegex()
        download(regex, appDir)
    }

    fun downloadChromeDriver() {
        val regex = """^chromedriver_[a-zA-Z0-9]+\.zip$""".toRegex()
        download(regex, driverDir)
    }

    private fun download(regex: Regex, file: File) {
        val item = items.first { it.name.matches(regex) }
        val urlStr = item.mediaLink
        if (urlStr.isEmpty()) throw RuntimeException("Unable to download Chrome for revision because the download link was not obtained.")
        val url = URI.create(urlStr).toURL()
        val zip = File(file, item.name)
        url.connection(proxy).getInputStream().use { input ->
            zip.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Zip.unzip(zip)
        zip.delete()
    }

    val items: ArrayList<Item> by lazy {
        val template =
            "https://www.googleapis.com/storage/v1/b/chromium-browser-snapshots/o?delimiter=/&prefix=${positioner.platform.name}/${positioner.revision}/&fields=items(kind,mediaLink,metadata,name,size,updated),kind,prefixes,nextPageToken"
        val url = URI.create(template).toURL()
        val connection = url.connection(proxy)
        val json = connection.inputStream.use {
            it.readAllBytes().decodeToString()
        }
        parseJson(json)
    }

    private fun parseJson(json: String): ArrayList<Item> {
        val jsonInput: JsonInput = Json().newInput(StringReader(json))
        val from = jsonInput.read<HashMap<String, ArrayList<HashMap<String, String>>>>(HashMap::class.java)
        val list = ArrayList<Item>()
        from["items"]?.forEach {
            val name = it["name"]?.split("/")?.last() ?: ""
            list.add(Item(it["mediaLink"] ?: "", name))
        }
        return list
    }

    data class Item(
        val mediaLink: String,
        val name: String
    )

    companion object {
        const val BASE_URL = "https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o/"
        const val LAST_CHANGE = "LAST_CHANGE"
        const val SPACE = "%2F"
        const val ALT_MEDIA = "?alt=media"

        fun createURL(platform: Platform, revision: String, fileName: String = ""): URL {
            val fileName0 = if (fileName.isEmpty()) "" else "$SPACE$fileName"
            return URI.create("$BASE_URL${platform.name}${SPACE}${revision}${fileName0}${ALT_MEDIA}").toURL()
        }
    }
}