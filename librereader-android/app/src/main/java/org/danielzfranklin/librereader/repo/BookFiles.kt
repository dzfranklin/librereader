package org.danielzfranklin.librereader.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import org.danielzfranklin.librereader.model.BookID
import java.io.File

class BookFiles(private val context: Context, private val id: BookID) {
    private val parent = context.getDir(IMPORTED_DIR, Context.MODE_PRIVATE)
    private val dir = File(parent, id.toString())

    val epubFile = File(dir, EPUB_FILENAME)
    val coverFile = File(dir, COVER_FILENAME)

    /** Idempotently initialize.
     *
     * You must call `initialize()` before writing to files
     *
     * @return True if not already initialized
     */
    fun initialize(): Boolean = dir.mkdir()

    fun coverBitmap(): Bitmap {
        val source = ImageDecoder.createSource(coverFile)
        return ImageDecoder.decodeBitmap(source)
    }

    companion object {
        private const val IMPORTED_DIR = "imported_books"
        private const val EPUB_FILENAME = "epub"
        private const val COVER_FILENAME = "cover"
    }
}