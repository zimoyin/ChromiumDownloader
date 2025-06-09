package io.github.headlesschrome.download

import io.github.headlesschrome.location.Positioner
import java.io.File
import java.io.OutputStream
import java.net.Proxy
import java.net.URLConnection

/**
 *
 * @author : zimo
 * @date : 2025/03/10
 */
abstract class AbsChromiumDownloader(
    val positioner: Positioner,
    val proxy: Proxy? = null,
    val path: String = CHROME_DOWNLOAD_PATH,
    val rootDir: File = File(path).resolve(positioner.revision),
    val appDir: File = rootDir.resolve("app"),
    val driverDir: File = rootDir.resolve("driver"),
) {
    val downloadState: DownloadState = DownloadState(
        positioner,
        rootDir.absolutePath,
        this@AbsChromiumDownloader.appDir.absolutePath,
        this@AbsChromiumDownloader.driverDir.absolutePath,
        proxy,
    )

    fun initRootDir() {
        rootDir.mkdirs()
        if (!rootDir.exists()) throw RuntimeException("rootDir not created")
    }

    fun initAppDir() {
        this@AbsChromiumDownloader.appDir.mkdirs()
        if (!this@AbsChromiumDownloader.appDir.exists()) throw RuntimeException("appDir not created")
    }

    fun initDriverDir() {
        this@AbsChromiumDownloader.driverDir.mkdirs()
        if (!this@AbsChromiumDownloader.driverDir.exists()) throw RuntimeException("driverDir not created")
    }

    abstract fun downloadChrome()

    abstract fun downloadChromeDriver()

    protected fun URLConnection.copyTo(output: OutputStream, stateKey: DownloadState.Type) {
        val contentLengthLong = contentLengthLong
        var total: Long = 0
        inputStream.use { input ->
            output.use {
                val buffer = ByteArray(1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    total += read
                    downloadState.updateState(stateKey, DownloadState.State(contentLengthLong, total))
                }
            }
        }
    }

    protected fun URLConnection.copyTo(outputFIle: File, stateKey: DownloadState.Type) {
        copyTo(outputFIle.outputStream(), stateKey)
    }


    class DownloadState(
        val positioner: Positioner,
        val rootDir: String,
        val appDir: String,
        val driverDir: String,
        val proxy: Proxy? = null,
    ) {
        private val listeners: MutableList<(DownloadState) -> Unit> = mutableListOf()

        var chromeDownloadState: State? = null
            private set
        var driverDownloadState: State? = null
            private set

        internal fun updateState(key: Type, state: State) {
            when (key) {
                Type.CHROME -> {
                    chromeDownloadState = state
                }

                Type.DRIVER -> {
                    driverDownloadState = state
                }
            }
            listeners.forEach {
                it.invoke(this)
            }
        }

        fun onChange(block: (DownloadState) -> Unit) {
            listeners.add(block)
        }

        class State(
            val contentLength: Long,
            val totalLength: Long,
        ) {
            val isComplete: Boolean
                get() = totalLength == contentLength
        }

        enum class Type {
            CHROME,
            DRIVER
        }
    }
}