package io.github.zimoyin.kuake;

import io.github.headlesschrome.ChromiumLoader;
import io.github.headlesschrome.download.ChromiumDownloader;
import io.github.headlesschrome.download.HuaweicloudChromiumDownloader;
import io.github.headlesschrome.utils.CWindow;
import io.github.headlesschrome.utils.ChromiumEx;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author : zimo
 * &#064;date : 2025/05/09
 */
public class Test {
    public static void main(String[] args) {
        ChromiumDownloader downloader = new ChromiumDownloader("127.0.0.1", 8070);
        ChromiumLoader loader = new ChromiumLoader(downloader);
        ChromeOptions options = loader.downloadAndLoad();
        ChromeDriver driver = new ChromeDriver(options);

        // 创建 chromium 扩展
        ChromiumEx chromium = new ChromiumEx(driver);
        // 阻塞等待 chromium 关闭
        chromium.blockUntilQuit(it->{
            // 打开一个网页
            CWindow window1 = it.getWindow();
            // window 是对标签页的封装
            CWindow window = it.get("https://baidu.com");
        });
    }
}
