package io.github.headlesschrome

import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
import org.openqa.selenium.chrome.ChromeOptions
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

/**
 * @author : zimo
 * @date : 2025/02/08
 */
class ChromiumLoader {
    companion object {
        /**
         * 下载并加载Chrome,如果存在则不下载
         */
        @JvmOverloads
        fun downloadAndLoad(
            proxy: Proxy? = null,
            path: String = "./chrome",
            platform: Platform = Platform.currentPlatform()
        ): ChromeOptions {
            val chromePath = kotlin.runCatching {
                findChrome(path)
            }.getOrElse {
                val download = ChromiumDownloader(Positioner.getLastPosition(platform,proxy), proxy, path)
                download.downloadChrome()
                download.downloadChromeDriver()
                findChrome(path)
            }
            val driverPath = findChromeDriver(path)
            System.setProperty("webdriver.chrome.driver", driverPath)
            return ChromeOptions().setBinary(chromePath)
        }

        /**
         * 加载Chrome以及驱动
         */
        fun load(path: String = "./chrome"): ChromeOptions {
            System.setProperty("webdriver.chrome.driver", findChromeDriver(path))
            return ChromeOptions().setBinary(findChrome(path))
        }

        fun findChrome(path: String = "./chrome"): String {
            return when (Platform.currentPlatform()) {
                Platform.Linux -> {
                    val userPath = Paths.get(path)
                    val chromeExecutable = Files.walk(userPath)
                        .filter { Files.isExecutable(it) }
                        .filter { file ->
                            val fileName = file.fileName.name
                            fileName == "google-chrome" || fileName == "chromium" || fileName == "chrome" || fileName.contains(
                                "chrome",
                                ignoreCase = true
                            )
                        }
                        .findFirst()
                        .orElseGet {
                            val defaultPaths = listOf(
                                Paths.get("/usr/bin/google-chrome"),
                                Paths.get("/usr/bin/chromium"),
                                Paths.get("/usr/bin/chromium-browser"),
                                Paths.get("/usr/local/bin/chromium")
                            )
                            defaultPaths.firstOrNull { Files.exists(it) }
                                ?: throw RuntimeException("Chrome executable not found in $path or default locations")
                        }
                    chromeExecutable.toString()
                }

                Platform.Mac -> {
                    val userPath = Paths.get(path)
                    val chromeExecutable = Files.walk(userPath)
                        .filter { Files.isExecutable(it) }
                        .filter { file ->
                            val fileName = file.fileName.name
                            fileName == "Google Chrome" || fileName.contains("chrome", ignoreCase = true)
                        }
                        .findFirst()
                        .orElseGet {
                            val defaultPath = Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
                            if (Files.exists(defaultPath)) {
                                defaultPath
                            } else {
                                throw RuntimeException("Chrome executable not found in $path or default location")
                            }
                        }
                    chromeExecutable.toString()
                }

                Platform.Win -> {
                    Files.walk(Paths.get(path))
                        .filter { it.extension == "exe" }
                        .filter { it.fileName.name.contains("chrome", ignoreCase = true) }
                        .findFirst()
                        .getOrDefault(Paths.get("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"))
                        .toString()
                }

                else -> throw RuntimeException("Unsupported platform")
            }
        }

        fun findChromeDriver(path: String = "./chrome"): String {
            return when (Platform.currentPlatform()) {
                Platform.Linux -> {
                    Files.walk(Paths.get(path))
                        .filter { Files.isExecutable(it) }
                        .filter { it.fileName.name == "chromedriver" }
                        .findFirst()
                        .orElseThrow { RuntimeException("chromedriver not found in $path") }
                        .toString()
                }

                Platform.Mac -> {
                    Files.walk(Paths.get(path))
                        .filter { Files.isExecutable(it) }
                        .filter { it.fileName.name == "chromedriver" }
                        .findFirst()
                        .orElseThrow { RuntimeException("chromedriver not found in $path") }
                        .toString()
                }

                Platform.Win -> {
                    Files.walk(Paths.get(path))
                        .filter { it.extension == "exe" }
                        .filter { it.fileName.name.contains("chromedriver", ignoreCase = true) }
                        .findFirst()
                        .orElseThrow { RuntimeException("chromedriver not found in $path") }
                        .toString()
                }

                else -> throw RuntimeException("Unsupported platform")
            }
        }
    }
}