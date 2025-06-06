package io.github.headlesschrome.utils

import org.openqa.selenium.By
import org.openqa.selenium.By.id
import org.openqa.selenium.JavascriptExecutor
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


