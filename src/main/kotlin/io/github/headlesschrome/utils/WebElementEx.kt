package io.github.headlesschrome.utils

import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

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

fun WebElement.html(driver: WebDriver): String {
    val js = driver as JavascriptExecutor
    return js.executeScript("return arguments[0].outerHTML;", this) as String
}

fun WebElement.innerHTML(driver: WebDriver): String {
    val js = driver as JavascriptExecutor
    return js.executeScript("return arguments[0].innerHTML;", this) as String
}

val WebElement.parent: WebElement?
    get() = runCatching { findElement(By.xpath("./..")) }.getOrNull()


/**
 * 判断元素是否可点击
 */
fun WebElement.isElementClickable(driver: WebDriver): Boolean {
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
    return topElement == this
}

