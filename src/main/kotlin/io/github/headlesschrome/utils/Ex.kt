package io.github.headlesschrome.utils

import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogEntries
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.Base64
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.io.encoding.ExperimentalEncodingApi


val ChromeDriver.currentWindow: CWindow
    get() = CWindow(this, this.windowHandle)

val ChromeDriver.windows: List<CWindow>
    get() = this.windowHandles.map { it -> CWindow(this, it) }

open class CWindow(
    open val driver: ChromeDriver,
    private val windowHandleID: String,
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
    val currentWindow: CWindow
        get() = driver.currentWindow

    val targetLocator: WebDriver.TargetLocator
        get() = driver.switchTo()

    var size: Dimension
        get() = aroundWindow { driver.manage().window().size }
        set(value) = aroundWindow {
            driver.manage().window().size = value
        }

    var url: String?
        get() = aroundWindow { driver.currentUrl }
        set(value) {
            value?.let { get(it) }
        }

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

    fun <T : Any> executeScriptAsT(@Language("javascript") script: String, vararg args: Any?): T? = aroundWindow<T?> {
        return@aroundWindow driver.executeScript(script, *args) as? T
    }

    fun executeScript(@Language("javascript") script: String, vararg args: Any?): Any? = aroundWindow {
        return@aroundWindow driver.executeScript(script, *args)
    }

    fun <T : Any> executeAsyncScriptAsT(@Language("javascript") script: String, vararg args: Any?): T? =
        aroundWindow<T?> {
            return@aroundWindow driver.executeAsyncScript(script, *args) as? T
        }

    fun executeAsyncScript(@Language("javascript") script: String, vararg args: Any?): Any? = aroundWindow {
        return@aroundWindow driver.executeAsyncScript(script, *args)
    }

    fun switchToThis() {
        driver.switchTo().window(windowHandleID)
    }

    override fun findElements(by: By): List<WebElement> = aroundWindow {
        driver.findElements(by)
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
    @Deprecated("use targetLocator instead")
    override fun switchTo(): WebDriver.TargetLocator = aroundWindow {
        switchToThis()
        return@aroundWindow driver.switchTo()
    }

    /**
     * 切换到指定窗口
     */
    fun switchTo(windown: CWindow): CWindow {
        driver.switchTo().window(windown.windowHandleID)
        return currentWindow
    }

    /**
     * 切换到指定窗口
     */
    fun switchTo(windowHandle: String): CWindow {
        driver.switchTo().window(windowHandle)
        return currentWindow
    }

    override fun navigate(): WebDriver.Navigation = aroundWindow {
        return@aroundWindow driver.navigate()
    }

    override fun manage(): WebDriver.Options = aroundWindow {
        return@aroundWindow driver.manage()
    }

    fun back() = aroundWindow {
        driver.navigate().back()
        return@aroundWindow this
    }

    fun forward() = aroundWindow {
        driver.navigate().forward()
    }

    fun switchToFrame(frame: WebElement) = aroundWindow {
        driver.switchTo().frame(frame)
    }

    override fun get(url: String) = aroundWindow {
        driver.get(url)
    }

    @JvmOverloads
    fun get(url: String? = "about:blank", isNewTab: Boolean = false): CWindow = aroundWindow {
        if (isNewTab) newTab().switchToThis()
        get(url ?: "about:blank")
        return@aroundWindow this
    }

    @JvmOverloads
    fun get(url: URL, isNewTab: Boolean = false): CWindow = aroundWindow {
        get(url.toString(), isNewTab)
        return@aroundWindow this
    }

    @JvmOverloads
    fun get(file: File, isNewTab: Boolean = false): CWindow = aroundWindow {
        get(file.toURI().toURL().toString(), isNewTab)
        return@aroundWindow this
    }

    @JvmOverloads
    fun get(url: URI, isNewTab: Boolean = false): CWindow = aroundWindow {
        get(url.toURL(), isNewTab)
        return@aroundWindow this
    }

    /**
     * 加载 HTML
     */
    @JvmOverloads
    fun load(@Language("html") html: String, isNewTab: Boolean = false): CWindow = aroundWindow {
        if (isNewTab) newTab().switchToThis()
        println(this)
        // 打开一个空白页面
        get("about:blank")
        // 通过 JavaScript 注入 HTML
        executeScript("document.body.innerHTML = arguments[0];", html)
        return@aroundWindow this
    }

    @JvmOverloads
    fun load(file: File, isNewTab: Boolean = false): CWindow = aroundWindow {
        get(file, isNewTab)
        return@aroundWindow this
    }

    @JvmOverloads
    fun load(url: URL, isNewTab: Boolean = false): CWindow = aroundWindow {
        get(url, isNewTab)
        return@aroundWindow this
    }

    @JvmOverloads
    fun load(url: URI, isNewTab: Boolean = false): CWindow = aroundWindow {
        get(url, isNewTab)
        return@aroundWindow this
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

    fun alert(): Alert = aroundWindow {
        return@aroundWindow driver.switchTo().alert()
    }

    fun scrollTo(x: Int, y: Int) = aroundWindow {
        driver.executeScript("window.scrollTo($x, $y)")
    }

    fun screenshotAsFile(path: String? = null): File = aroundWindow {
        return@aroundWindow driver.screenshotAsFile(path)
    }

    inline fun <reified T : Any> screenshotAsT(): T = aroundWindow {
        return@aroundWindow driver.screenshot<T>()
    }

    @JvmOverloads
    fun newWindow(url: String? = currentUrl) = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.switchTo().newWindow(WindowType.WINDOW).let {
            if (url == null) it.get("about:blank") else it.get(url)
            CWindow(it as ChromeDriver, it.windowHandle)
        }
    }

    @JvmOverloads
    fun newTab(url: String? = currentUrl) = aroundWindow {
        driver.switchTo().newWindow(WindowType.TAB).let {
            if (url == null) it.get("about:blank") else it.get(url)
            CWindow(it as ChromeDriver, it.windowHandle)
        }
    }

    fun fullscreen() = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.manage().window().fullscreen()
    }

    fun maximize() = aroundWindow {
        driver.switchTo().window(windowHandleID)
        driver.manage().window().maximize()
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

    @JvmSynthetic
    fun onClose(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            while (!isClose()) {
                delay(200)
            }
            block()
        }
    }

    override fun getWindowHandles(): MutableSet<String> {
        return driver.windowHandles
    }

    override fun getWindowHandle(): String = windowHandleID

    fun <T> aroundWindow(currentWindowId: String = currentWindowHandle, block: CWindow.() -> T): T {
        if (driver.windowHandles.contains(currentWindowId)) {
            driver.switchTo().window(windowHandleID)
        }
        val result = kotlin.runCatching { block() }
        if (driver.windowHandles.contains(currentWindowId)) {
            driver.switchTo().window(currentWindowId)
        }
        return result.getOrThrow()
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


/**
 *
 * @author : zimo
 * @date : 2025/02/09
 */
inline fun ChromeDriver.use(block: ChromeDriver.() -> Unit) {
    try {
        deleteWebDriverSign()
        block()
    } finally {
        quit()
    }
}

inline fun ChromeDriver.isQuit(): Boolean {
    return try {
        windowHandles.isEmpty()
    } catch (e: WebDriverException) {
        true
    }.also { isQuit ->
        if (isQuit) quit()
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
    deleteWebDriverSign()
    Runtime.getRuntime().addShutdownHook(Thread {
        quit()
    })
    block()
}


inline fun ChromeDriver.screenshotAsFile(path: String? = null): File {
    return screenshot<File>().let { if (path != null) it.copyTo(File(path), true) else it }
}


/**
 * 获取日志
 */
inline fun ChromeDriver.logs(logType: String = LogType.BROWSER, level: Level = Level.ALL): List<LogEntry> {
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
inline fun ChromeDriver.get(url: String? = "about:blank", isNewTab: Boolean = false): CWindow {
    if (isNewTab) this.currentWindow.newTab().switchToThis()
    get(url ?: "about:blank")
    return currentWindow
}

inline fun ChromeDriver.get(url: URL, isNewTab: Boolean = false): CWindow {
    get(url.toString(), isNewTab)
    return currentWindow
}

inline fun ChromeDriver.get(file: File, isNewTab: Boolean = false): CWindow {
    get(file.toURI().toURL().toString(), isNewTab)
    return currentWindow
}

inline fun ChromeDriver.get(url: URI, isNewTab: Boolean = false): CWindow {
    get(url.toURL(), isNewTab)
    return currentWindow
}

/**
 * 加载 HTML
 */
inline fun ChromeDriver.load(@Language("html") html: String, isNewTab: Boolean = false): CWindow {
    if (isNewTab) this.currentWindow.newTab().switchToThis()
    // 打开一个空白页面
    get("about:blank")
    // 通过 JavaScript 注入 HTML
    executeScript("document.body.innerHTML = arguments[0];", html)
    return currentWindow
}

inline fun ChromeDriver.load(file: File, isNewTab: Boolean = false): CWindow {
    get(file, isNewTab)
    return currentWindow
}

inline fun ChromeDriver.load(url: URL, isNewTab: Boolean = false): CWindow {
    get(url, isNewTab)
    return currentWindow
}

inline fun ChromeDriver.load(url: URI, isNewTab: Boolean = false): CWindow {
    get(url, isNewTab)
    return currentWindow
}

inline fun ChromeDriver.loadCss(@Language("css") css: String): CWindow {
    executeScript(
        """
        const style = document.createElement('style');
        style.textContent = arguments[0];
        document.head.appendChild(style);
    """, css
    )
    return currentWindow
}

/**
 * 截图并返回对应类型的截图
 * * T = File | Path | URL | URI | Base64 | Base64.Encoder | BufferedImage | String | File | ByteArray | InputStream | Any
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> ChromeDriver.screenshot(): T {
    return when (T::class.java) {

        ImageIO::class.java -> {
            ImageIO.read(getScreenshotAs<File>(OutputType.FILE)) as T
        }

        Path::class.java -> {
            getScreenshotAs<File>(OutputType.FILE).toPath() as T
        }

        URL::class.java -> {
            getScreenshotAs<File>(OutputType.FILE).toURI().toURL() as T
        }

        URI::class.java -> {
            getScreenshotAs<File>(OutputType.FILE).toURI() as T
        }

        Base64::class.java -> {
            getScreenshotAs<String>(OutputType.BASE64) as T
        }

        kotlin.io.encoding.Base64::class.java -> {
            Base64.getEncoder().encodeToString(getScreenshotAs<ByteArray>(OutputType.BYTES)) as T
        }

        Base64::getEncoder::class.java -> {
            Base64.getEncoder().encodeToString(getScreenshotAs<ByteArray>(OutputType.BYTES)) as T
        }

        BufferedImage::class.java -> {
            ImageIO.read(getScreenshotAs<File>(OutputType.FILE)) as T
        }

        String::class.java -> {
            getScreenshotAs<String>(OutputType.BASE64) as T
        }

        File::class.java -> {
            getScreenshotAs<File>(OutputType.FILE) as T
        }

        ByteArray::class.java -> {
            getScreenshotAs<ByteArray>(OutputType.BYTES) as T
        }

        InputStream::class.java -> {
            ByteArrayInputStream(getScreenshotAs<ByteArray>(OutputType.BYTES)) as T
        }

        Any::class.java -> {
            getScreenshotAs<String>(OutputType.BASE64) as T
        }

        else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
    }
}


fun ChromeOptions.enableNoSandbox(): ChromeOptions {
    addArguments("--no-sandbox")
    return this
}

fun ChromeOptions.enableHeadless(): ChromeOptions {
    addArguments("--headless")
    return this
}

fun ChromeOptions.enableHeadlessNew(): ChromeOptions {
    addArguments("--headless=new")
    return this
}

/**
 * 禁用浏览器信息栏上的“Chrome正受到自动测试软件的控制。” 提示
 */
fun ChromeOptions.enableDisableInfobars(): ChromeOptions {
    addArguments("--disable-infobars")
    addArguments("disable-infobars")
    this.setExperimentalOption("excludeSwitches", arrayOf("enable-automation"))
    return this
}

fun WebDriver.deleteWebDriverSign() {
    kotlin.runCatching {
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

/**
 * 启用无痕模式
 */
fun ChromeOptions.enableIncognito(): ChromeOptions {
    addArguments("--incognito")
    return this
}

/**
 * 禁用GPU
 */
fun ChromeOptions.enableDisableGpu(): ChromeOptions {
    addArguments("--disable-gpu")
    return this
}

/**
 * 允许运行不安全的网页: 可以直接无提示访问http网站
 */
fun ChromeOptions.enableAllowRunningInsecureContent(): ChromeOptions {
    addArguments("--allow-running-insecure-content")
    return this
}

/**
 * 禁用图片
 */
fun ChromeOptions.enableDisableImage(): ChromeOptions {
    addArguments("--blink-settings=imagesEnabled=false")
    addPreference("profile.managed_default_content_settings.images", 2)
    return this
}

/**
 * 禁用CSS
 */
@Deprecated("Deprecated")
fun ChromeOptions.enableDisableCss(): ChromeOptions {
    addArguments("--disable-features=CSS")
    addPreference("permissions.default.stylesheet", 2)
    return this
}

/**
 * 添加浏览器偏好设置
 */
fun ChromeOptions.addPreference(key: String, value: Any): ChromeOptions {
    val chromeOptions = asMap()["goog:chromeOptions"] as Map<String, Any>
    val prefs = chromeOptions.getOrDefault("prefs", mutableMapOf<String, Any>())
    if (prefs is MutableMap<*, *>) {
        (prefs as MutableMap<String, Any>)[key] = value
    }
    setExperimentalOption("prefs", prefs)
    return this
}

/**
 * 禁用JavaScript
 */
fun ChromeOptions.enableDisableJavaScript(): ChromeOptions {
    addArguments("--disable-javascript")
    addPreference("profile.managed_default_content_settings.javascript", 2)
    return this
}

/**
 * 忽略SSL错误
 */
fun ChromeOptions.enableIgnoreSslErrors(): ChromeOptions {
    addArguments("--ignore-ssl-errors=yes")
    addArguments("--ignore-certificate-errors")
    addArguments("--allow-insecure-localhost")
    return this
}


/**
 * 设置浏览器窗口大小
 */
fun ChromeOptions.setWindowSize(width: Int, height: Int): ChromeOptions {
    addArguments("--window-size=${width}x${height}")
    return this
}

/**
 * 配置代理服务器
 */
fun ChromeOptions.setProxyServer(proxy: String): ChromeOptions {
    addArguments("--proxy-server=$proxy")
    return this
}


/**
 * 禁用setuid沙盒
 */
fun ChromeOptions.disableSetuidSandbox(): ChromeOptions {
    addArguments("--disable-setuid-sandbox")
    return this
}

/**
 * 禁用/dev/shm使用（解决内存问题）
 */
fun ChromeOptions.disableDevShmUsage(): ChromeOptions {
    addArguments("--disable-dev-shm-usage")
    return this
}

/**
 * 指定用户数据目录
 */
fun ChromeOptions.setUserProfileDir(profilePath: String): ChromeOptions {
    addArguments("--user-data-dir=$profilePath")
    return this
}

/**
 * 禁用默认浏览器检查
 */
fun ChromeOptions.disableDefaultBrowserCheck(): ChromeOptions {
    addArguments("--no-default-browser-check")
    return this
}

/**
 * 允许弹窗
 */
fun ChromeOptions.disablePopupBlocking(): ChromeOptions {
    addArguments("--disable-popup-blocking")
    return this
}

/**
 * 禁用扩展
 */
fun ChromeOptions.disableExtensions(): ChromeOptions {
    addArguments("--disable-extensions")
    return this
}

/**
 * 禁用首次运行检查
 */
fun ChromeOptions.disableFirstRun(): ChromeOptions {
    addArguments("--no-first-run")
    return this
}

/**
 * 启动时最大化窗口
 */
fun ChromeOptions.startMaximized(): ChromeOptions {
    addArguments("--start-maximized")
    return this
}

/**
 * 禁用通知
 */
fun ChromeOptions.disableNotifications(): ChromeOptions {
    addArguments("--disable-notifications")
    return this
}

/**
 * 启用自动化通知
 */
fun ChromeOptions.enableAutomation(): ChromeOptions {
    addArguments("--enable-automation")
    return this
}

/**
 * 禁用XSS防护
 */
fun ChromeOptions.disableXssAuditor(): ChromeOptions {
    addArguments("--disable-xss-auditor")
    return this
}

/**
 * 禁用Web安全策略
 */
fun ChromeOptions.disableWebSecurity(): ChromeOptions {
    addArguments("--disable-web-security")
    return this
}

/**
 * 禁用WebGL
 */
fun ChromeOptions.disableWebGL(): ChromeOptions {
    addArguments("--disable-webgl")
    return this
}

/**
 * 指定主目录路径
 */
fun ChromeOptions.setHomeDir(homeDir: String): ChromeOptions {
    addArguments("--homedir=$homeDir")
    return this
}

/**
 * 指定磁盘缓存目录
 */
fun ChromeOptions.setDiskCacheDir(cacheDir: String): ChromeOptions {
    addArguments("--disk-cache-dir=$cacheDir")
    return this
}

/**
 * 禁用浏览器缓存
 */
fun ChromeOptions.disableCache(): ChromeOptions {
    addArguments("--disable-cache")
    return this
}

/**
 * 排除特定启动参数
 */
fun ChromeOptions.excludeSwitches(vararg switches: String): ChromeOptions {
    setExperimentalOption("excludeSwitches", switches.toList())
    return this
}

/**
 * 启用日志记录
 */
fun ChromeOptions.enableLoggingPrefs(
    loggingPrefs: LoggingPreferences = LoggingPreferences().apply {
        enable(LogType.BROWSER, Level.ALL)
        enable(LogType.DRIVER, Level.WARNING)
        enable(LogType.PERFORMANCE, Level.INFO)
        enable(LogType.PROFILER, Level.INFO)
        enable(LogType.SERVER, Level.INFO)
        enable(LogType.CLIENT, Level.INFO)
    },
): ChromeOptions {
    setCapability("goog:loggingPrefs", loggingPrefs)
    return this
}

/**
 * 使用参数添加扩展
 * 推荐开启：
 * enableDisableExtensionsFileAccessCheck
 * @param paths 扩展路径,可以是 .crx 文件路径,也可以是文件夹路径
 * @return ChromeOptions
 */
fun ChromeOptions.loadExtensions(vararg paths: String): ChromeOptions {
    val paths0 = paths.asSequence()
        .map { File(it) }
        .filter { it.exists() }
        .filter { if (it.isFile) it.extension == "crx" else true }
        .filter { if (it.isDirectory) it.resolve("manifest.json").exists() else true }
        .joinToString(",") { it.canonicalPath }
    // 加载扩展
    addArguments("--load-extension=$paths0")
    return this
}

/**
 * 主要功能：禁用 Chrome 对扩展程序访问本地文件系统的安全检查。
 * 默认行为：Chrome 默认会阻止未明确授权的扩展程序访问本地文件（出于安全考虑）。
 *
 * 启用后：
 * 允许扩展程序直接读写本地文件（无需用户手动授权）。
 * 扩展可通过 file:// 协议访问本地文件路径。
 */
fun ChromeOptions.enableDisableExtensionsFileAccessCheck(): ChromeOptions {
    addArguments("--disable-extensions-file-access-check")
    return this
}

/**
 * 主要功能：禁用浏览器的弹窗拦截功能。
 * 默认行为：Chrome 默认拦截非用户触发的弹窗（如 window.open() 或 <a target="_blank">）。
 *
 * 启用后：
 * 所有弹窗（包括广告、新标签页）均不会被拦截。
 * 允许通过 JavaScript 自由创建新窗口。
 */
fun ChromeOptions.enableDisablePopupBlocking(): ChromeOptions {
    addArguments("--disable-popup-blocking")
    return this
}
fun ChromeOptions.getOptions(): Map<String, Any> {
    return asMap()["goog:chromeOptions"] as Map<String, Any>
}

fun ChromeOptions.getPrefOptions(): MutableMap<String, Any> {
    val prefs = getOptions().getOrDefault("prefs", mutableMapOf<String, Any>())
    return prefs as MutableMap<String, Any>
}

fun ChromeOptions.getArgOptions(): MutableMap<String, Any> {
    val args = getOptions().getOrDefault("args", mutableMapOf<String, Any>())
    return args as MutableMap<String, Any>
}