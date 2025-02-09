package io.github.headlesschrome

import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.location.Positioner
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
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrElse
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
            val download by lazy { ChromiumDownloader(Positioner.getLastPosition(platform, proxy), proxy, path) }
            val chromePath = kotlin.runCatching {
                findChrome(path)
            }.getOrElse {
                download.downloadChrome()
                findChrome(path)
            }

            val driverPath = kotlin.runCatching {
                findChromeDriver(path)
            }.getOrElse {
                download.downloadChromeDriver()
                findChromeDriver(path)
            }

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
                Platform.Linux, Platform.Linux_x64 -> {
                    val userPath = Paths.get(path)
                    val chromeExecutable = Files.walk(userPath)
                        .filter { file ->
                            val fileName = file.fileName.name
                            fileName == "google-chrome" || fileName == "chromium" || fileName == "chrome"
                        }.filter {
                            !Files.isDirectory(it)
                        }
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


        fun findChromeDriver(path: String = "./chrome"): String {
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

        fun getChromeVersion(chromePath: String = "./chrome"): String {
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

