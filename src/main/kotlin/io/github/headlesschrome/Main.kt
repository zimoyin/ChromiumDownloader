package io.github.headlesschrome

import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
import io.github.headlesschrome.utils.use
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.logs.LoggerBuilder
import io.opentelemetry.sdk.logs.internal.LoggerConfig
import org.openqa.selenium.chrome.ChromeDriver
import java.net.InetSocketAddress
import java.net.Proxy

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */


fun main() {
    println("平台: " + Platform.currentPlatform())
    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 8070))
    runCatching {
        ChromiumLoader.findChromeDriver().let {
            println(it)
            println(ChromiumLoader.getChromeDriverVersion(it))
        }
    }

    runCatching {
        ChromiumLoader.findChrome().let {
            println(it)
            println(ChromiumLoader.getChromeVersion(it))
        }
    }
    val options = ChromiumLoader.downloadAndLoad(proxy)
    // 启用无头模式
    options.addArguments("--headless")
    options.addArguments("--user-data-dir=./tmp/chrome-profile-${System.currentTimeMillis()}") // 使用一个新的临时目录
    ChromeDriver(options).use {
        get("https://www.baidu.com")
    }
}