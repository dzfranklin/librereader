package org.danielzfranklin.librereader.repo

import android.net.Uri
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.Database
import org.danielzfranklin.librereader.LibreReaderApplication
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle

class Repo(private val app: LibreReaderApplication) : CoroutineScope {
    override val coroutineContext = Job()

    private val dbDriver = AndroidSqliteDriver(Database.Schema, app, DB_FILE)
    private val database = Database(dbDriver)
    private val bookDao = BookDao(app, database)

    fun listBooks(): Flow<List<BookMeta>> = bookDao.list()

    suspend fun updatePosition(position: BookPosition) {
        bookDao.updatePosition(position)
    }

    suspend fun importBook(uri: Uri): BookID {
        val meta = EpubImport(coroutineContext, app, uri).import()
        bookDao.insert(meta)
        return meta.id
    }

    suspend fun getBook(id: BookID): BookMeta = bookDao.get(id)

    fun getBookStyleFlow(id: BookID): Flow<BookStyle> = bookDao.getBookStyleFlow(id)

    /**
     * Assumes the id is valid
     */
    suspend fun getEpub(id: BookID): Book = withContext(Dispatchers.IO) {
        val stream = BookFiles(app, id).epubFile.inputStream()
        // False positive, see <https://youtrack.jetbrains.com/issue/KTIJ-830>
        @Suppress("BlockingMethodInNonBlockingContext")
        EpubReader().readEpub(stream)
    }

    companion object {
        private var instance: Repo? = null

        fun initialize(application: LibreReaderApplication) {
            instance = Repo(application)
        }

        fun get(): Repo {
            return instance ?: throw IllegalStateException("Repo not initialized")
        }

        private const val DB_FILE = "main.db"
    }
}