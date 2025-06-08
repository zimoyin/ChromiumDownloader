package io.github.headlesschrome

import io.github.headlesschrome.download.AbsChromiumDownloader
import io.github.headlesschrome.download.CHROME_DOWNLOAD_PATH
import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.utils.setUserProfileDir
import kotlinx.coroutines.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrDefault

/**
 * 注意：如果设置了 downloader0 则会覆盖 scanPath
 * @author : zimo
 * @date : 2025/02/08
 */
class ChromiumLoader(
    var scanPath: String = CHROME_DOWNLOAD_PATH,
    private var downloader0: AbsChromiumDownloader? = null,
    private val proxy: Proxy? = downloader0?.proxy,
) {
    var platform: Platform = Platform.currentPlatform()
        private set

    init {
        downloader0?.apply {
            scanPath = path
            platform = positioner.platform
        }
    }

    constructor(
        scanPath: String = CHROME_DOWNLOAD_PATH,
        downloader0: AbsChromiumDownloader? = null,
    ) : this(scanPath, downloader0, downloader0?.proxy)

    constructor(
        downloader0: AbsChromiumDownloader? = null,
        proxy: Proxy? = downloader0?.proxy,
    ) : this(CHROME_DOWNLOAD_PATH, downloader0, proxy)

    constructor(
        proxy: Proxy? = null,
    ) : this(CHROME_DOWNLOAD_PATH, null, proxy)

    constructor(
        downloader0: AbsChromiumDownloader? = null,
    ) : this(CHROME_DOWNLOAD_PATH, downloader0)


    val downloader: AbsChromiumDownloader by lazy {
        downloader0 ?: ChromiumDownloader(proxy, ChromiumDownloader.getLastPosition(platform, proxy), scanPath)
    }

    val chromePath: String by lazy {
        findChrome(scanPath)
    }

    val chromeDriverPath: String by lazy {
        findChromeDriver(scanPath)
    }

    val chromeVersion: String by lazy {
        getChromeVersion(chromePath)
    }

    val chromeDriverVersion: String by lazy {
        getChromeDriverVersion(chromeDriverPath)
    }

    /**
     * 建议用户数据存储位置
     */
    val defaultChromeUserProfileDir: String by lazy {
        File(chromePath).parentFile.resolve("chrome-user-data").canonicalPath
    }

    fun load(): ChromeOptions = load(scanPath).apply {
        kotlin.runCatching { setUserProfileDir(defaultChromeUserProfileDir) }
    }

    @JvmOverloads
    fun downloadAndLoad(isPathMatchingEnabled: Boolean = true): ChromeOptions =
        downloadAndLoad(proxy, scanPath, platform, downloader, isPathMatchingEnabled).apply {
            kotlin.runCatching { setUserProfileDir(defaultChromeUserProfileDir) }
        }


    companion object {
        /**
         * 下载并加载Chrome,如果存在则不下载
         */
        @JvmOverloads
        fun downloadAndLoad(
            proxy: Proxy? = null,
            path: String = CHROME_DOWNLOAD_PATH,
            platform: Platform = Platform.currentPlatform(),
            downloader: AbsChromiumDownloader? = null,
            isPathMatchingEnabled: Boolean = false,
        ): ChromeOptions = runBlocking(Dispatchers.IO) {
            val download = async {
                downloader ?: ChromiumDownloader(proxy, ChromiumDownloader.getLastPosition(platform, proxy), path)
            }
            val chromePath = async {
                kotlin.runCatching {
                    findChrome(path).apply {
                        val chromeFile = File(this).canonicalFile.path
                        val targetFile = File(path).canonicalFile.path
                        if (isPathMatchingEnabled && !chromeFile.startsWith(targetFile)) throw RuntimeException("Path $path is not in the Chrome executable path")
                    }
                }.getOrElse {
                    download.await().downloadChrome()
                    findChrome(path)
                }
            }

            val driverPath = async {
                kotlin.runCatching {
                    findChromeDriver(path).apply {
                        val chromeFile = File(this).canonicalFile.path
                        val targetFile = File(path).canonicalFile.path
                        if (isPathMatchingEnabled && !chromeFile.startsWith(targetFile)) throw RuntimeException("Path $path is not in the Chrome Driver executable path")
                    }
                }.getOrElse {
                    download.await().downloadChromeDriver()
                    findChromeDriver(path)
                }
            }

            System.setProperty("webdriver.chrome.driver", driverPath.await())
            return@runBlocking ChromeOptions().setBinary(chromePath.await())
        }

        /**
         * 加载Chrome以及驱动
         */
        fun load(path: String = CHROME_DOWNLOAD_PATH): ChromeOptions {
            System.setProperty("webdriver.chrome.driver", findChromeDriver(path))
            return ChromeOptions().setBinary(findChrome(path))
        }

        fun findChrome(path: String = CHROME_DOWNLOAD_PATH): String {
            return when (Platform.currentPlatform()) {
                Platform.Linux, Platform.Linux_x64 -> {
                    val userPath = Paths.get(path)
                    val chromeExecutable = Files.walk(userPath)
                        .filter { file ->
                            val fileName = file.fileName.name
                            fileName == "google-chrome" || fileName == "chromium" || fileName == "chrome"
                        }.filter {
                            !Files.isDirectory(it)
                        }
                        .filter { !it.fileName.name.contains("chromedriver", true) }
                        .setPermission()
                        .findFirst()
                        .orElseGet {
                            listOf(
                                Paths.get("/usr/bin/google-chrome"),
                                Paths.get("/usr/bin/chromium"),
                                Paths.get("/usr/bin/chromium-browser"),
                                Paths.get("/usr/local/bin/chromium")
                            ).firstOrNull { Files.exists(it) }
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
                        .filter { !it.fileName.name.contains("chromedriver", true) }
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
                        .filter { !it.fileName.name.contains("chromedriver", true) }
                        .findFirst()
                        .getOrDefault(Paths.get("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"))
                        .apply { if (!this.exists()) throw RuntimeException("Chrome executable not found in $path") }
                        .toString()
                }

                else -> {
                    Files.walk(Paths.get(path))
                        .filter { Files.isExecutable(it) }
                        .filter { file ->
                            val fileName = file.fileName.name
                            fileName == "google-chrome" || fileName == "chromium" || fileName == "chrome" || fileName.contains(
                                "chrome",
                                ignoreCase = true
                            )
                        }
                        .findFirst()
                        .orElseThrow { RuntimeException("Chrome executable not found in $path") }
                        .toString()

                }
            }
        }

        private fun Stream<Path>.setPermission() = this.map {
            if (!Files.isExecutable(it)) {
                kotlin.runCatching {
                    Files.setPosixFilePermissions(
                        it,
                        EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE
                        )
                    )
                }
            }
            it
        }


        fun findChromeDriver(path: String = CHROME_DOWNLOAD_PATH): String {
            return when (Platform.currentPlatform()) {
                Platform.Linux, Platform.Linux_x64 -> {
                    Files.walk(Paths.get(path))
                        .filter { it.fileName.name == "chromedriver" }
                        .filter {
                            !Files.isDirectory(it)
                        }
                        .setPermission()
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

                else -> {
                    Files.walk(Paths.get(path))
                        .filter { Files.isExecutable(it) }
                        .filter { it.fileName.name == "chromedriver" }
                        .findFirst()
                        .orElseThrow { RuntimeException("chromedriver not found in $path") }
                        .toString()
                }
            }
        }

        fun getChromeVersion(driver: ChromeDriver): String {
            return driver.capabilities.browserVersion
        }

        fun getChromeVersion(chromePath: String = CHROME_DOWNLOAD_PATH): String {
            val executable = if (File(chromePath).isFile) chromePath else findChrome()
            val command = if (Platform.currentPlatform() == Platform.Win) {
                listOf("powershell", "-command", "&{(Get-Item '$executable').VersionInfo.ProductVersion}")
            } else {
                listOf(executable, "--version")
            }
            val output = executeCommand(command)
            return parseChromeVersion(output)
        }

        fun getChromeDriverVersion(chromeDriverPath: String = "./chromedriver"): String {
            val executable = if (File(chromeDriverPath).isFile) chromeDriverPath else findChromeDriver()
            val command = listOf(executable, "--version")
            val output = executeCommand(command)
            return parseChromeDriverVersion(output)
        }

        private fun executeCommand(command: List<String>): String {
            return try {
                val process = ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroy()
                    throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
                }

                val output = process.inputStream.bufferedReader().readText().trim()
                val error = process.errorStream.bufferedReader().readText().trim()

                if (process.exitValue() != 0) {
                    throw RuntimeException("Command failed (${process.exitValue()}): $error")
                }

                output
            } catch (e: Exception) {
                throw RuntimeException("Error executing command: ${command.joinToString(" ")}", e)
            }
        }

        private fun parseChromeVersion(output: String): String {
            return output.split(" ")
                .firstOrNull { it.matches("\\d+(\\.\\d+){3}".toRegex()) }
                ?: throw RuntimeException("Failed to parse Chrome version from: $output")
        }

        private fun parseChromeDriverVersion(output: String): String {
            return output.split(" ")
                .firstOrNull { it.matches("\\d+(\\.\\d+){3}".toRegex()) }
                ?: throw RuntimeException("Failed to parse ChromeDriver version from: $output")
        }
    }
}

