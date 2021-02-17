package org.danielzfranklin.librereader.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun InputStream.writeTo(outFile: File) {
    this.use { input ->
        FileOutputStream(outFile).use { output ->
            // See <https://stackoverflow.com/a/56074084>
            val buffer = ByteArray(4 * 1024)
            while (true) {
                val byteCount = input.read(buffer)
                if (byteCount < 0) {
                    break
                }
                output.write(buffer, 0, byteCount)
            }
            output.flush()
        }
    }
}