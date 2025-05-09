package io.github.headlesschrome.utils

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.io.File
import java.util.logging.Level

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
 * 设置 userAgent
 */
fun ChromeOptions.setUserAgent(userAgent: String): ChromeOptions {
    addArguments("--user-agent=$userAgent")
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