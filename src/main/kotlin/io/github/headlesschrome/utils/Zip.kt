package io.github.headlesschrome.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
class Zip {
    companion object{
        fun unzip(zipFile: File) {
            // 确认传入的文件是ZIP文件并且存在
            if (!zipFile.exists()) {
                throw IllegalArgumentException("The specified file does not exist.")
            }

            if (!zipFile.name.lowercase().endsWith(".zip")) {
                throw IllegalArgumentException("The specified file is not a valid ZIP file.")
            }

            val outputDir = zipFile.parentFile ?: run {
                throw IllegalStateException("Unable to determine the output directory.")
            }

            try {
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val outputFile = File(outputDir, entry!!.name)
                        if (entry!!.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            FileOutputStream(outputFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("An error occurred during extraction.", e)
            }
        }
    }

}