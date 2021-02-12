package org.danielzfranklin.librereader.repo

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.LibreReaderApplication
import org.danielzfranklin.librereader.model.Book
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.util.atomicUpdate
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class Repo(private val app: LibreReaderApplication) : CoroutineScope {
    override val coroutineContext = Job()

    // TODO: Replace with real store
    private val positions = ConcurrentHashMap<BookID, MutableStateFlow<BookPosition>>()
    private val store = ConcurrentHashMap<BookID, Book>()
    private val metaStore = MutableStateFlow(emptyList<BookMeta>())

    fun listBooks(): StateFlow<List<BookMeta>> {
        return metaStore.asStateFlow()
    }

    fun updatePosition(id: BookID, position: BookPosition) {
        positions[id]!!.value = position
    }

    suspend fun importBook(uri: Uri): BookID {
        val meta = EpubImport(coroutineContext, app, uri).import()

        metaStore.atomicUpdate { it + listOf(meta) }

        val position = MutableStateFlow(meta.position)
        positions[meta.id] = position

        val importedDir = app.getDir(EpubImport.IMPORTED_DIR, Context.MODE_PRIVATE)
        val bookDir = File(importedDir, meta.id.toString())
        val epub = withContext(Dispatchers.IO) {
            async {
                // False positive, see <https://youtrack.jetbrains.com/issue/KTIJ-830>
                @Suppress("BlockingMethodInNonBlockingContext")
                EpubReader().readEpub(File(bookDir, EpubImport.STORED_EPUB_FILE).inputStream())
            }
        }
        store[meta.id] = Book(
            meta.id,
            meta.style,
            position.asStateFlow(),
            epub.await()
        )

        return meta.id
    }

    fun getBook(id: BookID): Book? {
        return store[id]
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