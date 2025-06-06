package io.github.headlesschrome.utils

import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.LogEntries
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Sleeper
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.logging.Level

/**
 * 获取当前窗口
 */
val ChromeDriver.window: CWindow
    get() = windows.firstOrNull { it.windowHandle == windowHandle } ?: CWindow(this, windowHandle, isWindowSynchronized)

/**
 * 获取所有窗口
 */
val ChromeDriver.windows: List<CWindow>
    get() = cwz.getOrPut(this) { mutableListOf() }.apply {
        removeIf { it.windowHandle !in windowHandles.toSet() }
        windowHandles.forEach { handle ->
            if (none { it.windowHandle == handle }) add(CWindow(this@windows, handle, isWindowSynchronized))
        }
    }

var ChromeDriver.isWindowSynchronized: Boolean
    get() = cws.getOrPut(this) { true }
    set(value) {
        cws[this] = value
    }


private val cws = HashMap<ChromeDriver, Boolean>()
private val cwz = HashMap<ChromeDriver, MutableList<CWindow>>()

fun List<CWindow>.newTab(url: String = "about:blank", switchTo: Boolean = true): CWindow {
    return first().newTab(url, switchTo)
}

fun List<CWindow>.newWindow(url: String = "about:blank", switchTo: Boolean = true): CWindow {
    return first().newWindow(url, switchTo)
}

fun List<CWindow>.closeWindow(windowHandleID: String) {
    for (cw in this) {
        if (windowHandleID == cw.windowHandleID) cw.close()
    }
}

/**
 * 聚合窗口对象，将所有类的API基本聚合在了一起
 * 该类对象，可以在其他当前窗体下操作另外的窗体，他将临时的切换到其他窗体中进行操作
 * @param driver 浏览器对象
 * @param windowHandleID 窗口ID
 * @param isSynchronized 是否同步，默认为 true，即同步。针对非当前窗体时，如果为 false，则不会对切换窗体时进行锁操作
 */
open class CWindow(
    open val driver: ChromeDriver,
    val windowHandleID: String,
    var isSynchronized: Boolean = true,
) : WebDriver {

    init {
        deleteWebDriverSign()
    }

    fun deleteWebDriverSign() {
        driver.deleteWebDriverSign()
    }

    val currentWindowHandle: String
        get() = driver.windowHandle

    val windows: List<CWindow>
        get() = driver.windows

    val window: CWindow
        get() = driver.window

    val targetLocator: WebDriver.TargetLocator
        get() = driver.switchTo()

    var size: Dimension
        get() = aroundWindow { driver.manage().window().size }
        set(value) = aroundWindow {
            driver.manage().window().size = value
        }


    var position: Point
        get() = aroundWindow { driver.manage().window().position }
        set(value) = aroundWindow {
            driver.manage().window().position = value
        }

    var url: String?
        get() = aroundWindow { driver.currentUrl }
        set(value) {
            value?.let { get(it) }
        }

    val cookieManager: CookieManager = CookieManager(this)

    /**
     * 获取当前窗口的日志
     * 注意需要使用 ChromeOptions.enableLoggingPrefs 后才生效
     * @return LogEntries 浏览器控制台日志
     */
    fun logs(): LogEntries = aroundWindow {
        val logEntries = driver.manage().logs().get(LogType.BROWSER)
        return@aroundWindow logEntries
    }

    /**
     * 监听当前窗体的控制台日志。注意需要使用 ChromeOptions.enableLoggingPrefs 后才生效
     * @param logType 日志类型
     * @param level 日志等级
     * @param callback 回调
     * @return Job 调用 cancel() 取消监听
     */
    @JvmSynthetic
    inline fun logListener(
        logType: String = LogType.BROWSER,
        level: Level = Level.ALL,
        crossinline callback: (LogEntry) -> Unit,
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (!this@CWindow.isClose()) {
                manage().logs().get(logType).map {
                    if (it.level.intValue() >= level.intValue()) callback(it)
                }
                delay(20)
            }
        }
    }

    fun setTitle(title: String?) {
        driver.executeScript("document.title = '$title';")
    }

    /**
     * 在当前所选框架或窗口的上下文中执行 JavaScript。提供的脚本片段将作为匿名函数的主体执行。
     * 在脚本中，用于 document 引用当前文档。请注意，脚本执行完毕后，局部变量将不可用，但全局变量将保留。
     * 如果脚本具有返回值（即，如果脚本包含语句 return ），则将执行以下步骤：
     * 对于 HTML 元素，此方法返回 WebElement
     * 对于十进制，则返回 Double
     * 对于非十进制数，将返回 Long
     * 对于布尔值，将返回布尔值
     * 对于所有其他情况，将返回 String。
     * 对于数组，返回一个 List<Object>每个对象都遵循上述规则。我们支持嵌套列表。
     * 对于映射，返回 Map<String、Object>其值遵循上述规则。
     * 除非值为 null 或没有返回值，其中返回 null
     * 参数必须是数字、布尔值、String、WebElement 或上述任意组合的 List。如果参数不满足这些条件，将引发异常。参数将通过 “arguments” 魔术变量提供给 JavaScript，就像通过 “Function.apply” 调用函数一样
     * @param script – 要执行的 JavaScript
     * @param args – 脚本的参数。可能为空
     * @return Boolean、Long、Double、String、List、Map 或 WebElement 之一。或 null。
     */
    fun <T : Any> executeScriptAsT(@Language("javascript") script: String, vararg args: Any?): T? = aroundWindow<T?> {
        return@aroundWindow driver.executeScript(script, *args) as? T
    }

    /**
     * 在当前所选框架或窗口的上下文中执行 JavaScript。提供的脚本片段将作为匿名函数的主体执行。
     * 在脚本中，用于 document 引用当前文档。请注意，脚本执行完毕后，局部变量将不可用，但全局变量将保留。
     * 如果脚本具有返回值（即，如果脚本包含语句 return ），则将执行以下步骤：
     * 对于 HTML 元素，此方法返回 WebElement
     * 对于十进制，则返回 Double
     * 对于非十进制数，将返回 Long
     * 对于布尔值，将返回布尔值
     * 对于所有其他情况，将返回 String。
     * 对于数组，返回一个 List<Object>每个对象都遵循上述规则。我们支持嵌套列表。
     * 对于映射，返回 Map<String、Object>其值遵循上述规则。
     * 除非值为 null 或没有返回值，其中返回 null
     * 参数必须是数字、布尔值、String、WebElement 或上述任意组合的 List。如果参数不满足这些条件，将引发异常。参数将通过 “arguments” 魔术变量提供给 JavaScript，就像通过 “Function.apply” 调用函数一样
     * @param script – 要执行的 JavaScript
     * @param args – 脚本的参数。可能为空
     * @return Boolean、Long、Double、String、List、Map 或 WebElement 之一。或 null。
     */
    fun executeScript(@Language("javascript") script: String, vararg args: Any?): Any? = aroundWindow {
        return@aroundWindow driver.executeScript(script, *args)
    }

    /**
     * 在当前所选框架或窗口的上下文中执行异步 JavaScript 片段。与 executing synchronous JavaScript不同，使用此方法执行的脚本必须通过调用提供的回调来显式表示它们已完成。此回调始终作为最后一个参数注入到执行的函数中。
     * 传递给回调函数的第一个参数将用作脚本的结果。此值将按如下方式处理：
     * 对于 HTML 元素，此方法返回 WebElement
     * 对于数字，将返回 Long
     * 对于布尔值，将返回布尔值
     * 对于所有其他情况，将返回 String。
     * 对于数组，返回一个 List<Object>每个对象都遵循上述规则。我们支持嵌套列表。
     * 对于映射，返回 Map<String、Object>其值遵循上述规则。
     * 除非值为 null 或没有返回值，其中返回 null
     * 要执行的脚本的默认超时时间为 0ms。在大多数情况下，包括以下示例，必须事先将脚本超时 WebDriver.Timeouts.scriptTimeout(java.time.Duration) 设置为足够大的值。
     * 示例 #1：在被测浏览器中执行休眠。
     * long start = System.currentTimeMillis();
     * ((JavascriptExecutor) driver).executeAsyncScript(
     *     "window.setTimeout(arguments[arguments.length - 1], 500);");
     * System.out.println(
     *     "Elapsed time: " + (System.currentTimeMillis() - start));
     * 示例 #2：将测试与 AJAX 应用程序同步：
     * WebElement composeButton = driver.findElement(By.id("compose-button"));
     * composeButton.click();
     * ((JavascriptExecutor) driver).executeAsyncScript(
     *     "var callback = arguments[arguments.length - 1];" +
     *     "mailClient.getComposeWindowWidget().onload(callback);");
     * driver.switchTo().frame("composeWidget");
     * driver.findElement(By.id("to")).sendKeys("bog@example.com");
     * 示例 #3： 注入 XMLHttpRequest 并等待结果：
     * Object response = ((JavascriptExecutor) driver).executeAsyncScript(
     *     "var callback = arguments[arguments.length - 1];" +
     *     "var xhr = new XMLHttpRequest();" +
     *     "xhr.open('GET', '/resource/data.json', true);" +
     *     "xhr.onreadystatechange = function() {" +
     *     "  if (xhr.readyState == 4) {" +
     *     "    callback(xhr.responseText);" +
     *     "  }" +
     *     "};" +
     *     "xhr.send();");
     * JsonObject json = new JsonParser().parse((String) response);
     * assertEquals("cheese", json.get("food").getAsString());
     * 脚本参数必须是数字、布尔值、String、WebElement 或上述任意组合的 List。如果参数不满足这些条件，将引发异常。参数将通过 “arguments” 变量提供给 JavaScript。
     * 形参:
     * script – 要执行的 JavaScript。
     * args – 脚本的参数。可能为空。
     * 返回值:
     * Boolean、Long、String、List、Map、WebElement 或 null 之一。
     * 请参阅:
     * WebDriver.Timeouts.scriptTimeout(java.time.Duration)
     */
    fun <T : Any> executeAsyncScriptAsT(@Language("javascript") script: String, vararg args: Any?): T? =
        aroundWindow<T?> {
            return@aroundWindow driver.executeAsyncScript(script, *args) as? T
        }

    /**
     * 在当前所选框架或窗口的上下文中执行异步 JavaScript 片段。与 executing synchronous JavaScript不同，使用此方法执行的脚本必须通过调用提供的回调来显式表示它们已完成。此回调始终作为最后一个参数注入到执行的函数中。
     * 传递给回调函数的第一个参数将用作脚本的结果。此值将按如下方式处理：
     * 对于 HTML 元素，此方法返回 WebElement
     * 对于数字，将返回 Long
     * 对于布尔值，将返回布尔值
     * 对于所有其他情况，将返回 String。
     * 对于数组，返回一个 List<Object>每个对象都遵循上述规则。我们支持嵌套列表。
     * 对于映射，返回 Map<String、Object>其值遵循上述规则。
     * 除非值为 null 或没有返回值，其中返回 null
     * 要执行的脚本的默认超时时间为 0ms。在大多数情况下，包括以下示例，必须事先将脚本超时 WebDriver.Timeouts.scriptTimeout(java.time.Duration) 设置为足够大的值。
     * 示例 #1：在被测浏览器中执行休眠。
     * long start = System.currentTimeMillis();
     * ((JavascriptExecutor) driver).executeAsyncScript(
     *     "window.setTimeout(arguments[arguments.length - 1], 500);");
     * System.out.println(
     *     "Elapsed time: " + (System.currentTimeMillis() - start));
     * 示例 #2：将测试与 AJAX 应用程序同步：
     * WebElement composeButton = driver.findElement(By.id("compose-button"));
     * composeButton.click();
     * ((JavascriptExecutor) driver).executeAsyncScript(
     *     "var callback = arguments[arguments.length - 1];" +
     *     "mailClient.getComposeWindowWidget().onload(callback);");
     * driver.switchTo().frame("composeWidget");
     * driver.findElement(By.id("to")).sendKeys("bog@example.com");
     * 示例 #3： 注入 XMLHttpRequest 并等待结果：
     * Object response = ((JavascriptExecutor) driver).executeAsyncScript(
     *     "var callback = arguments[arguments.length - 1];" +
     *     "var xhr = new XMLHttpRequest();" +
     *     "xhr.open('GET', '/resource/data.json', true);" +
     *     "xhr.onreadystatechange = function() {" +
     *     "  if (xhr.readyState == 4) {" +
     *     "    callback(xhr.responseText);" +
     *     "  }" +
     *     "};" +
     *     "xhr.send();");
     * JsonObject json = new JsonParser().parse((String) response);
     * assertEquals("cheese", json.get("food").getAsString());
     * 脚本参数必须是数字、布尔值、String、WebElement 或上述任意组合的 List。如果参数不满足这些条件，将引发异常。参数将通过 “arguments” 变量提供给 JavaScript。
     * 形参:
     * script – 要执行的 JavaScript。
     * args – 脚本的参数。可能为空。
     * 返回值:
     * Boolean、Long、String、List、Map、WebElement 或 null 之一。
     * 请参阅:
     * WebDriver.Timeouts.scriptTimeout(java.time.Duration)
     */
    fun executeAsyncScript(@Language("javascript") script: String, vararg args: Any?): Any? = aroundWindow {
        return@aroundWindow driver.executeAsyncScript(script, *args)
    }

    fun switchToThis(): CWindow {
        driver.switchTo().window(windowHandleID)
        return this
    }

    fun htmlAsWebElement(): WebElement = aroundWindow {
        return@aroundWindow findElement(By.tagName("html"))
    }

    fun bodyAsWebElement(): WebElement = aroundWindow {
        return@aroundWindow findElement(By.tagName("body"))
    }

    fun headerAsWebElement(): WebElement = aroundWindow {
        return@aroundWindow findElement(By.tagName("head"))
    }

    fun findElementById(id: String): WebElement = aroundWindow {
        return@aroundWindow findElement(By.id(id))
    }

    fun findElementByXpath(xpath: String): WebElement = aroundWindow {
        return@aroundWindow findElement(By.xpath(xpath))
    }

    fun findElementsByXpath(xpath: String): List<WebElement> = aroundWindow {
        return@aroundWindow findElements(By.xpath(xpath))
    }

    fun findElementsByClassName(className: String): List<WebElement> = aroundWindow {
        return@aroundWindow findElements(By.className(className))
    }

    fun findElementsByTagName(tagName: String): List<WebElement> = aroundWindow {
        return@aroundWindow findElements(By.tagName(tagName))
    }

    fun findElementsByCssSelector(cssSelector: String): List<WebElement> = aroundWindow {
        return@aroundWindow findElements(By.cssSelector(cssSelector))
    }

    override fun findElements(by: By): List<WebElement> = aroundWindow {
        return@aroundWindow driver.findElements(by)
    }

    override fun findElement(by: By): WebElement = aroundWindow {
        return@aroundWindow driver.findElement(by)
    }

    fun network() = aroundWindow {
        return@aroundWindow driver.network()
    }

    /**
     * switchTo() 是 Selenium 中用于切换操作上下文的核心方法，返回一个 TargetLocator 对象。
     * 这里直接切换到当前窗口，并返回一个 TargetLocator 对象。
     * 如果想要返回一个 TargetLocator 对象，调用 targetLocator 属性即可
     */
    @JvmSynthetic
    @Deprecated("use targetLocator instead", ReplaceWith("targetLocator"))
    override fun switchTo(): WebDriver.TargetLocator = aroundWindow {
        switchToThis()
        return@aroundWindow driver.switchTo()
    }

    /**
     * 切换到指定窗口
     */
    fun switchTo(windown: CWindow): CWindow {
        driver.switchTo().window(windown.windowHandleID)
        return window
    }

    /**
     * 切换到指定窗口
     */
    fun switchTo(windowHandle: String): CWindow {
        driver.switchTo().window(windowHandle)
        return window
    }

    @Deprecated("可能会带来 BUG", ReplaceWith("back(), forward()..."))
    @JvmSynthetic
    override fun navigate(): WebDriver.Navigation = aroundWindow {
        return@aroundWindow driver.navigate()
    }

    @Deprecated("可能会带来 BUG", ReplaceWith("cookieManager, timeouts"))
    @JvmSynthetic
    override fun manage(): WebDriver.Options = aroundWindow {
        return@aroundWindow driver.manage()
    }

    fun timeouts() = aroundWindow {
        return@aroundWindow driver.manage().timeouts()
    }

    fun back() = aroundWindow {
        driver.navigate().back()
        return@aroundWindow this
    }

    fun forward() = aroundWindow {
        driver.navigate().forward()
    }

    fun refresh() = aroundWindow {
        driver.navigate().refresh()
    }

    fun loadNextUrl(url: String) = aroundWindow {
        driver.navigate().to(url)
    }

    fun switchToFrame(frame: WebElement) = aroundWindow {
        driver.switchTo().frame(frame)
    }

    @JvmSynthetic
    override fun get(url: String) = aroundWindow {
        driver.get(url)
    }

    @JvmOverloads
    fun get(url0: String? = null, isNewTab: Boolean = false): CWindow = aroundWindow {
        val url = if (url0.isNullOrEmpty()) "about:blank" else url0
        return@aroundWindow if (isNewTab) newTab(url).switchToThis()
        else get(url ?: "about:blank").let { this }
    }

    @JvmOverloads
    fun get(file: File, isNewTab: Boolean = false): CWindow = aroundWindow {
        return@aroundWindow get(file.toURI().toURL().toString(), isNewTab)
    }

    /**
     * 加载 HTML
     */
    @JvmOverloads
    fun loadHtml(@Language("html") html: String, isNewTab: Boolean = false): CWindow = aroundWindow {
        // 打开一个空白页面
        val cw = if (isNewTab) newTab("about:blank").switchToThis()
        else get("about:blank").let { this }
        // 通过 JavaScript 注入 HTML
        executeScript("document.body.innerHTML = arguments[0];", html)
        return@aroundWindow cw
    }

    fun loadCss(@Language("css") css: String): CWindow = aroundWindow {
        executeScript(
            """
        const style = document.createElement('style');
        style.textContent = arguments[0];
        document.head.appendChild(style);
    """, css
        )
        return@aroundWindow this
    }

    override fun getCurrentUrl(): String? = aroundWindow {
        return@aroundWindow driver.currentUrl
    }

    override fun getTitle(): String? = aroundWindow {
        return@aroundWindow driver.title
    }

    override fun getPageSource(): String? = aroundWindow {
        return@aroundWindow driver.pageSource
    }

    /**
     * switchTo alert
     */
    fun alert(): Alert = aroundWindow {
        return@aroundWindow driver.switchTo().alert()
    }

    /**
     * 创建alert
     */
    fun createAlert(text: String): Alert = aroundWindow {
        driver.executeScript("alert('$text');")
        return@aroundWindow driver.switchTo().alert()
    }

    /**
     * 可以使用等待来进行页面等待或者JS等待
     * WebDriverWait + executeScript
     * WebDriverWait + ExpectedConditions
     */
    fun wait(
        timeout: Duration,
        sleep: Duration = Duration.ofMillis(500L),
        clock: Clock = Clock.systemDefaultZone(),
        sleeper: Sleeper = Sleeper.SYSTEM_SLEEPER,
    ): WebDriverWait {
        return driver.WebDriverWait(timeout, sleep, clock, sleeper)
    }

    /**
     * 可以使用等待来进行页面等待或者JS等待
     * WebDriverWait + executeScript
     * WebDriverWait + ExpectedConditions
     */
    fun wait(
        timeout: Long,
        sleep: Long = 500L,
        unit: ChronoUnit = ChronoUnit.MILLIS,
        clock: Clock = Clock.systemDefaultZone(),
        sleeper: Sleeper = Sleeper.SYSTEM_SLEEPER,
    ): WebDriverWait {
        return driver.WebDriverWait(timeout, sleep, unit, clock, sleeper)
    }


    fun fluentWait(
        timeout: Duration,
        pollingEvery: Duration = Duration.ofMillis(500L),
        message: String = "timeout",
        ignoringExceptions: List<Class<out Throwable>> = emptyList(),
    ): FluentWait<ChromeDriver> {
        return driver.FluentWait(timeout, pollingEvery, message, ignoringExceptions)
    }


    fun fluentWait(
        timeout: Long,
        pollingEvery: Long = 500L,
        message: String = "timeout",
        ignoringExceptions: List<Class<out Throwable>> = emptyList(),
        unit: ChronoUnit = ChronoUnit.MILLIS,
    ): FluentWait<ChromeDriver> {
        return driver.FluentWait(
            Duration.of(timeout, unit),
            Duration.of(pollingEvery, unit),
            message,
            ignoringExceptions
        )
    }


    fun scrollTo(x: Int, y: Int) = aroundWindow {
        driver.executeScript("window.scrollTo($x, $y)")
    }

    fun screenshotAsFile(path: String? = null): File = aroundWindow {
        return@aroundWindow driver.screenshotAsFile(path)
    }

    inline fun <reified T : Any> screenshotAsT(): T = aroundWindow {
        return@aroundWindow driver.screenshotAsT<T>()
    }

    /**
     * 创建 Actions 用于模拟鼠标键盘操作。只针对当前窗口，如果在操作过程中出现窗口切换会导致操作失误
     */
    fun actions(block: CWindow.(Actions) -> Unit) = aroundWindow {
        block(Actions(driver))
    }

    @JvmOverloads
    fun newWindow(url: String? = currentUrl, switchTo: Boolean = false) = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.switchTo().newWindow(WindowType.WINDOW).let {
            if (url == null) it.get("about:blank") else it.get(url)
            CWindow(it as ChromeDriver, it.windowHandle)
        }
    }.apply {
        if (switchTo) switchTo(this) else this
    }

    @JvmOverloads
    fun newTab(url: String? = currentUrl, switchTo: Boolean = false) = aroundWindow {
        driver.switchTo().newWindow(WindowType.TAB).let {
            if (url.isNullOrEmpty()) it.get("about:blank") else it.get(url)
            CWindow(it as ChromeDriver, it.windowHandle)
        }
    }.apply {
        if (switchTo) switchTo(this) else this
    }

    fun fullscreen() = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.manage().window().fullscreen()
    }

    fun maximize() = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.manage().window().maximize()
    }

    fun minimize() = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.manage().window().minimize()
    }

    fun resize(width: Int, height: Int) = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.manage().window().size = Dimension(width, height)
    }

    override fun close() = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.close()
    }

    fun isClose(): Boolean {
        return !driver.windowHandles.contains(windowHandleID)
    }

    override fun quit() {
        driver.quit()
    }

    @JvmSynthetic
    fun onQuit(block: suspend () -> Unit) {
        driver.onQuit(block)
    }

    /**
     * 监听窗口关闭
     */
    @JvmSynthetic
    fun onClose(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            while (!isClose()) {
                delay(200)
            }
            block()
        }
    }

    /**
     * 监听窗口创建
     */
    @JvmSynthetic
    fun onCreateWindow(block: suspend (CWindow) -> Unit): Job {
        return driver.onCreateWindow(block)
    }

    @Deprecated("use windows instead")
    override fun getWindowHandles(): MutableSet<String> {
        return driver.windowHandles
    }

    @Deprecated("use windowHandleID instead")
    override fun getWindowHandle(): String = windowHandleID

    fun <T> aroundWindow(currentWindowId: String = currentWindowHandle, block: CWindow.() -> T): T {
        return if (isSynchronized) {
            synchronized(driver) {
                aroundWindowResult(currentWindowId, block)
            }
        } else {
            aroundWindowResult(currentWindowId, block)
        }.getOrThrow()
    }

    private fun <T> CWindow.aroundWindowResult(
        currentWindowId: String,
        block: CWindow.() -> T,
    ): Result<T> {
        if (driver.windowHandles.contains(currentWindowId)) {
            driver.switchTo().window(windowHandleID)
        }
        val result = kotlin.runCatching {
            if (isSynchronized) synchronized(driver) { block() }
            else block()
        }
        if (driver.windowHandles.contains(currentWindowId)) {
            driver.switchTo().window(currentWindowId)
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CWindow

        if (driver != other.driver) return false
        if (windowHandleID != other.windowHandleID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = driver.hashCode()
        result = 31 * result + windowHandleID.hashCode()
        return result
    }

    override fun toString(): String {
        return windowHandleID
    }
}

class CookieManager(
    val window: CWindow,
) {

    private fun <T> aroundWindow(block: WebDriver.Options.() -> T): T {
        return window.aroundWindow {
            block(manage())
        }
    }

    val cookies: Set<Cookie>
        get() = aroundWindow { cookies }

    fun get(key: String): Cookie? {
        return aroundWindow { getCookieNamed(key) }
    }

    fun getValue(key: String): String? {
        return aroundWindow { getCookieNamed(key)?.value }
    }

    fun builder(key: String, value: String) = Cookie.Builder(key, value)

    fun add(key: String, value: String) {
        aroundWindow { addCookie(Cookie.Builder(key, value).build()) }
    }

    fun add(vararg cookies: Cookie) {
        aroundWindow {
            for (cookie in cookies) addCookie(cookie)
        }
    }

    fun add(cookies: List<Cookie>) {
        aroundWindow {
            for (cookie in cookies) addCookie(cookie)
        }
    }

    fun delete(name: String): Cookie? = aroundWindow {
        val temp = get(name)
        deleteCookieNamed(name)
        return@aroundWindow temp
    }

    fun deleteAll() {
        aroundWindow { deleteAllCookies() }
    }
}