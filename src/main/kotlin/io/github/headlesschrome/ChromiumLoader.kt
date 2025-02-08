package io.github.headlesschrome

import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
import org.openqa.selenium.chrome.ChromeOptions
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
class ChromiumLoader {
    companion object {
        fun downloadAndLoad(proxy: Proxy? = null,path: String = "./chrome"): ChromeOptions {
            val download = ChromiumDownloader(Positioner.getLastPosition(), proxy, path)
            val chromePath = kotlin.runCatching {
                findChrome(path)
            }.getOrElse {
                download.downloadChrome()
                download.downloadChromeDriver()
                findChrome(path)
            }
            val driverPath = findChromeDriver(path)
            System.setProperty("webdriver.chrome.driver", driverPath)
            return ChromeOptions().setBinary(chromePath)
        }

        fun load(path: String = "./chrome"): ChromeOptions {
            System.setProperty("webdriver.chrome.driver", findChromeDriver(path))
            return ChromeOptions().setBinary(findChrome(path))
        }

        fun findChrome(path: String = "./chrome"): String {
            return when (Platform.currentPlatform()) {
                Platform.Linux -> {
                    TODO()
                }

                Platform.Mac -> {
                    TODO()
                }

                Platform.Win -> {
                    // 查询指定位置是否存在 chrome
                    Files.walk(Paths.get(path))
                        .filter { it.extension == "exe" }
                        .filter { it.fileName.toString().contains("chrome") }
                        .findFirst()
                        .getOrDefault(Paths.get("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"))
                        .toString()
                }

                else -> throw RuntimeException("not support platform")
            }
        }

        fun findChromeDriver(path: String = "./chrome"): String {
            // 深度优先
            return when (Platform.currentPlatform()) {
                Platform.Linux -> {
                    TODO()
                }

                Platform.Mac -> {
                    TODO()
                }

                Platform.Win -> {
                    Files.walk(Paths.get(path))
                        .filter { it.extension == "exe" }
                        .filter { it.fileName.toString().contains("chromedriver") }
                        .findFirst()
                        .get()
                        .toString()
                }

                else -> throw RuntimeException("not support platform")
            }
        }
    }
}