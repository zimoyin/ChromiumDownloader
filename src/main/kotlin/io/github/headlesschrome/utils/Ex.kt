package io.github.headlesschrome.utils

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

/**
 *
 * @author : zimo
 * @date : 2025/02/09
 */
inline fun ChromeDriver.use(block: WebDriver.() -> Unit) {
    try {
        block()
    } finally {
        quit()
    }
}