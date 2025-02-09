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
    // 注意 Root 运行需要关闭沙盒
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--ignore-ssl-errors=yes")
    options.addArguments("--ignore-certificate-errors")
//    options.addArguments("--headless")
    ChromeDriver(options).use {
        get("https://www.baidu.com")

    }
}