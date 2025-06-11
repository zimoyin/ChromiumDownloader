package io.github.headlesschrome.utils

import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions

/**
 *
 * @author : zimo
 * @date : 2025/06/10
 */

fun Actions.moveToLocation(element: WebElement){
    val point = element.location
    moveToLocation(point.x, point.y)
}