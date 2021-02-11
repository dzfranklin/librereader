package org.danielzfranklin.librereader.repo

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.LibreReaderApplication
import org.danielzfranklin.librereader.model.Book
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class Repo(private val app: LibreReaderApplication) {
    // TODO: Replace with real store
    private val positions = ConcurrentHashMap<BookID, MutableStateFlow<BookPosition>>()
    private val store = ConcurrentHashMap<BookID, Book>()

    fun updatePosition(id: BookID,  position: BookPosition) {
        positions[id]!!.value = position
    }

    fun importBook(uri: Uri): BookID {
        val idStream = openUri(uri)
        val stream = openUri(uri)

        // TODO: compute in parallel
        val id = BookID.forEpub(idStream)
        val epub = EpubReader().readEpub(stream)

        val position = MutableStateFlow(BookPosition(id, 0, 0))
        val style = BookStyle()
        positions[id] = position
        store[id] = Book(id, style, position.asStateFlow(), epub)

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