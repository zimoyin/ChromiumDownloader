package io.github.zimoyin.kuake

import io.github.headlesschrome.ChromiumLoader
import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.download.HuaweicloudChromiumDownloader
import io.github.headlesschrome.utils.blockUntilQuitSuspend
import io.github.headlesschrome.utils.enableDisableCss
import io.github.headlesschrome.utils.enableDisableInfobars
import io.github.headlesschrome.utils.enableIgnoreSslErrors
import io.github.headlesschrome.utils.enableLoggingPrefs
import io.github.headlesschrome.utils.enableNoSandbox
import io.github.headlesschrome.utils.get
import io.github.headlesschrome.utils.setUserAgent
import io.github.headlesschrome.utils.window
import jdk.internal.agent.resources.agent
import jdk.internal.net.http.common.Log.headers
import org.openqa.selenium.Cookie
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.devtools.DevTools


suspend fun main() {
    val loader = ChromiumLoader(HuaweicloudChromiumDownloader())
    val options = loader.downloadAndLoad(true)
    println("Chrome 版本: " + loader.chromeVersion)
    println("ChromeDriver 版本: " + loader.chromeDriverVersion)
    println("Chrome 路径: " + loader.chromePath)
    println("ChromeDriver 路径: " + loader.chromeDriverPath)
    println("当前平台： " + loader.platform)

    options.enableNoSandbox()
    options.enableDisableInfobars()
    options.enableDisableCss()
    options.enableIgnoreSslErrors()
    options.enableLoggingPrefs()

    options.setUserAgent("Mozilla/5.0 (Linux; U; Android 12; zh-CN; 23127PN0CC Build/V417IR) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/100.0.4896.58 Quark/7.11.4.814 Mobile Safari/537.36")
    options.addArguments("--autoplay-policy=no-user-gesture-required")
    options.addArguments("--enable-features=EnableOpusPlayback,EnableH264Playback")
    options.addArguments("--use-fake-ui-for-media-stream")
    options.addArguments("--use-fake-device-for-media-stream")
    options.addArguments("--enable-features=Widevine")
    options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))
    options.setExperimentalOption("useAutomationExtension", false)

    val qr_code = "https://su.quark.cn/4_eMHBJ?token=st9c9633356tajenpgg5ydypw5f62mzx&client_id=532&ssb=weblogin&uc_param_str=&uc_biz_str=S%3Acustom%7COPT%3ASAREA%400%7COPT%3AIMMERSIVE%401%7COPT%3ABACK_BTN_STYLE%400"
    val cna = "dx6iIL9Z+z8CAWoI2k6MRCJb"
    val ucsession = "Ict9SHllEc6eCuG9"
    val isg = "BP7-BSphGrW-nU6W6fAO1Xh7TxZAP8K5lSw8wKgHasE8S54lEM8SySSpx1Ei6LrR"

    // :method: GET
    //:authority: su.quark.cn
    //:path: /4_eMHBJ?token=st9c9633356tajenpgg5ydypw5f62mzx&client_id=532&ssb=weblogin&uc_param_str=&uc_biz_str=S%3Acustom%7COPT%3ASAREA%400%7COPT%3AIMMERSIVE%401%7COPT%3ABACK_BTN_STYLE%400
    //:scheme: https
    //upgrade-insecure-requests: 1
    //user-agent: Mozilla/5.0 (Linux; U; Android 12; zh-CN; 23127PN0CC Build/V417IR) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/100.0.4896.58 Quark/7.11.4.814 Mobile Safari/537.36
    //accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8
    //sec-fetch-site: none
    //sec-fetch-mode: navigate
    //sec-fetch-dest: document
    //accept-language: zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7
    //cookie: cna=dx6iIL9Z+z8CAWoI2k6MRCJb
    //cookie: ucsession=Ict9SHllEc6eCuG9
    //cookie: isg=BC0t-CSIOZdkW9177mVNELdWN8unimFc6Vcu1G8yaUQz5k2YN9pxLHun1nyl_XkU
    //accept-encoding: gzip, deflate, br


    ChromeDriver(options).blockUntilQuitSuspend {
        get(qr_code)
        window.cookieManager.apply {
            add("cna",cna)
            add("ucsession",ucsession)
            add("isg",isg)
        }
        window.cookieManager.cookies.forEach {
            println("${it.domain} = ${it.name}:${it.value}")
        }
    }
}