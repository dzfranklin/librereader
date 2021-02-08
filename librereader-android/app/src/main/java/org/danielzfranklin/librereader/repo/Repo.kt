package org.danielzfranklin.librereader.repo

import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.LibreReaderApplication
import org.danielzfranklin.librereader.repo.model.Book
import org.danielzfranklin.librereader.repo.model.BookID
import org.danielzfranklin.librereader.repo.model.BookPosition
import org.danielzfranklin.librereader.repo.model.BookStyle
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class Repo(private val app: LibreReaderApplication) {
    // TODO: Replace with real store
    private val store = ConcurrentHashMap<BookID, Book>()

    fun importBook(uri: Uri): BookID {
        val idStream = openUri(uri)
        val stream = openUri(uri)

        val id = BookID.forEpub(idStream)
        val epub = EpubReader().readEpub(stream)

        val position = BookPosition(id, 0, 0)
        val style = BookStyle()
        store[id] = Book(style, position, epub)

        return id
    }

    fun getBook(id: BookID): Book? {
        return store[id]
    }

    private fun openUri(uri: Uri): InputStream {
        // TODO: Error handling
        return app.contentResolver.openInputStream(uri)!!
    }

    companion object {
        private var instance: Repo? = null

        fun initialize(application: LibreReaderApplication) {
            instance = Repo(application)
        }

        fun get(): Repo {
            return instance ?: throw IllegalStateException("Repo not initialized")
        }
    }
}