## Chrome 自动下载配置程序

这是一个自动化的下载程序，可以下载最新的或者某一次修订的chromium 以及其驱动.
下面的代码将会下载最新版本的chromium和chromedriver到 ./chrome/修订号/ 下
```kotlin
val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 8070))
val positioner = Positioner.getLastPosition(proxy) // 获取最新版本的修订号
val downloader = ChromiumDownloader(positioner,proxy) // 创建下载器
downloader.downloadChrome()
downloader.downloadChromeDriver()
println("下载完成")
```
本程序虽然是下载，但是集成了 selenium ,所以需要使用 selenium 的时候，不需要手动引入了
**静态方法**
```kotlin
println("平台: " + Platform.currentPlatform())
val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 8070))
runCatching {
    ChromiumLoader.findChromeDriver().let {
        println(it)
        println(ChromiumLoader.getChromeDriverVersion(it))
    }
}

runCatching {
    ChromiumLoader.findChrome().let {
        println(it)
        println(ChromiumLoader.getChromeVersion(it))
    }
}
// 如果不存在则去下载
val options = ChromiumLoader.downloadAndLoad(proxy)
// 启用无头模式
options.addArguments("--disable-dev-shm-usage")
options.addArguments("--ignore-ssl-errors=yes")
options.addArguments("--ignore-certificate-errors")
options.addArguments("--headless")
options.addArguments("--no-sandbox")
ChromeDriver(options).use {
    get("https://www.baidu.com")
}
```
由于 ChromeDriver 和 Chrome 在外网因此需要代理，对于没有代理的可以使用国内的镜像进行下载，这里使用的华为云的镜像（chrome 镜像最近更新为 2021）
**实例化对象**
```kotlin
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
```

如果程序没有在你指定的位置下载 chrome 而是使用的其他位置的 chrome，你可以使用 downloader 进行下载
```kotlin
val loader = ChromiumLoader(HuaweicloudChromiumDownloader())
loader.downloadAndLoad(true)
// 或者手动下载
loader.downloader.downloadChrome()
loader.downloader.downloadChromeDriver()
```
---- 
已经发布到了 Maven 仓库
```xml
<dependency>
    <groupId>io.github.zimoyin</groupId>
    <artifactId>ChromiumDownloader</artifactId>
    <version>1.2.22</version>
</dependency>
```

## ChromiumEx 类
该类是用于将 Kotlin ChromeDriver 拓展方法封装到一个类中方便 Java 使用

## Kotlin ChromeDriver拓展方法
* use 使用完毕后关闭浏览器
* isQuit 是否退出
* onQuit 浏览器关闭监听
* blockUntilQuit 阻塞当前线程直到浏览器关闭
* blockUntilQuitSuspend 阻塞当前协程直到浏览器关闭
* finally JVM 关闭时退出浏览器
* screenshotAsFile 报错截图到文件
* screenshot 截图并返回指定类型（可以返回绝大部分类型）
* deleteWebDriverSign 通过覆盖 `navigator.webdriver` 属性隐藏自动化特征
* currentWindow 返回当前Tap或者Window 的封装
* windows 返回所有窗口的封装（封装的窗口对象可以不切换到窗口就可进行操作）
* logListener 添加浏览器标签页日志监听
* logs 返回日志
* load 加载网页文件或者加载网页源码
* get 各种重载

## Kotlin ChromeOptions 拓展方法
* **enableLoggingPrefs** 启用日志
* **enableNoSandbox** 启用无沙盒模式（添加 `--no-sandbox` 参数）
* **enableHeadless** 启用无头模式（添加 `--headless` 参数）
* **enableDisableInfobars** 禁用自动化提示信息栏（移除"Chrome正受到自动测试软件的控制"提示）
* **enableIncognito** 启用无痕模式（添加 `--incognito` 参数）
* **enableDisableGpu** 禁用GPU加速（添加 `--disable-gpu` 参数）
* **enableAllowRunningInsecureContent** 允许加载不安全内容（如直接访问HTTP网站）
* **enableDisableImage** 禁用图片加载（通过参数和偏好设置双重控制）
* **enableDisableCss** <span style="color:red">(已弃用)</span> 禁用CSS渲染
* **addPreference** 添加浏览器偏好设置（用于配置下载路径、内容拦截等高级设置）
* **enableDisableJavaScript** 禁用JavaScript执行
* **enableIgnoreSslErrors** 忽略SSL证书错误（允许访问自签名证书网站）
* **setWindowSize** 设置浏览器窗口尺寸（参数：宽度和高度）
* **setProxyServer** 配置代理服务器（参数格式：`host:port`）
* **disableSetuidSandbox** 禁用setuid沙盒机制
* **disableDevShmUsage** 禁用`/dev/shm`内存共享（解决Docker容器内存不足问题）
* **setUserProfileDir** 指定自定义用户数据目录（参数：绝对路径）
* **disableDefaultBrowserCheck** 禁用默认浏览器检测
* **disablePopupBlocking** 允许所有弹窗
* **disableExtensions** 禁用所有浏览器扩展
* **disableFirstRun** 跳过首次运行初始化
* **startMaximized** 启动时窗口最大化
* **disableNotifications** 禁用浏览器通知
* **enableAutomation** 显式启用自动化模式（与反检测机制冲突时使用）
* **disableXssAuditor** 禁用XSS防护机制
* **disableWebSecurity** 关闭同源策略（允许跨域请求）
* **disableWebGL** 禁用WebGL渲染
* **setHomeDir** 指定浏览器主目录路径
* **setDiskCacheDir** 自定义磁盘缓存目录
* **disableCache** 完全禁用浏览器缓存
* **excludeSwitches** 排除特定启动参数（参数：可变参数列表）