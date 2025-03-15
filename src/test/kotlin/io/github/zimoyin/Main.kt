package io.github.zimoyin

import io.github.headlesschrome.ChromiumLoader
import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
suspend fun main() {
    // ChromiumDownloader
    // HuaweicloudChromiumDownloader
//    val loader = ChromiumLoader(HuaweicloudChromiumDownloader())
    val loader = ChromiumLoader(ChromiumDownloader("127.0.0.1", 8070))
    val options = loader.downloadAndLoad(true)
    println("Chrome 版本: " + loader.chromeVersion)
    println("ChromeDriver 版本: " + loader.chromeDriverVersion)
    println("Chrome 路径: " + loader.chromePath)
    println("ChromeDriver 路径: " + loader.chromeDriverPath)
    println("当前平台： " + loader.platform)
    // 注意 Root 运行需要关闭沙盒
    options.enableNoSandbox()
    options.enableDisableInfobars()
    options.enableDisableCss()
    options.enableIgnoreSslErrors()
    options.enableLoggingPrefs()

    options.addArguments("--autoplay-policy=no-user-gesture-required")
    options.addArguments("--enable-features=EnableOpusPlayback,EnableH264Playback")
    options.addArguments("--use-fake-ui-for-media-stream")
    options.addArguments("--use-fake-device-for-media-stream")
    options.addArguments("--enable-features=Widevine")

//    options.enableHeadless()
    ChromeDriver(options).blockUntilQuitSuspend {
        get("https://bilibili.com")
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
        os.contains("nix") || os.contains("mac") -> listOf(
            "sh",
            "-c",
            "ps aux | grep -Ei 'chrome|chromium' | grep -v grep"
        )

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
