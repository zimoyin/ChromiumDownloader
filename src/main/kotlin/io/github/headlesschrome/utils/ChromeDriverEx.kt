package io.github.headlesschrome.utils

import io.github.headlesschrome.download.HuaweicloudChromiumDownloader
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Sleeper
import org.openqa.selenium.support.ui.WebDriverWait
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 监听窗口创建
 */
inline fun ChromeDriver.onCreateWindow(crossinline block: suspend CWindow.() -> Unit): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        val list = mutableSetOf<String>()
        while (true) {
            runCatching {
                for (id in windowHandles.iterator()) {
                    if (!list.contains(id)) {
                        list.add(id)
                        runCatching { CWindow(this@onCreateWindow, id).block() }
                    }
                }
                list.clear()
                list.addAll(windowHandles)
            }
            delay(200)
        }
    }
}

/**
 *
 * @author : zimo
 * @date : 2025/02/09
 */
inline fun ChromeDriver.use(block: ChromeDriver.() -> Unit) {
    try {
        block()
    } finally {
        quit()
    }
}

fun ChromeDriver.isQuit(): Boolean {
    return try {
        windowHandles.isEmpty()
    } catch (e: WebDriverException) {
        true
    }
}

/**
 * 浏览器关闭后执行
 */
fun ChromeDriver.onQuit(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        while (!isQuit()) {
            delay(200)
        }
        block()
    }
}

/**
 * 阻塞直到浏览器关闭
 */
fun ChromeDriver.blockUntilQuit(block: (suspend ChromeDriver.() -> Unit) = {}) {
    this.finally()
    runBlocking {
        block()
    }
    while (!isQuit()) {
        Thread.sleep(200)
    }
}

/**
 * 阻塞直到浏览器关闭
 */
suspend fun ChromeDriver.blockUntilQuitSuspend(block: (suspend ChromeDriver.() -> Unit) = {}) {
    this.finally()
    block()
    while (!isQuit()) {
        delay(200)
    }
}

/**
 * 在JVM关闭时退出浏览器
 */
inline fun ChromeDriver.finally(block: (ChromeDriver.() -> Unit) = { }) {
    Runtime.getRuntime().addShutdownHook(Thread {
        runCatching { quit() }
    })
    block()
}

/**
 * 获取日志
 * 注意需要开启 ChromeOptions.enableLoggingPrefs 后才生效
 * @param logType 日志类型
 * @param level 日志等级
 */
fun ChromeDriver.logs(logType: String = LogType.BROWSER, level: Level = Level.ALL): List<LogEntry> {
    return manage().logs().get(logType).filter { it.level.intValue() >= level.intValue() }
}

/**
 * 监听当前窗体的控制台日志。注意需要使用 ChromeOptions.enableLoggingPrefs 后才生效
 * @param windowHandle 窗口句柄
 * @param logType 日志类型
 * @param level 日志等级
 * @param callback 回调
 * @return Job 调用 cancel() 取消监听
 */
inline fun ChromeDriver.logListener(
    windowHandle: String = this.windowHandle,
    logType: String = LogType.BROWSER,
    level: Level = Level.ALL,
    crossinline callback: (LogEntry) -> Unit,
): Job {
    val cw = windows.first { it.windowHandle == windowHandle }
    return CoroutineScope(Dispatchers.IO).launch {
        while (!cw.isClose()) {
            manage().logs().get(logType).map {
                if (it.level.intValue() >= level.intValue()) callback(it)
            }
            delay(20)
        }
    }
}

/**
 * 打开指定连接，如果没有则打开空窗体
 */
fun ChromeDriver.get(url: String? = "about:blank", isNewTab: Boolean = false): CWindow {
    if (isNewTab) this.window.newTab().switchToThis()
    get(url ?: "about:blank")
    return window
}

fun ChromeDriver.get(url: URL, isNewTab: Boolean = false): CWindow {
    get(url.toString(), isNewTab)
    return window
}

fun ChromeDriver.get(file: File, isNewTab: Boolean = false): CWindow {
    get(file.toURI().toURL().toString(), isNewTab)
    return window
}

fun ChromeDriver.get(url: URI, isNewTab: Boolean = false): CWindow {
    get(url.toURL(), isNewTab)
    return window
}

/**
 * 加载 HTML
 */
fun ChromeDriver.load(@Language("html") html: String, isNewTab: Boolean = false): CWindow {
    if (isNewTab) this.window.newTab().switchToThis()
    // 打开一个空白页面
    get("about:blank")
    // 通过 JavaScript 注入 HTML
    executeScript("document.body.innerHTML = arguments[0];", html)
    return window
}

fun ChromeDriver.load(file: File, isNewTab: Boolean = false): CWindow {
    get(file, isNewTab)
    return window
}

fun ChromeDriver.load(url: URL, isNewTab: Boolean = false): CWindow {
    get(url, isNewTab)
    return window
}

fun ChromeDriver.load(url: URI, isNewTab: Boolean = false): CWindow {
    get(url, isNewTab)
    return window
}

/**
 * 可以使用等待来进行页面等待或者JS等待
 * WebDriverWait + executeScript
 * WebDriverWait + ExpectedConditions
 */
fun ChromeDriver.wait(
    timeout: Duration,
    sleep: Duration = Duration.ofMillis(500L),
    clock: Clock = Clock.systemDefaultZone(),
    sleeper: Sleeper = Sleeper.SYSTEM_SLEEPER,
): WebDriverWait {
    return this.WebDriverWait(timeout, sleep, clock, sleeper)
}

/**
 * 可以使用等待来进行页面等待或者JS等待
 * WebDriverWait + executeScript
 * WebDriverWait + ExpectedConditions
 */
fun ChromeDriver.wait(
    timeout: Long,
    sleep: Long = 500L,
    unit: ChronoUnit = ChronoUnit.MILLIS,
    clock: Clock = Clock.systemDefaultZone(),
    sleeper: Sleeper = Sleeper.SYSTEM_SLEEPER,
): WebDriverWait {
    return this.WebDriverWait(Duration.of(timeout, unit), Duration.of(sleep, unit), clock, sleeper)
}

/**
 * 创建一个 Actions 对象, 可以进行鼠标键盘操作
 */
fun ChromeDriver.Actions(): Actions {
    return Actions(this)
}

/**
 * 创建一个 Actions 对象, 可以进行鼠标键盘操作
 */
fun ChromeDriver.actions(): Actions {
    return Actions(this)
}

/**
 * 可以使用等待来进行页面等待或者JS等待
 * WebDriverWait + executeScript
 * WebDriverWait + ExpectedConditions
 */
fun ChromeDriver.WebDriverWait(
    timeout: Duration,
    sleep: Duration = Duration.ofMillis(500L),
    clock: Clock = Clock.systemDefaultZone(),
    sleeper: Sleeper = Sleeper.SYSTEM_SLEEPER,
): WebDriverWait {
    return WebDriverWait(this, timeout, sleep, clock, sleeper)
}

/**
 * 可以使用等待来进行页面等待或者JS等待
 * WebDriverWait + executeScript
 * WebDriverWait + ExpectedConditions
 */
fun ChromeDriver.WebDriverWait(
    timeout: Long,
    sleep: Long = 500L,
    unit: ChronoUnit = ChronoUnit.MILLIS,
    clock: Clock = Clock.systemDefaultZone(),
    sleeper: Sleeper = Sleeper.SYSTEM_SLEEPER,
): WebDriverWait {
    return WebDriverWait(this, Duration.of(timeout, unit), Duration.of(sleep, unit), clock, sleeper)
}

fun ChromeDriver.FluentWait(
    timeout: Duration,
    pollingEvery: Duration = Duration.ofMillis(500L),
    message: String = "timeout",
    ignoringExceptions: List<Class<out Throwable>> = emptyList(),
): FluentWait<ChromeDriver> {
    return FluentWait(this)
        .withTimeout(timeout)
        .pollingEvery(pollingEvery)
        .ignoreAll(ignoringExceptions)
        .withMessage(message)
}

fun ChromeDriver.loadCss(@Language("css") css: String): CWindow {
    executeScript(
        """
        const style = document.createElement('style');
        style.textContent = arguments[0];
        document.head.appendChild(style);
    """, css
    )
    return window
}

/**
 * 删除当前页面 WebDriver 标识
 */
fun WebDriver.deleteWebDriverSign() {
    runCatching {
        val js = this as JavascriptExecutor
        // 方法 2: 覆盖 getter（推荐）
        js.executeScript(
            """
        Object.defineProperty(navigator, 'webdriver', {
            get: () => undefined
        });
        """.trimIndent()
        )
    }
}


fun WebDriver.htmlAsWebElement(): WebElement {
    return findElement(By.tagName("html"))
}

fun WebDriver.bodyAsWebElement(): WebElement {
    return findElement(By.tagName("body"))
}

fun WebDriver.headerAsWebElement(): WebElement {
    return findElement(By.tagName("head"))
}

fun WebDriver.findElementById(id: String): WebElement {
    return findElement(By.id(id))
}

fun WebDriver.findElementByXpath(xpath: String): WebElement {
    return findElement(By.xpath(xpath))
}

fun WebDriver.findElementsByXpath(xpath: String): List<WebElement> {
    return findElements(By.xpath(xpath))
}

fun WebDriver.findElementsByClassName(className: String): List<WebElement> {
    return findElements(By.className(className))
}

fun WebDriver.findElementsByTagName(tagName: String): List<WebElement> {
    return findElements(By.tagName(tagName))
}

fun WebDriver.findElementsByCssSelector(cssSelector: String): List<WebElement> {
    return findElements(By.cssSelector(cssSelector))
}


/**
 * 等待元素可见的时候获取元素
 * @param timeout 超时时间
 * @param by By
 * @return WebElement
 */
fun ChromeDriver.findElementWithWait(by: By, timeout: Long = 5 * 1000): WebElement {
    return runCatching {
        wait(timeout).until(
            ExpectedConditions.visibilityOfElementLocated(by)
        )
    }.getOrElse { throw NoSuchElementException("Not  found element: $by") }
}

/**
 * 等待元素可见的时候获取元素
 * @param timeout 超时时间
 * @param by By
 */
fun ChromeDriver.findElementsWithWait(by: By, timeout: Long = 5 * 1000): List<WebElement> {
    return runCatching {
        wait(timeout).until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(by)
        )
    }.getOrElse { throw NoSuchElementException("Not  found element: $by") }
}


/**
 * 等待元素可见的时候获取元素
 * @param timeout 超时时间
 * @param by By
 * @return WebElement
 */
fun ChromeDriver.findElementWithWait(by: By, timeout: Duration = Duration.ofSeconds(5)): WebElement {
    return runCatching {
        wait(timeout).until(
            ExpectedConditions.visibilityOfElementLocated(by)
        )
    }.getOrElse { throw NoSuchElementException("Not  found element: $by") }
}

/**
 * 等待元素可见的时候获取元素
 * @param timeout 超时时间
 * @param by By
 */
fun ChromeDriver.findElementsWithWait(by: By, timeout: Duration = Duration.ofSeconds(5)): List<WebElement> {
    return runCatching {
        wait(timeout).until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(by)
        )
    }.getOrElse { throw NoSuchElementException("Not  found element: $by") }
}

/**
 * 获取指定位置的元素
 */
fun WebDriver.getElementAtPosition(x: Int, y: Int): WebElement {
    return (this as JavascriptExecutor).executeScript(
        "return document.elementFromPoint(arguments[0], arguments[1]);",
        x,
        y
    ) as WebElement
}

/**
 * 注入脚本链接
 */
fun ChromeDriver.injectionScriptTag(url: String) {
    executeScript(
        """
                    var script = document.createElement('script');
                    script.src = '$url'; 
                    script.type = 'text/javascript';
                    document.head.appendChild(script);
                """.trimIndent()
    )
}


val WebDriver.scrollSize: Dimension
    get() = Dimension(
        ((this as JavascriptExecutor).executeScript("return document.body.scrollWidth;") as Long).toInt(),
        ((this as JavascriptExecutor).executeScript("return document.body.scrollHeight;") as Long).toInt()
    )

val WebDriver.userAgent: String
    get() = (this as JavascriptExecutor).executeScript("return navigator.userAgent;") as String

val WebDriver.html: String
    get() = pageSource as String