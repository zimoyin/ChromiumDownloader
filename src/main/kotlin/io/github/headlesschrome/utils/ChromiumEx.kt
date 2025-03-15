package io.github.headlesschrome.utils

import kotlinx.coroutines.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.util.function.Consumer
import java.util.logging.Level

/**
 * ChromiumEx 扩展类，用于增强 ChromeDriver 的功能。适用于 Java
 * 使用方式：
 * ```java
 * ChromiumEx chromiumEx = new ChromiumEx(driver);
 * ```
 * kotlin 推荐以下方法
 * ```kotlin
 * driver.currentWindow
 * driver.windows
 * ```
 * @author : zimo
 * @date : 2025/03/14
 */
class ChromiumEx(
    override val driver: ChromeDriver,
) : CWindow(driver, driver.windowHandle) {
    init {
        deleteWebDriverSign()
    }

    fun onQuit(c: Consumer<Unit>) {
        onQuit {
            c.accept(Unit)
        }
    }

    fun onClose(c: Consumer<Unit>) {
        onClose {
            c.accept(Unit)
        }
    }

    /**
     * 监听当前窗体的控制台日志。注意需要使用 ChromiumEx.enableLoggingPrefs 后才生效
     * @param logType 日志类型
     * @param level 日志等级
     * @param callback 回调
     * @return Job 调用 cancel() 取消监听
     */
    @JvmOverloads
    fun logListener(
        logType: String = LogType.BROWSER,
        level: Level = Level.ALL,
        callback: Consumer<LogEntry>,
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (!this@ChromiumEx.isClose()) {
                manage().logs().get(logType).map {
                    if (it.level.intValue() >= level.intValue()) callback.accept(it)
                }
                delay(20)
            }
        }
    }

    /**
     * 监听窗口创建
     */
    fun onCreateWindow(c: Consumer<CWindow>): Job = driver.onCreateWindow {
        c.accept(this)
    }

    /**
     * 创建 Actions 用于模拟鼠标键盘操作。只针对当前窗口，如果在操作过程中出现窗口切换会导致操作失误
     */
    fun actions(c: Consumer<Actions>): Unit = actions {
        c.accept(Actions(it, this))
    }

    class Actions(
        val actions: org.openqa.selenium.interactions.Actions,
        val window: CWindow,
    )

    companion object {
        /**
         * 启用日志监听
         */
        @JvmOverloads
        fun enableLoggingPrefs(
            chromeOptions: ChromeOptions,
            loggingPrefs: LoggingPreferences = LoggingPreferences().apply {
                enable(LogType.BROWSER, Level.ALL)
                enable(LogType.DRIVER, Level.WARNING)
                enable(LogType.PERFORMANCE, Level.INFO)
                enable(LogType.PROFILER, Level.INFO)
                enable(LogType.SERVER, Level.INFO)
                enable(LogType.CLIENT, Level.INFO)
            },
        ) {
            chromeOptions.setCapability("goog:loggingPrefs", loggingPrefs)
        }

        /**
         * 监听窗口创建
         */
        fun onCreateWindow(driver: ChromeDriver, c: Consumer<CWindow>): Job = driver.onCreateWindow {
            c.accept(this)
        }
    }
}