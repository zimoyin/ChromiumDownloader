package io.github.headlesschrome

import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.download.HuaweicloudChromiumDownloader
import io.github.headlesschrome.location.Platform
import io.github.headlesschrome.utils.*
import org.openqa.selenium.chrome.ChromeDriver
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Proxy

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
suspend fun main() {
    // ChromiumDownloader
    // HuaweicloudChromiumDownloader
    val loader = ChromiumLoader(HuaweicloudChromiumDownloader())
    val options = loader.downloadAndLoad()
    println("Chrome 版本: " + loader.chromeVersion)
    println("ChromeDriver 版本: " + loader.chromeDriverVersion)
    println("Chrome 路径: " + loader.chromePath)
    println("ChromeDriver 路径: " + loader.chromeDriverPath)
    println("当前平台： "+loader.platform)
    // 注意 Root 运行需要关闭沙盒
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--ignore-ssl-errors=yes")
    options.addArguments("--ignore-certificate-errors")
//    options.addArguments("--headless")
    ChromeDriver(options).blockUntilQuitSuspend {
        get("https://www.baidu.com")
    }
}

/**
 * 检测系统中是否存在 Chrome 或 Chromium 进程
 * @return 存在返回 true，否则返回 false
 */
fun _isChromeOrChromiumProcessExists(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    val command = when {
        os.contains("win") -> listOf("cmd.exe", "/c", "tasklist")
        os.contains("nix") || os.contains("mac") -> listOf("sh", "-c", "ps aux | grep -Ei 'chrome|chromium' | grep -v grep")
        else -> throw UnsupportedOperationException("Unsupported OS: $os")
    }

    return try {
        val process = ProcessBuilder(command).start()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            // Windows 需要检查进程名，Unix 系统只要有输出即为存在
            if (os.contains("win")) {
                reader.lineSequence().any { line ->
                    line.contains("chrome.exe", ignoreCase = true) ||
                            line.contains("chromium.exe", ignoreCase = true)
                }
            } else {
                reader.readLine() != null // 只要有一行输出即认为存在
            }
        }.also {
            process.destroy() // 确保进程资源释放
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
