package io.github.headlesschrome.utils

import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.WrapsDriver
import org.openqa.selenium.remote.RemoteWebElement

/**
 *
 * @author : zimo
 * @date : 2025/06/06
 */
fun WebElement.findElementById(id: String): WebElement? {
    return runCatching { findElement(By.id(id)) }.getOrNull()
}

fun WebElement.findElementByXpath(xpath: String): WebElement? {
    return runCatching { findElement(By.xpath(xpath)) }.getOrNull()
}

fun WebElement.findElementsByXpath(xpath: String): List<WebElement> {
    return findElements(By.xpath(xpath))
}

fun WebElement.findElementsByClassName(className: String): List<WebElement> {
    return findElements(By.className(className))
}

fun WebElement.findElementsByTagName(tagName: String): List<WebElement> {
    return findElements(By.tagName(tagName))
}

fun WebElement.findElementsByCssSelector(cssSelector: String): List<WebElement> {
    return findElements(By.cssSelector(cssSelector))
}

val WebElement.children: List<WebElement>
    get() = findElements(By.xpath("./*"))

@OptIn(ExperimentalStdlibApi::class)
fun WebElement.html(driver: WebDriver = this.driver): String {
    val js = driver as JavascriptExecutor
    return js.executeScript("return arguments[0].outerHTML;", this) as String
}

@OptIn(ExperimentalStdlibApi::class)
fun WebElement.innerHTML(driver: WebDriver = this.driver): String {
    val js = driver as JavascriptExecutor
    return js.executeScript("return arguments[0].innerHTML;", this) as String
}

val WebElement.parent: WebElement?
    get() = runCatching { findElement(By.xpath("./..")) }.getOrNull()


@ExperimentalStdlibApi
val WebElement.driver: WebDriver
    get() = (this as RemoteWebElement).wrappedDriver

/**
 * 判断元素是否可点击
 */
@OptIn(ExperimentalStdlibApi::class)
fun WebElement.isElementClickable(driver: WebDriver = this.driver): Boolean {
    return runCatching {
        // 获取元素的位置和大小
        val location: Point = getLocation()
        val size: Dimension = getSize()

        // 计算元素的中心点坐标
        val x: Int = location.getX() + size.getWidth() / 2
        val y: Int = location.getY() + size.getHeight() / 2

        // 使用 JavaScript 获取该坐标点的最上层元素
        val script = "return document.elementFromPoint(arguments[0], arguments[1]);"
        val topElement = (driver as JavascriptExecutor).executeScript(script, x, y) as WebElement?

        // 判断该元素是否为目标元素
        topElement == this
    }.getOrDefault(false)
}

