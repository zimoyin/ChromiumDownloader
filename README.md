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
    <version>1.2.28</version>
</dependency>
```

## ChromiumEx 类 (JAVA)
该类是用于将 Kotlin ChromeDriver 拓展方法封装到一个类中方便 Java 使用
```java
ChromiumLoader loader = new ChromiumLoader(new HuaweicloudChromiumDownloader());
ChromeOptions options = loader.downloadAndLoad(true);
ChromeOptionsExKt.enableHeadless(options);
ChromeDriver driver = new ChromeDriver(options);
ChromiumEx ex = new ChromiumEx(driver);
```

## ChromeOptionsExKt 类
该类用于 ChromeOptions 的拓展方法
* Java
```java
ChromeOptionsExKt.enableHeadless(options);
// ...
```
* Kotlin
```kotlin
options.enableHeadless();
```

## CWindow 类
对 ChromeDriver 的聚合封装。
* Java 可以通过 ChromiumEx 获取 CWindow 的子类实现
```java
ChromiumEx ex = new ChromiumEx(driver);
// ex.blockUntilQuit 阻塞当前线程直到浏览器关闭
ex.blockUntilQuit(ex0->{
    CWindow main = ex0.get("https://baidu.com");
});
```
* Kotlin 通过 ChromeDriver.window 获取当前窗口的实现
```kotlin
ChromeDriver(options).blockUntilQuitSuspend {
    get(qr_code)
    var main = window
}
```

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
* 略