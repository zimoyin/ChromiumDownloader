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
    override fun toString(): String {
        return "Positioner(platform=$platform, revision='$revision')"
    }
}
