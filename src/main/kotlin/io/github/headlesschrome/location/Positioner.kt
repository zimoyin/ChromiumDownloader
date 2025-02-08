package io.github.headlesschrome.location

import io.github.headlesschrome.download.ChromiumDownloader
import io.github.headlesschrome.download.ChromiumDownloader.Companion.ALT_MEDIA
import io.github.headlesschrome.download.ChromiumDownloader.Companion.BASE_URL
import io.github.headlesschrome.download.ChromiumDownloader.Companion.LAST_CHANGE
import io.github.headlesschrome.download.ChromiumDownloader.Companion.SPACE
import io.github.headlesschrome.utils.connection
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
class Positioner(
    val platform: Platform,
    val revision: String,
) {

    companion object {


        fun getLastPosition(proxy: Proxy? = null): Positioner {
            return getLastPosition(Platform.currentPlatform(), proxy)
        }

        @JvmOverloads
        fun getLastPosition(platform: Platform = Platform.currentPlatform(), proxy: Proxy? = null): Positioner {
            val url = ChromiumDownloader.createURL(platform, LAST_CHANGE)

            // 如果提供了代理，则使用代理打开连接；否则直接打开连接
            val connection = url.connection(proxy)
            val revision = connection.inputStream.use {
                it.readAllBytes().decodeToString()
            }
            return Positioner(platform, revision)
        }
    }

    override fun toString(): String {
        return "Positioner(platform=$platform, revision='$revision')"
    }
}
