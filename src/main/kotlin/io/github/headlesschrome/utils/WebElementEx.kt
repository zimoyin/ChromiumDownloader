package io.github.headlesschrome.utils

import org.openqa.selenium.By
import org.openqa.selenium.By.id
import org.openqa.selenium.WebElement

/**
 *
 * @author : zimo
 * @date : 2025/06/06
 */

fun WebElement.findElementById(id: String): WebElement {
    return findElement(By.id(id))
}

fun WebElement.findElementByXpath(xpath: String): WebElement {
    return findElement(By.xpath(xpath))
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