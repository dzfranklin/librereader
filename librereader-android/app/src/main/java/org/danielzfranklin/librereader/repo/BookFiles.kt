package org.danielzfranklin.librereader.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import org.danielzfranklin.librereader.model.BookID
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

class BookFiles private constructor(dir: File) {
    val epubFile = File(dir, EPUB_FILENAME)
    val coverFile = File(dir, COVER_FILENAME)

    fun coverBitmap(): Bitmap? {
        val source = ImageDecoder.createSource(coverFile)
        return try {
            ImageDecoder.decodeBitmap(source)
        } catch (e: FileNotFoundException) {
            Timber.w(e, "Cover not found")
            null
        }
    }

    companion object {
        fun open(context: Context, id: BookID): BookFiles? {
            val dir = getDir(context, id)
            return if (dir.exists()) BookFiles(dir) else null
        }

        fun create(context: Context, id: BookID): BookFiles {
            val dir = getDir(context, id)
            if (dir.exists()) {
                Timber.w("Dir already exists, clearing")
                for (child in dir.listFiles()!!) {
                    child.deleteRecursively()
                }
            } else {
                dir.mkdir()
            }
            return BookFiles(dir)
        }

        private fun getDir(context: Context, id: BookID): File {
            val parent = context.getDir(IMPORTED_DIR, Context.MODE_PRIVATE)
            return File(parent, id.toString())
        }

        private const val IMPORTED_DIR = "imported_books"
        private const val EPUB_FILENAME = "epub"
        private const val COVER_FILENAME = "cover"
    }
}