package io.github.headlesschrome.download

import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
import io.github.headlesschrome.utils.Zip
import io.github.headlesschrome.utils.connection
import org.openqa.selenium.json.Json
import org.openqa.selenium.json.JsonInput
import java.io.File
import java.io.StringReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL

const val CHROME_DOWNLOAD_PATH = "./chrome"

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
    proxy: Proxy? = null,
    positioner: Positioner = getLastPosition(proxy),
    path: String = CHROME_DOWNLOAD_PATH,
    rootDir: File = File(path).resolve(positioner.revision),
    appDir: File = rootDir.resolve("app"),
    driverDir: File = rootDir.resolve("driver"),
) : AbsChromiumDownloader(positioner, proxy, path, rootDir, appDir, driverDir) {

    constructor(host: String, port: Int) : this(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))

    constructor(
        host: String,
        port: Int,
        positioner: Positioner = getLastPosition(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))),
        path: String = CHROME_DOWNLOAD_PATH,
        rootDir: File = File(path).resolve(positioner.revision),
        appDir: File = rootDir.resolve("app"),
        driverDir: File = rootDir.resolve("driver"),
    ) : this(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)), positioner, path, rootDir, appDir, driverDir)


    /**
     * 获取当前平台和指定修订号的下载项列表。
     */
    val items: ArrayList<Item> by lazy {
        val template =
            "https://www.googleapis.com/storage/v1/b/chromium-browser-snapshots/o?delimiter=/&prefix=${positioner.platform.name}/${positioner.revision}/&fields=items(kind,mediaLink,metadata,name,size,updated),kind,prefixes,nextPageToken"
        val url = URI.create(template).toURL()
        val connection = url.connection(proxy)
        val json = connection.inputStream.use {
            it.readBytes().decodeToString()
        }
        parseJson(json)
    }

    override fun downloadChrome() {
        val regex = """^chrome-[a-z]+\.zip$""".toRegex()
        try {
            initAppDir()
            download(regex, appDir)
        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to download chrome revision: ${positioner.revision} in platform: ${positioner.platform}",
                e
            )
        }
    }

    override fun downloadChromeDriver() {
        val regex = """^chromedriver_[a-zA-Z0-9]+\.zip$""".toRegex()
        try {
            initDriverDir()
            download(regex, driverDir)
        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to download chrome driver revision: ${positioner.revision} in platform: ${positioner.platform}",
                e
            )
        }
    }

    private fun download(regex: Regex, file: File) {
        val item = items.firstOrNull() { it.name.matches(regex) }
            ?: throw NullPointerException("Unable to download File for revision because no matching files found")
        val urlStr = item.mediaLink
        if (urlStr.isEmpty()) throw RuntimeException("Unable to download File for revision because the download link was not obtained.")
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

    /**
     * Item 是一个数据类，用于表示一个下载项，包括媒体链接和名称。
     */
    data class Item(
        val mediaLink: String,
        val name: String,
    )

    companion object {
        const val BASE_URL = "https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o/"
        const val LAST_CHANGE = "LAST_CHANGE"
        const val SPACE = "%2F"
        const val ALT_MEDIA = "?alt=media"


        /**
         * 创建一个URL，用于下载指定平台的指定修订号的文件。
         */
        fun createURL(platform: Platform, revision: String, fileName: String = ""): URL {
            val fileName0 = if (fileName.isEmpty()) "" else "$SPACE$fileName"
            return URI.create("$BASE_URL${platform.name}$SPACE${revision}${fileName0}$ALT_MEDIA")
                .toURL()
        }


        fun getLastPosition(proxy: Proxy? = null): Positioner {
            return getLastPosition(Platform.currentPlatform(), proxy)
        }

        @JvmOverloads
        fun getLastPosition(platform: Platform = Platform.currentPlatform(), proxy: Proxy? = null): Positioner {
            val url = ChromiumDownloader.createURL(platform, LAST_CHANGE)

            // 如果提供了代理，则使用代理打开连接；否则直接打开连接
            val connection = url.connection(proxy)
            val revision = connection.inputStream.use {
                it.readBytes().decodeToString()
            }
            return Positioner(platform, revision)
        }
    }
}