package io.github.headlesschrome.utils

import java.net.Proxy
import java.net.URI
import java.net.URL
import java.net.URLConnection

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
fun String.toUrl(): URL {
    return URI(this).toURL()
}

fun URL.connection(proxy: Proxy? = null): URLConnection {
    return when (proxy != null) {
        true -> this.openConnection(proxy)
        false -> this.openConnection()
    }
}