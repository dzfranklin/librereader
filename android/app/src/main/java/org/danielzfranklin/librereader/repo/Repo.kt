package org.danielzfranklin.librereader.repo

import org.danielzfranklin.librereader.LibreReaderApplication
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.danielzfranklin.librereader.model.BookID
import org.danielzfranklin.librereader.model.BookMeta
import org.danielzfranklin.librereader.model.BookPosition
import org.danielzfranklin.librereader.model.BookStyle
import java.util.zip.ZipInputStream

class Repo(private val app: LibreReaderApplication) : CoroutineScope {
    override val coroutineContext = Job()

    private val bookDao = BookDao.create(app)

    fun listBooks(): Flow<List<BookMeta>> = bookDao.list()

    suspend fun updatePosition(position: BookPosition) {
        bookDao.updatePosition(position)
    }

    suspend fun importBook(uri: Uri): BookID {
        val meta = EpubImport(coroutineContext, app, uri).import()
        bookDao.insert(meta)
        return meta.id
    }

    suspend fun getCover(id: BookID): Bitmap? = withContext(Dispatchers.IO) {
        BookFiles.open(app, id)?.coverBitmap()
    }

    fun getBookStyleFlow(id: BookID): Flow<BookStyle?> = bookDao.getBookStyleFlow(id)

    suspend fun getPosition(id: BookID): BookPosition? = bookDao.getPosition(id)

    suspend fun getEpub(id: BookID): Book? = withContext(Dispatchers.IO) {
        val files = BookFiles.open(app, id) ?: return@withContext null

        files.epubFile.inputStream().use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                // False positive, see <https://youtrack.jetbrains.com/issue/KTIJ-830>
                @Suppress("BlockingMethodInNonBlockingContext")
                EpubReader().readEpub(zipStream)
            }
        }
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